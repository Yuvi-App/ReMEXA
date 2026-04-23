package remexa.audio.smaf;

import java.util.List;
import java.util.Objects;

public record SmafRenderedAudio(int sampleRate, int channelCount, int frameCount, byte[] pcm16Le) {
    public static SmafRenderedAudio mix(List<Layer> layers) {
        Objects.requireNonNull(layers, "layers");
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("At least one rendered SMAF layer is required");
        }

        SmafRenderedAudio reference = null;
        int maxFrames = 1;
        for (Layer layer : layers) {
            if (layer == null || layer.audio() == null) {
                continue;
            }
            if (reference == null) {
                reference = layer.audio();
            } else if (reference.sampleRate() != layer.audio().sampleRate()
                    || reference.channelCount() != layer.audio().channelCount()) {
                throw new IllegalArgumentException("Rendered SMAF layers must share the same format");
            }
            maxFrames = Math.max(maxFrames, layer.audio().frameCount());
        }

        if (reference == null) {
            throw new IllegalArgumentException("No rendered SMAF audio was provided");
        }

        int channels = reference.channelCount();
        float[] mix = new float[maxFrames * channels];
        for (Layer layer : layers) {
            if (layer == null || layer.audio() == null || layer.gain() <= 0.0f) {
                continue;
            }
            float[] channelGains = channelGains(layer.gain(), layer.panpot(), channels);
            byte[] pcm = layer.audio().pcm16Le();
            int inputOffset = 0;
            for (int frame = 0; frame < layer.audio().frameCount(); frame++) {
                int outputOffset = frame * channels;
                for (int channel = 0; channel < channels; channel++) {
                    mix[outputOffset + channel] += (readSample(pcm, inputOffset) / 32768.0f) * channelGains[channel];
                    inputOffset += 2;
                }
            }
        }

        return new SmafRenderedAudio(reference.sampleRate(), channels, maxFrames, encodePcm16Le(mix));
    }

    public record Layer(SmafRenderedAudio audio, float gain, int panpot) {
        public Layer {
            Objects.requireNonNull(audio, "audio");
            gain = Math.max(0.0f, gain);
            panpot = Math.max(0, Math.min(127, panpot));
        }
    }

    private static float[] channelGains(float gain, int panpot, int channelCount) {
        if (channelCount <= 1) {
            return new float[]{gain};
        }
        float pan = Math.max(-1.0f, Math.min(1.0f, (panpot - 64.0f) / 63.0f));
        float leftGain = gain * (pan > 0.0f ? 1.0f - pan : 1.0f);
        float rightGain = gain * (pan < 0.0f ? 1.0f + pan : 1.0f);
        float[] gains = new float[channelCount];
        gains[0] = leftGain;
        gains[1] = rightGain;
        for (int channel = 2; channel < channelCount; channel++) {
            gains[channel] = gain;
        }
        return gains;
    }

    private static byte[] encodePcm16Le(float[] mix) {
        byte[] output = new byte[mix.length * 2];
        int offset = 0;
        for (float sample : mix) {
            int encoded = Math.round(sample * 32767.0f);
            writeSample(output, offset, encoded);
            offset += 2;
        }
        return output;
    }

    private static short readSample(byte[] input, int offset) {
        int low = input[offset] & 0xFF;
        int high = input[offset + 1];
        return (short) ((high << 8) | low);
    }

    private static void writeSample(byte[] output, int offset, int sample) {
        int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        output[offset] = (byte) (clamped & 0xFF);
        output[offset + 1] = (byte) ((clamped >>> 8) & 0xFF);
    }
}
