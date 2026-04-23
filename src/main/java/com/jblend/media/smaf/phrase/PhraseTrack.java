package com.jblend.media.smaf.phrase;

public class PhraseTrack extends com.jblend.media.smaf.phrase.PhraseTrackBase {
    public static final int NO_DATA = 0;
    public static final int READY = 0;
    public static final int PLAYING = 0;
    public static final int PAUSED = 0;
    public static final int DEFAULT_VOLUME = 0;
    public static final int DEFAULT_PANPOT = 0;

    public void setPhrase (com.jblend.media.smaf.phrase.Phrase p) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setPhrase", p);
    }

    public com.jblend.media.smaf.phrase.Phrase getPhrase () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getPhrase");
        return null;
    }

    public void removePhrase () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "removePhrase");
    }

    public void setSubjectTo (com.jblend.media.smaf.phrase.PhraseTrack master) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setSubjectTo", master);
    }

    public com.jblend.media.smaf.phrase.PhraseTrack getSyncMaster () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getSyncMaster");
        return null;
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "play");
    }

    public void play (int loop) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "play", loop);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "stop");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "resume");
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getState");
        return 0;
    }

    public void setVolume (int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setVolume", value);
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getVolume");
        return 0;
    }

    public void setPanpot (int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setPanpot", value);
    }

    public int getPanpot () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getPanpot");
        return 0;
    }

    public void mute (boolean mute) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "mute", mute);
    }

    public boolean isMute () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "isMute");
        return false;
    }

    public int getID () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "getID");
        return 0;
    }

    public void setEventListener (com.jblend.media.smaf.phrase.PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhraseTrack", "setEventListener", l);
    }
}
