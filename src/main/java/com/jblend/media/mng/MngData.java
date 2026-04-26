package com.jblend.media.mng;

public class MngData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "mng";

    public MngData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "MngData");
    }

    public MngData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "MngData", name);
    }

    public MngData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "MngData", data);
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "getHeight");
        return 0;
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "getMediaType");
        return type;
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngData", "setData", data);
        super.setData(data);
    }
}
