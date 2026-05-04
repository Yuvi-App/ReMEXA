package remexa.audio.smaf;

interface SmafStreamingSession extends AutoCloseable {
    int sampleRate();

    int channelCount();

    int render(float[] output, int maxFrames) throws Exception;

    void rewind() throws Exception;

    /**
     * Configures whether the session should end at the SEQU-defined sequence
     * boundary (tight loop) or render the natural FM tail. Tight mode is used
     * for all but the final iteration of a looping playback so that parallel
     * tracks with matching SEQU lengths stay sample-aligned across loops.
     */
    default void setLoopMode(boolean tightLoop) throws Exception {
    }

    @Override
    default void close() throws Exception {
    }
}
