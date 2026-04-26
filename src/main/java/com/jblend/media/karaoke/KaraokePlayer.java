package com.jblend.media.karaoke;

public class KaraokePlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    public KaraokePlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "KaraokePlayer");
    }

    public KaraokePlayer (com.jblend.media.karaoke.KaraokeData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "KaraokePlayer", data);
        if (data != null) {
            super.setData(data);
        }
    }

    public KaraokePlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "KaraokePlayer", data);
        if (data != null) {
            super.setData(new com.jblend.media.karaoke.KaraokeData(data));
        }
    }

    public void setData (com.jblend.media.karaoke.KaraokeData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setData", data);
        super.setData(data);
    }

    public int getCurrent () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getCurrent");
        return 0;
    }

    public void seek (int time) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "seek", time);
    }

    public void setTranspose (int shift) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setTranspose", shift);
    }

    public int getTranspose () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getTranspose");
        return 0;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getVolume");
        return 0;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setVolume", volume);
    }

    public void setPlayEnd (int pos) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setPlayEnd", pos);
    }

    public int getPlayEnd () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getPlayEnd");
        return 0;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setBounds", x, y, width, height);
    }

    public void addKaraokePlayerListener (com.jblend.media.karaoke.KaraokePlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "addKaraokePlayerListener", l);
    }

    public void removeKaraokePlayerListener (com.jblend.media.karaoke.KaraokePlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "removeKaraokePlayerListener", l);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getX");
        return 0;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getY");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getHeight");
        return 0;
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getOriginX");
        return 0;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getOriginY");
        return 0;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setOrigin", offset_x, offset_y);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getMediaWidth");
        var data = currentData();
        return data instanceof com.jblend.media.karaoke.KaraokeData karaoke ? karaoke.getWidth() : 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getMediaHeight");
        var data = currentData();
        return data instanceof com.jblend.media.karaoke.KaraokeData karaoke ? karaoke.getHeight() : 0;
    }

    public int getSpeed () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getSpeed");
        return 0;
    }

    public void setSpeed (int speed) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setSpeed", speed);
    }

    public int getChannelVolume (int ch) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getChannelVolume", ch);
        return 0;
    }

    public void setChannelVolume (int ch, int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setChannelVolume", ch, volume);
    }

    public int getHVVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getHVVolume");
        return 0;
    }

    public void setHVVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setHVVolume", volume);
    }
}
