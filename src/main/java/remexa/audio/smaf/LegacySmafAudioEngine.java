package remexa.audio.smaf;

final class LegacySmafAudioEngine implements YamahaAudioEngine {
    private final SmafSequencedRenderer renderer;

    LegacySmafAudioEngine() {
        renderer = SmafSequencedRenderer.legacyFueTrek();
    }

    @Override
    public String id() {
        return "legacy";
    }

    @Override
    public String label() {
        return "Legacy FueTrek";
    }

    @Override
    public SmafRenderedAudio render(SmafRenderContext context) throws Exception {
        return renderer.render(
                context.sequence(),
                context.sequenceSysExEvents(),
                context.startupPackets(),
                context.pcmClipData(),
                context.pcmTriggers());
    }
}
