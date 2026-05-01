package com.jblend.media.smaf;

import com.jblend.media.MediaData;
import com.jblend.media.MediaImageOperator;
import remexa.audio.smaf.SmafPlayback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SmafPlayer extends com.jblend.media.MediaPlayer implements MediaImageOperator {
    private final List<SmafPlayerListener> smafListeners = new CopyOnWriteArrayList<>();

    private SmafPlayback playback;
    private int volume = 127;
    private int transpose;
    private int speed = 100;
    private int playEnd;
    private int x;
    private int y;
    private int width;
    private int height;
    private int originX;
    private int originY;

    public SmafPlayer() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "SmafPlayer");
    }

    public SmafPlayer(com.jblend.media.smaf.SmafData data) {
        this();
        if (data != null) {
            setData(data);
        }
    }

    public SmafPlayer(byte[] data) {
        this();
        if (data != null) {
            setData(new com.jblend.media.smaf.SmafData(data));
        }
    }

    public void setData(com.jblend.media.smaf.SmafData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setData", data);
        closePlayback();
        super.setData(data);
    }

    public int getCurrent() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getCurrent");
        return 0;
    }

    public void seek(int time) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "seek", time);
    }

    public void setTranspose(int shift) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setTranspose", shift);
        transpose = shift;
    }

    public int getTranspose() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getTranspose");
        return transpose;
    }

    public int getVolume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getVolume");
        return volume;
    }

    public void setVolume(int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setVolume", volume);
        this.volume = Math.max(0, Math.min(127, volume));
        if (playback != null) {
            playback.setVolume(this.volume);
        }
    }

    public int getSpeed() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getSpeed");
        return speed;
    }

    public void setSpeed(int speed) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setSpeed", speed);
        this.speed = speed;
    }

    public void setPlayEnd(int pos) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setPlayEnd", pos);
        playEnd = pos;
    }

    public int getPlayEnd() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getPlayEnd");
        return playEnd;
    }

    public void setBounds(int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setBounds", x, y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addSmafPlayerListener(com.jblend.media.smaf.SmafPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "addSmafPlayerListener", l);
        if (l == null) {
            throw new NullPointerException("SmafPlayer.addSmafPlayerListener: listener is null");
        }
        smafListeners.add(l);
    }

    public void removeSmafPlayerListener(com.jblend.media.smaf.SmafPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "removeSmafPlayerListener", l);
        smafListeners.remove(l);
    }

    public int getX() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getX");
        return x;
    }

    public int getY() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getY");
        return y;
    }

    public int getWidth() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getWidth");
        return width;
    }

    public int getHeight() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getHeight");
        return height;
    }

    public int getOriginX() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getOriginX");
        return originX;
    }

    public int getOriginY() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getOriginY");
        return originY;
    }

    public void setOrigin(int offsetX, int offsetY) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "setOrigin", offsetX, offsetY);
        originX = offsetX;
        originY = offsetY;
    }

    public int getMediaWidth() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getMediaWidth");
        var data = currentData();
        return data instanceof com.jblend.media.smaf.SmafData smaf ? smaf.getWidth() : 0;
    }

    public int getMediaHeight() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.SmafPlayer", "getMediaHeight");
        var data = currentData();
        return data instanceof com.jblend.media.smaf.SmafData smaf ? smaf.getHeight() : 0;
    }

    @Override
    protected void onPlay() {
        SmafPlayback delegate = ensurePlayback();
        delegate.setVolume(volume);
        delegate.play(loopsForever() ? 0 : Math.max(1, loopCount()));
    }

    @Override
    protected void onStop() {
        if (playback != null) {
            playback.stop();
        }
    }

    @Override
    protected void onPause() {
        if (playback != null) {
            playback.pause();
        }
    }

    @Override
    protected void onResume() {
        if (playback != null) {
            playback.resume();
        }
    }

    private SmafPlayback ensurePlayback() {
        if (playback != null) {
            return playback;
        }
        byte[] raw = currentRawData();
        if (raw == null || raw.length == 0) {
            throw new IllegalStateException("SmafPlayer has no SMAF payload");
        }
        try {
            playback = SmafPlayback.create(raw);
            playback.setVolume(volume);
            playback.setListener(this::handlePlaybackEvent);
            playback.prepareAsync();
            return playback;
        } catch (Exception exception) {
            notifyError();
            throw new RuntimeException("Failed to create SMAF playback", exception);
        }
    }

    private void handlePlaybackEvent(int eventId) {
        if (eventId == -1) {
            notifyRepeatCompleted();
            return;
        }
        for (SmafPlayerListener listener : smafListeners) {
            listener.eventOccurred(this, eventId);
        }
    }

    private void closePlayback() {
        if (playback == null) {
            return;
        }
        try {
            playback.close();
        } catch (Exception ignored) {
        } finally {
            playback = null;
        }
    }
}
