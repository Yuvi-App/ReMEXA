package remexa.audio.spatial;

import java.util.Arrays;

/** Stereo feedback delay network. Native decay times; software room coloration (not a Yamaha DSP port). */
final class RoomReverb {
    private static final double[][] PROFILES = {
            {2.4, .45, .060}, {.75, .12, .010}, {2.0, .18, .050}, {1.5, .50, .050},
            {1.6, .35, .020}, {1.2, .75, .025}, {2.8, .50, .090}, {1, .4, 0},
            {.65, .40, .005}, {1, .90, .008}
    };
    private static final int[] DELAYS = {1493, 1601, 1747, 1867, 1999, 2137, 2281, 2423};
    private final int sampleRate;
    private final float[][] lines = new float[8][];
    private final int[] indices = new int[8];
    private final float[] filtered = new float[8];
    private final float[] feedback = new float[8];
    private float[] preDelay;
    private int preIndex;
    private int preset = -1;
    private int decayMillis;
    private float damping;
    float left;
    float right;

    RoomReverb(int sampleRate) { this.sampleRate = sampleRate; }

    void configure(int preset, int decayMillis) {
        if (this.preset == preset && this.decayMillis == decayMillis) return;
        if (this.preset != preset) {
            double[] profile = PROFILES[preset];
            for (int i = 0; i < 8; i++) {
                lines[i] = new float[Math.max(1, (int) Math.round(DELAYS[i] * profile[0] * sampleRate / 44100))];
            }
            preDelay = new float[Math.max(1, (int) Math.round(profile[2] * sampleRate))];
            // Preserve the damping corner across sample rates.
            damping = (float) Math.pow(profile[1], 44100.0 / sampleRate);
            this.preset = preset;
            reset();
        }
        this.decayMillis = decayMillis;
        for (int i = 0; i < 8; i++) {
            feedback[i] = (float) Math.pow(.001, lines[i].length / (sampleRate * decayMillis / 1000.0));
        }
    }

    void tick(float input) {
        float delayed = preDelay[preIndex];
        preDelay[preIndex] = input;
        if (++preIndex == preDelay.length) preIndex = 0;
        float sum = 0;
        left = right = 0;
        for (int i = 0; i < 8; i++) {
            float value = lines[i][indices[i]];
            filtered[i] = value * (1 - damping) + filtered[i] * damping;
            sum += filtered[i];
            left += value * ((i & 1) == 0 ? .25f : -.25f);
            right += value * ((i & 2) == 0 ? .25f : -.25f);
        }
        // Householder reflection: orthogonal mixing keeps the network stable for any RT60.
        for (int i = 0; i < 8; i++) {
            lines[i][indices[i]] = delayed * .25f + (filtered[i] - sum * .25f) * feedback[i];
            if (++indices[i] == lines[i].length) indices[i] = 0;
        }
    }

    int tailFrames() { return (int) (sampleRate * (decayMillis / 1000.0 * 1.25 + .4)); }

    void reset() {
        for (float[] line : lines) if (line != null) Arrays.fill(line, 0);
        if (preDelay != null) Arrays.fill(preDelay, 0);
        Arrays.fill(indices, 0);
        Arrays.fill(filtered, 0);
        preIndex = 0;
        left = right = 0;
    }
}
