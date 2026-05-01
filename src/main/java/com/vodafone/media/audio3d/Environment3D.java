package com.vodafone.media.audio3d;

public final class Environment3D {
    public static final int OUTPUT_DEVICE_HEADPHONE = 1;
    public static final int OUTPUT_DEVICE_SPEAKER = 2;

    private static final Environment3D DEFAULT = new Environment3D();

    private String reverbPreset = "None";
    private int outputDevice = OUTPUT_DEVICE_SPEAKER;

    private Environment3D() {
    }

    public static Environment3D getDefaultEnvironment3D() {
        return DEFAULT;
    }

    public synchronized String getReverbPreset() {
        return reverbPreset;
    }

    public synchronized void setReverbPreset(String preset) {
        reverbPreset = preset == null ? "None" : preset;
    }

    public synchronized int getOutputDevice() {
        return outputDevice;
    }

    public synchronized void setOutputDevice(int outputDevice) {
        this.outputDevice = outputDevice;
    }
}
