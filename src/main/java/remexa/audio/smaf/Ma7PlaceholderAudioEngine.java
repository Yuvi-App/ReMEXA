package remexa.audio.smaf;

import org.recompile.mobile.Mobile;

import java.io.IOException;

final class Ma7PlaceholderAudioEngine implements YamahaAudioEngine {
    @Override
    public String id() {
        return "ma7";
    }

    @Override
    public String label() {
        return "MA7 placeholder";
    }

    @Override
    public SmafRenderedAudio render(SmafRenderContext context) throws Exception {
        Mobile.log(Mobile.LOG_WARNING,
                "MA7 SMAF data detected, but the MA7 renderer is not implemented yet. "
                        + "Falling back to host MIDI output.");
        throw new IOException("MA7 rendering is not implemented yet");
    }
}
