package com.jblend.media.karaoke;

public class ReferenceEvent {
    private final int[] totalTime;
    private final int[] gamut;
    private final int[] pitchBend;

    public ReferenceEvent() {
        this(new int[0], new int[0], new int[0]);
    }

    public ReferenceEvent(int[] totalTime, int[] gamut, int[] pitchBend) {
        this.totalTime = totalTime == null ? new int[0] : totalTime.clone();
        this.gamut = gamut == null ? new int[0] : gamut.clone();
        this.pitchBend = pitchBend == null ? new int[0] : pitchBend.clone();
    }

    public static com.jblend.media.karaoke.ReferenceEvent empty() {
        return new com.jblend.media.karaoke.ReferenceEvent();
    }

    public int[] getTotalTime (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceEvent", "getTotalTime", offset, length);
        return copyRange(totalTime, offset, length);
    }

    public int[] getGamut (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceEvent", "getGamut", offset, length);
        return copyRange(gamut, offset, length);
    }

    public int[] getPitchBend (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceEvent", "getPitchBend", offset, length);
        return copyRange(pitchBend, offset, length);
    }

    public int getLength () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceEvent", "getLength");
        return Math.min(totalTime.length, Math.min(gamut.length, pitchBend.length));
    }

    private static int[] copyRange(int[] source, int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("ReferenceEvent: offset and length must be non-negative.");
        }
        if (length == 0 || offset >= source.length) {
            return new int[length];
        }
        int[] out = new int[length];
        int available = Math.min(length, source.length - offset);
        System.arraycopy(source, offset, out, 0, available);
        return out;
    }
}
