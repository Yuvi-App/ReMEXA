package com.jblend.media.smaf.phrase;

import remexa.audio.smaf.SmafPlayback;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import remexa.host.runtime.MidletRuntime;
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
    private PhraseTrack subjectTo;
    private PhraseTrackListener listener;
    private GroupLoopCoordinator loopCoordinator;
    private ClassLoader ownerClassLoader;
    private int volume = 127;
    private int panpot = 64;
    private boolean muted;
    private boolean terminalEventDispatched;
    private boolean forcedStopTerminalEventAllowed = true;

    PhraseTrack(int id) {
        this.id = id;
    }

    void reserveFor(ClassLoader ownerClassLoader) {
        this.ownerClassLoader = ownerClassLoader;
    }

    boolean isOwnedBy(ClassLoader candidate) {
        return candidate == null || ownerClassLoader == candidate;
    }

    void clearOwner() {
        ownerClassLoader = null;
    }

    public void setPhrase(Phrase phrase) {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " setPhrase(size="
                + (phrase == null ? 0 : phrase.getSize()) + ")");
        try {
            cancelLoopCoordinator();
            dispatchTerminalEventIfNeeded("replace");
            closePlayback();
            this.phrase = phrase;
            terminalEventDispatched = false;
            if (phrase == null) {
                return;
            }
            playback = SmafPlayback.create(phrase.getData());
            playback.setVolume(muted ? 0 : volume);
            playback.setPanpot(panpot);
            playback.setListener(this::handlePlaybackEvent);
            playback.prepareAsync();
        } catch (Exception exception) {
            DebugLog.log(LogCategory.AUDIO, PhraseTrack.class.getName(),
                    "Track " + id + " setPhrase failed: " + exception.getMessage());
            throw new RuntimeException("Failed to create SMAF playback", exception);
        }
    }

    public void removePhrase() {
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " removePhrase()");
        boolean dispatchTerminalEvent = shouldDispatchTerminalEvent();
        cancelLoopCoordinator();
        stopInternal(new HashSet<>());
        clearSyncRelationship();
        closePlayback();
        this.phrase = null;
        if (dispatchTerminalEvent) {
            dispatchTerminalEvent("removePhrase");
        }
    }

    public void setEventListener(PhraseTrackListener listener) {
        this.listener = listener;
    }

    public void setSubjectTo(PhraseTrack masterTrack) {
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
    }

    public void mute(boolean value) {
        muted = value;
        if (playback != null) {
            playback.setVolume(effectiveVolume());
        }
    }

    public boolean isMute() {
        return muted;
    }

    public int getState() {
        if (playback == null) {
            return NO_DATA;
        }
        return playback.getState();
    }

    public void play() {
        MidletRuntime.ensureThreadActive();
        play(1);
    }

    public void play(int loop) {
        MidletRuntime.ensureThreadActive();
        DebugLog.log(LogCategory.MEDIA, PhraseTrack.class.getName(), "Track " + id + " play(loop=" + loop + ")");
        if (subjectTo != null) {
            return;
        }
        try {
            playGroup(loop);
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
        boolean dispatchTerminalEvent = shouldDispatchTerminalEvent();
        cancelLoopCoordinator();
        stopInternal(new HashSet<>());
        if (dispatchTerminalEvent) {
            dispatchTerminalEvent("stop");
        }
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
        terminalEventDispatched = false;
        forcedStopTerminalEventAllowed = loop == 1;
        playback.play(loop);
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.playInternal(loop, visited);
        }
    }

    private void playGroup(int loop) {
        cancelLoopCoordinator();
        List<PhraseTrack> group = new ArrayList<>();
        collectPlaybackGroup(group, new HashSet<>());
        preparePlaybackGroup(group);
        for (PhraseTrack track : group) {
            track.forcedStopTerminalEventAllowed = loop == 1;
        }
        if (group.size() == 1 || loop == 1) {
            startPreparedPlaybackGroup(group, loop);
            return;
        }
        GroupLoopCoordinator coordinator = new GroupLoopCoordinator(group, loop);
        coordinator.start();
    }

    private void collectPlaybackGroup(List<PhraseTrack> group, Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
            return;
        }
        group.add(this);
        for (PhraseTrack slaveTrack : slaveTracks) {
            slaveTrack.collectPlaybackGroup(group, visited);
        }
    }

    private static void preparePlaybackGroup(List<PhraseTrack> group) {
        for (PhraseTrack track : group) {
            track.ensurePlayback();
        }
        for (PhraseTrack track : group) {
            try {
                track.playback.prefetch();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to prepare phrase track " + track.id, exception);
            }
        }
    }

    private static void startPreparedPlaybackGroup(List<PhraseTrack> group, int loop) {
        List<PhraseTrack> started = new ArrayList<>(group.size());
        try {
            for (PhraseTrack track : group) {
                track.terminalEventDispatched = false;
                track.playback.play(loop);
                started.add(track);
            }
        } catch (RuntimeException exception) {
            for (PhraseTrack track : started) {
                try {
                    track.playback.stop();
                } catch (RuntimeException ignored) {
                }
            }
            throw exception;
        }
    }

    private void handlePlaybackEvent(int eventId) {
        if (eventId == -1) {
            terminalEventDispatched = true;
            GroupLoopCoordinator coordinator = loopCoordinator;
            if (coordinator != null) {
                coordinator.onTrackCompleted(this);
                return;
            }
        }
        dispatchExternalEvent(eventId);
    }

    private void dispatchExternalEvent(int eventId) {
        PhraseTrackListener currentListener = listener;
        if (currentListener != null) {
            currentListener.eventOccurred(eventId);
        }
    }

    private void dispatchTerminalEventIfNeeded(String reason) {
        if (shouldDispatchTerminalEvent()) {
            dispatchTerminalEvent(reason);
        }
    }

    private boolean shouldDispatchTerminalEvent() {
        if (terminalEventDispatched || playback == null) {
            return false;
        }
        int state = playback.getState();
        return forcedStopTerminalEventAllowed && (state == PLAYING || state == PAUSED);
    }

    private void dispatchTerminalEvent(String reason) {
        terminalEventDispatched = true;
        DebugLog.log(LogCategory.AUDIO, PhraseTrack.class.getName(),
                "Track " + id + " dispatch terminal event after " + reason);
        dispatchExternalEvent(-1);
    }

    private void stopInternal(Set<PhraseTrack> visited) {
        if (!visited.add(this)) {
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

    private void cancelLoopCoordinator() {
        PhraseTrack masterTrack = subjectTo == null ? this : subjectTo;
        GroupLoopCoordinator coordinator = masterTrack.loopCoordinator;
        if (coordinator != null) {
            coordinator.cancel();
        }
    }

    private int effectiveVolume() {
        return muted ? 0 : volume;
    }

    private static String describeException(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        StringBuilder description = new StringBuilder(96);
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 4) {
            if (depth > 0) {
                description.append(" <- ");
            }
            description.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                description.append(": ").append(message);
            }
            current = current.getCause();
            depth++;
        }
        return description.toString();
    }

    private static final class GroupLoopCoordinator {
        private final List<PhraseTrack> group;
        private final Set<PhraseTrack> completedTracks = new HashSet<>();
        private int remainingRepeats;
        private boolean cancelled;

        private GroupLoopCoordinator(List<PhraseTrack> group, int loop) {
            this.group = List.copyOf(group);
            this.remainingRepeats = loop == 0 ? -1 : Math.max(0, loop - 1);
            attachLocked();
        }

        private void start() {
            startPreparedPlaybackGroup(group, 1);
        }

        private synchronized void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            completedTracks.clear();
            detachLocked();
        }

        private void onTrackCompleted(PhraseTrack track) {
            boolean restart = false;
            boolean finished = false;
            synchronized (this) {
                if (cancelled || track.loopCoordinator != this) {
                    return;
                }
                completedTracks.add(track);
                if (completedTracks.size() < group.size()) {
                    return;
                }
                completedTracks.clear();
                if (remainingRepeats == -1 || remainingRepeats > 0) {
                    if (remainingRepeats > 0) {
                        remainingRepeats--;
                    }
                    restart = true;
                } else {
                    cancelled = true;
                    detachLocked();
                    finished = true;
                }
            }
            if (restart) {
                try {
                    startPreparedPlaybackGroup(group, 1);
                } catch (RuntimeException exception) {
                    DebugLog.log(LogCategory.AUDIO, PhraseTrack.class.getName(),
                            "Grouped loop restart failed: " + exception.getMessage());
                    cancel();
                    finished = true;
                }
            }
            if (finished) {
                for (PhraseTrack phraseTrack : group) {
                    phraseTrack.dispatchExternalEvent(-1);
                }
            }
        }

        private synchronized void attachLocked() {
            for (PhraseTrack track : group) {
                track.loopCoordinator = this;
            }
        }

        private synchronized void detachLocked() {
            for (PhraseTrack track : group) {
                if (track.loopCoordinator == this) {
                    track.loopCoordinator = null;
                }
            }
        }
    }
}
