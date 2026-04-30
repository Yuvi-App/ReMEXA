package remexa.audio.smaf;

final class BufferedSmafStreamingSession implements SmafStreamingSession {
    private final SmafRenderedAudio audio;
    private int framePosition;

    BufferedSmafStreamingSession(SmafRenderedAudio audio) {
        this.audio = audio;
    }

    @Override
    public int sampleRate() {
        return audio.sampleRate();
    }

    @Override
    public int channelCount() {
        return audio.channelCount();
    }

    @Override
    public int render(float[] output, int maxFrames) {
        if (maxFrames <= 0) {
            return 0;
        }
        int available = audio.frameCount() - framePosition;
        if (available <= 0) {
            return 0;
        }
        int frames = Math.min(maxFrames, available);
        mixIntoBuffer(audio.pcm16Le(), framePosition, frames, audio.channelCount(), output);
        framePosition += frames;
        return frames;
    }

    @Override
    public void rewind() {
        framePosition = 0;
    }

    private static void mixIntoBuffer(byte[] pcm,
                                      int startFrame,
                                      int frames,
                                      int channelCount,
                                      float[] output) {
        int inputOffset = startFrame * channelCount * 2;
        int outputOffset = 0;
        for (int frame = 0; frame < frames; frame++) {
            for (int channel = 0; channel < channelCount; channel++) {
                output[outputOffset++] = readSample(pcm, inputOffset) / 32768.0f;
                inputOffset += 2;
            }
        }
    }

    private static short readSample(byte[] input, int offset) {
        int low = input[offset] & 0xFF;
        int high = input[offset + 1];
        return (short) ((high << 8) | low);
    }
}
