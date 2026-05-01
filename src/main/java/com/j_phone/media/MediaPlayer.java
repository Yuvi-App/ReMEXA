package com.j_phone.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import org.recompile.mobile.Mobile;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public class MediaPlayer extends javax.microedition.lcdui.Canvas {
    private static final String LOG_SOURCE = MediaPlayer.class.getName();
    private static final Set<MediaPlayer> ACTIVE_PLAYERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private final ClassLoader ownerClassLoader;
    private byte[] mediaData;
    private String mediaUrl;
    private int contentX;
    private int contentY;
    private boolean playing;
    private boolean paused;
    private MediaPlayerListener listener;
    private Player delegate;
    private PlayerListener delegateListener;
    private boolean delegatePaused;
    private boolean fallbackMode;
    private boolean suppressDelegateStopEvent;
    private boolean runtimeShutdown;

    protected MediaPlayer() {
        ownerClassLoader = MidletRuntime.currentAppClassLoader();
        ACTIVE_PLAYERS.add(this);
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer");
    }

    public static void shutdownOwnedPlayers(ClassLoader ownerClassLoader) {
        MediaPlayer[] snapshot;
        synchronized (ACTIVE_PLAYERS) {
            snapshot = ACTIVE_PLAYERS.toArray(MediaPlayer[]::new);
        }
        for (MediaPlayer player : snapshot) {
            if (player == null || !player.isOwnedBy(ownerClassLoader)) {
                continue;
            }
            player.shutdownFromRuntime();
        }
    }

    public MediaPlayer (byte[] data) {
        this();
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", data);
        this.mediaData = data;
    }

    public MediaPlayer (java.lang.String url) throws java.io.IOException {
        this();
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "MediaPlayer", url);
        this.mediaUrl = url;
    }


    public void setMediaData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", data);
        if (runtimeShutdown) {
            return;
        }
        releaseDelegate();
        this.mediaData = data;
        this.mediaUrl = null;
        this.playing = false;
        this.paused = false;
        this.fallbackMode = false;
    }

    public void setMediaData (java.lang.String url) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "setMediaData", url);
        if (runtimeShutdown) {
            return;
        }
        releaseDelegate();
        this.mediaUrl = url;
        this.mediaData = null;
        this.playing = false;
        this.paused = false;
        this.fallbackMode = false;
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
        MidletRuntime.ensureThreadActive();
        if (runtimeShutdown) {
            return;
        }
        if (startDelegatePlayback(false)) {
            return;
        }
        enterFallbackPlayback();
    }

    public void play (boolean isRepeat) {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "play", isRepeat);
        MidletRuntime.ensureThreadActive();
        if (runtimeShutdown) {
            return;
        }
        if (startDelegatePlayback(isRepeat)) {
            return;
        }
        enterFallbackPlayback();
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "stop");
        if (stopDelegatePlayback(true)) {
            return;
        }
        this.playing = false;
        this.paused = false;
        fireStateChanged(MediaPlayerListener.STOPPED);
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "pause");
        if (pauseDelegatePlayback()) {
            return;
        }
        this.playing = false;
        this.paused = true;
        fireStateChanged(MediaPlayerListener.PAUSED);
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.media.MediaPlayer", "resume");
        if (resumeDelegatePlayback()) {
            return;
        }
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

    private boolean isOwnedBy(ClassLoader candidate) {
        return candidate == null || ownerClassLoader == candidate;
    }

    private synchronized void shutdownFromRuntime() {
        if (!ACTIVE_PLAYERS.remove(this)) {
            return;
        }
        releaseDelegate();
        playing = false;
        paused = false;
        delegatePaused = false;
        fallbackMode = false;
        suppressDelegateStopEvent = false;
        runtimeShutdown = true;
    }

    private boolean startDelegatePlayback(boolean repeat) {
        if (!hasMedia()) {
            return false;
        }
        if (!ensureDelegate()) {
            return false;
        }
        try {
            if (delegate.getState() == Player.STARTED) {
                return true;
            }
            delegate.setLoopCount(repeat ? -1 : 1);
            delegate.start();
            delegatePaused = false;
            playing = true;
            paused = false;
            fireStateChanged(MediaPlayerListener.PLAYED);
            return true;
        } catch (MediaException | IllegalStateException exception) {
            logMediaIssue("Failed to start delegated media playback", exception);
            releaseDelegate();
            return false;
        }
    }

    private boolean stopDelegatePlayback(boolean resetPosition) {
        if (delegate == null) {
            return false;
        }
        try {
            suppressDelegateStopEvent = true;
            if (delegate.getState() == Player.STARTED) {
                delegate.stop();
            }
            if (resetPosition) {
                delegate.setMediaTime(0L);
            }
        } catch (MediaException | IllegalStateException exception) {
            logMediaIssue("Failed to stop delegated media playback", exception);
        } finally {
            suppressDelegateStopEvent = false;
        }
        delegatePaused = false;
        playing = false;
        paused = false;
        fireStateChanged(MediaPlayerListener.STOPPED);
        return true;
    }

    private boolean pauseDelegatePlayback() {
        if (delegate == null || delegate.getState() != Player.STARTED) {
            return false;
        }
        try {
            suppressDelegateStopEvent = true;
            delegate.stop();
            delegatePaused = true;
            playing = false;
            paused = true;
            fireStateChanged(MediaPlayerListener.PAUSED);
            return true;
        } catch (MediaException | IllegalStateException exception) {
            logMediaIssue("Failed to pause delegated media playback", exception);
            return false;
        } finally {
            suppressDelegateStopEvent = false;
        }
    }

    private boolean resumeDelegatePlayback() {
        if (delegate == null || !delegatePaused) {
            return false;
        }
        try {
            delegate.start();
            delegatePaused = false;
            playing = true;
            paused = false;
            fireStateChanged(MediaPlayerListener.PLAYED);
            return true;
        } catch (MediaException | IllegalStateException exception) {
            logMediaIssue("Failed to resume delegated media playback", exception);
            releaseDelegate();
            return false;
        }
    }

    private boolean ensureDelegate() {
        if (fallbackMode) {
            return false;
        }
        if (delegate != null) {
            return true;
        }
        byte[] source;
        try {
            source = loadSourceBytes();
        } catch (IOException exception) {
            logMediaIssue("Failed to load media source", exception);
            fallbackMode = true;
            return false;
        }
        if (source == null || source.length == 0) {
            fallbackMode = true;
            return false;
        }
        try {
            String contentType = guessContentType(source, mediaUrl);
            delegate = Manager.createPlayer(new ByteArrayInputStream(source), contentType);
            delegateListener = this::handleDelegateEvent;
            delegate.addPlayerListener(delegateListener);
            fallbackMode = false;
            return true;
        } catch (IOException | MediaException | IllegalArgumentException exception) {
            logMediaIssue("Falling back to stubbed J-Phone media playback", exception);
            releaseDelegate();
            fallbackMode = true;
            return false;
        }
    }

    private byte[] loadSourceBytes() throws IOException {
        if (mediaData != null) {
            return mediaData;
        }
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return null;
        }
        return Mobile.getMIDletResourceAsByteArray(mediaUrl);
    }

    private String guessContentType(byte[] source, String sourceName) {
        if (startsWith(source, 'M', 'M', 'M', 'D')) {
            return "application/x-smaf";
        }
        if (startsWith(source, 'M', 'T', 'h', 'd')) {
            return "audio/midi";
        }
        if (startsWith(source, 'R', 'I', 'F', 'F')) {
            return "audio/x-wav";
        }
        var normalizedName = sourceName == null ? "" : sourceName.trim().toLowerCase(Locale.ROOT);
        if (normalizedName.endsWith(".mmf") || normalizedName.endsWith(".smaf") || normalizedName.endsWith(".mld")) {
            return "application/x-smaf";
        }
        if (normalizedName.endsWith(".mid") || normalizedName.endsWith(".midi")) {
            return "audio/midi";
        }
        if (normalizedName.endsWith(".wav")) {
            return "audio/x-wav";
        }
        return "unknown";
    }

    private boolean startsWith(byte[] source, int... bytes) {
        if (source.length < bytes.length) {
            return false;
        }
        for (int index = 0; index < bytes.length; index++) {
            if ((source[index] & 0xFF) != (bytes[index] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private void handleDelegateEvent(Player player, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event)
                || PlayerListener.STOPPED.equals(event)
                || PlayerListener.STOPPED_AT_TIME.equals(event)
                || PlayerListener.CLOSED.equals(event)) {
            if (suppressDelegateStopEvent) {
                return;
            }
            delegatePaused = false;
            playing = false;
            paused = false;
            fireStateChanged(MediaPlayerListener.STOPPED);
            return;
        }
        if (PlayerListener.ERROR.equals(event)) {
            delegatePaused = false;
            playing = false;
            paused = false;
            logMediaIssue("Delegated media playback reported an error", eventData);
            fireStateChanged(MediaPlayerListener.STOPPED);
        }
    }

    private void enterFallbackPlayback() {
        this.playing = true;
        this.paused = false;
        fireStateChanged(MediaPlayerListener.PLAYED);
    }

    private void releaseDelegate() {
        if (delegate != null && delegateListener != null) {
            delegate.removePlayerListener(delegateListener);
        }
        if (delegate != null) {
            try {
                delegate.close();
            } catch (RuntimeException ignored) {
                // Best-effort release; the stub fallback remains usable.
            }
        }
        delegate = null;
        delegateListener = null;
        delegatePaused = false;
    }

    private void logMediaIssue(String message, Object detail) {
        DebugLog.log(LogCategory.MEDIA, LOG_SOURCE, message + ": " + detail);
    }

    private void fireStateChanged(int state) {
        if (listener != null) {
            listener.mediaStateChanged(state);
        }
    }
}
