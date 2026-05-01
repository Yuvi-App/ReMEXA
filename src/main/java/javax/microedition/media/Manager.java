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
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.decoders.WAVTools;
import javax.microedition.media.decoders.WAVYamahaADPCMDecoder;
import remexa.audio.smaf.SmafPlayback;

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
            SmafPlayback.prewarm(source);
            return new SmafPlayer(source, normalizedType.isEmpty() ? "application/x-smaf" : normalizedType);
        }
        if (isMidiType(normalizedType, source)) {
            return new MidiPlayer(source, normalizedType.isEmpty() ? "audio/midi" : normalizedType);
        }
        if (isWavType(normalizedType, source)) {
            return new WavPlayer(source, normalizedType.isEmpty() ? "audio/x-wav" : normalizedType);
        }
        throw new MediaException("Unsupported media type: " + type);
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
            this.ownerClassLoader = Thread.currentThread().getContextClassLoader();
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
                throw new MediaException("Failed to realize MIDI player.", exception);
            }
        }

        @Override
        protected synchronized void doStart() throws MediaException {
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
                throw new MediaException("Failed to realize SMAF player.", exception);
            }
        }

        @Override
        protected synchronized void doPrefetch() throws MediaException {
            if (playback == null) {
                throw new MediaException("SMAF player was not realized.");
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
                throw new MediaException("SMAF player was not realized.");
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
        private Clip clip;
        private volatile boolean ignoreNextStopEvent;

        private WavPlayer(byte[] source, String contentType) {
            super(contentType);
            this.source = source;
        }

        @Override
        protected synchronized void doRealize() throws MediaException {
            try {
                clip = openClip(source);
                clip.addLineListener(this::handleLineEvent);
                onVolumeChanged();
            } catch (MediaException exception) {
                closeQuietly();
                throw exception;
            } catch (Exception exception) {
                closeQuietly();
                throw new MediaException("Failed to realize WAV player.", exception);
            }
        }

        @Override
        protected synchronized void doStart() throws MediaException {
            if (clip == null) {
                throw new MediaException("WAV player was not realized.");
            }
            try {
                if (clip.getFramePosition() >= clip.getFrameLength()) {
                    clip.setFramePosition(0);
                }
                onVolumeChanged();
                if (loopsForever(loopCount()) || loopCount() > 1) {
                    clip.loop(loopsForever(loopCount()) ? Clip.LOOP_CONTINUOUSLY : Math.max(0, loopCount() - 1));
                } else {
                    clip.start();
                }
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to start WAV player.", exception);
            }
        }

        @Override
        protected synchronized void doStop() throws MediaException {
            if (clip == null) {
                return;
            }
            try {
                stopClip(true);
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
            if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (muted() || volumeLevel() <= 0) {
                gain.setValue(gain.getMinimum());
                return;
            }
            float normalized = Math.max(0.0001f, volumeLevel() / 100.0f);
            float gainDb = (float) (20.0 * Math.log10(normalized));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), gainDb)));
        }

        @Override
        protected synchronized boolean isStarted() {
            return clip != null && clip.isRunning();
        }

        @Override
        protected synchronized long doSetMediaTime(long now) throws MediaException {
            if (clip == null) {
                return 0L;
            }
            long targetMicros = Math.max(0L, now) * 1000L;
            long maxMicros = clip.getMicrosecondLength();
            if (maxMicros > 0L) {
                targetMicros = Math.min(targetMicros, maxMicros);
            }
            boolean wasRunning = clip.isRunning();
            stopClip(false);
            clip.setMicrosecondPosition(targetMicros);
            if (wasRunning) {
                if (loopsForever(loopCount()) || loopCount() > 1) {
                    clip.loop(loopsForever(loopCount()) ? Clip.LOOP_CONTINUOUSLY : Math.max(0, loopCount() - 1));
                } else {
                    clip.start();
                }
            }
            return clip.getMicrosecondPosition() / 1000L;
        }

        @Override
        protected synchronized long doGetMediaTime() {
            return clip == null ? 0L : clip.getMicrosecondPosition() / 1000L;
        }

        @Override
        protected synchronized long doGetDuration() {
            return clip == null ? TIME_UNKNOWN : clip.getMicrosecondLength() / 1000L;
        }

        private void handleLineEvent(LineEvent event) {
            if (event.getType() != LineEvent.Type.STOP) {
                return;
            }
            if (ignoreNextStopEvent) {
                ignoreNextStopEvent = false;
                return;
            }
            Clip currentClip = clip;
            if (currentClip != null && currentClip.getFrameLength() > 0
                    && currentClip.getFramePosition() >= currentClip.getFrameLength()) {
                notifyEndOfMedia();
            }
        }

        private void stopClip(boolean resetPosition) {
            if (clip == null) {
                return;
            }
            if (clip.isRunning()) {
                ignoreNextStopEvent = true;
                clip.stop();
            }
            if (resetPosition) {
                clip.setFramePosition(0);
            }
        }

        private void closeQuietly() {
            Clip currentClip = clip;
            clip = null;
            ignoreNextStopEvent = false;
            if (currentClip != null) {
                try {
                    if (currentClip.isRunning()) {
                        currentClip.stop();
                    }
                } catch (RuntimeException ignored) {
                }
                try {
                    currentClip.close();
                } catch (RuntimeException ignored) {
                }
            }
        }

        private static Clip openClip(byte[] source) throws Exception {
            try {
                return openClipWithAudioSystem(source);
            } catch (UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException exception) {
                byte[] decoded = decodeLegacyWav(source);
                try {
                    return openClipWithAudioSystem(decoded);
                } catch (UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException fallbackException) {
                    fallbackException.addSuppressed(exception);
                    throw fallbackException;
                }
            }
        }

        private static Clip openClipWithAudioSystem(byte[] source)
                throws IOException, UnsupportedAudioFileException, LineUnavailableException {
            try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(source))) {
                AudioFormat rawFormat = rawStream.getFormat();
                AudioFormat targetFormat = clipFriendlyFormat(rawFormat);
                if (!rawFormat.matches(targetFormat) && AudioSystem.isConversionSupported(targetFormat, rawFormat)) {
                    try (AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, rawStream)) {
                        Clip clip = AudioSystem.getClip();
                        clip.open(converted);
                        return clip;
                    }
                }
                Clip clip = AudioSystem.getClip();
                clip.open(rawStream);
                return clip;
            }
        }

        private static AudioFormat clipFriendlyFormat(AudioFormat rawFormat) {
            if (AudioFormat.Encoding.PCM_SIGNED.equals(rawFormat.getEncoding())
                    && rawFormat.getSampleSizeInBits() >= 16) {
                return rawFormat;
            }
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
                if (audioFormat == 0x11) {
                    return WAVYamahaADPCMDecoder.ADPCMBDecode(payload, sampleRate, channels);
                }
                throw new MediaException("Unsupported WAV encoding: " + audioFormat);
            } catch (IOException exception) {
                throw new MediaException("Failed to decode legacy WAV audio.", exception);
            }
        }
    }
}
