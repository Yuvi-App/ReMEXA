package com.jblend.media.smaf.phrase;

public class AudioPhraseTrack extends com.jblend.media.smaf.phrase.PhraseTrackBase {
    public static final int NO_DATA = 0;
    public static final int READY = 0;
    public static final int PLAYING = 0;
    public static final int PAUSED = 0;
    public static final int DEFAULT_VOLUME = 0;
    public static final int DEFAULT_PANPOT = 0;

    public void setPhrase (com.jblend.media.smaf.phrase.AudioPhrase p) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setPhrase", p);
    }

    public com.jblend.media.smaf.phrase.AudioPhrase getPhrase () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getPhrase");
        return null;
    }

    public void removePhrase () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "removePhrase");
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "play");
    }

    public void play (int loop) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "play", loop);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "stop");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "resume");
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getState");
        return 0;
    }

    public void setVolume (int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setVolume", value);
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getVolume");
        return 0;
    }

    public void setPanpot (int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setPanpot", value);
    }

    public int getPanpot () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getPanpot");
        return 0;
    }

    public void mute (boolean mute) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "mute", mute);
    }

    public boolean isMute () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "isMute");
        return false;
    }

    public int getID () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getID");
        return 0;
    }

    public void setEventListener (com.jblend.media.smaf.phrase.PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setEventListener", l);
    }
}
