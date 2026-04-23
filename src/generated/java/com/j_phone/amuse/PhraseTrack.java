package com.j_phone.amuse;

public class PhraseTrack {
    public static final int DEFAULT_VOLUME = 0;

    public void setPhrase (com.j_phone.amuse.Phrase p) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setPhrase", p);
    }

    public void removePhrase () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "removePhrase");
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "play");
    }

    public void play (int loop) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "play", loop);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "stop");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "resume");
    }

    public boolean isPlaying () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "isPlaying");
        return false;
    }

    public com.j_phone.amuse.Phrase getPhrase () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getPhrase");
        return null;
    }

    public void setVolume (int value) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setVolume", value);
    }

    public void mute (boolean mute) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "mute", mute);
    }

    public boolean isMute () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "isMute");
        return false;
    }

    public int getID () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getID");
        return 0;
    }

    public void setSubjectTo (com.j_phone.amuse.PhraseTrack master) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setSubjectTo", master);
    }

    public com.j_phone.amuse.PhraseTrack getSyncMaster () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getSyncMaster");
        return null;
    }

    public void setEventListener (com.j_phone.amuse.PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setEventListener", l);
    }
}
