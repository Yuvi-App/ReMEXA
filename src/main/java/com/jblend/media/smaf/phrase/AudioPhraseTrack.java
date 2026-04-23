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

public final class AudioPhraseTrack {
    private final PhraseTrack delegate;

    AudioPhraseTrack(int id) {
        this.delegate = new PhraseTrack(id);
    }

    public void setPhrase(AudioPhrase phrase) {
        delegate.setPhrase(new Phrase(phrase.getData()));
    }

    public void removePhrase() {
        delegate.removePhrase();
    }

    public AudioPhrase getPhrase() {
        Phrase phrase = delegate.phrase();
        return phrase == null ? null : new AudioPhrase(phrase.getData());
    }

    public void play() {
        delegate.play();
    }

    public void play(int loop) {
        delegate.play(loop);
    }

    public void stop() {
        delegate.stop();
    }

    public void pause() {
        delegate.pause();
    }

    public void resume() {
        delegate.resume();
    }

    public int getState() {
        return delegate.getState();
    }

    public void setVolume(int value) {
        delegate.setVolume(value);
    }

    public int getVolume() {
        return delegate.volume();
    }

    public void setPanpot(int value) {
        delegate.setPanpot(value);
    }

    public int getPanpot() {
        return delegate.panpot();
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
}
