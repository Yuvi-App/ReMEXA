package com.jblend.media.smaf.phrase;

import remexa.audio.smaf.SmafRenderedAudio;
import remexa.audio.smaf.SmafPlayback;
import remexa.audio.smaf.SmafRenderedPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class PhraseTrack {
    public static final int NO_DATA = 1;
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 5;

    private final int id;
    private final List<PhraseTrack> slaveTracks = new ArrayList<>();

    private Phrase phrase;
    private SmafPlayback playback;
    private SmafRenderedPlayer linkedRenderedPlayer;
    private PhraseTrack subjectTo;
    private PhraseTrackListener listener;
    private int volume = 127;
    private int panpot = 64;
    private boolean muted;

    PhraseTrack(int id) {
        this.id = id;
    }

    public void setPhrase(Phrase phrase) {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " setPhrase(size="
                + (phrase == null ? 0 : phrase.getSize()) + ")");
        try {
            closeMasterLinkedRenderedPlayback();
            closePlayback();
            this.phrase = phrase;
            if (phrase == null) {
                return;
            }
            playback = SmafPlayback.create(phrase.getData());
            playback.setVolume(muted ? 0 : volume);
            playback.setPanpot(panpot);
            playback.setListener(listener);
        } catch (Exception exception) {
            DebugLog.log(LogCategory.AUDIO, PhraseTrack.class.getName(),
                    "Track " + id + " setPhrase failed: " + exception.getMessage());
            throw new RuntimeException("Failed to create SMAF playback", exception);
        }
    }

    public void removePhrase() {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " removePhrase()");
        closeMasterLinkedRenderedPlayback();
        stopInternal(new HashSet<>());
        clearSyncRelationship();
        closePlayback();
        this.phrase = null;
    }

    public void setEventListener(PhraseTrackListener listener) {
        this.listener = listener;
        if (playback != null) {
            playback.setListener(listener);
        }
        if (linkedRenderedPlayer != null) {
            linkedRenderedPlayer.setListener(listener);
        }
    }

    public void setSubjectTo(PhraseTrack masterTrack) {
        closeMasterLinkedRenderedPlayback();
        if (masterTrack != null) {
            masterTrack.closeMasterLinkedRenderedPlayback();
        }
        if (subjectTo != null) {
            subjectTo.slaveTracks.remove(this);
        }
        if (masterTrack == this || createsCycle(masterTrack)) {
            this.subjectTo = null;
            return;
        }
        this.subjectTo = masterTrack;
        if (masterTrack != null && !masterTrack.slaveTracks.contains(this)) {
            masterTrack.slaveTracks.add(this);
        }
    }

    public void setVolume(int value) {
        this.volume = Math.max(0, Math.min(127, value));
        if (playback != null) {
            playback.setVolume(effectiveVolume());
        }
        if (linkedRenderedPlayer != null) {
            linkedRenderedPlayer.setVolume(effectiveVolume());
        }
    }

    public void mute(boolean value) {
        muted = value;
        if (playback != null) {
            playback.setVolume(effectiveVolume());
        }
        if (linkedRenderedPlayer != null) {
            linkedRenderedPlayer.setVolume(effectiveVolume());
        }
    }

    public boolean isMute() {
        return muted;
    }

    public int getState() {
        PhraseTrack masterTrack = groupMaster();
        if (masterTrack.linkedRenderedPlayer != null) {
            return masterTrack.linkedRenderedPlayer.getState();
        }
        if (playback == null) {
            return NO_DATA;
        }
        return playback.getState();
    }

    public void play() {
        play(1);
    }

    public void play(int loop) {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " play(loop=" + loop + ")");
        if (subjectTo != null) {
            return;
        }
        try {
            playInternal(loop, new HashSet<>());
        } catch (RuntimeException exception) {
            DebugLog.log(LogCategory.AUDIO, PhraseTrack.class.getName(),
                    "Track " + id + " play failed: " + exception.getMessage());
            throw exception;
        }
    }

    public void stop() {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " stop()");
        if (subjectTo != null) {
            return;
        }
        stopInternal(new HashSet<>());
    }

    public void pause() {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " pause()");
        if (subjectTo != null) {
            return;
        }
        pauseInternal(new HashSet<>());
    }

    public void resume() {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " resume()");
        if (subjectTo != null) {
            return;
        }
        resumeInternal(new HashSet<>());
    }

    public Phrase phrase() {
        return phrase;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public PhraseTrack subjectTo() {
        return subjectTo;
    }

    public PhraseTrack getSubjectTo() {
        return subjectTo;
    }

    public PhraseTrack getSyncMaster() {
        return subjectTo;
    }

    public int volume() {
        return volume;
    }

    public int getVolume() {
        return volume;
    }

    public int panpot() {
        return panpot;
    }

    public int getPanpot() {
        return panpot;
    }

    public int getID() {
        return id;
    }

    public void setPanpot(int value) {
        panpot = Math.max(0, Math.min(127, value));
        if (playback != null) {
            playback.setPanpot(panpot);
        }
        if (linkedRenderedPlayer != null) {
            linkedRenderedPlayer.setPanpot(panpot);
        }
    }

    private void ensurePlayback() {
        if (playback == null) {
            throw new RuntimeException("Cannot play phrase track without phrase data");
        }
    }

    private void playInternal(int loop, Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        ensurePlayback();
        if (tryPlayLinkedRendered(loop)) {
            return;
        }
        playback.play(loop);
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.playInternal(loop, visited);
        }
    }

    private void stopInternal(Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        PhraseTrack masterTrack = groupMaster();
        if (masterTrack.linkedRenderedPlayer != null) {
            if (this == masterTrack) {
                masterTrack.linkedRenderedPlayer.stop();
            }
            return;
        }
        if (playback != null) {
            playback.stop();
        }
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.stopInternal(visited);
        }
    }

    private void pauseInternal(Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        PhraseTrack masterTrack = groupMaster();
        if (masterTrack.linkedRenderedPlayer != null) {
            if (this == masterTrack) {
                masterTrack.linkedRenderedPlayer.pause();
            }
            return;
        }
        if (playback != null) {
            playback.pause();
        }
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.pauseInternal(visited);
        }
    }

    private void resumeInternal(Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        PhraseTrack masterTrack = groupMaster();
        if (masterTrack.linkedRenderedPlayer != null) {
            if (this == masterTrack) {
                masterTrack.linkedRenderedPlayer.resume();
            }
            return;
        }
        if (playback != null) {
            playback.resume();
        }
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.resumeInternal(visited);
        }
    }

    private boolean createsCycle(PhraseTrack masterTrack) {
        PhraseTrack current = masterTrack;
        while (current != null) {
            if (current == this) {
                return true;
            }
            current = current.subjectTo;
        }
        return false;
    }

    private void clearSyncRelationship() {
        if (subjectTo != null) {
            subjectTo.slaveTracks.remove(this);
            subjectTo = null;
        }
        if (!slaveTracks.isEmpty()) {
            List<PhraseTrack> oldSlaves = new ArrayList<>(slaveTracks);
            slaveTracks.clear();
            for (PhraseTrack slaveTrack : oldSlaves) {
                if (slaveTrack.subjectTo == this) {
                    slaveTrack.subjectTo = null;
                }
            }
        }
    }

    private void closePlayback() {
        if (playback != null) {
            playback.close();
            playback = null;
        }
    }

    private boolean tryPlayLinkedRendered(int loop) {
        if (!subjectToRoot() || slaveTracks.isEmpty()) {
            return false;
        }

        List<PhraseTrack> linkedTracks = new ArrayList<>();
        collectLinkedTracks(linkedTracks, new HashSet<>());
        if (linkedTracks.size() <= 1) {
            return false;
        }

        int masterVolume = effectiveVolume();
        float baseVolume = masterVolume <= 0 ? 1.0f : masterVolume;
        List<SmafRenderedAudio.Layer> layers = new ArrayList<>(linkedTracks.size());
        try {
            for (PhraseTrack track : linkedTracks) {
                if (track.playback == null) {
                    return false;
                }
                SmafRenderedAudio audio = track.playback.renderedAudio();
                if (audio == null) {
                    return false;
                }
                float gain = masterVolume <= 0 ? 0.0f : track.effectiveVolume() / baseVolume;
                layers.add(new SmafRenderedAudio.Layer(audio, gain, track.panpot));
            }
        } catch (Exception exception) {
            return false;
        }

        closeLinkedRenderedPlayback();
        linkedRenderedPlayer = new SmafRenderedPlayer(SmafRenderedAudio.mix(layers));
        linkedRenderedPlayer.setListener(listener);
        linkedRenderedPlayer.setVolume(masterVolume);
        linkedRenderedPlayer.setPanpot(64);
        linkedRenderedPlayer.play(loop);
        return true;
    }

    private void collectLinkedTracks(List<PhraseTrack> linkedTracks, Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        linkedTracks.add(this);
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.collectLinkedTracks(linkedTracks, visited);
        }
    }

    private boolean subjectToRoot() {
        return subjectTo == null;
    }

    private int effectiveVolume() {
        return muted ? 0 : volume;
    }

    private PhraseTrack groupMaster() {
        PhraseTrack current = this;
        while (current.subjectTo != null) {
            current = current.subjectTo;
        }
        return current;
    }

    private void closeMasterLinkedRenderedPlayback() {
        groupMaster().closeLinkedRenderedPlayback();
    }

    private void closeLinkedRenderedPlayback() {
        if (linkedRenderedPlayer != null) {
            linkedRenderedPlayer.close();
            linkedRenderedPlayer = null;
        }
    }
}
