package com.jblend.media.smaf;

public class SmafPlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    public SmafPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "SmafPlayer");
    }

    public SmafPlayer (com.jblend.media.smaf.SmafData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "SmafPlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public SmafPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "SmafPlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.smaf.SmafData(data));
        }
    }

    public void setData (com.jblend.media.smaf.SmafData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setData", data);
        super.setData(data);
    }

    public int getCurrent () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getCurrent");
        return 0;
    }

    public void seek (int time) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "seek", time);
    }

    public void setTranspose (int shift) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setTranspose", shift);
    }

    public int getTranspose () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getTranspose");
        return 0;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getVolume");
        return 0;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setVolume", volume);
    }

    public int getSpeed () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getSpeed");
        return 0;
    }

    public void setSpeed (int speed) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setSpeed", speed);
    }

    public void setPlayEnd (int pos) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setPlayEnd", pos);
    }

    public int getPlayEnd () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getPlayEnd");
        return 0;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setBounds", x, y, width, height);
    }

    public void addSmafPlayerListener (com.jblend.media.smaf.SmafPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "addSmafPlayerListener", l);
    }

    public void removeSmafPlayerListener (com.jblend.media.smaf.SmafPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "removeSmafPlayerListener", l);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getX");
        return 0;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getY");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getHeight");
        return 0;
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getOriginX");
        return 0;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getOriginY");
        return 0;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setOrigin", offset_x, offset_y);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getMediaWidth");
        var data = currentData();
        return data instanceof com.jblend.media.smaf.SmafData smaf ? smaf.getWidth() : 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getMediaHeight");
        var data = currentData();
        return data instanceof com.jblend.media.smaf.SmafData smaf ? smaf.getHeight() : 0;
    }
}
