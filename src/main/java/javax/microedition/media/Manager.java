package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.microedition.media.control.VolumeControl;
import remexa.audio.smaf.SmafPlayback;

public final class Manager {
    private static final String CONTROL_PACKAGE = "javax.microedition.media.control.";

    private Manager() {
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (stream == null) {
            throw new IllegalArgumentException("stream");
        }
        byte[] source = readAllBytes(stream);
        String normalizedType = normalizeContentType(type);
        if (isSmafType(normalizedType, source)) {
            return new SmafPlayer(source, normalizedType.isEmpty() ? "application/x-smaf" : normalizedType);
        }
        if (isMidiType(normalizedType, source)) {
            return new MidiPlayer(source, normalizedType.isEmpty() ? "audio/midi" : normalizedType);
        }
        throw new MediaException("Unsupported media type: " + type);
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
        private final PlayerVolumeControl volumeControl = new PlayerVolumeControl(this);
        private final Control[] controls = new Control[]{volumeControl};

        private int state = UNREALIZED;
        private int loopCount = 1;

        private AbstractPlayer(String contentType) {
            this.contentType = contentType;
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
        }

        @Override
        public synchronized void stop() throws MediaException {
            ensureNotClosed();
            if (state != STARTED) {
                return;
            }
            doStop();
            state = PREFETCHED;
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
        }

        @Override
        public synchronized void setLoopCount(int count) {
            if (count == 0 || count < -1) {
                throw new IllegalArgumentException("Invalid loop count: " + count);
            }
            if (state == STARTED || state == CLOSED) {
                throw new IllegalStateException("Cannot change loop count in the current player state.");
            }
            loopCount = count;
            applyLoopCount(count);
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

        final void applyVolumeControl() throws MediaException {
            onVolumeChanged();
        }

        private void ensureNotClosed() {
            if (state == CLOSED) {
                throw new IllegalStateException("Player is closed.");
            }
        }

        protected void applyLoopCount(int count) {
        }

        protected void doPrefetch() throws MediaException {
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
                sequencer.setLoopCount(count < 0 ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, count - 1));
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
                pausedByStop = false;
                onVolumeChanged();
            } catch (Exception exception) {
                closeQuietly();
                throw new MediaException("Failed to realize SMAF player.", exception);
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
                    playback.play(loopCount() < 0 ? 0 : loopCount());
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

        private void closeQuietly() {
            if (playback != null) {
                playback.close();
                playback = null;
            }
            pausedByStop = false;
        }
    }
}
