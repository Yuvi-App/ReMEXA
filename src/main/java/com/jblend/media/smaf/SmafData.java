package com.jblend.media.smaf;

public class SmafData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "smaf";

    public SmafData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "SmafData");
    }

    public SmafData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "SmafData", name);
    }

    public SmafData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "SmafData", data);
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "setData", data);
        super.setData(data);
    }

    public int getContentType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getContentType");
        return 0;
    }

    public int getTagStart (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getTagStart", tag);
        return 0;
    }

    public int getTagEnd (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getTagEnd", tag);
        return 0;
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getMediaType");
        return type;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafData", "getHeight");
        return 0;
    }
}
