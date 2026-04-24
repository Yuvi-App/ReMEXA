package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

public final class Manager {
    private Manager() {
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (stream == null) {
            throw new IllegalArgumentException("stream");
        }
        String normalizedType = type == null ? "" : type.trim().toLowerCase();
        if (normalizedType.isEmpty()
                || normalizedType.equals("audio/midi")
                || normalizedType.equals("audio/x-midi")
                || normalizedType.equals("audio/sp-midi")
                || normalizedType.equals("audio/mid")) {
            return new MidiPlayer(readAllBytes(stream), normalizedType.isEmpty() ? "audio/midi" : normalizedType);
        }
        throw new MediaException("Unsupported media type: " + type);
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

    private static final class MidiPlayer implements Player {
        private final byte[] source;
        private final String contentType;
        private Sequencer sequencer;
        private int state = UNREALIZED;
        private int loopCount = 1;

        private MidiPlayer(byte[] source, String contentType) {
            this.source = source;
            this.contentType = contentType;
        }

        @Override
        public synchronized void realize() throws MediaException {
            if (state == CLOSED) {
                throw new MediaException("Player is closed.");
            }
            if (state >= REALIZED) {
                return;
            }
            try {
                sequencer = MidiSystem.getSequencer();
                sequencer.open();
                sequencer.setSequence(MidiSystem.getSequence(new ByteArrayInputStream(source)));
                state = REALIZED;
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException exception) {
                closeQuietly();
                throw new MediaException("Failed to realize MIDI player.", exception);
            }
        }

        @Override
        public synchronized void prefetch() throws MediaException {
            if (state == CLOSED) {
                throw new MediaException("Player is closed.");
            }
            realize();
            state = PREFETCHED;
        }

        @Override
        public synchronized void start() throws MediaException {
            if (state == CLOSED) {
                throw new MediaException("Player is closed.");
            }
            prefetch();
            try {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0L);
                sequencer.setLoopStartPoint(0L);
                sequencer.setLoopEndPoint(-1L);
                sequencer.setLoopCount(loopCount < 0 ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, loopCount - 1));
                sequencer.start();
                state = STARTED;
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to start MIDI player.", exception);
            }
        }

        @Override
        public synchronized void stop() throws MediaException {
            if (state == CLOSED) {
                return;
            }
            if (sequencer == null) {
                return;
            }
            try {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0L);
                state = PREFETCHED;
            } catch (RuntimeException exception) {
                throw new MediaException("Failed to stop MIDI player.", exception);
            }
        }

        @Override
        public synchronized void deallocate() {
            if (state == CLOSED) {
                return;
            }
            closeQuietly();
            state = UNREALIZED;
        }

        @Override
        public synchronized void close() {
            closeQuietly();
            state = CLOSED;
        }

        @Override
        public synchronized void setLoopCount(int count) {
            loopCount = count;
            if (sequencer != null && state != CLOSED) {
                sequencer.setLoopCount(count < 0 ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, count - 1));
            }
        }

        @Override
        public synchronized int getState() {
            if (state == STARTED && sequencer != null && !sequencer.isRunning()) {
                state = PREFETCHED;
            }
            return state;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public Control getControl(String controlType) {
            return null;
        }

        @Override
        public Control[] getControls() {
            return new Control[0];
        }

        private void closeQuietly() {
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
        }
    }
}
