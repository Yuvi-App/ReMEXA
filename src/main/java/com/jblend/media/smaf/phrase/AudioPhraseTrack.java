/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package com.jblend.media.smaf.phrase;

import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class AudioPhraseTrack {
    public static final int NO_DATA = PhraseTrack.NO_DATA;
    public static final int READY = PhraseTrack.READY;
    public static final int PLAYING = PhraseTrack.PLAYING;
    public static final int PAUSED = PhraseTrack.PAUSED;
    private static final int DEFAULT_VOLUME = 100;
    private static final float AUDIO_PHRASE_GAIN = audioPhraseGain();

    private final PhraseTrack delegate;
    private int volume = DEFAULT_VOLUME;
    private int lastLoop = 1;

    AudioPhraseTrack(int id) {
        this.delegate = new PhraseTrack(id);
        this.delegate.setVolume(effectiveVolume(volume));
    }

    public void setPhrase(AudioPhrase phrase) {
        DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(), "Track " + getID() + " setPhrase(size="
                + (phrase == null ? 0 : phrase.getSize()) + ")");
        delegate.setPhrase(Phrase.unchecked(phrase.getData()));
    }

    public void removePhrase() {
        DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(), "Track " + getID() + " removePhrase()");
        delegate.removePhrase();
    }

    public AudioPhrase getPhrase() {
        Phrase phrase = delegate.phrase();
        return phrase == null ? null : new AudioPhrase(phrase.getData());
    }

    public void play() {
        MidletRuntime.ensureThreadActive();
        DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(), "Track " + getID() + " play(loop=1)");
        lastLoop = 1;
        delegate.play();
    }

    public void play(int loop) {
        MidletRuntime.ensureThreadActive();
        DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(), "Track " + getID() + " play(loop=" + loop + ")");
        lastLoop = loop;
        delegate.play(loop);
    }

    public void stop() {
        DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(), "Track " + getID() + " stop()");
        delegate.stop();
    }

    public void pause() {
        delegate.pause();
    }

    public void resume() {
        if (delegate.getState() == PhraseTrack.PAUSED) {
            DebugLog.log(LogCategory.MEDIA, AudioPhraseTrack.class.getName(),
                    "Track " + getID() + " resume() restarting phrase from beginning");
            delegate.play(lastLoop);
            return;
        }
        delegate.resume();
    }

    public int getState() {
        return delegate.getState();
    }

    public void setVolume(int value) {
        volume = Math.max(0, Math.min(127, value));
        delegate.setVolume(effectiveVolume(volume));
    }

    public int getVolume() {
        return volume;
    }

    public void setPanpot(int value) {
        delegate.setPanpot(value);
    }

    public int getPanpot() {
        return delegate.getPanpot();
    }

    public void mute(boolean mute) {
        delegate.mute(mute);
    }

    public boolean isMute() {
        return delegate.isMute();
    }

    public int getID() {
        return delegate.getID();
    }

    public void setEventListener(PhraseTrackListener listener) {
        delegate.setEventListener(listener);
    }

    PhraseTrack delegate() {
        return delegate;
    }

    private static int effectiveVolume(int value) {
        return Math.max(0, Math.min(127, Math.round(value * AUDIO_PHRASE_GAIN)));
    }

    private static float audioPhraseGain() {
        try {
            return Math.max(0.0f,
                    Float.parseFloat(System.getProperty("remexa.smaf.audioPhraseGain", "0.35")));
        } catch (NumberFormatException exception) {
            return 0.55f;
        }
    }
}
