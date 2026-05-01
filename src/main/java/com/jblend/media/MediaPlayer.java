package com.jblend.media;

import remexa.host.runtime.MidletRuntime;

public abstract class MediaPlayer {
    public static final int NO_DATA = 0;
    public static final int READY = 1;
    public static final int PLAYING = 2;
    public static final int PAUSED = 3;
    public static final int ERROR = 0x10000;

    protected static final int REAL_WIDTH =
            remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).width();
    protected static final int REAL_HEIGHT =
            remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).height();
    private static final java.util.Set<com.jblend.media.MediaPlayer> ACTIVE_PLAYERS =
            java.util.Collections.synchronizedSet(
                    java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private final java.util.List<com.jblend.media.MediaPlayerListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final ClassLoader ownerClassLoader;
    private com.jblend.media.MediaData data;
    private int state = NO_DATA;
    private int repeatCount;
    private boolean repeatInfinite;
    private boolean runtimeShutdown;

    public MediaPlayer () {
        ownerClassLoader = MidletRuntime.currentAppClassLoader();
        ACTIVE_PLAYERS.add(this);
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "MediaPlayer");
    }

    public static void shutdownOwnedPlayers(ClassLoader ownerClassLoader) {
        com.jblend.media.MediaPlayer[] snapshot;
        synchronized (ACTIVE_PLAYERS) {
            snapshot = ACTIVE_PLAYERS.toArray(com.jblend.media.MediaPlayer[]::new);
        }
        for (com.jblend.media.MediaPlayer player : snapshot) {
            if (player == null || !player.isOwnedBy(ownerClassLoader)) {
                continue;
            }
            player.shutdownFromRuntime();
        }
    }

    public void setData (com.jblend.media.MediaData data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "setData", data);
        if (runtimeShutdown) {
            return;
        }
        if (state == PLAYING || state == PAUSED) {
            throw new IllegalStateException("MediaPlayer.setData: cannot change data while playing or paused.");
        }
        this.data = data;
        transitionTo(data == null ? NO_DATA : READY);
    }

    public void play () {
        play(1);
    }

    public void play (boolean isRepeat) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "play", isRepeat);
        startPlayback(isRepeat ? Integer.MAX_VALUE : 1, isRepeat);
    }

    public void play (int count) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "play", count);
        startPlayback(Math.max(1, count), false);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "stop");
        if (state == NO_DATA || state == READY) {
            return;
        }
        onStop();
        transitionTo(data == null ? NO_DATA : READY);
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "pause");
        if (state != PLAYING) {
            return;
        }
        onPause();
        transitionTo(PAUSED);
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "resume");
        if (state != PAUSED) {
            return;
        }
        onResume();
        transitionTo(PLAYING);
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "getState");
        return state;
    }

    public void addMediaPlayerListener (com.jblend.media.MediaPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "addMediaPlayerListener", l);
        if (l == null) {
            throw new NullPointerException("MediaPlayer.addMediaPlayerListener: listener is null");
        }
        listeners.add(l);
    }

    public void removeMediaPlayerListener (com.jblend.media.MediaPlayerListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "removeMediaPlayerListener", l);
        listeners.remove(l);
    }

    protected static void addNativeMediaEventDispatcher (com.jblend.io.j2me.events.NativeMediaEventDispatcher dispatcher) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaPlayer", "addNativeMediaEventDispatcher", dispatcher);
    }

    /** Subclass hook invoked when {@link #play} starts producing output. */
    protected void onPlay() {
    }

    /** Subclass hook invoked when {@link #stop} halts playback. */
    protected void onStop() {
    }

    /** Subclass hook invoked when {@link #pause} suspends playback. */
    protected void onPause() {
    }

    /** Subclass hook invoked when {@link #resume} continues playback. */
    protected void onResume() {
    }

    /** Subclass hook invoked when the host tears down an appli runtime. */
    protected void onDispose() {
    }

    /** Subclasses invoke this when a single repeat iteration finishes. */
    protected final void notifyRepeatCompleted() {
        if (repeatInfinite || --repeatCount > 0) {
            for (var l : listeners) {
                l.playerRepeated(this);
            }
            return;
        }
        transitionTo(data == null ? NO_DATA : READY);
    }

    /** Subclasses invoke this to signal an unrecoverable playback failure. */
    protected final void notifyError() {
        transitionTo(ERROR);
    }

    protected final com.jblend.media.MediaData currentData() {
        return data;
    }

    protected final byte[] currentRawData() {
        return data == null ? null : data.rawData().clone();
    }

    protected final int loopCount() {
        return repeatCount;
    }

    protected final boolean loopsForever() {
        return repeatInfinite;
    }

    private void startPlayback(int count, boolean infinite) {
        MidletRuntime.ensureThreadActive();
        if (runtimeShutdown) {
            return;
        }
        if (state == NO_DATA) {
            throw new IllegalStateException("MediaPlayer.play: no media data set.");
        }
        if (state == ERROR) {
            throw new IllegalStateException("MediaPlayer.play: player is in ERROR state; call stop() first.");
        }
        if (state == PLAYING) {
            return;
        }
        repeatCount = count;
        repeatInfinite = infinite;
        onPlay();
        transitionTo(PLAYING);
    }

    private boolean isOwnedBy(ClassLoader candidate) {
        return candidate == null || ownerClassLoader == candidate;
    }

    private synchronized void shutdownFromRuntime() {
        if (!ACTIVE_PLAYERS.remove(this)) {
            return;
        }
        try {
            if (state == PLAYING || state == PAUSED) {
                onStop();
            }
        } catch (RuntimeException ignored) {
            // Runtime teardown is best effort; continue closing any native resources.
        }
        try {
            onDispose();
        } catch (RuntimeException ignored) {
            // Keep shutdown resilient even if an emulated player is already half-closed.
        }
        runtimeShutdown = true;
        data = null;
        state = NO_DATA;
        repeatCount = 0;
        repeatInfinite = false;
    }

    private void transitionTo(int newState) {
        if (state == newState) {
            return;
        }
        state = newState;
        for (var l : listeners) {
            l.playerStateChanged(this);
        }
    }
}
