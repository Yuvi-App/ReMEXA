package com.jblend.media.smaf.phrase;

import java.util.ArrayList;
import java.util.List;

public class PhrasePlayer {
    private static final PhrasePlayer INSTANCE = new PhrasePlayer();
    private final List<PhraseTrack> tracks = new ArrayList<>();
    private final List<AudioPhraseTrack> audioTracks = new ArrayList<>();

    private PhrasePlayer() {
        for (int i = 0; i < 16; i++) {
            tracks.add(new PhraseTrack(i));
        }
        for (int i = 0; i < 4; i++) {
            audioTracks.add(new AudioPhraseTrack(i));
        }
    }

    public static PhrasePlayer getPlayer() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getPlayer");
        return INSTANCE;
    }

    public void disposePlayer() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposePlayer");
        for (var track : tracks) {
            track.removePhrase();
        }
        for (var track : audioTracks) {
            track.removePhrase();
        }
    }

    public PhraseTrack getTrack() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrack");
        for (int i = tracks.size() - 1; i >= 0; i--) {
            var track = tracks.get(i);
            if (track.getState() == PhraseTrack.NO_DATA) {
                return track;
            }
        }
        return tracks.getLast();
    }

    public AudioPhraseTrack getAudioTrack() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrack");
        for (int i = audioTracks.size() - 1; i >= 0; i--) {
            var track = audioTracks.get(i);
            if (track.getState() == AudioPhraseTrack.NO_DATA) {
                return track;
            }
        }
        return audioTracks.getLast();
    }

    public int getTrackCount() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrackCount");
        return tracks.size();
    }

    public int getAudioTrackCount() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrackCount");
        return audioTracks.size();
    }

    public PhraseTrack getTrack(int index) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getTrack", index);
        return tracks.get(index);
    }

    public AudioPhraseTrack getAudioTrack(int index) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "getAudioTrack", index);
        return audioTracks.get(index);
    }

    public void disposeTrack(PhraseTrack track) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposeTrack", track);
        if (track != null) {
            track.removePhrase();
        }
    }

    public void disposeAudioTrack(AudioPhraseTrack track) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "disposeAudioTrack", track);
        if (track != null) {
            track.removePhrase();
        }
    }

    public void pause() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "pause");
        for (var track : tracks) {
            if (track.getState() == PhraseTrack.PLAYING) {
                track.pause();
            }
        }
    }

    public void resume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "resume");
        for (var track : tracks) {
            if (track.getState() == PhraseTrack.PAUSED) {
                track.resume();
            }
        }
    }

    public void kill() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.PhrasePlayer", "kill");
        disposePlayer();
    }
}
