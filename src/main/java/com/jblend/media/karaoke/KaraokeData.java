package com.jblend.media.karaoke;

public class KaraokeData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "karaoke";
    private static final int DEFAULT_WIDTH = 240;
    private static final int DEFAULT_HEIGHT = 260;
    private com.jblend.media.karaoke.ReferenceScore referenceScore;

    public KaraokeData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "KaraokeData");
    }

    public KaraokeData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "KaraokeData", name);
    }

    public KaraokeData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "KaraokeData", data);
    }

    public int getContentType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getContentType");
        return 0;
    }

    public int getTagStart (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getTagStart", tag);
        return 0;
    }

    public int getTagEnd (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getTagEnd", tag);
        return 0;
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getMediaType");
        return type;
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "setData", data);
        super.setData(data);
        referenceScore = null;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getWidth");
        return DEFAULT_WIDTH;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getHeight");
        return DEFAULT_HEIGHT;
    }

    public com.jblend.media.karaoke.ReferenceScore getReferenceScore () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokeData", "getReferenceScore");
        if (referenceScore == null) {
            referenceScore = com.jblend.media.karaoke.ReferenceScore.empty();
        }
        return referenceScore;
    }
}
