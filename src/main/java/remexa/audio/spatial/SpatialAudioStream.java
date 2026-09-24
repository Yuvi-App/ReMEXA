package remexa.audio.spatial;

import java.util.Arrays;
import remexa.audio.spatial.Audio3DScene.Vector;

/** Per-playback post-synthesis effects. All methods except controls run under the owning player's lock. */
public final class SpatialAudioStream {
    @FunctionalInterface public interface Reader {
        /** Read interleaved source samples, including loop transitions; zero means final EOF. */
        int read(float[] output, int maxFrames) throws Exception;
    }

    private static final int RING_FRAMES = 2048;
    private static final int FILTER_RADIUS = 12;
    private static final double SOUND_SPEED = 343000; // millimetres/second
    private final int sampleRate;
    private final int channels;
    private final Audio3DSource source;
    private final float[] ring;
    private final float[] readBuffer;
    private final float[] earDelay;
    private final RoomReverb reverb;
    private final PinnaFilter pinna;
    private final double smoothing;
    private long loaded;
    private long end = Long.MAX_VALUE;
    private double position;
    private double rate = 1, pan, gain = 1, spatialMix, rear, wet;
    private boolean initialized;
    private boolean wetActive;
    private boolean fedReverb;
    private int tailRemaining = -1;
    private int earIndex;
    private float shadowLeft, shadowRight;

