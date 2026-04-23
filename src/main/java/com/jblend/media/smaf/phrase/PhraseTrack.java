package com.jblend.media.smaf.phrase;

public class PhraseTrack extends PhraseTrackBase {
    public static final int NO_DATA = 1;
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 5;
    public static final int DEFAULT_VOLUME = 127;
    public static final int DEFAULT_PANPOT = 64;

    private final int id;
    private Phrase phrase;
    private PhraseTrack syncMaster;
    private PhraseTrackListener listener;
    private int state = NO_DATA;
    private int volume = DEFAULT_VOLUME;
    private int panpot = DEFAULT_PANPOT;
    private boolean muted;

    PhraseTrack(int id) {
        this.id = id;
    }

    public void setPhrase(Phrase p) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setPhrase", p);
        phrase = p;
        state = p == null ? NO_DATA : READY;
    }

    public Phrase getPhrase() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getPhrase");
        return phrase;
    }

    public void removePhrase() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "removePhrase");
        phrase = null;
        syncMaster = null;
        state = NO_DATA;
    }

    public void setSubjectTo(PhraseTrack master) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setSubjectTo", master);
        syncMaster = master == this ? null : master;
    }

    public PhraseTrack getSyncMaster() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getSyncMaster");
        return syncMaster;
    }

    public void play() {
        play(1);
    }

    public void play(int loop) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "play", loop);
        if (phrase != null) {
            state = PLAYING;
        }
    }

    public void stop() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "stop");
        if (phrase != null) {
            state = READY;
        }
    }

    public void pause() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "pause");
        if (state == PLAYING) {
            state = PAUSED;
        }
    }

    public void resume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "resume");
        if (state == PAUSED) {
            state = PLAYING;
        }
    }

    public int getState() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getState");
        return state;
    }

    public void setVolume(int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setVolume", value);
        volume = Math.max(0, Math.min(127, value));
    }

    public int getVolume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getVolume");
        return volume;
    }

    public void setPanpot(int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setPanpot", value);
        panpot = Math.max(0, Math.min(127, value));
    }

    public int getPanpot() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getPanpot");
        return panpot;
    }

    public void mute(boolean mute) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "mute", mute);
        muted = mute;
    }

    public boolean isMute() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "isMute");
        return muted;
    }

    public int getID() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getID");
        return id;
    }

    public void setEventListener(PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setEventListener", l);
        listener = l;
    }

    PhraseTrackListener listener() {
        return listener;
    }
}
