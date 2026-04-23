package com.j_phone.amuse;

public class PhraseTrack {
    public static final int DEFAULT_VOLUME = 127;
    private final com.jblend.media.smaf.phrase.PhraseTrack delegate;
    private com.j_phone.amuse.Phrase currentPhrase;
    private com.j_phone.amuse.PhraseTrack syncMaster;

    PhraseTrack(com.jblend.media.smaf.phrase.PhraseTrack delegate) {
        this.delegate = delegate;
    }

    public void setPhrase (com.j_phone.amuse.Phrase p) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setPhrase", p);
        currentPhrase = p;
        delegate.setPhrase(p == null ? null : p.delegate());
    }

    public void removePhrase () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "removePhrase");
        currentPhrase = null;
        delegate.removePhrase();
    }

    public void play () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "play");
        delegate.play();
    }

    public void play (int loop) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "play", loop);
        delegate.play(loop);
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "stop");
        delegate.stop();
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "pause");
        delegate.pause();
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "resume");
        delegate.resume();
    }

    public boolean isPlaying () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "isPlaying");
        return delegate.getState() == com.jblend.media.smaf.phrase.PhraseTrack.PLAYING;
    }

    public com.j_phone.amuse.Phrase getPhrase () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getPhrase");
        return currentPhrase;
    }

    public void setVolume (int value) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setVolume", value);
        delegate.setVolume(value);
    }

    public void mute (boolean mute) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "mute", mute);
        delegate.mute(mute);
    }

    public boolean isMute () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "isMute");
        return delegate.isMute();
    }

    public int getID () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getID");
        return delegate.getID();
    }

    public void setSubjectTo (com.j_phone.amuse.PhraseTrack master) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setSubjectTo", master);
        syncMaster = master;
        delegate.setSubjectTo(master == null ? null : master.delegate);
    }

    public com.j_phone.amuse.PhraseTrack getSyncMaster () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "getSyncMaster");
        return syncMaster;
    }

    public void setEventListener (com.j_phone.amuse.PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhraseTrack", "setEventListener", l);
        delegate.setEventListener(l == null ? null : l::eventOccurred);
    }

    com.jblend.media.smaf.phrase.PhraseTrack delegate() {
        return delegate;
    }
}
