package remexa.audio.smaf.fuetrek;

import remexa.audio.smaf.fuetrek.Sampler;
import remexa.audio.smaf.fuetrek.SamplerProvider;

/**
 * FueTrek-oriented sampler profile validated against the authoritative lib002
 * stack (`MFiSoundLibMFi5.dll` plus `MFiSynth_ft.dll`), with
 * `SH_MFi4PlugIn.dll` retained as the matching plugin-side ABI wrapper.
 */
public final class FueTrekSamplerProvider implements SamplerProvider {
    public static final float SAMPLE_RATE = 32000.0f;
    public static final int MAX_POLYPHONY = 64;
    public static final int MIN_FRAME_SIZE = 128;
    public static final int MAX_FRAME_SIZE = 4096;
    public static final int FRAME_GRANULARITY = 128;

    private final FueTrekRom rom = FueTrekRom.load();

    @Override
    public Sampler instance(float sampleRate) {
        return new FueTrekSampler(rom, sampleRate, MAX_POLYPHONY);
    }
}
