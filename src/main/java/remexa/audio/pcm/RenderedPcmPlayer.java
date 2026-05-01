package remexa.audio.pcm;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderedPcmPlayer implements AutoCloseable {
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 5;

    private static final int CHUNK_FRAMES = 512;
    private static final int LINE_BUFFER_FRAMES = CHUNK_FRAMES * 4;
    private static final long IDLE_CLOSE_MILLIS = 1_500L;
    private static final Object ENGINE_REGISTRY_LOCK = new Object();
    private static final Map<OutputFormatKey, SharedEngine> ENGINES = new HashMap<>();

    private final PlaybackHandle handle;

    public RenderedPcmPlayer(RenderedPcmAudio audio) {
        if (audio == null) {
            throw new NullPointerException("audio");
        }
        handle = sharedEngine(new OutputFormatKey(audio.sampleRate(), audio.channelCount())).open(audio);
    }

    public static void prewarm(int sampleRate, int channelCount) {
        sharedEngine(new OutputFormatKey(sampleRate, channelCount)).prewarm();
    }

    public int getState() {
        return handle.getState();
    }

    public void setCompletionListener(Runnable listener) {
        handle.setCompletionListener(listener);
    }

    public void setVolume(int value) {
        handle.setVolume(value);
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
            int channels = Math.max(1, formatKey.channelCount());
            this.mixBuffer = new float[CHUNK_FRAMES * channels];
            this.sessionBuffer = new float[CHUNK_FRAMES * channels];
            this.pcmBuffer = new byte[CHUNK_FRAMES * channels * 2];
        }

        PlaybackHandle open(RenderedPcmAudio audio) {
            PlaybackHandle handle = new PlaybackHandle(this, audio);
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
                    "remexa-pcm-rendered-" + formatKey.sampleRate() + "hz-" + formatKey.channelCount() + "ch");
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
                    int frames = handle.renderInto(sessionBuffer, CHUNK_FRAMES);
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
        private final RenderedPcmAudio audio;
        private final Object stateLock = new Object();

        private Runnable completionListener;
        private boolean closed;
        private boolean playing;
        private boolean paused;
        private int framePosition;
        private int remainingLoops;
        private int volume = 127;
        private long playbackEpoch;
        private boolean completionPending;
        private boolean completionNeedsCurrentWrite;
        private long completionTargetFrame = -1L;

        private PlaybackHandle(SharedEngine engine, RenderedPcmAudio audio) {
            this.engine = engine;
            this.audio = audio;
        }

        int getState() {
            synchronized (stateLock) {
                if (paused) {
                    return PAUSED;
                }
                return playing ? PLAYING : READY;
            }
        }

        void setCompletionListener(Runnable listener) {
            synchronized (stateLock) {
                this.completionListener = listener;
            }
        }

        void setVolume(int value) {
            synchronized (stateLock) {
                volume = Math.max(0, Math.min(127, value));
            }
        }

        void play(int loopCount) {
            synchronized (stateLock) {
                playbackEpoch++;
                clearCompletionStateLocked();
                framePosition = 0;
                remainingLoops = loopCount == 0 ? -1 : Math.max(0, loopCount - 1);
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
            }
        }

        int renderInto(float[] output, int maxFrames) {
            int framesToWrite;
            int startFrame;
            int channelCount = audio.channelCount();
            float gain;
            long chunkEpoch;
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
                        armCompletionLocked(false);
                        return 0;
                    }
                    available = audio.frameCount() - framePosition;
                }
                framesToWrite = Math.min(maxFrames, available);
                startFrame = framePosition;
                gain = volume / 127.0f;
                framePosition += framesToWrite;
                chunkEpoch = playbackEpoch;
                if (framePosition >= audio.frameCount() && !advanceLoopLocked()) {
                    paused = false;
                    playing = false;
                    framePosition = 0;
                    armCompletionLocked(true);
                }
            }

            mixIntoBuffer(audio.pcm16Le(), startFrame, framesToWrite, channelCount, gain, output);
            synchronized (stateLock) {
                return closed || playbackEpoch != chunkEpoch ? 0 : framesToWrite;
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

        private boolean advanceLoopLocked() {
            if (remainingLoops == -1 || remainingLoops > 0) {
                if (remainingLoops > 0) {
                    remainingLoops--;
                }
                framePosition = 0;
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

        private void dispatchCompletion() {
            Runnable listener;
            synchronized (stateLock) {
                listener = completionListener;
            }
            if (listener == null) {
                return;
            }
            Thread callbackThread = new Thread(listener, "remexa-pcm-callback");
            callbackThread.setDaemon(true);
            callbackThread.start();
        }

        private static void mixIntoBuffer(byte[] pcm,
                                          int startFrame,
                                          int frames,
                                          int channelCount,
                                          float gain,
                                          float[] output) {
            int inputOffset = startFrame * channelCount * 2;
            int outputOffset = 0;
            for (int frame = 0; frame < frames; frame++) {
                for (int channel = 0; channel < channelCount; channel++) {
                    output[outputOffset++] = (readSample(pcm, inputOffset) / 32768.0f) * gain;
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
