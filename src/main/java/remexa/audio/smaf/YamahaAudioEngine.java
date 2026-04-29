package remexa.audio.smaf;

interface YamahaAudioEngine {
    String id();

    String label();

    SmafRenderedAudio render(SmafRenderContext context) throws Exception;
}
