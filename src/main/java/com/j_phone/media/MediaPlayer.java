package com.j_phone.media;

public class MediaPlayer extends javax.microedition.lcdui.Canvas {
    protected MediaPlayer() {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer");
    }

    public MediaPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", data);
    }

    public MediaPlayer (java.lang.String url) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", url);
    }


    public void setMediaData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", data);
    }

    public void setMediaData (java.lang.String url) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", url);
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getMediaWidth");
        return 0;
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getMediaHeight");
        return 0;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getWidth");
        return 0;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getHeight");
        return 0;
    }

    public void setContentPos (int x, int y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setContentPos", x, y);
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "play");
    }

    public void play (boolean isRepeat) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "play", isRepeat);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "stop");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "resume");
    }

    public void setMediaPlayerListener (com.j_phone.media.MediaPlayerListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaPlayerListener", listener);
    }

    protected void paint (javax.microedition.lcdui.Graphics g) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "paint", g);
    }

    protected void showNotify () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "showNotify");
    }

    protected void hideNotify () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "hideNotify");
    }

    public final void setFullScreenMode (boolean mode) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setFullScreenMode", mode);
    }

    public final javax.microedition.lcdui.Ticker getTicker () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getTicker");
        return null;
    }

    public final java.lang.String getTitle () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getTitle");
        return "";
    }

    public final void setTicker (javax.microedition.lcdui.Ticker ticker) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setTicker", ticker);
    }

    public final void setTitle (java.lang.String title) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setTitle", title);
    }
}
