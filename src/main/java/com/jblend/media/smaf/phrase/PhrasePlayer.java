package com.jblend.media.smaf.phrase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhrasePlayer {
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
        return INSTANCE;
    }

    public PhraseTrack getTrack() {
        for (int i = tracks.size() - 1; i >= 0; i--) {
            PhraseTrack track = tracks.get(i);
            if (track.getState() == PhraseTrack.NO_DATA) {
                return track;
            }
        }
        throw new IllegalStateException("No free phrase tracks available");
    }

    public PhraseTrack getTrack(int index) {
        return tracks.get(index);
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public AudioPhraseTrack getAudioTrack() {
        for (int i = audioTracks.size() - 1; i >= 0; i--) {
            AudioPhraseTrack track = audioTracks.get(i);
            if (track.getState() == PhraseTrack.NO_DATA) {
                return track;
            }
        }
        throw new IllegalStateException("No free audio phrase tracks available");
    }

    public AudioPhraseTrack getAudioTrack(int index) {
        return audioTracks.get(index);
    }

    public int getAudioTrackCount() {
        return audioTracks.size();
    }

    public void disposeTrack(PhraseTrack track) {
        if (track != null) {
            track.stop();
            track.removePhrase();
        }
    }

    public void disposeAudioTrack(AudioPhraseTrack track) {
        if (track != null) {
            track.stop();
            track.removePhrase();
        }
    }

    public void disposePlayer() {
        for (PhraseTrack track : tracks) {
            disposeTrack(track);
        }
        for (AudioPhraseTrack track : audioTracks) {
            disposeAudioTrack(track);
        }
    }

    public void kill() {
        disposePlayer();
    }

    public void pause() {
        visitActive(track -> {
            if (track.getState() == PhraseTrack.PLAYING) {
                track.pause();
            }
        });
    }

    public void resume() {
        visitActive(track -> {
            if (track.getState() == PhraseTrack.PAUSED) {
                track.resume();
            }
        });
    }

    private void visitActive(java.util.function.Consumer<PhraseTrack> action) {
        Set<PhraseTrack> visited = new HashSet<>();
        for (PhraseTrack track : tracks) {
            if (track != null && visited.add(track)) {
                action.accept(track);
            }
        }
        for (AudioPhraseTrack track : audioTracks) {
            if (track != null) {
                PhraseTrack delegate = track.delegate();
                if (visited.add(delegate)) {
                    action.accept(delegate);
                }
            }
        }
    }
}
