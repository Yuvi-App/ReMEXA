package remexa.audio.smaf;

interface SmafSynthAdapter {
    void reset();

    void drumEnable(int channel, boolean enable);

    boolean isFinished();

    void keyOff(int channel, int key);

    void keyOn(int channel, int key, float velocity);

    void bankChange(int channel, int bank);

    void programChange(int channel, int program);

    void pitchBend(int channel, float semitones);

    void pitchBendRange(int channel, float range);

    void volume(int channel, float volume);

    void panpot(int channel, float panpot);

    default void modulation(int channel, int value) {
    }

    void render(float[] samples, int offset, int frames, float left, float right, boolean erase, boolean clamp);

    void sysEx(byte[] message);

    default void sysEx(int sourceBank, byte[] message) {
        sysEx(message);
    }
}
