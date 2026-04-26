package com.jblend.media.jpeg;

public class JpegData extends com.jblend.media.MediaData {
    public static final java.lang.String type = "jpeg";

    public JpegData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "JpegData");
    }

    public JpegData (java.lang.String name) throws java.io.IOException {
        super(name);
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "JpegData", name);
    }

    public JpegData (byte[] data) {
        super(data);
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "JpegData", data);
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "getHeight");
        return 0;
    }

    public java.lang.String getMediaType () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "getMediaType");
        return type;
    }

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegData", "setData", data);
        super.setData(data);
    }
}
