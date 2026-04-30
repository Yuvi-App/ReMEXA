package remexa.audio.smaf;

interface SmafStreamingSession extends AutoCloseable {
    int sampleRate();

    int channelCount();

    int render(float[] output, int maxFrames) throws Exception;

    void rewind() throws Exception;

    @Override
    default void close() throws Exception {
    }
}
