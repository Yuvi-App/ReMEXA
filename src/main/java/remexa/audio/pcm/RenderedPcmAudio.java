package remexa.audio.pcm;

public record RenderedPcmAudio(int sampleRate, int channelCount, int frameCount, byte[] pcm16Le) {
}
