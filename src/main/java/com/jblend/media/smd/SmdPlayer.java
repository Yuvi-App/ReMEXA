package com.jblend.media.smd;

public class SmdPlayer extends com.jblend.media.MediaPlayer {
    public SmdPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer");
    }

    public SmdPlayer (com.jblend.media.smd.SmdData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public SmdPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.smd.SmdData(data));
        }
    }

    public void setData (com.jblend.media.smd.SmdData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "setData", data);
        super.setData(data);
    }

    public int getCurrent () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "getCurrent");
        return 0;
    }

    public void setTone (int tone) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "setTone", tone);
    }

    public int getTone () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "getTone");
        return 0;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "getVolume");
        return 0;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "setVolume", volume);
    }
}
