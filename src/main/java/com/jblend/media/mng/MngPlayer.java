package com.jblend.media.mng;

public class MngPlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    public MngPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "MngPlayer");
    }

    public MngPlayer (com.jblend.media.mng.MngData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "MngPlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public MngPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "MngPlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.mng.MngData(data));
        }
    }

    public void setData (com.jblend.media.mng.MngData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "setData", data);
        super.setData(data);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getX");
        return 0;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getY");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getHeight");
        return 0;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "setBounds", x, y, width, height);
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getOriginX");
        return 0;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getOriginY");
        return 0;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "setOrigin", offset_x, offset_y);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getMediaWidth");
        return 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "getMediaHeight");
        return 0;
    }

    public void repaintCurrent () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.mng.MngPlayer", "repaintCurrent");
    }
}
