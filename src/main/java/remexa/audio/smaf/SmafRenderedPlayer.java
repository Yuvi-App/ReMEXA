package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhraseTrackListener;

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

public final class SmafRenderedPlayer implements SmafAudioPlayer {
    private static final int CHUNK_FRAMES = 512;
    private static final int LINE_BUFFER_FRAMES = CHUNK_FRAMES * 4;
    private static final long IDLE_CLOSE_MILLIS = 1_500L;
    private static final Object ENGINE_REGISTRY_LOCK = new Object();
    private static final Map<OutputFormatKey, SharedEngine> ENGINES = new HashMap<>();

    private final PlaybackHandle handle;

    public SmafRenderedPlayer(SmafRenderedAudio audio) {
        this(audio, List.of());
    }

    public SmafRenderedPlayer(SmafRenderedAudio audio, List<SMAFDecoder.SequenceUserEvent> userEvents) {
        handle = sharedEngine(new OutputFormatKey(audio.sampleRate(), audio.channelCount())).open(audio, userEvents);
    }

    public static void prewarm(int sampleRate, int channelCount) {
        sharedEngine(new OutputFormatKey(sampleRate, channelCount)).prewarm();
    }

    public int getState() {
        return handle.getState();
    }

    public void setListener(PhraseTrackListener listener) {
        handle.setListener(listener);
    }

    public void setVolume(int value) {
        handle.setVolume(value);
    }

    public void setPanpot(int value) {
        handle.setPanpot(value);
    }

    public void play(int loopCount) {
        handle.play(loopCount);
    }

    public void stop() {
        handle.stop();
    }

    public void pause() {
        handle.pause();
    }

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
        private final float[] mixBuffer;
        private final float[] sessionBuffer;
        private final byte[] pcmBuffer;

        private Thread worker;
        private SourceDataLine line;
        private long writtenFrames;
        private long idleCloseDeadlineMs = Long.MAX_VALUE;

        private SharedEngine(OutputFormatKey formatKey) {
            this.formatKey = formatKey;
            this.mixBuffer = new float[CHUNK_FRAMES * Math.max(1, formatKey.channelCount())];
            this.sessionBuffer = new float[CHUNK_FRAMES * Math.max(1, formatKey.channelCount())];
            this.pcmBuffer = new byte[CHUNK_FRAMES * Math.max(1, formatKey.channelCount()) * 2];
        }

        PlaybackHandle open(SmafRenderedAudio audio, List<SMAFDecoder.SequenceUserEvent> userEvents) {
            PlaybackHandle handle = new PlaybackHandle(this, audio, userEvents);
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
                } catch (LineUnavailableException | IllegalArgumentException ignored) {
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
                    "remexa-smaf-rendered-" + formatKey.sampleRate() + "hz-" + formatKey.channelCount() + "ch");
            worker.setDaemon(true);
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

