package remexa.audio.spatial;

/** Analytic elevation cue; not the original Yamaha 23-tap HRTF tables. */
final class PinnaFilter {
    private final int sampleRate;
    private final double smoothing;
    private final double[] coefficients = new double[5];
    private final double[] target = new double[5];
    private final double[] z1 = new double[2], z2 = new double[2];
    private boolean initialized;

    PinnaFilter(int sampleRate) {
        this.sampleRate = sampleRate;
        smoothing = 1 - Math.exp(-1.0 / (sampleRate * .005));
    }

    void direction(double elevation, double rear) {
        // A broad, shallow notch moves upward with elevation. Scale it below Nyquist
        // for the 8/16 kHz speech sources common in handset applications.
        double frequency = Math.min(sampleRate * .45,
                Math.min(8500, sampleRate * .32) * (1 + elevation * .25 - rear * .15));
        double omega = 2 * Math.PI * frequency / sampleRate;
        double alpha = Math.sin(omega) / 4;
        double amplitude = .707945784; // -6 dB peaking EQ
        double a0 = 1 + alpha / amplitude;
        target[0] = (1 + alpha * amplitude) / a0;
        target[1] = -2 * Math.cos(omega) / a0;
        target[2] = (1 - alpha * amplitude) / a0;
        target[3] = target[1];
        target[4] = (1 - alpha / amplitude) / a0;
        if (!initialized) {
            System.arraycopy(target, 0, coefficients, 0, 5);
            initialized = true;
        }
    }

    void advance() {
        for (int i = 0; i < 5; i++) coefficients[i] += (target[i] - coefficients[i]) * smoothing;
    }

    float tick(float input, int channel) {
        double output = coefficients[0] * input + z1[channel];
        z1[channel] = coefficients[1] * input - coefficients[3] * output + z2[channel];
        z2[channel] = coefficients[2] * input - coefficients[4] * output;
        return (float) output;
    }

    void reset() {
        z1[0] = z1[1] = z2[0] = z2[1] = 0;
        initialized = false;
    }
}
