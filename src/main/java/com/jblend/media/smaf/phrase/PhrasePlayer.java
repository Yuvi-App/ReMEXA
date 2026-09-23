package com.jblend.media.smaf.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import remexa.host.runtime.MidletRuntime;

public final class PhrasePlayer {
    private static final Map<ClassLoader, PhrasePlayer> PLAYERS = new IdentityHashMap<>();

    private final ClassLoader ownerClassLoader;
    private boolean disposed;

    private final List<PhraseTrack> tracks = new ArrayList<>();
    private final List<AudioPhraseTrack> audioTracks = new ArrayList<>();
    private final Set<PhraseTrack> reservedTracks =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AudioPhraseTrack> reservedAudioTracks =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private PhrasePlayer(ClassLoader ownerClassLoader) {
        this.ownerClassLoader = ownerClassLoader;
        for (int i = 0; i < 4; i++) {
            tracks.add(new PhraseTrack(i, ownerClassLoader));
        }
        for (int i = 0; i < 4; i++) {
            audioTracks.add(new AudioPhraseTrack(i, ownerClassLoader));
        }
    }

    public static PhrasePlayer getPlayer() {
        synchronized (PLAYERS) {
            MidletRuntime.ensureThreadActive();
            return PLAYERS.computeIfAbsent(currentOwnerClassLoader(), PhrasePlayer::new);
        }
    }

    public synchronized PhraseTrack getTrack() {
        ensureActive();
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
        ensureActive();
        PhraseTrack track = tracks.get(index);
        reservedTracks.add(track);
        return track;
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public synchronized AudioPhraseTrack getAudioTrack() {
        ensureActive();
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
        ensureActive();
        AudioPhraseTrack track = audioTracks.get(index);
        reservedAudioTracks.add(track);
        return track;
    }

    public int getAudioTrackCount() {
        return audioTracks.size();
    }

    public synchronized void disposeTrack(PhraseTrack track) {
        int index = tracks.indexOf(track);
        if (index >= 0) {
            track.dispose();
            tracks.set(index, new PhraseTrack(index, ownerClassLoader));
            reservedTracks.remove(track);
        }
    }

    public synchronized void disposeAudioTrack(AudioPhraseTrack track) {
        int index = audioTracks.indexOf(track);
        if (index >= 0) {
            track.delegate().dispose();
            audioTracks.set(index, new AudioPhraseTrack(index, ownerClassLoader));
            reservedAudioTracks.remove(track);
        }
    }

    public synchronized void disposePlayer() {
        for (PhraseTrack track : List.copyOf(tracks)) {
            disposeTrack(track);
        }
        for (AudioPhraseTrack track : List.copyOf(audioTracks)) {
            disposeAudioTrack(track);
        }
        reservedTracks.clear();
        reservedAudioTracks.clear();
    }

    public synchronized void kill() {
        if (disposed || !MidletRuntime.isAppActive(ownerClassLoader)) {
            return;
        }
        // Games cache these handles and use kill() when changing screens. Only
        // disposal or app shutdown retires a handle and removes its listener.
        tracks.forEach(PhraseTrack::resetPlayback);
        audioTracks.forEach(track -> track.delegate().resetPlayback());
        reservedTracks.clear();
        reservedAudioTracks.clear();
    }

    public void disposePlayerOwnedBy(ClassLoader ownerClassLoader) {
        List<PhrasePlayer> retired = new ArrayList<>();
        synchronized (PLAYERS) {
            PLAYERS.entrySet().removeIf(entry -> {
                if (ownerClassLoader != null && entry.getKey() != ownerClassLoader) {
                    return false;
                }
                retired.add(entry.getValue());
                return true;
            });
        }
        for (PhrasePlayer player : retired) {
            player.shutdown();
        }
    }

    private synchronized void shutdown() {
        disposed = true;
        tracks.forEach(PhraseTrack::dispose);
        audioTracks.forEach(track -> track.delegate().dispose());
        reservedTracks.clear();
        reservedAudioTracks.clear();
    }

    private void ensureActive() {
        if (disposed || !MidletRuntime.isAppActive(ownerClassLoader)) {
            throw new IllegalStateException("Phrase player belongs to a closed appli");
        }
    }

    public void killOwnedBy(ClassLoader ownerClassLoader) {
        disposePlayerOwnedBy(ownerClassLoader);
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

    private synchronized void visitActive(java.util.function.Consumer<PhraseTrack> action) {
        ensureActive();
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

    private static ClassLoader currentOwnerClassLoader() {
        return MidletRuntime.currentAppClassLoader();
    }
}
