package com.jblend.media.smd;

public class SmdPlayer extends com.jblend.media.MediaPlayer {
    public SmdPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer");
    }

    public SmdPlayer (com.jblend.media.smd.SmdData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer", data);
    }

    public SmdPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "SmdPlayer", data);
    }


    public void setData (com.jblend.media.smd.SmdData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "setData", data);
    }

    public void setData (com.jblend.media.MediaData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "setData", data);
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

    public void play () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "play");
    }

    public void play (boolean isRepeat) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "play", isRepeat);
    }

    public void play (int count) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "play", count);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "stop");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "resume");
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "getState");
        return 0;
    }

    public void addMediaPlayerListener (com.jblend.media.MediaPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "addMediaPlayerListener", l);
    }

    public void removeMediaPlayerListener (com.jblend.media.MediaPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smd.SmdPlayer", "removeMediaPlayerListener", l);
    }
}
