package com.jblend.media.smd;

public class SmdData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "smd";

    public SmdData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdData", "SmdData");
    }

    public SmdData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdData", "SmdData", name);
    }

    public SmdData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdData", "SmdData", data);
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdData", "getMediaType");
        return type;
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdData", "setData", data);
        super.setData(data);
    }
}
