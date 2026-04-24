package com.jblend.media.smaf.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class PhrasePlayer {
    private static final PhrasePlayer INSTANCE = new PhrasePlayer();

    private final List<PhraseTrack> tracks = new ArrayList<>();
    private final List<AudioPhraseTrack> audioTracks = new ArrayList<>();
    private final Set<PhraseTrack> reservedTracks =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AudioPhraseTrack> reservedAudioTracks =
            Collections.newSetFromMap(new IdentityHashMap<>());

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

    public synchronized PhraseTrack getTrack() {
        for (int i = tracks.size() - 1; i >= 0; i--) {
            PhraseTrack track = tracks.get(i);
            if (!reservedTracks.contains(track)) {
                reservedTracks.add(track);
                return track;
            }
        }
        throw new IllegalStateException("No free phrase tracks available");
    }

    public synchronized PhraseTrack getTrack(int index) {
        PhraseTrack track = tracks.get(index);
        reservedTracks.add(track);
        return track;
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public synchronized AudioPhraseTrack getAudioTrack() {
        for (int i = audioTracks.size() - 1; i >= 0; i--) {
            AudioPhraseTrack track = audioTracks.get(i);
            if (!reservedAudioTracks.contains(track)) {
                reservedAudioTracks.add(track);
                return track;
            }
        }
        throw new IllegalStateException("No free audio phrase tracks available");
    }

    public synchronized AudioPhraseTrack getAudioTrack(int index) {
        AudioPhraseTrack track = audioTracks.get(index);
        reservedAudioTracks.add(track);
        return track;
    }

    public int getAudioTrackCount() {
        return audioTracks.size();
    }

    public synchronized void disposeTrack(PhraseTrack track) {
        if (track != null) {
            track.stop();
            track.removePhrase();
            reservedTracks.remove(track);
        }
    }

    public synchronized void disposeAudioTrack(AudioPhraseTrack track) {
        if (track != null) {
            track.stop();
            track.removePhrase();
            reservedAudioTracks.remove(track);
        }
    }

    public synchronized void disposePlayer() {
        for (PhraseTrack track : tracks) {
            disposeTrack(track);
        }
        for (AudioPhraseTrack track : audioTracks) {
            disposeAudioTrack(track);
        }
        reservedTracks.clear();
        reservedAudioTracks.clear();
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
