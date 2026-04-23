package com.jblend.media.smaf.phrase;

public class AudioPhraseTrack extends PhraseTrackBase {
    public static final int NO_DATA = 1;
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 5;
    public static final int DEFAULT_VOLUME = 127;
    public static final int DEFAULT_PANPOT = 64;

    private final int id;
    private AudioPhrase phrase;
    private PhraseTrackListener listener;
    private int state = NO_DATA;
    private int volume = DEFAULT_VOLUME;
    private int panpot = DEFAULT_PANPOT;
    private boolean muted;

    AudioPhraseTrack(int id) {
        this.id = id;
    }

    public void setPhrase(AudioPhrase p) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setPhrase", p);
        phrase = p;
        state = p == null ? NO_DATA : READY;
    }

    public AudioPhrase getPhrase() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getPhrase");
        return phrase;
    }

    public void removePhrase() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "removePhrase");
        phrase = null;
        state = NO_DATA;
    }

    public void play() {
        play(1);
    }

    public void play(int loop) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "play", loop);
        if (phrase != null) {
            state = PLAYING;
        }
    }

    public void stop() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "stop");
        if (phrase != null) {
            state = READY;
        }
    }

    public void pause() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "pause");
        if (state == PLAYING) {
            state = PAUSED;
        }
    }

    public void resume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "resume");
        if (state == PAUSED) {
            state = PLAYING;
        }
    }

    public int getState() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getState");
        return state;
    }

    public void setVolume(int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setVolume", value);
        volume = Math.max(0, Math.min(127, value));
    }

    public int getVolume() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getVolume");
        return volume;
    }

    public void setPanpot(int value) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setPanpot", value);
        panpot = Math.max(0, Math.min(127, value));
    }

    public int getPanpot() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getPanpot");
        return panpot;
    }

    public void mute(boolean mute) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "mute", mute);
        muted = mute;
    }

    public boolean isMute() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "isMute");
        return muted;
    }

    public int getID() {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "getID");
        return id;
    }

    public void setEventListener(PhraseTrackListener l) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.smaf.phrase.AudioPhraseTrack", "setEventListener", l);
        listener = l;
    }

    PhraseTrackListener listener() {
        return listener;
    }
}
