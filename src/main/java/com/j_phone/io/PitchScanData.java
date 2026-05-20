package com.j_phone.io;

public class PitchScanData {
    private final int[] pitch;
    private final int[] voice;
    private int currentIndex;
    private boolean overflowNotified;

    protected PitchScanData() {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "PitchScanData");
        this.pitch = new int[1];
        this.voice = new int[1];
    }

    public PitchScanData (int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "PitchScanData", size);
        if (size <= 0) {
            throw new IllegalArgumentException("PitchScanData: size must be positive.");
        }
        this.pitch = new int[size];
        this.voice = new int[size];
        java.util.Arrays.fill(this.pitch, -1);
    }


    public synchronized int[] getPitch (int offset, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getPitch", offset, size);
        return copyRange(pitch, offset, size, -1);
    }

    public synchronized int[] getVoice (int offset, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getVoice", offset, size);
        return copyRange(voice, offset, size, 0);
    }

    public synchronized int getCurrentIndex () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getCurrentIndex");
        return currentIndex;
    }

    synchronized void reset() {
        currentIndex = 0;
        overflowNotified = false;
        java.util.Arrays.fill(pitch, -1);
        java.util.Arrays.fill(voice, 0);
    }

    synchronized boolean appendSample(int pitchValue, int voiceValue) {
        if (currentIndex < pitch.length) {
            pitch[currentIndex] = pitchValue;
            voice[currentIndex] = clamp(voiceValue, 0, 15);
            currentIndex++;
            return false;
        }

        int last = pitch.length - 1;
        pitch[last] = pitchValue;
        voice[last] = clamp(voiceValue, 0, 15);
        if (!overflowNotified) {
            overflowNotified = true;
            return true;
        }
        return false;
    }

    private int[] copyRange(int[] source, int offset, int size, int fill) {
        if (offset < 0 || size < 0) {
            throw new IllegalArgumentException("PitchScanData: offset and size must be non-negative.");
        }
        int[] out = new int[size];
        if (fill != 0) {
            java.util.Arrays.fill(out, fill);
        }
        if (size == 0 || offset >= currentIndex) {
            return out;
        }
        int available = Math.min(size, currentIndex - offset);
        System.arraycopy(source, offset, out, 0, available);
        return out;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
