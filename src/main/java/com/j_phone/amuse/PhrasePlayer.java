package com.j_phone.amuse;

public class PhrasePlayer {
    protected int trackCount = 0;
    protected com.j_phone.amuse.PhraseTrack[] tracks = null;
    protected boolean[] useFlag = null;
    protected static com.j_phone.amuse.PhrasePlayer phrasePlayer = null;

    protected PhrasePlayer () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "PhrasePlayer");
    }


    public static com.j_phone.amuse.PhrasePlayer getPlayer () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getPlayer");
        return null;
    }

    public com.j_phone.amuse.PhraseTrack getTrack () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrack");
        return null;
    }

    public int getTrackCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackCount");
        return 0;
    }

    public com.j_phone.amuse.PhraseTrack getTrack (int track) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrack", track);
        return null;
    }

    public com.j_phone.amuse.PhraseTrack getTrackPair () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackPair");
        return null;
    }

    public com.j_phone.amuse.PhraseTrack getTrackPair (int track) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackPair", track);
        return null;
    }

    public void disposeTrack (com.j_phone.amuse.PhraseTrack t) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "disposeTrack", t);
    }

    public void kill () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "kill");
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "resume");
    }
}
