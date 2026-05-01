package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.decoders.WAVTools;
import javax.microedition.media.decoders.WAVYamahaADPCMDecoder;
import remexa.audio.pcm.RenderedPcmAudio;
import remexa.audio.pcm.RenderedPcmPlayer;
import remexa.audio.smaf.SmafPlayback;
import remexa.host.runtime.MidletRuntime;

public final class Manager {
    private static final String CONTROL_PACKAGE = "javax.microedition.media.control.";
    private static final Set<AbstractPlayer> ACTIVE_PLAYERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private Manager() {
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (stream == null) {
            throw new IllegalArgumentException("stream");
        }
        byte[] source = readAllBytes(stream);
        String normalizedType = normalizeContentType(type);
        if (isSmafType(normalizedType, source)) {
            try {
                SmafPlayback.prewarm(source);
            } catch (RuntimeException ignored) {
                // Prewarm is only an optimization; player creation must stay non-fatal.
            }
            return new SmafPlayer(source, normalizedType.isEmpty() ? "application/x-smaf" : normalizedType);
        }
        if (isMidiType(normalizedType, source)) {
            return new MidiPlayer(source, normalizedType.isEmpty() ? "audio/midi" : normalizedType);
        }
        if (isWavType(normalizedType, source)) {
            return new WavPlayer(source, normalizedType.isEmpty() ? "audio/x-wav" : normalizedType);
        }
        return new SilentPlayer(normalizedType.isEmpty() ? "application/octet-stream" : normalizedType);
    }

    public static void shutdownOwnedPlayers(ClassLoader ownerClassLoader) {
        AbstractPlayer[] snapshot;
        synchronized (ACTIVE_PLAYERS) {
            snapshot = ACTIVE_PLAYERS.toArray(AbstractPlayer[]::new);
        }
        for (AbstractPlayer player : snapshot) {
            if (player == null || !player.isOwnedBy(ownerClassLoader)) {
                continue;
            }
            try {
                player.close();
            } catch (RuntimeException ignored) {
                // Best-effort shutdown for app teardown.
            }
        }
    }

    private static String normalizeContentType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isMidiType(String normalizedType, byte[] source) {
        return normalizedType.isEmpty()
                ? hasAsciiPrefix(source, "MThd")
                : normalizedType.equals("audio/midi")
                || normalizedType.equals("audio/x-midi")
                || normalizedType.equals("audio/sp-midi")
                || normalizedType.equals("audio/spmidi")
                || normalizedType.equals("audio/mid")
                || (normalizedType.equals("unknown") && hasAsciiPrefix(source, "MThd"));
    }

    private static boolean isSmafType(String normalizedType, byte[] source) {
        return normalizedType.equals("application/x-smaf")
                || normalizedType.equals("audio/x-smaf")
                || normalizedType.equals("audio/mmf")
                || normalizedType.equals("ott")
                || (normalizedType.isEmpty() && hasAsciiPrefix(source, "MMMD"))
                || (normalizedType.equals("unknown") && hasAsciiPrefix(source, "MMMD"));
    }

    private static boolean isWavType(String normalizedType, byte[] source) {
        return normalizedType.equals("audio/x-wav")
                || normalizedType.equals("audio/wav")
                || normalizedType.equals("audio/wave")
                || normalizedType.equals("audio/x-pn-wav")
                || (normalizedType.isEmpty() && hasAsciiPrefix(source, "RIFF"))
                || (normalizedType.equals("unknown") && hasAsciiPrefix(source, "RIFF"));
    }

