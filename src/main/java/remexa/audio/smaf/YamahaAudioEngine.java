package remexa.audio.smaf;

interface YamahaAudioEngine {
    String id();

    String label();

    SmafRenderedAudio render(SmafRenderContext context) throws Exception;

    default SmafStreamingSession openStream(SmafRenderContext context) throws Exception {
        return new BufferedSmafStreamingSession(render(context));
    }
}
