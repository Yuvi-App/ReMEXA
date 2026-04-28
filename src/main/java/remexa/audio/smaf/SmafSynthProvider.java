package remexa.audio.smaf;

interface SmafSynthProvider {
    SmafSynthAdapter instance(float sampleRate);
}
