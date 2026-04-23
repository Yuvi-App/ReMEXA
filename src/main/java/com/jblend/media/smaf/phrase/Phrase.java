package com.jblend.media.smaf.phrase;

import java.io.IOException;
import remexa.host.runtime.MidletRuntime;

public class Phrase extends PhraseBase {
    private final byte[] data;

    protected Phrase() {
        this.data = new byte[0];
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.Phrase", "Phrase");
    }

    public Phrase(String url) throws IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.Phrase", "Phrase", url);
        try (var stream = MidletRuntime.openResource(url)) {
            if (stream == null) {
                throw new IOException("Phrase resource not found: " + url);
            }
            this.data = stream.readAllBytes();
        }
    }

    public Phrase(byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.Phrase", "Phrase", data);
        this.data = data == null ? new byte[0] : data.clone();
    }

    public int getSize() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.Phrase", "getSize");
        return data.length;
    }

    public int getUseTracks() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.Phrase", "getUseTracks");
        return 1;
    }

    byte[] data() {
        return data.clone();
    }
}
