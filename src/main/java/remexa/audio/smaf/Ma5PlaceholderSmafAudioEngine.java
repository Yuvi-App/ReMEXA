package remexa.audio.smaf;

import org.recompile.mobile.Mobile;

final class Ma5PlaceholderSmafAudioEngine implements YamahaAudioEngine {
    private final LegacySmafAudioEngine legacy;

    Ma5PlaceholderSmafAudioEngine(LegacySmafAudioEngine legacy) {
        this.legacy = legacy;
    }

    @Override
    public String id() {
        return "ma5";
    }

    @Override
    public String label() {
        return "MA5 placeholder";
    }

    @Override
    public SmafRenderedAudio render(SmafRenderContext context) throws Exception {
        Mobile.log(Mobile.LOG_WARNING,
                "MA5 backend is not rebuilt yet; using legacy FueTrek transition renderer for A/B comparison.");
        return legacy.render(context);
    }
}
