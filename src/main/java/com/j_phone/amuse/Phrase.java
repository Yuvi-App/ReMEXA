package com.j_phone.amuse;

public class Phrase {
    private final com.jblend.media.smaf.phrase.Phrase delegate;

    protected Phrase() {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.Phrase", "Phrase");
        delegate = com.jblend.media.smaf.phrase.Phrase.unchecked(new byte[0]);
    }

    public Phrase (java.lang.String url) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.Phrase", "Phrase", url);
        delegate = new com.jblend.media.smaf.phrase.Phrase(url);
    }

    public Phrase (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.Phrase", "Phrase", data);
        delegate = new com.jblend.media.smaf.phrase.Phrase(data);
    }


    public int getSize () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.Phrase", "getSize");
        return delegate.getSize();
    }

    public int getUseTracks () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.Phrase", "getUseTracks");
        return delegate.getUseTracks();
    }

    com.jblend.media.smaf.phrase.Phrase delegate() {
        return delegate;
    }
}
