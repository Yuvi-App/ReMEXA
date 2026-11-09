package remexa.audio.smaf.ma5;

import java.io.IOException;
import java.io.InputStream;
import remexa.audio.smaf.ma3.MA3Rom;

public final class MA5WaveRom {
    public static final int WAVE_COUNT = 29;

    private MA5WaveRom() { }

    public static byte[] encoded(int id) {
        if (id < 0 || id >= WAVE_COUNT) {
            throw new IndexOutOfBoundsException("Invalid PCM ROM wave: " + id);
        }
        if (id < 7) {
            return MA3Rom.MA3_WAVEROM.bytes(id);
        }
        String resource = String.format("rom/wave-%02x.adpcm", id);
        try (InputStream input = MA5WaveRom.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing PCM ROM wave: " + id);
            }
            byte[] data = input.readAllBytes();
            if (data.length == 0) {
                throw new IllegalStateException("Empty PCM ROM wave: " + id);
            }
            return data;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read PCM ROM wave: " + id, e);
        }
    }
}
