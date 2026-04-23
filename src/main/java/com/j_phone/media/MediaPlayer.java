package com.j_phone.media;

public class MediaPlayer extends javax.microedition.lcdui.Canvas {
    private byte[] mediaData;
    private String mediaUrl;
    private int contentX;
    private int contentY;
    private boolean playing;
    private boolean paused;
    private MediaPlayerListener listener;

    protected MediaPlayer() {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer");
    }

    public MediaPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", data);
        this.mediaData = data;
    }

    public MediaPlayer (java.lang.String url) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", url);
        this.mediaUrl = url;
    }


    public void setMediaData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", data);
        this.mediaData = data;
        this.mediaUrl = null;
    }

    public void setMediaData (java.lang.String url) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", url);
        this.mediaUrl = url;
        this.mediaData = null;
    }

    public int getMediaWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getMediaWidth");
        return getWidth();
    }

    public int getMediaHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getMediaHeight");
        return getHeight();
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getWidth");
        return super.getWidth();
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "getHeight");
        return super.getHeight();
    }

    public void setContentPos (int x, int y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setContentPos", x, y);
        this.contentX = x;
        this.contentY = y;
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "play");
        this.playing = true;
        this.paused = false;
        fireStateChanged(MediaPlayerListener.PLAYED);
    }

    public void play (boolean isRepeat) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "play", isRepeat);
        play();
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "stop");
        this.playing = false;
        this.paused = false;
        fireStateChanged(MediaPlayerListener.STOPPED);
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "pause");
        this.playing = false;
        this.paused = true;
        fireStateChanged(MediaPlayerListener.PAUSED);
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "resume");
        this.playing = true;
        this.paused = false;
        fireStateChanged(MediaPlayerListener.PLAYED);
    }

    public void setMediaPlayerListener (com.j_phone.media.MediaPlayerListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaPlayerListener", listener);
        this.listener = listener;
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

    boolean hasMedia() {
        return mediaData != null || (mediaUrl != null && !mediaUrl.isBlank());
    }

    private void fireStateChanged(int state) {
        if (listener != null) {
            listener.mediaStateChanged(state);
        }
    }
}
