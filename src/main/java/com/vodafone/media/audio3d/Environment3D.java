package com.vodafone.media.audio3d;

/** Vodafone environment state; spatial and reverb processing are not yet applied to host audio. */
public final class Environment3D {
    public static final int DEVICE_UNKNOWN = 0;
    public static final int DEVICE_HEADPHONES = 1;
    public static final int DEVICE_SPEAKERS = 2;
    public static final int OUTPUT_DEVICE_HEADPHONE = DEVICE_HEADPHONES;
    public static final int OUTPUT_DEVICE_SPEAKER = DEVICE_SPEAKERS;

    public static final String REVERB_ARENA = "Arena";
    public static final String REVERB_BATHROOM = "Bathroom";
    public static final String REVERB_CAVE = "Cave";
    public static final String REVERB_CITY = "City";
    public static final String REVERB_CONCERTHALL = "Concert Hall";
    public static final String REVERB_FOREST = "Forest";
    public static final String REVERB_MOUNTAINS = "Mountains";
    public static final String REVERB_NONE = "None";
    public static final String REVERB_ROOM = "Room";
    public static final String REVERB_UNDERWATER = "Under Water";

    private static final String[] REVERB_PRESETS = {
            REVERB_ARENA, REVERB_BATHROOM, REVERB_CAVE, REVERB_CITY, REVERB_CONCERTHALL,
            REVERB_FOREST, REVERB_MOUNTAINS, REVERB_NONE, REVERB_ROOM, REVERB_UNDERWATER
    };

    private static final Environment3D DEFAULT = new Environment3D();

    // The SDK keeps a separate decay time for each preset (milliseconds).
    private final int[] reverbTimes = {1800, 1480, 2910, 1490, 1800, 1490, 1490, 0, 400, 1490};
    private String reverbPreset = REVERB_NONE;
    private int outputDevice = OUTPUT_DEVICE_SPEAKER;
    private int[] listenerOrientation = {0, 0, -1, 0, 1, 0};
    private int[] listenerPosition = {0, 0, 0};
    private int[] listenerVelocity = {0, 0, 0};
    private boolean deferredCommit;

    private Environment3D() {
    }

    public static Environment3D getDefaultEnvironment3D() {
        return DEFAULT;
    }

    public synchronized String getReverbPreset() {
        return reverbPreset;
    }

    public synchronized void setReverbPreset(String preset) {
        if (preset == null) {
            throw new NullPointerException("preset");
        }
        for (String candidate : REVERB_PRESETS) {
            if (candidate.equals(preset)) {
                reverbPreset = preset;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown reverb preset: " + preset);
    }

    public String[] getReverbPresets() {
        return REVERB_PRESETS.clone();
    }

    public synchronized int getReverbTime() {
        return reverbTimes[presetIndex()];
    }

    public synchronized int setReverbTime(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Negative reverb time");
        }
        // MEXA 2.3: set at 0x00511be7, get at 0x00511bc0 in mexa_emulator.exe.
        // Active presets clamp to 300..30000 ms; "None" always returns zero.
        if (!REVERB_NONE.equals(reverbPreset)) {
            reverbTimes[presetIndex()] = Math.max(300, Math.min(30000, milliseconds));
        }
        return getReverbTime();
    }

    private int presetIndex() {
        for (int index = 0; index < REVERB_PRESETS.length; index++) {
            if (REVERB_PRESETS[index].equals(reverbPreset)) {
                return index;
            }
        }
        throw new IllegalStateException("Unknown stored reverb preset");
    }

    public int getMaxSourceChannels() {
        // MEXA 2.3's native limit at 0x005117d0.
        return 4;
    }

    public int getAvailableSourceChannels() {
        return javax.microedition.media.Manager.getAvailableAudio3DSourceChannels();
    }

    public synchronized int[] getListenerOrientation() {
        return listenerOrientation.clone();
    }

    public synchronized void setListenerOrientation(int frontX, int frontY, int frontZ,
                                                     int upX, int upY, int upZ) {
        if ((frontX == 0 && frontY == 0 && frontZ == 0) || (upX == 0 && upY == 0 && upZ == 0)
                || (double) frontX * upX + (double) frontY * upY + (double) frontZ * upZ != 0) {
            throw new IllegalArgumentException("Listener axes must be nonzero and perpendicular");
        }
        listenerOrientation = new int[]{frontX, frontY, frontZ, upX, upY, upZ};
    }

    public synchronized int[] getListenerPosition() {
        return listenerPosition.clone();
    }

    public synchronized void setListenerPosition(int x, int y, int z) {
        listenerPosition = new int[]{x, y, z};
    }

    public synchronized int[] getListenerVelocity() {
        return listenerVelocity.clone();
    }

    public synchronized void setListenerVelocity(int x, int y, int z) {
        listenerVelocity = new int[]{x, y, z};
    }

    public synchronized boolean isDeferredCommit() {
        return deferredCommit;
    }

    public synchronized void setDeferredCommit(boolean deferred) {
        deferredCommit = deferred;
    }

    public void commit() {
        // Retain the control state. Spatial/reverb DSP is not implemented by the host mixer yet.
    }

    public synchronized int getOutputDevice() {
        return outputDevice;
    }

    public synchronized void setOutputDevice(int outputDevice) {
        if (outputDevice < DEVICE_UNKNOWN || outputDevice > DEVICE_SPEAKERS) {
            throw new IllegalArgumentException("Unknown output device: " + outputDevice);
        }
        this.outputDevice = outputDevice;
    }
}