    public SpatialAudioStream(int sampleRate, int channels, Audio3DSource source) {
        if (sampleRate <= 0 || channels < 1 || channels > 2 || source == null) {
            throw new IllegalArgumentException("Spatial audio requires mono or stereo PCM and a source");
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.source = source;
        ring = new float[RING_FRAMES * channels];
        readBuffer = new float[256 * channels];
        earDelay = new float[Math.max(2, (int) Math.ceil(sampleRate * .0007) + 2)];
        reverb = new RoomReverb(sampleRate);
        pinna = new PinnaFilter(sampleRate);
        smoothing = 1 - Math.exp(-1.0 / (sampleRate * .005));
    }

    public void reset() {
        loaded = 0;
        end = Long.MAX_VALUE;
        position = 0;
        initialized = wetActive = fedReverb = false;
        tailRemaining = -1;
        earIndex = 0;
        shadowLeft = shadowRight = 0;
        Arrays.fill(ring, 0);
        Arrays.fill(earDelay, 0);
        reverb.reset();
        pinna.reset();
    }

    /** Nominal source time, excluding the reverberation tail and including loop iterations. */
    public long mediaTimeMicros() { return (long) (Math.min(position, end) * 1_000_000 / sampleRate); }

    public int render(float[] output, int maxFrames, Reader reader) throws Exception {
        var state = source.snapshot();
        boolean dynamic = state.mode() == 2;
        Vector delta = state.placement().position();
        Vector velocity = state.placement().velocity();
        if (!state.placement().relative()) {
            delta = state.listener().toLocal(delta.minus(state.listener().position()));
            velocity = state.listener().toLocal(velocity.minus(state.listener().velocity()));
        }
        double distance = delta.length();
        double targetPan = dynamic && distance > 0 ? delta.x() / distance : 0;
        double targetRear = dynamic && distance > 0 ? Math.max(0, delta.z() / distance) : 0;
        pinna.direction(dynamic && distance > 0 ? delta.y() / distance : 0, targetRear);
        double minimum = Math.max(100, state.minDistance());
        double clampedDistance = Math.max(minimum, Math.min(state.maxDistance(), distance));
        // M7 0x10069030 scales 20*log10(min/distance) by the fixed-point rolloff.
        // MEXA halves the API percentage before forwarding it, giving this power law.
        double targetGain = dynamic ? Math.pow(minimum / clampedDistance, state.rolloff() / 200.0) : 1;
        double radialVelocity = distance == 0 ? 0 : velocity.dot(delta) / distance;
        double targetRate = dynamic ? Math.max(.5, Math.min(2, SOUND_SPEED
                / (SOUND_SPEED + Math.max(-SOUND_SPEED * .5, radialVelocity)))) : 1;
        boolean wantsWet = state.mode() != 0 && state.preset() != 7 && state.reverbLevel() > 0;
        double targetWet = wantsWet ? state.reverbLevel() / 100.0 : 0;
        if (wantsWet) reverb.configure(state.preset(), state.decayMillis());
        if (!wantsWet && wetActive) {
            // Disabling a mode/preset/send must not resurrect an old room on re-enable.
            reverb.reset();
            fedReverb = false;
            wet = 0;
        }
        wetActive = wantsWet;
        if (!initialized) {
            pan = targetPan; rear = targetRear; gain = targetGain; rate = targetRate;
            spatialMix = dynamic ? 1 : 0; wet = targetWet;
            initialized = true;
        }
        int frames = 0;
        while (frames < maxFrames) {
            pan += (targetPan - pan) * smoothing;
            rear += (targetRear - rear) * smoothing;
            gain += (targetGain - gain) * smoothing;
            rate += (targetRate - rate) * smoothing;
            spatialMix += ((dynamic ? 1 : 0) - spatialMix) * smoothing;
            wet += (targetWet - wet) * smoothing;
            boolean exact = Math.abs(rate - 1) < 1e-9 && position == Math.rint(position);
            long needed = (long) position + (exact ? 0 : FILTER_RADIUS);
            loadThrough(needed, reader);
            boolean exhausted = position >= end;
            if (exhausted) {
                if (tailRemaining < 0) {
                    tailRemaining = wantsWet && fedReverb ? reverb.tailFrames()
                            : spatialMix > .001 ? spatialTailFrames() : 0;
                }
                if (!wantsWet) tailRemaining = Math.min(tailRemaining, spatialTailFrames());
                if (tailRemaining == 0) break;
                tailRemaining--;
            }
            float left = exhausted ? 0 : sample(position, 0, exact);
            float right = exhausted ? 0 : channels == 1 ? left : sample(position, 1, exact);
            if (!exhausted) position += rate;
            float mono = (left + right) * .5f;
            earDelay[earIndex] = mono;
            double delay = state.outputDevice() == 1 ? Math.abs(pan) * sampleRate * .00063 : 0;
            float delayed = delayedEar(delay);
            // Head shadow is strongest at the far ear and behind the listener.
            double leftCutoff = 16000 - 11000 * Math.max(0, pan) - 6500 * rear;
            double rightCutoff = 16000 - 11000 * Math.max(0, -pan) - 6500 * rear;
            double leftAlpha = 1 - Math.exp(-2 * Math.PI * Math.max(1500, leftCutoff) / sampleRate);
            double rightAlpha = 1 - Math.exp(-2 * Math.PI * Math.max(1500, rightCutoff) / sampleRate);
            shadowLeft += (float) (leftAlpha * ((pan > 0 ? delayed : mono) - shadowLeft));
            shadowRight += (float) (rightAlpha * ((pan < 0 ? delayed : mono) - shadowRight));
            pinna.advance();
            // A far ear remains audible on headphones, allowing its delay/shadow cue to work.
            double earPan = state.outputDevice() == 1 ? pan * .9 : pan;
            float spatialLeft = (float) (pinna.tick(shadowLeft, 0) * Math.sqrt((1 - earPan) * .5) * gain);
            float spatialRight = (float) (pinna.tick(shadowRight, 1) * Math.sqrt((1 + earPan) * .5) * gain);
            float resultLeft = (float) (left * (1 - spatialMix) + spatialLeft * spatialMix);
            float resultRight = (float) (right * (1 - spatialMix) + spatialRight * spatialMix);
            if (wantsWet) {
                float send = (float) (mono * gain * wet);
                if (Math.abs(send) > 1e-8) fedReverb = true;
                reverb.tick(send);
                resultLeft += reverb.left;
                resultRight += reverb.right;
            }
            output[frames * 2] = resultLeft;
            output[frames * 2 + 1] = resultRight;
            if (++earIndex == earDelay.length) earIndex = 0;
            frames++;
        }
        return frames;
    }

    private void loadThrough(long needed, Reader reader) throws Exception {
        while (loaded <= needed && end == Long.MAX_VALUE) {
            int frames = reader.read(readBuffer, readBuffer.length / channels);
            if (frames <= 0) { end = loaded; return; }
            for (int frame = 0; frame < frames; frame++) {
                int target = (int) ((loaded + frame) % RING_FRAMES) * channels;
                System.arraycopy(readBuffer, frame * channels, ring, target, channels);
            }
            loaded += frames;
        }
    }

    private int spatialTailFrames() { return Math.max(earDelay.length, sampleRate / 50); }

    private float sample(double at, int channel, boolean exact) {
        if (exact) return raw((long) at, channel);
        // Windowed sinc, with a rate-dependent low-pass when approaching, prevents pitch-up aliasing.
        int radius = FILTER_RADIUS;
        long center = (long) at;
        double cutoff = Math.min(1, 1 / rate), value = 0, weight = 0;
        for (long frame = center - radius + 1; frame <= center + radius; frame++) {
            double offset = frame - at;
            double x = Math.PI * offset * cutoff;
            double sinc = Math.abs(x) < 1e-10 ? 1 : Math.sin(x) / x;
            double w = sinc * (.5 + .5 * Math.cos(Math.PI * offset / radius));
            value += raw(frame, channel) * w;
            weight += w;
        }
        return (float) (value / weight);
    }

    private float raw(long frame, int channel) {
        return frame < 0 || frame >= end ? 0 : ring[(int) (frame % RING_FRAMES) * channels + channel];
    }

    private float delayedEar(double delay) {
        double index = earIndex - delay;
        if (index < 0) index += earDelay.length;
        int first = (int) index;
        double fraction = index - first;
        return (float) (earDelay[first] * (1 - fraction) + earDelay[(first + 1) % earDelay.length] * fraction);
    }
}
