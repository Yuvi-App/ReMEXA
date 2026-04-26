package com.jblend.media.png;

public class PngData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "png";

    public PngData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "PngData");
    }

    public PngData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "PngData", name);
    }

    public PngData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "PngData", data);
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "getHeight");
        return 0;
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "getMediaType");
        return type;
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngData", "setData", data);
        super.setData(data);
    }
}
