package com.jblend.media.smaf.phrase;

public class AudioPhrase extends com.jblend.media.smaf.phrase.PhraseBase {
    protected AudioPhrase() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhrase", "AudioPhrase");
    }

    public AudioPhrase (java.lang.String url) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhrase", "AudioPhrase", url);
    }

    public AudioPhrase (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhrase", "AudioPhrase", data);
    }


    public int getSize () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhrase", "getSize");
        return 0;
    }

    public int getUseTracks () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhrase", "getUseTracks");
        return 0;
    }
}
