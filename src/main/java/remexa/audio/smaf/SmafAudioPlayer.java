package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhraseTrackListener;

interface SmafAudioPlayer extends AutoCloseable {
    int getState();

    void setListener(PhraseTrackListener listener);

    void setVolume(int value);

    void setPanpot(int value);

    void play(int loopCount);

    void stop();

    void pause();

    void resume();

    @Override
    void close();
}
