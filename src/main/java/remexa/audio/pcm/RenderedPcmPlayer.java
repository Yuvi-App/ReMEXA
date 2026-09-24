package remexa.audio.pcm;

import remexa.audio.AudioCallbacks;
import remexa.audio.spatial.Audio3DSource;
import remexa.audio.spatial.SpatialAudioStream;
import remexa.host.runtime.MidletRuntime;

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
        this(audio, null);
    }

    public RenderedPcmPlayer(RenderedPcmAudio audio, Audio3DSource spatialSource) {
        if (audio == null) {
            throw new NullPointerException("audio");
        }
        // Keep existing multichannel WAV output usable; Audio3D describes mono/stereo sources.
        if (audio.channelCount() > 2) spatialSource = null;
        handle = sharedEngine(new OutputFormatKey(audio.sampleRate(), spatialSource == null ? audio.channelCount() : 2))
                .open(audio, spatialSource);
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
        private long outputEpoch;
        private long idleCloseDeadlineMs = Long.MAX_VALUE;

        private SharedEngine(OutputFormatKey formatKey) {
            this.formatKey = formatKey;
            int channels = Math.max(1, formatKey.channelCount());
            this.mixBuffer = new float[CHUNK_FRAMES * channels];
            this.sessionBuffer = new float[CHUNK_FRAMES * channels];
            this.pcmBuffer = new byte[CHUNK_FRAMES * channels * 2];
        }

        PlaybackHandle open(RenderedPcmAudio audio, Audio3DSource spatialSource) {
            PlaybackHandle handle = new PlaybackHandle(this, audio, spatialSource);
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
            if (worker != null && worker.isAlive()) {
                return;
            }
            worker = AudioCallbacks.hostThread(this::runLoop,
                    "remexa-pcm-rendered-" + formatKey.sampleRate() + "hz-" + formatKey.channelCount() + "ch");
            worker.setDaemon(true);
            worker.start();
        }

        private void runLoop() {
            try {
                mixLoop();
            } finally {
                synchronized (engineLock) {
                    if (worker == Thread.currentThread()) {
                        worker = null;
                        closeLineLocked();
                    }
                }
            }
        }

        private void mixLoop() {
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

                SourceDataLine targetLine = null;
                long writeEpoch = -1L;
                long writtenBefore;
                long writtenAfter;
                long playedFrames;
                try {
                    synchronized (engineLock) {
                        // A retired snapshot must not reopen the device after app shutdown.
                        if (mixedFrames > 0) {
                            if (snapshot.stream().noneMatch(PlaybackHandle::hasWork)) {
                                continue;
                            }
                            ensureLineLocked();
                        }
                        targetLine = line;
                        writeEpoch = outputEpoch;
                        writtenBefore = writtenFrames;
                    }
                    // Device writes may wait for buffer space. Player control and close
                    // must remain able to acquire engineLock while that happens.
                    int written = mixedFrames > 0 ? targetLine.write(pcmBuffer, 0, encodePcm(mixedFrames)) : 0;
                    synchronized (engineLock) {
                        if (line != targetLine || outputEpoch != writeEpoch) {
                            continue;
                        }
                        writtenFrames += written / (formatKey.channelCount() * 2);
                        writtenAfter = writtenFrames;
                        playedFrames = targetLine == null ? writtenAfter : targetLine.getLongFramePosition();
                    }
                } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException exception) {
                    synchronized (engineLock) {
                        if (targetLine != null && (line != targetLine || outputEpoch != writeEpoch)) {
                            continue;
                        }
                        closeLineLocked();
                    }
                    for (PlaybackHandle handle : snapshot) {
                        handle.failPlayback();
                    }
                    continue;
                }

                for (PlaybackHandle handle : snapshot) {
                    handle.bindCompletionTarget(writtenBefore, writtenAfter);
                    handle.dispatchReadyCompletion(playedFrames, notifications);
                }
                notifications.forEach(Runnable::run);
                if (mixedFrames == 0) {
                    // Only completion remains: let the device drain without spinning
                    // on its frame position and contending with the game's controls.
                    synchronized (engineLock) {
                        try {
                            engineLock.wait(5L);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
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
            outputEpoch++;
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, LINE_BUFFER_FRAMES * format.getFrameSize());
            line.start();
            writtenFrames = 0L;
        }

        private void closeLineLocked() {
            if (line == null) {
                return;
            }
            outputEpoch++;
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
        private final ClassLoader ownerClassLoader = MidletRuntime.currentAppClassLoader();
        private final SharedEngine engine;
        private final RenderedPcmAudio audio;
        private final SpatialAudioStream spatial;
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

        private PlaybackHandle(SharedEngine engine, RenderedPcmAudio audio, Audio3DSource spatialSource) {
            this.engine = engine;
            this.audio = audio;
            spatial = spatialSource == null ? null : new SpatialAudioStream(audio.sampleRate(), audio.channelCount(), spatialSource);
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
                playbackEpoch++;
                this.completionListener = closed ? null : listener;
            }
        }

        void setVolume(int value) {
            synchronized (stateLock) {
                volume = Math.max(0, Math.min(127, value));
            }
        }

        void play(int loopCount) {
            synchronized (stateLock) {
                ensureOpenLocked();
                playbackEpoch++;
                clearCompletionStateLocked();
                framePosition = 0;
                remainingLoops = loopCount == 0 ? -1 : Math.max(0, loopCount - 1);
                if (spatial != null) spatial.reset();
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
                ensureOpenLocked();
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
                completionListener = null;
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
            if (spatial != null) return renderSpatial(output, maxFrames);
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

        private int renderSpatial(float[] output, int maxFrames) {
            synchronized (stateLock) {
                if (closed || !playing) return 0;
                int frames;
                try {
                    frames = spatial.render(output, maxFrames, this::readSpatialInput);
                } catch (Exception exception) {
                    failPlayback();
                    return 0;
                }
                float gain = volume / 127.0f;
                // Volume/mute follows the effects, including their accumulated tails.
                for (int i = 0; i < frames * 2; i++) output[i] *= gain;
                if (frames == 0) {
                    paused = playing = false;
                    framePosition = 0;
                    armCompletionLocked(false);
                }
                return frames;
            }
        }

        private int readSpatialInput(float[] output, int maxFrames) {
            int available = audio.frameCount() - framePosition;
            if (available <= 0) {
                if (audio.frameCount() == 0 || !advanceLoopLocked()) return 0;
                available = audio.frameCount();
            }
            int frames = Math.min(maxFrames, available);
            mixIntoBuffer(audio.pcm16Le(), framePosition, frames, audio.channelCount(), 1, output);
            framePosition += frames;
            return frames;
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
            long epoch;
            synchronized (stateLock) {
                if (!completionPending || completionTargetFrame < 0L || playedFrames < completionTargetFrame) {
                    return;
                }
                clearCompletionStateLocked();
                epoch = playbackEpoch;
            }
            notifications.add(() -> dispatchCompletion(epoch));
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

        private void ensureOpenLocked() {
            if (closed || !MidletRuntime.isAppActive(ownerClassLoader)) {
                throw new IllegalStateException("Audio playback belongs to a closed player or appli");
            }
        }

        private void dispatchCompletion(long epoch) {
            AudioCallbacks.dispatch(ownerClassLoader, "remexa-pcm-callback", () -> {
                Runnable currentListener;
                synchronized (stateLock) {
                    if (closed || playbackEpoch != epoch) {
                        return;
                    }
                    currentListener = completionListener;
                }
                if (currentListener != null) {
                    currentListener.run();
                }
            });
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
