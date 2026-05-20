package com.jblend.media.karaoke;

import remexa.audio.smaf.SmafPlayback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KaraokePlayer extends com.jblend.media.MediaPlayer implements com.jblend.media.MediaImageOperator {
    private final List<com.jblend.media.karaoke.KaraokePlayerListener> karaokeListeners = new CopyOnWriteArrayList<>();
    private SmafPlayback playback;
    private int volume = 127;
    private int transpose;
    private int speed = 100;
    private int playEnd = -1;
    private int x;
    private int y;
    private int width;
    private int height;
    private int originX;
    private int originY;
    private long currentPositionMs;
    private long startedAtMs;

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
        closePlayback();
        super.setData(data);
    }

    public int getCurrent () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getCurrent");
        if (getState() == PLAYING) {
            long elapsed = System.currentTimeMillis() - startedAtMs;
            return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
        }
        return currentPositionMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) currentPositionMs;
    }

    public void seek (int time) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "seek", time);
        currentPositionMs = Math.max(0, time);
        if (getState() == PLAYING) {
            startedAtMs = System.currentTimeMillis() - currentPositionMs;
        }
    }

    public void setTranspose (int shift) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setTranspose", shift);
        transpose = shift;
    }

    public int getTranspose () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getTranspose");
        return transpose;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getVolume");
        return volume;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setVolume", volume);
        this.volume = Math.max(0, Math.min(127, volume));
        if (playback != null) {
            playback.setVolume(this.volume);
        }
    }

    public void setPlayEnd (int pos) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setPlayEnd", pos);
        playEnd = pos;
    }

    public int getPlayEnd () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getPlayEnd");
        return playEnd;
    }

    public void setBounds (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setBounds", x, y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addKaraokePlayerListener (com.jblend.media.karaoke.KaraokePlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "addKaraokePlayerListener", l);
        if (l == null) {
            throw new NullPointerException("KaraokePlayer.addKaraokePlayerListener: listener is null");
        }
        karaokeListeners.add(l);
    }

    public void removeKaraokePlayerListener (com.jblend.media.karaoke.KaraokePlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "removeKaraokePlayerListener", l);
        karaokeListeners.remove(l);
    }

    public int getX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getX");
        return x;
    }

    public int getY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getY");
        return y;
    }

    public int getWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getWidth");
        return width;
    }

    public int getHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getHeight");
        return height;
    }

    public int getOriginX () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getOriginX");
        return originX;
    }

    public int getOriginY () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getOriginY");
        return originY;
    }

    public void setOrigin (int offset_x, int offset_y) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setOrigin", offset_x, offset_y);
        originX = offset_x;
        originY = offset_y;
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
        return speed;
    }

    public void setSpeed (int speed) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setSpeed", speed);
        this.speed = speed;
    }

    public int getChannelVolume (int ch) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getChannelVolume", ch);
        return volume;
    }

    public void setChannelVolume (int ch, int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setChannelVolume", ch, volume);
        setVolume(volume);
    }

    public int getHVVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "getHVVolume");
        return volume;
    }

    public void setHVVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.KaraokePlayer", "setHVVolume", volume);
        setVolume(volume);
    }

    @Override
    public void play() {
        super.play();
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    public void play(boolean isRepeat) {
        super.play(isRepeat);
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    public void play(int count) {
        super.play(count);
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    public void stop() {
        super.stop();
        currentPositionMs = 0L;
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    public void pause() {
        currentPositionMs = getCurrent();
        super.pause();
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    public void resume() {
        super.resume();
        startedAtMs = System.currentTimeMillis() - currentPositionMs;
        notifyKaraokeState(System.currentTimeMillis());
    }

    @Override
    protected void onPlay() {
        SmafPlayback delegate = ensurePlayback();
        delegate.setVolume(volume);
        delegate.play(loopsForever() ? 0 : Math.max(1, loopCount()));
        startedAtMs = System.currentTimeMillis() - currentPositionMs;
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

    @Override
    protected void onDispose() {
        closePlayback();
    }

    private SmafPlayback ensurePlayback() {
        if (playback != null) {
            return playback;
        }
        byte[] raw = currentRawData();
        if (raw == null || raw.length == 0) {
            throw new IllegalStateException("KaraokePlayer has no SMAF payload");
        }
        try {
            playback = SmafPlayback.create(raw);
            playback.setVolume(volume);
            playback.setListener(this::handlePlaybackEvent);
            playback.prepareAsync();
            return playback;
        } catch (Exception exception) {
            notifyError();
            throw new RuntimeException("Failed to create karaoke playback", exception);
        }
    }

    private void handlePlaybackEvent(int eventId) {
        if (eventId == -1) {
            currentPositionMs = 0L;
            notifyRepeatCompleted();
            notifyKaraokeState(System.currentTimeMillis());
            return;
        }
        for (var listener : karaokeListeners) {
            listener.eventOccurred(this, eventId);
        }
    }

    private void notifyKaraokeState(long time) {
        for (var listener : karaokeListeners) {
            listener.playerStateChanged(this, time);
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
