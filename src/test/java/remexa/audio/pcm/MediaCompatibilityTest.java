package remexa.audio.pcm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class MediaCompatibilityTest {
    @Test public void phraseMimeTypeUsesTheSamePlayerAsStandardSmaf() throws Exception {
        // A minimal empty SMAF container, with no proprietary game assets.
        byte[] source = {'M', 'M', 'M', 'D', 0, 0, 0, 2, 0, 0};
        Player standard = Manager.createPlayer(new ByteArrayInputStream(source), "application/x-smaf");
        Player phrase = Manager.createPlayer(new ByteArrayInputStream(source), "application/x-smaf-phrase");
        try {
            assertEquals(standard.getClass(), phrase.getClass());
        } finally {
            standard.close();
            phrase.close();
        }
    }

    @Test public void pcmTimingUsesMicrosecondsForTheApplicationStoryClock() throws Exception {
        Player player = Manager.createPlayer(new ByteArrayInputStream(wave()), "audio/x-wav");
        try {
            player.realize();
            assertEquals(1000000L, player.getDuration());
            // Simulate a completed/paused playback without relying on a physical sound device or a timer.
            var cachedTime = player.getClass().getDeclaredField("cachedMediaTimeMillis");
            cachedTime.setAccessible(true);
            cachedTime.setLong(player, 750L);
            assertEquals(750000L, player.getMediaTime());
        } finally {
            player.close();
        }
    }

    @Test public void vlcDecodingProducesNonSilentPcmWithoutPlayingIt() throws Exception {
        assumeTrue("Enable with -Dremexa.test.vlc=true on hosts with VLC 3.x", Boolean.getBoolean("remexa.test.vlc"));
        RenderedPcmAudio audio = VlcAudioDecoder.decode(wave());
        assertEquals(2, audio.channelCount());
        assertEquals(8000, audio.sampleRate());
        // VLC 3.x's transcode path can retain its final 50 ms packet on EOF.
        assertTrue("Unexpected decoded duration: " + audio.frameCount(),
                audio.frameCount() >= 7600 && audio.frameCount() <= 8000);
        int peak = 0;
        for (int i = 0; i < audio.pcm16Le().length; i += 2) {
            peak = Math.max(peak, Math.abs((short) ((audio.pcm16Le()[i] & 255) | (audio.pcm16Le()[i + 1] << 8))));
        }
        assertTrue("Decoded signal was silent", peak > 1000);
    }

    private static byte[] wave() throws Exception {
        int frames = 8000;
        byte[] pcm = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            int sample = (int) (8000 * Math.sin(2 * Math.PI * 440 * i / 8000));
            pcm[2 * i] = (byte) sample;
            pcm[2 * i + 1] = (byte) (sample >> 8);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var stream = new AudioInputStream(new ByteArrayInputStream(pcm),
                new AudioFormat(8000, 16, 1, true, false), frames)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, bytes);
        }
        return bytes.toByteArray();
    }
}
