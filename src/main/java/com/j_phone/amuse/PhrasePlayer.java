package com.j_phone.amuse;

public class PhrasePlayer {
    private static final com.j_phone.amuse.PhrasePlayer INSTANCE = new com.j_phone.amuse.PhrasePlayer();
    private final com.jblend.media.smaf.phrase.PhrasePlayer delegate;
    private final java.util.IdentityHashMap<com.jblend.media.smaf.phrase.PhraseTrack, com.j_phone.amuse.PhraseTrack> tracks =
            new java.util.IdentityHashMap<>();

    protected PhrasePlayer () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "PhrasePlayer");
        delegate = com.jblend.media.smaf.phrase.PhrasePlayer.getPlayer();
    }


    public static com.j_phone.amuse.PhrasePlayer getPlayer () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getPlayer");
        return INSTANCE;
    }

    public com.j_phone.amuse.PhraseTrack getTrack () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrack");
        return wrap(delegate.getTrack());
    }

    public int getTrackCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackCount");
        return delegate.getTrackCount();
    }

    public com.j_phone.amuse.PhraseTrack getTrack (int track) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrack", track);
        return wrap(delegate.getTrack(track));
    }

    public com.j_phone.amuse.PhraseTrack getTrackPair () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackPair");
        return getTrackPair(0);
    }

    public com.j_phone.amuse.PhraseTrack getTrackPair (int track) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "getTrackPair", track);
        return wrap(delegate.getTrack(Math.max(0, track & ~1)));
    }

    public void disposeTrack (com.j_phone.amuse.PhraseTrack t) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "disposeTrack", t);
        delegate.disposeTrack(t == null ? null : t.delegate());
    }

    public void kill () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "kill");
        delegate.kill();
    }

    public void pause () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "pause");
        delegate.pause();
    }

    public void resume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.PhrasePlayer", "resume");
        delegate.resume();
    }

    private com.j_phone.amuse.PhraseTrack wrap(com.jblend.media.smaf.phrase.PhraseTrack track) {
        if (track == null) {
            return null;
        }
        return tracks.computeIfAbsent(track, com.j_phone.amuse.PhraseTrack::new);
    }
}
