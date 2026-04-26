package com.jblend.media.png;

public class PngPlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    public PngPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "PngPlayer");
    }

    public PngPlayer (com.jblend.media.png.PngData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "PngPlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public PngPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "PngPlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.png.PngData(data));
        }
    }

    public void setData (com.jblend.media.png.PngData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "setData", data);
        super.setData(data);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getX");
        return 0;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getY");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getHeight");
        return 0;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "setBounds", x, y, width, height);
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getOriginX");
        return 0;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getOriginY");
        return 0;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "setOrigin", offset_x, offset_y);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getMediaWidth");
        var data = currentData();
        return data instanceof com.jblend.media.png.PngData png ? png.getWidth() : 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.png.PngPlayer", "getMediaHeight");
        var data = currentData();
        return data instanceof com.jblend.media.png.PngData png ? png.getHeight() : 0;
    }
}
