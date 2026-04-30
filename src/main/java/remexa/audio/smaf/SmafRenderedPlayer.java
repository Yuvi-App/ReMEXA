package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhraseTrackListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.microedition.media.decoders.SMAFDecoder;
import java.util.ArrayList;
import java.util.List;

public final class SmafRenderedPlayer implements AutoCloseable {
    private static final int CHUNK_FRAMES = 1024;

    private final SmafRenderedAudio audio;
    private final List<SMAFDecoder.SequenceUserEvent> userEvents;
    private final Object lock = new Object();

    private Thread worker;
    private SourceDataLine line;
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

    public SmafRenderedPlayer(SmafRenderedAudio audio) {
        this(audio, List.of());
    }

    public SmafRenderedPlayer(SmafRenderedAudio audio, List<SMAFDecoder.SequenceUserEvent> userEvents) {
        this.audio = audio;
        this.userEvents = userEvents == null ? List.of() : List.copyOf(userEvents);
    }

    public int getState() {
        synchronized (lock) {
            if (paused) {
                return SmafPlayback.PAUSED;
            }
            return playing ? SmafPlayback.PLAYING : SmafPlayback.READY;
        }
    }

    public void setListener(PhraseTrackListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    public void setVolume(int value) {
        synchronized (lock) {
            volume = Math.max(0, Math.min(127, value));
        }
    }

    public void setPanpot(int value) {
        synchronized (lock) {
            panpot = Math.max(0, Math.min(127, value));
        }
    }

    public void play(int loopCount) {
        synchronized (lock) {
            playbackEpoch++;
            framePosition = 0;
            remainingLoops = loopCount == 0 ? -1 : Math.max(0, loopCount - 1);
            nextUserEventIndex = 0;
            paused = false;
            playing = true;
            if (line != null) {
                line.stop();
                line.flush();
            }
            ensureWorkerLocked();
            lock.notifyAll();
        }
    }

    public void stop() {
        synchronized (lock) {
            playbackEpoch++;
            paused = false;
            playing = false;
            framePosition = 0;
            remainingLoops = 0;
            nextUserEventIndex = 0;
            if (line != null) {
                line.stop();
                line.flush();
            }
        }
    }

    public void pause() {
        synchronized (lock) {
            if (!playing) {
                return;
            }
            playbackEpoch++;
            paused = true;
            playing = false;
            if (line != null) {
                line.stop();
            }
        }
    }

    public void resume() {
        synchronized (lock) {
            if (!paused) {
                return;
            }
            playbackEpoch++;
            paused = false;
            playing = true;
            ensureWorkerLocked();
            if (line != null) {
                line.start();
            }
            lock.notifyAll();
        }
    }

    @Override
    public void close() {
        Thread workerToJoin;
        synchronized (lock) {
            closed = true;
            playbackEpoch++;
            paused = false;
            playing = false;
            if (line != null) {
                line.stop();
                line.flush();
            }
            workerToJoin = worker;
            lock.notifyAll();
        }
        if (workerToJoin != null && workerToJoin != Thread.currentThread()) {
            try {
                workerToJoin.join(2_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (lock) {
            closeLineLocked();
        }
    }

    private void ensureWorkerLocked() {
        if (worker != null) {
            return;
        }
        worker = new Thread(this::runLoop, "remexa-smaf-rendered");
        worker.setDaemon(true);
        worker.start();
    }

    private void runLoop() {
        byte[] chunkBuffer = new byte[CHUNK_FRAMES * 4];
        while (true) {
            PhraseTrackListener completionListener = null;
            synchronized (lock) {
                while (!closed && !playing) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (closed) {
                    closeLineLocked();
                    return;
                }
                ensureLineLocked();
            }

            int framesToWrite;
            int startFrame;
            float leftGain;
            float rightGain;
            long chunkEpoch;
            List<Integer> pendingUserEvents = List.of();
            synchronized (lock) {
                if (closed) {
                    closeLineLocked();
                    return;
                }
                if (!playing) {
                    continue;
                }
                int available = audio.frameCount() - framePosition;
                if (available <= 0) {
                    if (remainingLoops == -1 || remainingLoops > 0) {
                        if (remainingLoops > 0) {
                            remainingLoops--;
                        }
                        framePosition = 0;
                        nextUserEventIndex = 0;
                        available = audio.frameCount();
                    } else {
                        playing = false;
                        paused = false;
                        framePosition = 0;
                        if (line != null) {
                            line.drain();
                            line.stop();
                            line.flush();
                        }
                        completionListener = listener;
                    }
                }
                framesToWrite = Math.min(CHUNK_FRAMES, available);
                startFrame = framePosition;
                float gain = volume / 127.0f;
                float pan = Math.max(-1.0f, Math.min(1.0f, (panpot - 64.0f) / 63.0f));
                leftGain = gain * (pan > 0.0f ? 1.0f - pan : 1.0f);
                rightGain = gain * (pan < 0.0f ? 1.0f + pan : 1.0f);
                framePosition += framesToWrite;
                chunkEpoch = playbackEpoch;
                pendingUserEvents = consumeUserEventsLocked(startFrame, framesToWrite);
            }

            if (completionListener != null) {
                dispatchCompletion(completionListener);
                continue;
            }

            scaleIntoChunk(audio.pcm16Le(), startFrame, framesToWrite, leftGain, rightGain, chunkBuffer);
            synchronized (lock) {
                if (closed || !playing || chunkEpoch != playbackEpoch || line == null) {
                    continue;
                }
            }
            line.write(chunkBuffer, 0, framesToWrite * 4);
            for (int userEventId : pendingUserEvents) {
                if (listener != null) {
                    listener.eventOccurred(userEventId);
                }
            }
        }
    }

    private static void dispatchCompletion(PhraseTrackListener listener) {
        Thread callbackThread = new Thread(() -> listener.eventOccurred(-1), "remexa-smaf-callback");
        callbackThread.setDaemon(true);
        callbackThread.start();
    }

    private void ensureLineLocked() {
        if (line != null) {
            line.start();
            return;
        }
        AudioFormat format = new AudioFormat(audio.sampleRate(), 16, audio.channelCount(), true, false);
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, Math.max(CHUNK_FRAMES * 8, audio.sampleRate() / 2));
            line.start();
        } catch (LineUnavailableException exception) {
            throw new RuntimeException("Unable to open SMAF rendered audio output", exception);
        }
    }

    private void closeLineLocked() {
        if (line == null) {
            return;
        }
        line.stop();
        line.flush();
        line.close();
        line = null;
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

    private static void scaleIntoChunk(byte[] pcm,
                                       int startFrame,
                                       int frames,
                                       float leftGain,
                                       float rightGain,
                                       byte[] output) {
        int inputOffset = startFrame * 4;
        int outputOffset = 0;
        for (int frame = 0; frame < frames; frame++) {
            short left = readSample(pcm, inputOffset);
            short right = readSample(pcm, inputOffset + 2);
            writeSample(output, outputOffset, Math.round(left * leftGain));
            writeSample(output, outputOffset + 2, Math.round(right * rightGain));
            inputOffset += 4;
            outputOffset += 4;
        }
    }

    private static short readSample(byte[] input, int offset) {
        int low = input[offset] & 0xFF;
        int high = input[offset + 1];
        return (short) ((high << 8) | low);
    }

    private static void writeSample(byte[] output, int offset, int sample) {
        int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        output[offset] = (byte) (clamped & 0xFF);
        output[offset + 1] = (byte) ((clamped >>> 8) & 0xFF);
    }
}
