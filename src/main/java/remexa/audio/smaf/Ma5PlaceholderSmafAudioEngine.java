package remexa.audio.smaf;

import java.io.IOException;

import org.recompile.mobile.Mobile;

final class Ma5PlaceholderSmafAudioEngine implements YamahaAudioEngine {
    Ma5PlaceholderSmafAudioEngine() {
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
                "MA5 backend is not rebuilt yet; rendered MA5 playback is unavailable.");
        throw new IOException("MA5 backend is not rebuilt yet.");
    }
}