                long writtenBefore = writtenFrames;
                int mixedFrames = 0;
                List<Runnable> notifications = new ArrayList<>();
                for (PlaybackHandle handle : snapshot) {
                    Arrays.fill(sessionBuffer, 0.0f);
                    int frames = handle.renderInto(sessionBuffer, CHUNK_FRAMES, notifications);
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
                        for (PlaybackHandle handle : snapshot) {
                            handle.failPlayback();
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
                if (handles.get(i).isClosed()) {
                    handles.remove(i--);
                }
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
            line.open(format, LINE_BUFFER_FRAMES * format.getFrameSize());
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
    }

    private static final class PlaybackHandle {
        private final SharedEngine engine;
        private final SmafRenderedAudio audio;
        private final List<SMAFDecoder.SequenceUserEvent> userEvents;
        private final Object stateLock = new Object();

        private PhraseTrackListener listener;
        private boolean closed;
        private boolean playing;
        private boolean paused;
        private int framePosition;
        private int remainingLoops;
        private int nextUserEventIndex;
        private int volume = 127;
        private int panpot = 64;
        private long playbackEpoch;
        private boolean completionPending;
        private boolean completionNeedsCurrentWrite;
        private long completionTargetFrame = -1L;

        private PlaybackHandle(SharedEngine engine,
                               SmafRenderedAudio audio,
                               List<SMAFDecoder.SequenceUserEvent> userEvents) {
            this.engine = engine;
            this.audio = audio;
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
                playbackEpoch++;
                clearCompletionStateLocked();
                framePosition = 0;
                remainingLoops = loopCount == 0 ? -1 : Math.max(0, loopCount - 1);
                nextUserEventIndex = 0;
                paused = false;
                playing = true;
            }
            engine.wake();
        }

        void stop() {
            synchronized (stateLock) {
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = false;
                framePosition = 0;
                remainingLoops = 0;
                nextUserEventIndex = 0;
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
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = true;
            }
            engine.wake();
        }

        void close() {
            synchronized (stateLock) {
                playbackEpoch++;
                clearCompletionStateLocked();
                closed = true;
                paused = false;
                playing = false;
                framePosition = 0;
                remainingLoops = 0;
                nextUserEventIndex = 0;
            }
            engine.closeHandle(this);
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

        void failPlayback() {
            synchronized (stateLock) {
                playbackEpoch++;
                clearCompletionStateLocked();
                paused = false;
                playing = false;
                framePosition = 0;
                remainingLoops = 0;
                nextUserEventIndex = 0;
            }
        }

        int renderInto(float[] output, int maxFrames, List<Runnable> notifications) {
            int framesToWrite;
            int startFrame;
            int channelCount = audio.channelCount();
            float[] channelGains;
            long chunkEpoch;
            List<Integer> pendingUserEvents;
            synchronized (stateLock) {
                if (closed || !playing) {
                    return 0;
                }
                int available = audio.frameCount() - framePosition;
                if (available <= 0) {
                    if (!advanceLoopLocked()) {
                        paused = false;
                        playing = false;
                        framePosition = 0;
                        nextUserEventIndex = 0;
                        armCompletionLocked(false);
                        return 0;
                    }
                    available = audio.frameCount() - framePosition;
                }
                framesToWrite = Math.min(maxFrames, available);
                startFrame = framePosition;
                channelGains = channelGainsLocked(channelCount);
                framePosition += framesToWrite;
                chunkEpoch = playbackEpoch;
                pendingUserEvents = consumeUserEventsLocked(startFrame, framesToWrite);
                if (framePosition >= audio.frameCount() && !advanceLoopLocked()) {
                    paused = false;
                    playing = false;
                    framePosition = 0;
                    nextUserEventIndex = 0;
                    armCompletionLocked(true);
                }
            }

            mixIntoBuffer(audio.pcm16Le(), startFrame, framesToWrite, channelCount, channelGains, output);
            synchronized (stateLock) {
                if (closed || playbackEpoch != chunkEpoch) {
                    return 0;
                }
            }
            for (int userEventId : pendingUserEvents) {
                notifications.add(() -> dispatchUserEvent(userEventId));
            }
            return framesToWrite;
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

        private boolean advanceLoopLocked() {
            if (remainingLoops == -1 || remainingLoops > 0) {
                if (remainingLoops > 0) {
                    remainingLoops--;
                }
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

        private List<Integer> consumeUserEventsLocked(int startFrame, int frames) {
            if (userEvents.isEmpty() || frames <= 0) {
                return List.of();
            }
            long endFrameExclusive = (long) startFrame + frames;
            List<Integer> pending = new ArrayList<>();
            while (nextUserEventIndex < userEvents.size()) {
                SMAFDecoder.SequenceUserEvent userEvent = userEvents.get(nextUserEventIndex);
                long eventFrame = Math.round(userEvent.tick() * (audio.sampleRate() / 1000.0));
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

        private static void mixIntoBuffer(byte[] pcm,
                                          int startFrame,
                                          int frames,
                                          int channelCount,
                                          float[] channelGains,
                                          float[] output) {
            int inputOffset = startFrame * channelCount * 2;
            int outputOffset = 0;
            for (int frame = 0; frame < frames; frame++) {
                for (int channel = 0; channel < channelCount; channel++) {
                    output[outputOffset++] = (readSample(pcm, inputOffset) / 32768.0f) * channelGains[channel];
                    inputOffset += 2;
                }
            }
        }

        private static short readSample(byte[] input, int offset) {
            int low = input[offset] & 0xFF;
            int high = input[offset + 1];
            return (short) ((high << 8) | low);
        }
    }
}
