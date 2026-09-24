package remexa.audio.spatial;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpatialAudioStreamTest {
    private static final int RATE = 8000;

    @Test public void disabledEffectsPreserveStereoAndMonoSamplesAndDuration() throws Exception {
        var source = new Audio3DScene().createSource();
        float[] stereo = new float[1234];
        for (int i = 0; i < stereo.length; i++) stereo[i] = (i % 101 - 50) / 100f;
        assertArrayEquals(stereo, render(source, stereo, 2, 31), 0);
        float[] mono = tone(617, 430);
        float[] result = render(source, mono, 1, 71);
        assertEquals(mono.length * 2, result.length);
        for (int i = 0; i < mono.length; i++) {
            assertEquals(mono[i], result[2 * i], 0);
            assertEquals(mono[i], result[2 * i + 1], 0);
        }
    }

    @Test public void sourcePositionMovesSoundBetweenEars() throws Exception {
        var source = dynamic();
        source.setPosition(-1000, 0, -500);
        float[] left = render(source, tone(RATE, 600), 1, 512);
        source.setPosition(1000, 0, -500);
        float[] right = render(source, tone(RATE, 600), 1, 512);
        assertTrue(energy(left, 0, 0, RATE) > energy(left, 1, 0, RATE) * 10);
        assertTrue(energy(right, 1, 0, RATE) > energy(right, 0, 0, RATE) * 10);
        assertEquals(energy(left, 0, 0, RATE), energy(right, 1, 0, RATE), 1e-6);
    }

    @Test public void rolloffClampsDistanceAndZeroFactorDisablesAttenuation() throws Exception {
        var source = dynamic();
        source.setRolloff(100, 1000, 100);
        source.setPosition(0, 0, -100);
        double near = energy(render(source, tone(RATE, 400), 1, 512), 0, 0, RATE);
        source.setPosition(0, 0, -1000);
        double far = energy(render(source, tone(RATE, 400), 1, 512), 0, 0, RATE);
        source.setPosition(0, 0, -2000);
        double beyond = energy(render(source, tone(RATE, 400), 1, 512), 0, 0, RATE);
        assertEquals(.1, far / near, 1e-6);
        assertEquals(far, beyond, 1e-6);
        source.setRolloff(100, 1000, 0);
        assertEquals(near, energy(render(source, tone(RATE, 400), 1, 512), 0, 0, RATE), 1e-6);
    }

    @Test public void approachingAndRecedingSourcesChangePitchAndSourceDuration() throws Exception {
        var source = dynamic();
        source.setPosition(0, 0, -1000);
        source.setVelocity(0, 0, 34300);
        float[] approaching = render(source, tone(RATE * 2, 400), 1, 71);
        assertEquals(400 / .9, frequency(approaching), 2);
        assertEquals(RATE * 1.8 + RATE / 50, approaching.length / 2.0, 2);
        source.setVelocity(0, 0, -34300);
        float[] receding = render(source, tone(RATE * 2, 400), 1, 511);
        assertEquals(400 / 1.1, frequency(receding), 2);
        assertEquals(RATE * 2.2 + RATE / 50, receding.length / 2.0, 2);
    }

    @Test public void pitchUpFiltersFrequenciesAboveTheNewNyquistLimit() throws Exception {
        var source = dynamic();
        source.setPosition(0, 0, -1000);
        source.setVelocity(0, 0, 171500);
        float[] low = render(source, tone(RATE, 400), 1, 512);
        float[] high = render(source, tone(RATE, 3000), 1, 512);
        assertTrue(energy(high, 0, 100, 3800) < energy(low, 0, 100, 3800) * .001);
    }

    @Test public void reverbHasStereoTailAndLongerDecayRetainsMoreLateEnergy() throws Exception {
        var scene = new Audio3DScene();
        var source = scene.createSource();
        source.setMode(1);
        source.setReverbLevel(100);
        scene.setReverb(8, 300);
        float[] impulse = {1};
        float[] shortRoom = render(source, impulse, 1, 127);
        scene.setReverb(8, 2000);
        float[] longRoom = render(source, impulse, 1, 127);
        assertTrue(shortRoom.length > RATE / 2);
        assertTrue(longRoom.length > shortRoom.length);
        assertTrue(energy(longRoom, 0, RATE / 2, RATE) > energy(shortRoom, 0, RATE / 2, RATE) * 100);
        assertFalse(Arrays.equals(channel(longRoom, 0), channel(longRoom, 1)));
        source.setReverbLevel(0);
        assertArrayEquals(new float[]{1, 1}, render(source, impulse, 1, 127), 0);
        source.setReverbLevel(100);
        scene.setReverb(7, 0);
        assertArrayEquals(new float[]{1, 1}, render(source, impulse, 1, 127), 0);
    }

    @Test public void disablingReverbClearsAccumulatedTailAndResetDoesNotLeakIntoReplay() throws Exception {
        var scene = new Audio3DScene();
        var source = scene.createSource();
        source.setMode(1); source.setReverbLevel(100); scene.setReverb(2, 2000);
        var stream = new SpatialAudioStream(RATE, 1, source);
        var input = new Input(new float[]{1}, 1);
        float[] chunk = new float[1024];
        stream.render(chunk, 512, input);
        source.setMode(0);
        int frames = stream.render(chunk, 512, input);
        assertEquals(0, energy(Arrays.copyOf(chunk, frames * 2), 0, 0, frames), 0);
        source.setMode(1);
        stream.reset();
        var silence = new Input(new float[RATE], 1);
        while ((frames = stream.render(chunk, 512, silence)) > 0) {
            for (int i = 0; i < frames * 2; i++) assertEquals(0, chunk[i], 0);
        }
    }

    @Test public void outputIsIndependentOfChunkBoundariesAndLongDecayIsBounded() throws Exception {
        var scene = new Audio3DScene();
        var source = scene.createSource();
        source.setMode(2); source.setPosition(-80, 10, -100); source.setVelocity(100, 20, 30000);
        source.setReverbLevel(100); scene.setReverb(2, 1000);
        float[] input = tone(5000, 300);
        assertArrayEquals(render(source, input, 1, 1), render(source, input, 1, 512), 0);
        scene.setReverb(2, 30000);
        float[] output = render(source, new float[]{1}, 1, 512);
        assertTrue(output.length < RATE * 40 * 2);
        for (float sample : output) assertTrue(Float.isFinite(sample));
    }

    @Test public void silenceAndEmptyInputsFinishWithoutAReverbWait() throws Exception {
        var scene = new Audio3DScene();
        var source = scene.createSource();
        source.setMode(1); source.setReverbLevel(100); scene.setReverb(0, 30000);
        assertEquals(0, render(source, new float[0], 1, 512).length);
        assertEquals(200, render(source, new float[100], 1, 512).length);
    }

    @Test public void heightAndRearPositionChangeSpectralCuesAndHeadphonesKeepTheFarEarAudible() throws Exception {
        var scene = new Audio3DScene();
        scene.setOutputDevice(1);
        var source = scene.createSource(); source.setMode(2); source.setRolloff(1000, 1000, 100);
        source.setPosition(0, 1000, 0);
        float[] above = render(source, tone(RATE, 3200), 1, 256);
        source.setPosition(0, -1000, 0);
        float[] below = render(source, tone(RATE, 3200), 1, 256);
        assertTrue(energy(below, 0, 100, RATE) > energy(above, 0, 100, RATE) * 2);
        source.setPosition(0, 0, -1000);
        float[] front = render(source, tone(RATE, 400), 1, 256);
        source.setPosition(0, 0, 1000);
        float[] rear = render(source, tone(RATE, 400), 1, 256);
        assertFalse(Arrays.equals(front, rear));
        source.setPosition(1000, 0, 0);
        float[] side = render(source, tone(RATE, 400), 1, 256);
        assertTrue(energy(side, 0, 100, RATE) > .1);
        assertTrue(energy(side, 1, 100, RATE) > energy(side, 0, 100, RATE) * 10);
    }

    private static Audio3DSource dynamic() {
        var scene = new Audio3DScene();
        var source = scene.createSource(); source.setMode(2); source.setRolloff(1000, 1000, 100);
        return source;
    }
    private static float[] tone(int frames, double frequency) {
        float[] data = new float[frames];
        for (int i = 0; i < frames; i++) data[i] = (float) (.3 * Math.sin(2 * Math.PI * frequency * i / RATE));
        return data;
    }
    static float[] render(Audio3DSource source, float[] input, int channels, int chunkSize) throws Exception {
        var stream = new SpatialAudioStream(RATE, channels, source);
        var reader = new Input(input, channels);
        float[] output = new float[RATE * 45 * 2];
        float[] chunk = new float[chunkSize * 2];
        int frames, length = 0;
        while ((frames = stream.render(chunk, chunkSize, reader)) > 0) {
            assertTrue("Stream must terminate", length + frames * 2 <= output.length);
            System.arraycopy(chunk, 0, output, length, frames * 2);
            length += frames * 2;
        }
        return Arrays.copyOf(output, length);
    }
    private static class Input implements SpatialAudioStream.Reader {
        final float[] data; final int channels; int position;
        Input(float[] data, int channels) { this.data = data; this.channels = channels; }
        public int read(float[] output, int frames) {
            int count = Math.min(frames, (data.length - position) / channels);
            System.arraycopy(data, position, output, 0, count * channels);
            position += count * channels;
            return count;
        }
    }
    private static double energy(float[] data, int channel, int start, int end) {
        double sum = 0;
        for (int i = start; i < Math.min(end, data.length / 2); i++) sum += data[i * 2 + channel] * data[i * 2 + channel];
        return sum;
    }
    private static float[] channel(float[] data, int channel) {
        float[] result = new float[data.length / 2];
        for (int i = 0; i < result.length; i++) result[i] = data[i * 2 + channel];
        return result;
    }
    private static double frequency(float[] data) {
        int crossings = 0;
        for (int i = 801; i < 6400; i++) if (data[(i - 1) * 2] <= 0 && data[i * 2] > 0) crossings++;
        return crossings * RATE / 5599.0;
    }
}
