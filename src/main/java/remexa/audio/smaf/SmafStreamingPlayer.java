package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhraseTrackListener;
import org.recompile.mobile.Mobile;

import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SmafStreamingPlayer implements SmafAudioPlayer {
    private static final int TARGET_CHUNK_MILLIS = 20;
    private static final int TARGET_LINE_BUFFER_MILLIS = 96;
    private static final int MIN_CHUNK_FRAMES = 512;
    private static final int MIN_BUFFER_CHUNKS = 4;
    private static final long IDLE_CLOSE_MILLIS = 3_000L;
    private static final Object ENGINE_REGISTRY_LOCK = new Object();
    private static final Map<OutputFormatKey, SharedEngine> ENGINES = new HashMap<>();

    private final PlaybackHandle handle;

    SmafStreamingPlayer(SmafStreamingSession session, List<SMAFDecoder.SequenceUserEvent> userEvents) {
        SharedEngine engine = sharedEngine(new OutputFormatKey(session.sampleRate(), session.channelCount()));
        handle = engine.open(session, userEvents);
        engine.prewarm();
    }

    @Override
    public int getState() {
        return handle.getState();
    }

    @Override
    public void setListener(PhraseTrackListener listener) {
        handle.setListener(listener);
    }

    @Override
    public void setVolume(int value) {
        handle.setVolume(value);
    }

    @Override
    public void setPanpot(int value) {
        handle.setPanpot(value);
    }

    @Override
    public void play(int loopCount) {
        handle.play(loopCount);
    }

    @Override
    public void stop() {
        handle.stop();
    }

    @Override
    public void pause() {
        handle.pause();
    }

    @Override
    public void resume() {
        handle.resume();
    }

    @Override
    public void close() {
        handle.close();
    }

    private static SharedEngine sharedEngine(OutputFormatKey key) {
        synchronized (ENGINE_REGISTRY_LOCK) {
            return ENGINES.computeIfAbsent(key, SharedEngine::new);
        }
    }

    private record OutputFormatKey(int sampleRate, int channelCount) {
    }

    private static final class SharedEngine {
        private final OutputFormatKey formatKey;
        private final Object engineLock = new Object();
        private final List<PlaybackHandle> handles = new ArrayList<>();
        private final int chunkFrames;
        private final int lineBufferFrames;
        private final float[] mixBuffer;
        private final float[] sessionBuffer;
        private final byte[] pcmBuffer;

        private Thread worker;
        private SourceDataLine line;
        private long writtenFrames;
        private long idleCloseDeadlineMs = Long.MAX_VALUE;

        private SharedEngine(OutputFormatKey formatKey) {
            this.formatKey = formatKey;
            this.chunkFrames = Math.max(MIN_CHUNK_FRAMES, framesForMillis(formatKey.sampleRate(), TARGET_CHUNK_MILLIS));
            this.lineBufferFrames = Math.max(
                    chunkFrames * MIN_BUFFER_CHUNKS,
                    framesForMillis(formatKey.sampleRate(), TARGET_LINE_BUFFER_MILLIS));
            int channels = Math.max(1, formatKey.channelCount());
            this.mixBuffer = new float[chunkFrames * channels];
            this.sessionBuffer = new float[chunkFrames * channels];
            this.pcmBuffer = new byte[chunkFrames * channels * 2];
        }

        PlaybackHandle open(SmafStreamingSession session, List<SMAFDecoder.SequenceUserEvent> userEvents) {
            PlaybackHandle handle = new PlaybackHandle(this, session, userEvents);
            synchronized (engineLock) {
                handles.add(handle);
                idleCloseDeadlineMs = Long.MAX_VALUE;
            }
            return handle;
        }

        void prewarm() {
            synchronized (engineLock) {
                idleCloseDeadlineMs = System.currentTimeMillis() + IDLE_CLOSE_MILLIS;
                try {
                    ensureLineLocked();
                } catch (LineUnavailableException | IllegalArgumentException exception) {
                    return;
                }
                ensureWorkerLocked();
                engineLock.notifyAll();
            }
        }

        void wake() {
            synchronized (engineLock) {
                idleCloseDeadlineMs = Long.MAX_VALUE;
                ensureWorkerLocked();
                engineLock.notifyAll();
            }
        }

        private void ensureWorkerLocked() {
            if (worker != null) {
                return;
            }
            worker = new Thread(this::runLoop,
                    "remexa-smaf-stream-" + formatKey.sampleRate() + "hz-" + formatKey.channelCount() + "ch");
            worker.setDaemon(true);
            worker.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 2));
            worker.start();
        }

        private void runLoop() {
            while (true) {
                List<PlaybackHandle> snapshot;
                synchronized (engineLock) {
                    while (true) {
                        pruneClosedHandlesLocked();
                        if (hasRunnableHandleLocked()) {
                            idleCloseDeadlineMs = Long.MAX_VALUE;
                            snapshot = new ArrayList<>(handles);
                            break;
                        }
                        long waitMillis = idleWaitMillisLocked();
                        if (waitMillis < 0L) {
                            worker = null;
                            return;
                        }
                        try {
                            engineLock.wait(waitMillis);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            worker = null;
                            return;
                        }
                    }
                }

                Arrays.fill(mixBuffer, 0.0f);
                long writtenBefore = writtenFrames;
                int mixedFrames = 0;
                List<Runnable> notifications = new ArrayList<>();
                for (PlaybackHandle handle : snapshot) {
                    Arrays.fill(sessionBuffer, 0.0f);
                    int frames;
                    try {
                        frames = handle.renderInto(sessionBuffer, chunkFrames, notifications);
                    } catch (RuntimeException exception) {
                        handle.failPlayback(exception);
                        continue;
                    } catch (Exception exception) {
                        handle.failPlayback(new RuntimeException("Failed during streamed SMAF playback", exception));
                        continue;
                    }
                    if (frames <= 0) {
                        continue;
                    }
                    mixedFrames = Math.max(mixedFrames, frames);
                    int samples = frames * formatKey.channelCount();
                    for (int i = 0; i < samples; i++) {
                        mixBuffer[i] += sessionBuffer[i];
                    }
                }

                if (mixedFrames > 0) {
                    try {
                        synchronized (engineLock) {
                            ensureLineLocked();
                        }
                        SourceDataLine targetLine;
                        synchronized (engineLock) {
                            targetLine = line;
                        }
                        if (targetLine == null) {
                            continue;
                        }
                        int length = encodePcm(mixedFrames);
                        targetLine.write(pcmBuffer, 0, length);
                        writtenFrames += mixedFrames;
                    } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException exception) {
                        synchronized (engineLock) {
                            closeLineLocked();
                        }
                        RuntimeException failure =
                                new RuntimeException("Failed to write streamed SMAF output", exception);
                        for (PlaybackHandle handle : snapshot) {
                            handle.failPlayback(failure);
                        }
                    }
                }

                long playedFrames;
                synchronized (engineLock) {
                    playedFrames = line == null ? writtenFrames : line.getLongFramePosition();
                }
                for (PlaybackHandle handle : snapshot) {
                    handle.bindCompletionTarget(writtenBefore, writtenFrames);
                    handle.dispatchReadyCompletion(playedFrames, notifications);
                }
                notifications.forEach(Runnable::run);
            }
        }

        private boolean hasRunnableHandleLocked() {
            for (PlaybackHandle handle : handles) {
                if (handle.hasWork()) {
                    return true;
                }
            }
            return false;
        }

        private void pruneClosedHandlesLocked() {
            for (int i = 0; i < handles.size(); i++) {
                PlaybackHandle handle = handles.get(i);
                if (!handle.isClosed()) {
                    continue;
                }
                handles.remove(i--);
                handle.releaseResources();
            }
        }

        private void closeHandle(PlaybackHandle handle) {
            synchronized (engineLock) {
                pruneClosedHandlesLocked();
                if (!handles.contains(handle) && !hasRunnableHandleLocked()) {
                    closeLineLocked();
                }
                engineLock.notifyAll();
            }
        }

        private long idleWaitMillisLocked() {
            if (line == null) {
                return -1L;
            }
            long now = System.currentTimeMillis();
            if (idleCloseDeadlineMs == Long.MAX_VALUE) {
                idleCloseDeadlineMs = now + IDLE_CLOSE_MILLIS;
            }
            long remaining = idleCloseDeadlineMs - now;
            if (remaining > 0L) {
                return remaining;
            }
            closeLineLocked();
            return -1L;
        }

        private void ensureLineLocked() throws LineUnavailableException {
            if (line != null) {
                if (!line.isRunning()) {
                    line.start();
                }
                return;
            }
            AudioFormat format = new AudioFormat(
                    formatKey.sampleRate(),
                    16,
                    formatKey.channelCount(),
                    true,
                    false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, lineBufferFrames * format.getFrameSize());
            line.start();
            writtenFrames = 0L;
        }

        private void closeLineLocked() {
            if (line == null) {
                return;
            }
            line.stop();
            line.flush();
            line.close();
            line = null;
            writtenFrames = 0L;
            idleCloseDeadlineMs = Long.MAX_VALUE;
        }

        private int encodePcm(int frames) {
            int output = 0;
            int samples = frames * formatKey.channelCount();
            for (int i = 0; i < samples; i++) {
                float sample = Math.max(-1.0f, Math.min(1.0f, mixBuffer[i]));
                int value = Math.round(sample * 32767.0f);
                pcmBuffer[output++] = (byte) (value & 0xFF);
                pcmBuffer[output++] = (byte) ((value >>> 8) & 0xFF);
                mixBuffer[i] = 0.0f;
            }
            return output;
        }

        private static int framesForMillis(int sampleRate, int millis) {
            return Math.max(1, (int) Math.ceil(sampleRate * (millis / 1000.0)));
        }
    }

    private static final class PlaybackHandle {
        private final SharedEngine engine;
        private final SmafStreamingSession session;
        private final List<SMAFDecoder.SequenceUserEvent> userEvents;
        private final Object stateLock = new Object();

        private PhraseTrackListener listener;
        private boolean closed;
        private boolean resourcesReleased;
        private boolean playing;
        private boolean paused;
        private int remainingLoops;
        private int nextUserEventIndex;
        private int framePosition;
        private int volume = 127;
        private int panpot = 64;
        private long playbackEpoch;
        private boolean completionPending;
        private boolean completionNeedsCurrentWrite;
        private long completionTargetFrame = -1L;
        private RuntimeException playbackFailure;

        private PlaybackHandle(SharedEngine engine,
                               SmafStreamingSession session,
                               List<SMAFDecoder.SequenceUserEvent> userEvents) {
            this.engine = engine;
            this.session = session;
            this.userEvents = userEvents == null ? List.of() : List.copyOf(userEvents);
        }

        int getState() {
            synchronized (stateLock) {
                if (paused) {
                    return SmafPlayback.PAUSED;
                }
                return playing ? SmafPlayback.PLAYING : SmafPlayback.READY;
            }
        }

        void setListener(PhraseTrackListener listener) {
            synchronized (stateLock) {
                this.listener = listener;
            }
        }

        void setVolume(int value) {
            synchronized (stateLock) {
                volume = Math.max(0, Math.min(127, value));
            }
        }

        void setPanpot(int value) {
            synchronized (stateLock) {
                panpot = Math.max(0, Math.min(127, value));
            }
        }

        void play(int loopCount) {
            synchronized (stateLock) {
                throwIfFailedLocked();
                try {
                    session.rewind();
                } catch (Exception exception) {
                    throw new RuntimeException("Failed to prepare streamed SMAF playback", exception);
                }
                playbackEpoch++;
                clearCompletionStateLocked();
                framePosition = 0;
                nextUserEventIndex = 0;
                remainingLoops = loopCount == 0 ? -1 : Math.max(0, loopCount - 1);
                paused = false;
                playing = true;
            }
            engine.wake();
        }

        void stop() {
            synchronized (stateLock) {
                if (closed) {
                    return;
                }
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = false;
                framePosition = 0;
                nextUserEventIndex = 0;
                remainingLoops = 0;
            }
            engine.wake();
        }

        void pause() {
            synchronized (stateLock) {
                if (!playing) {
                    return;
                }
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = true;
                playing = false;
            }
            engine.wake();
        }

        void resume() {
            synchronized (stateLock) {
                if (!paused) {
                    return;
                }
                throwIfFailedLocked();
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = true;
            }
            engine.wake();
        }

        @SuppressWarnings("RedundantThrows")
        int renderInto(float[] output, int maxFrames, List<Runnable> notifications) throws Exception {
            while (true) {
                long epoch;
                int startFrame;
                int channelCount = session.channelCount();
                float[] channelGains;
                synchronized (stateLock) {
                    if (closed || !playing) {
                        return 0;
                    }
                    epoch = playbackEpoch;
                    startFrame = framePosition;
                    channelGains = channelGainsLocked(channelCount);
                }

                int frames = session.render(output, maxFrames);
                if (frames <= 0) {
                    synchronized (stateLock) {
                        if (closed || epoch != playbackEpoch) {
                            return 0;
                        }
                        if (advanceLoopLocked()) {
                            continue;
                        }
                        paused = false;
                        playing = false;
                        framePosition = 0;
                        nextUserEventIndex = 0;
                        armCompletionLocked(false);
                    }
                    return 0;
                }

                List<Integer> pendingUserEvents;
                synchronized (stateLock) {
                    if (closed || epoch != playbackEpoch) {
                        return 0;
                    }
                    pendingUserEvents = consumeUserEventsLocked(startFrame, frames, session.sampleRate());
                    framePosition = startFrame + frames;
                }
                applyChannelGains(output, frames, channelCount, channelGains);
                for (int eventId : pendingUserEvents) {
                    notifications.add(() -> dispatchUserEvent(eventId));
                }
                return frames;
            }
        }

        void bindCompletionTarget(long writtenBefore, long writtenAfter) {
            synchronized (stateLock) {
                if (!completionPending || completionTargetFrame >= 0L) {
                    return;
                }
                completionTargetFrame = completionNeedsCurrentWrite ? writtenAfter : writtenBefore;
                completionNeedsCurrentWrite = false;
            }
        }

        void dispatchReadyCompletion(long playedFrames, List<Runnable> notifications) {
            synchronized (stateLock) {
                if (!completionPending || completionTargetFrame < 0L || playedFrames < completionTargetFrame) {
                    return;
                }
                clearCompletionStateLocked();
            }
            notifications.add(this::dispatchCompletion);
        }

        boolean hasWork() {
            synchronized (stateLock) {
                return !closed && (playing || completionPending);
            }
        }

        boolean isClosed() {
            synchronized (stateLock) {
                return closed;
            }
        }

        void close() {
            synchronized (stateLock) {
                if (closed) {
                    return;
                }
                playbackEpoch++;
                clearCompletionStateLocked();
                closed = true;
                paused = false;
                playing = false;
                framePosition = 0;
                nextUserEventIndex = 0;
                remainingLoops = 0;
            }
            engine.closeHandle(this);
        }

        void releaseResources() {
            synchronized (stateLock) {
                if (resourcesReleased) {
                    return;
                }
                resourcesReleased = true;
            }
            try {
                session.close();
            } catch (Exception exception) {
                Mobile.log(Mobile.LOG_DEBUG, "Unable to close streamed SMAF session: " + exception.getMessage());
            }
        }

        void failPlayback(RuntimeException exception) {
            synchronized (stateLock) {
                playbackFailure = exception;
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = false;
                framePosition = 0;
                nextUserEventIndex = 0;
                remainingLoops = 0;
            }
            Mobile.log(Mobile.LOG_WARNING, "Streamed SMAF playback failed: " + exception.getMessage());
        }

        private boolean advanceLoopLocked() throws Exception {
            if (remainingLoops == -1 || remainingLoops > 0) {
                if (remainingLoops > 0) {
                    remainingLoops--;
                }
                session.rewind();
                framePosition = 0;
                nextUserEventIndex = 0;
                return true;
            }
            return false;
        }

        private void armCompletionLocked(boolean needsCurrentWrite) {
            completionPending = true;
            completionNeedsCurrentWrite = needsCurrentWrite;
            completionTargetFrame = -1L;
        }

        private void clearCompletionStateLocked() {
            completionPending = false;
            completionNeedsCurrentWrite = false;
            completionTargetFrame = -1L;
        }

        private List<Integer> consumeUserEventsLocked(int startFrame, int frames, int sampleRate) {
            if (userEvents.isEmpty() || frames <= 0) {
                return List.of();
            }
            long endFrameExclusive = (long) startFrame + frames;
            List<Integer> pending = new ArrayList<>();
            while (nextUserEventIndex < userEvents.size()) {
                SMAFDecoder.SequenceUserEvent userEvent = userEvents.get(nextUserEventIndex);
                long eventFrame = Math.round(userEvent.tick() * (sampleRate / 1000.0));
                if (eventFrame < startFrame) {
                    nextUserEventIndex++;
                    continue;
                }
                if (eventFrame >= endFrameExclusive) {
                    break;
                }
                pending.add(userEvent.eventId());
                nextUserEventIndex++;
            }
            return pending.isEmpty() ? List.of() : pending;
        }

        private float[] channelGainsLocked(int channelCount) {
            float gain = volume / 127.0f;
            if (channelCount <= 1) {
                return new float[]{gain};
            }
            float pan = Math.max(-1.0f, Math.min(1.0f, (panpot - 64.0f) / 63.0f));
            float leftGain = gain * (pan > 0.0f ? 1.0f - pan : 1.0f);
            float rightGain = gain * (pan < 0.0f ? 1.0f + pan : 1.0f);
            float[] gains = new float[channelCount];
            gains[0] = leftGain;
            gains[1] = rightGain;
            for (int channel = 2; channel < channelCount; channel++) {
                gains[channel] = gain;
            }
            return gains;
        }

        private void dispatchCompletion() {
            PhraseTrackListener currentListener;
            synchronized (stateLock) {
                currentListener = listener;
            }
            if (currentListener == null) {
                return;
            }
            Thread callbackThread = new Thread(() -> currentListener.eventOccurred(-1), "remexa-smaf-callback");
            callbackThread.setDaemon(true);
            callbackThread.start();
        }

        private void dispatchUserEvent(int eventId) {
            PhraseTrackListener currentListener;
            synchronized (stateLock) {
                currentListener = listener;
            }
            if (currentListener == null) {
                return;
            }
            currentListener.eventOccurred(eventId);
        }

        private void throwIfFailedLocked() {
            if (playbackFailure != null) {
                throw playbackFailure;
            }
        }

        private static void applyChannelGains(float[] output,
                                              int frames,
                                              int channelCount,
                                              float[] channelGains) {
            int samples = frames * channelCount;
            for (int i = 0; i < samples; i++) {
                output[i] *= channelGains[i % channelCount];
            }
        }
    }
}
