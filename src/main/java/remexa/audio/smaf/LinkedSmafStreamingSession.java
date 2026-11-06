package remexa.audio.smaf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Linked layers share one output clock */
final class LinkedSmafStreamingSession implements SmafStreamingSession {
    private final List<Layer> layers;
    private final int sampleRate;
    private final Supplier<Runnable> loopEvent;
    private final List<Runnable> notifications = new ArrayList<>();
    private float[] scratch = new float[0];
    private int loopCount = 1;
    private int remainingRepeats;
    private long iterationFrames;

    LinkedSmafStreamingSession(List<SmafStreamingSession> sources, int sampleRate, Supplier<Runnable> loopEvent) {
        if (sources.isEmpty()) throw new IllegalArgumentException("No linked layers");
        this.sampleRate = sampleRate;
        this.loopEvent = loopEvent;
        layers = sources.stream().map(Layer::new).toList();
    }

    void setLoopCount(int count) {
        loopCount = count == 0 ? 0 : Math.clamp(count, 1, 255);
    }

    @Override public int sampleRate() { return sampleRate; }
    @Override public int channelCount() { return 2; }

    @Override public int render(float[] output, int maxFrames) throws Exception {
        Arrays.fill(output, 0, maxFrames * 2, 0f);
        if (scratch.length < maxFrames * 2) scratch = new float[maxFrames * 2];
        int produced = 0;
        while (produced < maxFrames) {
            int mixed = 0;
            for (Layer layer : layers) {
                int count = maxFrames - produced;
                if (layer.audio.length < count * 2) layer.audio = new float[count * 2];
                layer.frames = layer.renderSequence(layer.audio, count);
                mixed = Math.max(mixed, layer.frames);
                layer.source.drainNotifications(notifications::add);
            }
            if (mixed > 0) {
                for (Layer layer : layers) {
                    for (int i = 0; i < layer.frames * 2; i++) output[produced * 2 + i] += layer.audio[i];
                    if (layer.frames < mixed) {
                        int tail = layer.renderTail(scratch, mixed - layer.frames);
                        for (int i = 0; i < tail * 2; i++) output[(produced + layer.frames) * 2 + i] += scratch[i];
                    }
                }
                produced += mixed;
                iterationFrames += mixed;
                continue;
            }
            if (iterationFrames == 0 || remainingRepeats == 0) break;
            if (remainingRepeats > 0) remainingRepeats--;
            if (loopEvent != null) notifications.add(loopEvent.get());
            rewindLayers(true);
        }
        return produced;
    }

    @Override public int renderTail(float[] output, int maxFrames) throws Exception {
        Arrays.fill(output, 0, maxFrames * 2, 0f);
        if (scratch.length < maxFrames * 2) scratch = new float[maxFrames * 2];
        int produced = 0;
        for (Layer layer : layers) {
            int frames = layer.renderTail(scratch, maxFrames);
            for (int i = 0; i < frames * 2; i++) output[i] += scratch[i];
            produced = Math.max(produced, frames);
        }
        return produced;
    }

    @Override public void rewind() throws Exception {
        notifications.clear();
        remainingRepeats = loopCount == 0 ? -1 : loopCount - 1;
        rewindLayers(false);
    }

    private void rewindLayers(boolean repeat) throws Exception {
        iterationFrames = 0;
        for (Layer layer : layers) layer.rewind(repeat);
    }

    @Override public void drainNotifications(Consumer<Runnable> sink) {
        notifications.forEach(sink);
        notifications.clear();
    }

    private final class Layer {
        private final SmafStreamingSession source;
        private float[] buffer = new float[0];
        private float[] input = new float[0];
        private int bufferedFrames;
        private long bufferStart;
        private long sourceEnd = Long.MAX_VALUE;
        private long outputPosition;
        private long phase;
        private float[] audio = new float[0];
        private int frames;
        private boolean sequenceEnded;
        private boolean tailMode;

        private Layer(SmafStreamingSession source) {
            if (source.channelCount() != 2 || source.sampleRate() <= 0)
                throw new IllegalArgumentException("Linked layers must provide stereo PCM at a positive rate");
            this.source = source;
        }

        private void rewind(boolean repeat) throws Exception {
            source.setLoopMode(true);
            if (repeat) source.repeat(); else source.rewind();
            bufferedFrames = 0;
            bufferStart = phase = outputPosition = 0;
            sourceEnd = Long.MAX_VALUE;
            sequenceEnded = tailMode = false;
        }

        private int renderSequence(float[] output, int maxFrames) throws Exception {
            if (sequenceEnded) return 0;
            int frames = render(output, maxFrames);
            if (frames < maxFrames) sequenceEnded = true;
            return frames;
        }

        private int renderTail(float[] output, int maxFrames) throws Exception {
            if (!tailMode) { tailMode = true; sourceEnd = Long.MAX_VALUE; }
            return render(output, maxFrames);
        }

        private int render(float[] output, int maxFrames) throws Exception {
            if (source.sampleRate() == sampleRate)
                return tailMode ? source.renderTail(output, maxFrames) : source.render(output, maxFrames);
            long first = phase / sampleRate;
            int discarded = (int) Math.min(bufferedFrames, first - bufferStart);
            if (discarded > 0) {
                System.arraycopy(buffer, discarded * 2, buffer, 0, (bufferedFrames - discarded) * 2);
                bufferedFrames -= discarded;
                bufferStart += discarded;
            }
            long needed = (phase + (maxFrames - 1L) * source.sampleRate()) / sampleRate + 1;
            int capacity = Math.toIntExact(needed - bufferStart + 1);
            if (buffer.length < capacity * 2) buffer = Arrays.copyOf(buffer, capacity * 2);
            while (bufferStart + bufferedFrames <= needed && sourceEnd == Long.MAX_VALUE) {
                int count = Math.toIntExact(needed - bufferStart - bufferedFrames + 1);
                if (input.length < count * 2) input = new float[count * 2];
                int frames = tailMode ? source.renderTail(input, count) : source.render(input, count);
                if (frames <= 0) {
                    sourceEnd = bufferStart + bufferedFrames;
                    break;
                }
                System.arraycopy(input, 0, buffer, bufferedFrames * 2, frames * 2);
                bufferedFrames += frames;
            }
            long end = sourceEnd == Long.MAX_VALUE ? Long.MAX_VALUE
                    : Math.round(sourceEnd * (double) sampleRate / source.sampleRate());
            int produced = 0;
            while (produced < maxFrames && outputPosition < end) {
                long index = phase / sampleRate;
                int offset = Math.toIntExact(index - bufferStart) * 2;
                int next = index + 1 < sourceEnd ? offset + 2 : offset;
                float fraction = (phase % sampleRate) / (float) sampleRate;
                output[produced * 2] = buffer[offset] + (buffer[next] - buffer[offset]) * fraction;
                output[produced * 2 + 1] = buffer[offset + 1] + (buffer[next + 1] - buffer[offset + 1]) * fraction;
                phase += source.sampleRate();
                outputPosition++;
                produced++;
            }
            return produced;
        }
    }
}