    private static boolean hasAsciiPrefix(byte[] source, String prefix) {
        if (source.length < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if ((byte) prefix.charAt(i) != source[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private abstract static class AbstractPlayer implements Player {
        private final String contentType;
        private final ClassLoader ownerClassLoader;
        private final PlayerVolumeControl volumeControl = new PlayerVolumeControl(this);
        private final Control[] controls = new Control[]{volumeControl};
        private final CopyOnWriteArrayList<PlayerListener> listeners = new CopyOnWriteArrayList<>();

        private int state = UNREALIZED;
        private int loopCount = 1;

        private AbstractPlayer(String contentType) {
            this.contentType = contentType;
            this.ownerClassLoader = MidletRuntime.currentAppClassLoader();
            ACTIVE_PLAYERS.add(this);
        }

        @Override
        public synchronized void realize() throws MediaException {
            ensureNotClosed();
            if (state >= REALIZED) {
                return;
            }
            doRealize();
            state = REALIZED;
        }

        @Override
        public synchronized void prefetch() throws MediaException {
            ensureNotClosed();
            if (state == STARTED) {
                return;
            }
            if (state < REALIZED) {
                realize();
            }
            doPrefetch();
            state = PREFETCHED;
        }

        @Override
        public synchronized void start() throws MediaException {
            MidletRuntime.ensureThreadActive();
            ensureNotClosed();
            if (state == STARTED) {
                return;
            }
            if (state < PREFETCHED) {
                prefetch();
            }
            doStart();
            state = STARTED;
            notifyListeners(PlayerListener.STARTED, Long.valueOf(safeMediaTime()));
        }

        @Override
        public synchronized void stop() throws MediaException {
            ensureNotClosed();
            if (state != STARTED) {
                return;
            }
            long stoppedAt = safeMediaTime();
            doStop();
            state = PREFETCHED;
            notifyListeners(PlayerListener.STOPPED, Long.valueOf(stoppedAt));
        }

        @Override
        public synchronized void deallocate() {
            ensureNotClosed();
            if (state == UNREALIZED || state == REALIZED) {
                return;
            }
            if (state == STARTED) {
                try {
                    doStop();
                } catch (MediaException exception) {
                    throw new IllegalStateException("Failed to stop player while deallocating.", exception);
                }
            }
            doDeallocate();
            state = UNREALIZED;
        }

        @Override
        public synchronized void close() {
            if (state == CLOSED) {
                return;
            }
            doClose();
            state = CLOSED;
            ACTIVE_PLAYERS.remove(this);
            notifyListeners(PlayerListener.CLOSED, null);
        }

        @Override
        public synchronized void setLoopCount(int count) {
            if (count < -1) {
                throw new IllegalArgumentException("Invalid loop count: " + count);
            }
            if (state == STARTED || state == CLOSED) {
                throw new IllegalStateException("Cannot change loop count in the current player state.");
            }
            loopCount = count;
            applyLoopCount(count);
        }

        @Override
        public void addPlayerListener(PlayerListener listener) {
            if (listener == null) {
                throw new NullPointerException("listener");
            }
            listeners.addIfAbsent(listener);
        }

        @Override
        public void removePlayerListener(PlayerListener listener) {
            if (listener == null) {
                return;
            }
            listeners.remove(listener);
        }

        @Override
        public synchronized long setMediaTime(long now) throws MediaException {
            ensureNotClosed();
            if (state < REALIZED) {
                realize();
            }
            return doSetMediaTime(now);
        }

        @Override
        public synchronized long getMediaTime() {
            if (state == CLOSED) {
                throw new IllegalStateException("Player is closed.");
            }
            if (state == UNREALIZED) {
                return TIME_UNKNOWN;
            }
            return safeMediaTime();
        }

        @Override
        public synchronized long getDuration() {
            if (state == CLOSED) {
                throw new IllegalStateException("Player is closed.");
            }
            if (state == UNREALIZED) {
                return TIME_UNKNOWN;
            }
            return safeDuration();
        }

        @Override
        public synchronized int getState() {
            if (state == STARTED && !isStarted()) {
                state = PREFETCHED;
            }
            return state;
        }

        @Override
        public synchronized String getContentType() {
            if (state == UNREALIZED || state == CLOSED) {
                throw new IllegalStateException("Player is not realized.");
            }
            return contentType;
        }

        @Override
        public synchronized Control getControl(String controlType) {
            if (controlType == null) {
                throw new IllegalArgumentException("controlType");
            }
            if (state == UNREALIZED || state == CLOSED) {
                throw new IllegalStateException("Player controls are not available in the current state.");
            }
            String normalized = controlType.indexOf('.') >= 0 ? controlType : CONTROL_PACKAGE + controlType;
            return normalized.equals(VolumeControl.class.getName()) ? volumeControl : null;
        }

        @Override
        public synchronized Control[] getControls() {
            if (state == UNREALIZED || state == CLOSED) {
                throw new IllegalStateException("Player controls are not available in the current state.");
            }
            return controls.clone();
        }

        final int loopCount() {
            return loopCount;
        }

        final int volumeLevel() {
            return volumeControl.getStoredLevel();
        }

        final boolean muted() {
            return volumeControl.getStoredMute();
        }

        final boolean isOwnedBy(ClassLoader candidate) {
            return candidate == null || ownerClassLoader == candidate;
        }

        final void applyVolumeControl() throws MediaException {
            onVolumeChanged();
            notifyListeners(PlayerListener.VOLUME_CHANGED, volumeControl);
        }

        private void ensureNotClosed() {
            if (state == CLOSED) {
                throw new IllegalStateException("Player is closed.");
            }
        }

        protected final boolean loopsForever(int count) {
            return count < 0 || count == 255;
        }

        protected final void notifyEndOfMedia() {
            synchronized (this) {
                if (state == CLOSED) {
                    return;
                }
                state = PREFETCHED;
            }
            notifyListeners(PlayerListener.END_OF_MEDIA, Long.valueOf(safeMediaTime()));
        }

        protected final void notifyEndOfMediaSoon() {
            Thread notifier = new Thread(() -> {
                for (int attempt = 0; attempt < 50; attempt++) {
                    synchronized (this) {
                        if (state == STARTED || state == CLOSED) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(1L);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                notifyEndOfMedia();
            }, "remexa-silent-player-end");
            notifier.setDaemon(true);
            notifier.start();
        }

        protected final void notifyError(Throwable throwable) {
            notifyListeners(PlayerListener.ERROR, throwable);
        }

        private long safeMediaTime() {
            try {
                return doGetMediaTime();
            } catch (RuntimeException ignored) {
                return TIME_UNKNOWN;
            }
        }

        private long safeDuration() {
            try {
                return doGetDuration();
            } catch (RuntimeException ignored) {
                return TIME_UNKNOWN;
            }
        }

        private void notifyListeners(String event, Object eventData) {
            for (PlayerListener listener : listeners) {
                try {
                    listener.playerUpdate(this, event, eventData);
                } catch (RuntimeException ignored) {
                }
            }
        }

        protected void applyLoopCount(int count) {
        }

        protected void doPrefetch() throws MediaException {
        }

        protected long doSetMediaTime(long now) throws MediaException {
            return now;
        }

        protected long doGetMediaTime() {
            return TIME_UNKNOWN;
        }

        protected long doGetDuration() {
            return TIME_UNKNOWN;
        }

        protected abstract void doRealize() throws MediaException;

        protected abstract void doStart() throws MediaException;

        protected abstract void doStop() throws MediaException;

        protected abstract void doDeallocate();

        protected abstract void doClose();

        protected abstract void onVolumeChanged() throws MediaException;

        protected abstract boolean isStarted();
    }

    private static final class PlayerVolumeControl implements VolumeControl {
        private final AbstractPlayer owner;

        private int level = 100;
        private boolean levelExplicitlySet;
        private boolean muted;

        private PlayerVolumeControl(AbstractPlayer owner) {
            this.owner = owner;
        }

        @Override
        public synchronized int getLevel() {
            return !levelExplicitlySet && owner.getState() == Player.REALIZED ? -1 : level;
        }

        @Override
        public synchronized boolean isMuted() {
            return muted;
        }

        @Override
        public synchronized int setLevel(int level) {
            int clamped = Math.max(0, Math.min(100, level));
            boolean changed = this.level != clamped || !levelExplicitlySet;
            this.level = clamped;
            this.levelExplicitlySet = true;
            if (changed) {
                try {
                    owner.applyVolumeControl();
                } catch (MediaException exception) {
                    throw new IllegalStateException("Failed to apply volume change.", exception);
                }
            }
            return clamped;
        }

        @Override
        public synchronized void setMute(boolean mute) {
            if (muted == mute) {
                return;
            }
            muted = mute;
            try {
                owner.applyVolumeControl();
            } catch (MediaException exception) {
                throw new IllegalStateException("Failed to apply mute change.", exception);
            }
        }

        private synchronized int getStoredLevel() {
            return level;
        }

        private synchronized boolean getStoredMute() {
            return muted;
        }
    }

    private static final class SilentPlayer extends AbstractPlayer {
        private SilentPlayer(String contentType) {
            super(contentType);
        }

        @Override
        protected void doRealize() {
        }

        @Override
        protected void doStart() {
            notifyEndOfMediaSoon();
        }

        @Override
        protected void doStop() {
        }

        @Override
        protected void doDeallocate() {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void onVolumeChanged() {
        }

        @Override
        protected boolean isStarted() {
            return false;
        }

        @Override
        protected long doGetMediaTime() {
            return 0L;
        }

        @Override
        protected long doGetDuration() {
            return 0L;
        }
    }

    private static final class MidiPlayer extends AbstractPlayer {
        private final byte[] source;
        private Sequencer sequencer;
        private Synthesizer synthesizer;
        private Receiver receiver;
        private Transmitter transmitter;

        private MidiPlayer(byte[] source, String contentType) {
            super(contentType);
            this.source = source;
        }

        @Override
        protected synchronized void doRealize() throws MediaException {
            try {
                sequencer = MidiSystem.getSequencer(false);
                synthesizer = MidiSystem.getSynthesizer();
                sequencer.open();
                synthesizer.open();
                receiver = synthesizer.getReceiver();
                transmitter = sequencer.getTransmitter();
                transmitter.setReceiver(receiver);
                sequencer.setSequence(MidiSystem.getSequence(new ByteArrayInputStream(source)));
                sequencer.addMetaEventListener(message -> {
                    if (message.getType() == 0x2F) {
                        notifyEndOfMedia();
                    }
                });
                applyLoopCount(loopCount());
                onVolumeChanged();
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException exception) {
                closeQuietly();
            }
        }

        @Override
        protected synchronized void doStart() throws MediaException {
            if (sequencer == null) {
                notifyEndOfMediaSoon();
                return;
            }
            try {
                if (sequencer.getMicrosecondPosition() >= sequencer.getMicrosecondLength()) {
                    sequencer.setMicrosecondPosition(0L);
                }
                applyLoopCount(loopCount());
                onVolumeChanged();
                sequencer.start();
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to start MIDI player.", exception);
            }
        }

        @Override
        protected synchronized void doStop() throws MediaException {
            if (sequencer == null) {
                return;
            }
            try {
                sequencer.stop();
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to stop MIDI player.", exception);
            }
        }

        @Override
        protected synchronized void doDeallocate() {
            closeQuietly();
        }

        @Override
        protected synchronized void doClose() {
            closeQuietly();
        }

        @Override
        protected synchronized void applyLoopCount(int count) {
            if (sequencer != null) {
                sequencer.setLoopStartPoint(0L);
                sequencer.setLoopEndPoint(-1L);
                sequencer.setLoopCount(loopsForever(count) ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, count - 1));
            }
        }

        @Override
        protected synchronized void onVolumeChanged() {
            if (synthesizer == null) {
                return;
            }
            int level = muted() ? 0 : Math.max(0, Math.min(127, Math.round(volumeLevel() * 127.0f / 100.0f)));
            for (MidiChannel channel : synthesizer.getChannels()) {
                if (channel != null) {
                    channel.controlChange(7, level);
                }
            }
        }

        @Override
        protected synchronized boolean isStarted() {
            return sequencer != null && sequencer.isRunning();
        }

        @Override
        protected synchronized long doSetMediaTime(long now) throws MediaException {
            if (sequencer == null) {
                return 0L;
            }
            long targetMicros = Math.max(0L, now) * 1000L;
            long maxMicros = sequencer.getMicrosecondLength();
            if (maxMicros > 0L) {
                targetMicros = Math.min(targetMicros, maxMicros);
            }
            try {
                sequencer.setMicrosecondPosition(targetMicros);
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to seek MIDI player.", exception);
            }
            return sequencer.getMicrosecondPosition() / 1000L;
        }

        @Override
        protected synchronized long doGetMediaTime() {
            return sequencer == null ? 0L : sequencer.getMicrosecondPosition() / 1000L;
        }

        @Override
        protected synchronized long doGetDuration() {
            return sequencer == null ? TIME_UNKNOWN : sequencer.getMicrosecondLength() / 1000L;
        }

        private void closeQuietly() {
            if (transmitter != null) {
                try {
                    transmitter.close();
                } catch (RuntimeException ignored) {
                }
                transmitter = null;
            }
            if (receiver != null) {
                try {
                    receiver.close();
                } catch (RuntimeException ignored) {
                }
                receiver = null;
            }
            if (sequencer != null) {
                try {
                    sequencer.stop();
                } catch (RuntimeException ignored) {
                }
                try {
                    sequencer.close();
                } catch (RuntimeException ignored) {
                }
                sequencer = null;
            }
            if (synthesizer != null) {
                try {
                    synthesizer.close();
                } catch (RuntimeException ignored) {
                }
                synthesizer = null;
            }
        }
    }

    private static final class SmafPlayer extends AbstractPlayer {
        private final byte[] source;
        private SmafPlayback playback;
        private boolean pausedByStop;

        private SmafPlayer(byte[] source, String contentType) {
            super(contentType);
            this.source = source;
        }

        @Override
        protected synchronized void doRealize() throws MediaException {
            try {
                playback = SmafPlayback.create(source);
                playback.setListener(eventId -> {
                    if (eventId == -1) {
                        notifyEndOfMedia();
                    }
                });
                playback.prepareAsync();
                pausedByStop = false;
                onVolumeChanged();
            } catch (Exception exception) {
                closeQuietly();
            }
        }

        @Override
        protected synchronized void doPrefetch() throws MediaException {
            if (playback == null) {
                return;
            }
            try {
                playback.prefetch();
                onVolumeChanged();
            } catch (Exception exception) {
                throw new MediaException("Failed to prefetch SMAF player.", exception);
            }
        }

        @Override
        protected synchronized void doStart() throws MediaException {
            if (playback == null) {
                notifyEndOfMediaSoon();
                return;
            }
            try {
                onVolumeChanged();
                if (pausedByStop) {
                    playback.resume();
                } else {
                    playback.play(loopsForever(loopCount()) ? 0 : Math.max(1, loopCount()));
                }
                pausedByStop = false;
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to start SMAF player.", exception);
            }
        }

        @Override
        protected synchronized void doStop() throws MediaException {
            if (playback == null) {
                return;
            }
            try {
                playback.pause();
                pausedByStop = true;
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to stop SMAF player.", exception);
            }
        }

        @Override
        protected synchronized void doDeallocate() {
            closeQuietly();
        }

        @Override
        protected synchronized void doClose() {
            closeQuietly();
        }

        @Override
        protected synchronized void onVolumeChanged() {
            if (playback == null) {
                return;
            }
            int level = muted() ? 0 : Math.max(0, Math.min(127, Math.round(volumeLevel() * 127.0f / 100.0f)));
            playback.setVolume(level);
        }

        @Override
        protected synchronized boolean isStarted() {
            return playback != null && playback.getState() == SmafPlayback.PLAYING;
        }

        @Override
        protected synchronized long doSetMediaTime(long now) throws MediaException {
            if (playback == null) {
                return 0L;
            }
            if (now > 0L) {
                throw new MediaException("SMAF seeking is not supported.");
            }
            boolean wasStarted = isStarted();
            playback.stop();
            pausedByStop = false;
            if (wasStarted) {
                playback.play(loopsForever(loopCount()) ? 0 : Math.max(1, loopCount()));
            }
            return 0L;
        }

        @Override
        protected synchronized long doGetMediaTime() {
            return 0L;
        }

        private void closeQuietly() {
            if (playback != null) {
                playback.close();
                playback = null;
            }
            pausedByStop = false;
        }
    }

    private static final class WavPlayer extends AbstractPlayer {
        private final byte[] source;
        private RenderedPcmAudio audio;
        private RenderedPcmPlayer playback;
        private volatile long playbackStartedAtNanos;
        private volatile long cachedMediaTimeMillis;

        private WavPlayer(byte[] source, String contentType) {
            super(contentType);
            this.source = source;
        }

        @Override
        protected synchronized void doRealize() throws MediaException {
            try {
                audio = openRenderedAudio(source);
                playback = new RenderedPcmPlayer(audio);
                playback.setCompletionListener(() -> {
                    long duration = durationMillis(audio);
                    cachedMediaTimeMillis = duration <= 0L || loopsForever(loopCount())
                            ? duration
                            : duration * Math.max(1, loopCount());
                    playbackStartedAtNanos = 0L;
                    notifyEndOfMedia();
                });
                RenderedPcmPlayer.prewarm(audio.sampleRate(), audio.channelCount());
                onVolumeChanged();
            } catch (MediaException exception) {
                closeQuietly();
            } catch (Exception exception) {
                closeQuietly();
            }
        }

        @Override
        protected synchronized void doStart() throws MediaException {
            if (playback == null || audio == null || audio.frameCount() <= 0) {
                notifyEndOfMediaSoon();
                return;
            }
            try {
                onVolumeChanged();
                cachedMediaTimeMillis = 0L;
                playbackStartedAtNanos = System.nanoTime();
                playback.play(loopsForever(loopCount()) ? 0 : Math.max(1, loopCount()));
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to start WAV player.", exception);
            }
        }

        @Override
        protected synchronized void doStop() throws MediaException {
            if (playback == null) {
                return;
            }
            try {
                playback.stop();
                playbackStartedAtNanos = 0L;
                cachedMediaTimeMillis = 0L;
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to stop WAV player.", exception);
            }
        }

        @Override
        protected synchronized void doDeallocate() {
            closeQuietly();
        }

        @Override
        protected synchronized void doClose() {
            closeQuietly();
        }

        @Override
        protected synchronized void onVolumeChanged() {
            if (playback == null) {
                return;
            }
            int level = muted() ? 0 : Math.round(Math.max(0, Math.min(100, volumeLevel())) * 127.0f / 100.0f);
            playback.setVolume(level);
        }

        @Override
        protected synchronized boolean isStarted() {
            return playback != null && playback.getState() == RenderedPcmPlayer.PLAYING;
        }

        @Override
        protected synchronized long doSetMediaTime(long now) throws MediaException {
            if (playback == null) {
                return 0L;
            }
            boolean wasStarted = isStarted();
            playback.stop();
            cachedMediaTimeMillis = 0L;
            playbackStartedAtNanos = 0L;
            if (wasStarted) {
                playbackStartedAtNanos = System.nanoTime();
                playback.play(loopsForever(loopCount()) ? 0 : Math.max(1, loopCount()));
            }
            return 0L;
        }

        @Override
        protected synchronized long doGetMediaTime() {
            if (playback == null || audio == null) {
                return 0L;
            }
            long startedAt = playbackStartedAtNanos;
            if (startedAt <= 0L || playback.getState() != RenderedPcmPlayer.PLAYING) {
                return cachedMediaTimeMillis;
            }
            long elapsedMillis = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
            long durationMillis = durationMillis(audio);
            if (durationMillis <= 0L) {
                return elapsedMillis;
            }
            if (loopsForever(loopCount())) {
                return elapsedMillis % durationMillis;
            }
            long totalMillis = durationMillis * Math.max(1, loopCount());
            return Math.min(elapsedMillis, totalMillis);
        }

        @Override
        protected synchronized long doGetDuration() {
            return audio == null ? TIME_UNKNOWN : durationMillis(audio);
        }

        private void closeQuietly() {
            RenderedPcmPlayer currentPlayback = playback;
            playback = null;
            audio = null;
            playbackStartedAtNanos = 0L;
            cachedMediaTimeMillis = 0L;
            if (currentPlayback != null) {
                try {
                    currentPlayback.close();
                } catch (RuntimeException ignored) {
                }
            }
        }

        private static RenderedPcmAudio openRenderedAudio(byte[] source) throws Exception {
            try {
                return openRenderedAudioWithAudioSystem(source);
            } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
                byte[] decoded = decodeLegacyWav(source);
                try {
                    return openRenderedAudioWithAudioSystem(decoded);
                } catch (UnsupportedAudioFileException | IllegalArgumentException fallbackException) {
                    fallbackException.addSuppressed(exception);
                    throw fallbackException;
                }
            }
        }

        private static RenderedPcmAudio openRenderedAudioWithAudioSystem(byte[] source)
                throws IOException, UnsupportedAudioFileException {
            try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(source))) {
                AudioFormat rawFormat = rawStream.getFormat();
                AudioFormat targetFormat = renderedFriendlyFormat(rawFormat);
                if (requiresConversion(rawFormat, targetFormat)) {
                    if (!AudioSystem.isConversionSupported(targetFormat, rawFormat)) {
                        throw new UnsupportedAudioFileException("Unsupported WAV format: " + rawFormat);
                    }
                    try (AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, rawStream)) {
                        return readRenderedAudio(converted, targetFormat);
                    }
                }
                return readRenderedAudio(rawStream, targetFormat);
            }
        }

        private static AudioFormat renderedFriendlyFormat(AudioFormat rawFormat) {
            int channels = Math.max(1, rawFormat.getChannels());
            float sampleRate = rawFormat.getSampleRate() > 0 ? rawFormat.getSampleRate() : 44100.0f;
            return new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false
            );
        }

        private static boolean requiresConversion(AudioFormat rawFormat, AudioFormat targetFormat) {
            return !AudioFormat.Encoding.PCM_SIGNED.equals(rawFormat.getEncoding())
                    || rawFormat.getSampleSizeInBits() != 16
                    || rawFormat.isBigEndian()
                    || rawFormat.getChannels() != targetFormat.getChannels()
                    || rawFormat.getFrameSize() != targetFormat.getFrameSize()
                    || Float.compare(rawFormat.getSampleRate(), targetFormat.getSampleRate()) != 0;
        }

        private static RenderedPcmAudio readRenderedAudio(AudioInputStream input, AudioFormat format)
                throws IOException {
            byte[] pcm = readAllBytes(input);
            int channels = Math.max(1, format.getChannels());
            int frameSize = Math.max(1, channels * 2);
            int usableLength = (pcm.length / frameSize) * frameSize;
            if (usableLength != pcm.length) {
                pcm = Arrays.copyOf(pcm, usableLength);
            }
            int frameCount = usableLength / frameSize;
            int sampleRate = Math.max(1, Math.round(format.getSampleRate()));
            return new RenderedPcmAudio(sampleRate, channels, frameCount, pcm);
        }

        private static long durationMillis(RenderedPcmAudio audio) {
            if (audio == null || audio.sampleRate() <= 0) {
                return TIME_UNKNOWN;
            }
            return Math.max(0L, Math.round(audio.frameCount() * 1000.0 / audio.sampleRate()));
        }

        private static byte[] decodeLegacyWav(byte[] source) throws MediaException {
            try (ByteArrayInputStream input = new ByteArrayInputStream(source)) {
                int[] header = WAVTools.readHeader(input);
                int audioFormat = header[0];
                int sampleRate = header[1];
                int channels = Math.max(1, header[2]);
                int bitsPerSample = header[4];
                int dataLength = Math.max(0, header[5]);
                int headerLength = Math.max(0, header[6]);
                int payloadStart = Math.min(source.length, headerLength);
                int payloadEnd = Math.min(source.length, payloadStart + dataLength);
                byte[] payload = Arrays.copyOfRange(source, payloadStart, payloadEnd);

                if (audioFormat == 1) {
                    return switch (bitsPerSample) {
                        case 4 -> WAVTools.convert4BitWav(payload, channels, sampleRate, false);
                        case 8 -> WAVTools.convert8BitWav(payload, channels, sampleRate, false);
                        case 12 -> WAVTools.convert12BitWav(payload, channels, sampleRate, true);
                        case 16 -> WAVTools.convert16BitWav(payload, channels, sampleRate, true);
                        default -> throw new MediaException("Unsupported PCM WAV bit depth: " + bitsPerSample);
                    };
                }
                if (audioFormat == 0x11 || audioFormat == 0x20) {
                    return WAVYamahaADPCMDecoder.ADPCMBDecode(payload, sampleRate, channels);
                }
                throw new MediaException("Unsupported WAV encoding: " + audioFormat);
            } catch (IOException exception) {
                throw new MediaException("Failed to decode legacy WAV audio.", exception);
            }
        }
    }
}
