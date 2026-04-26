package com.jblend.media.jpeg;

public class JpegPlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    public JpegPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "JpegPlayer");
    }

    public JpegPlayer (com.jblend.media.jpeg.JpegData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "JpegPlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public JpegPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "JpegPlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.jpeg.JpegData(data));
        }
    }

    public void setData (com.jblend.media.jpeg.JpegData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "setData", data);
        super.setData(data);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getX");
        return 0;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getY");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getHeight");
        return 0;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "setBounds", x, y, width, height);
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getOriginX");
        return 0;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getOriginY");
        return 0;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "setOrigin", offset_x, offset_y);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getMediaWidth");
        var data = currentData();
        return data instanceof com.jblend.media.jpeg.JpegData jpeg ? jpeg.getWidth() : 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.jpeg.JpegPlayer", "getMediaHeight");
        var data = currentData();
        return data instanceof com.jblend.media.jpeg.JpegData jpeg ? jpeg.getHeight() : 0;
    }
}
