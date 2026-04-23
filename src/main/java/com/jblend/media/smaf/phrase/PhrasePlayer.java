package com.jblend.media.smaf.phrase;

public class PhrasePlayer {
    protected int trackCount = 0;
    protected int audioTrackCount = 0;

    public static com.jblend.media.smaf.phrase.PhrasePlayer getPlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getPlayer");
        return null;
    }

    public void disposePlayer () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposePlayer");
    }

    public com.jblend.media.smaf.phrase.PhraseTrack getTrack () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrack");
        return null;
    }

    public com.jblend.media.smaf.phrase.AudioPhraseTrack getAudioTrack () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrack");
        return null;
    }

    public int getTrackCount () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrackCount");
        return 0;
    }

    public int getAudioTrackCount () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrackCount");
        return 0;
    }

    public com.jblend.media.smaf.phrase.PhraseTrack getTrack (int index) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrack", index);
        return null;
    }

    public com.jblend.media.smaf.phrase.AudioPhraseTrack getAudioTrack (int index) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrack", index);
        return null;
    }

    public void disposeTrack (com.jblend.media.smaf.phrase.PhraseTrack track) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposeTrack", track);
    }

    public void disposeAudioTrack (com.jblend.media.smaf.phrase.AudioPhraseTrack track) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposeAudioTrack", track);
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "pause");
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "resume");
    }

    public void kill () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "kill");
    }
}
