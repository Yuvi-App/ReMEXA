package remexa.audio.pcm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import remexa.host.media.VlcRuntime;

/** Decode local MP4/3GP audio to the PCM mixer, including AMR narration in V-Appli. */
public final class VlcAudioDecoder {
    private static final int CHANNELS = 2;
    private static final long MAX_PCM_BYTES = 128L * 1024 * 1024;

    private VlcAudioDecoder() { }

    public static RenderedPcmAudio decode(byte[] source) throws IOException {
        VlcRuntime.configure();
        Path input = Files.createTempFile("remexa-audio-", ".3gp");
        Path output = null;
        MediaPlayerFactory factory = null;
        MediaPlayer player = null;
        try {
            output = Files.createTempFile("remexa-audio-", ".wav");
            Files.write(input, source);
            factory = new MediaPlayerFactory("--no-video", "--no-osd", "--quiet");
            player = factory.mediaPlayers().newMediaPlayer();
            CountDownLatch finished = new CountDownLatch(1);
            AtomicBoolean failed = new AtomicBoolean();
            player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override public void finished(MediaPlayer mediaPlayer) {
                    finished.countDown();
                }

                @Override public void error(MediaPlayer mediaPlayer) {
                    failed.set(true);
                    finished.countDown();
                }
            });
            String destination = output.toAbsolutePath().toString().replace('\\', '/').replace("'", "\\'");
            String transcode = ":sout=#transcode{acodec=s16l,channels=" + CHANNELS
                    + "}:std{access=file,mux=wav,dst='" + destination + "'}";
            if (!player.media().play(input.toString(), transcode, ":no-sout-video")) {
                throw new IOException("VLC could not open compressed audio.");
            }
            // Transcoding is faster than real time and never opens a sound device.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!finished.await(100, TimeUnit.MILLISECONDS)) {
                if (Files.size(output) > MAX_PCM_BYTES || System.nanoTime() >= deadline) {
                    throw new IOException("Compressed audio exceeds the decode size or time limit.");
                }
            }
            player.controls().stop();
            if (failed.get()) {
                throw new IOException("VLC failed to decode compressed audio.");
            }
            long size = Files.size(output);
            if (size == 0 || size > MAX_PCM_BYTES) {
                throw new IOException("VLC produced invalid PCM audio (" + size + " bytes).");
            }
            // The WAV header reports the source rate, avoiding an unnecessary resampling stage.
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(output.toFile())) {
                AudioFormat format = stream.getFormat();
                if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                        || format.getSampleSizeInBits() != 16 || format.isBigEndian()
                        || format.getChannels() != CHANNELS) {
                    throw new IOException("Unexpected decoded audio format: " + format);
                }
                byte[] pcm = stream.readAllBytes();
                if (pcm.length == 0 || pcm.length % format.getFrameSize() != 0) {
                    throw new IOException("VLC produced incomplete PCM frames.");
                }
                return new RenderedPcmAudio(Math.round(format.getSampleRate()), CHANNELS,
                        pcm.length / format.getFrameSize(), pcm);
            }
        } catch (UnsupportedAudioFileException exception) {
            throw new IOException("VLC produced invalid WAV audio.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Audio decoding was interrupted.", exception);
        } catch (RuntimeException | LinkageError exception) {
            throw new IOException("VLC 3.x is required to decode MP4/3GP audio.", exception);
        } finally {
            try {
                if (player != null) player.release();
            } finally {
                try {
                    if (factory != null) factory.release();
                } finally {
                    Files.deleteIfExists(input);
                    if (output != null) Files.deleteIfExists(output);
                }
            }
        }
    }
}
