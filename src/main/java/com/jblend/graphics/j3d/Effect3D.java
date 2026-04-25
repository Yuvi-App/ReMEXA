package com.jblend.graphics.j3d;

public class Effect3D {
    public static final int NORMAL_SHADING = 0;
    public static final int TOON_SHADING = 1;
    private Light light;
    private int shading = NORMAL_SHADING;
    private int threshold;
    private int thresholdHigh;
    private int thresholdLow;
    private boolean semiTransparentEnabled;
    private Texture sphereMap;

    public Effect3D () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "Effect3D");
    }

    public Effect3D (com.jblend.graphics.j3d.Light light, int shading, boolean isEnabled, com.jblend.graphics.j3d.Texture sphereMap) {
        this.light = light;
        this.shading = shading;
        this.semiTransparentEnabled = isEnabled;
        this.sphereMap = sphereMap;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "Effect3D", light, shading, isEnabled, sphereMap);
    }


    public com.jblend.graphics.j3d.Light getLight () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getLight");
        return light;
    }

    public void setLight (com.jblend.graphics.j3d.Light light) {
        this.light = light;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setLight", light);
    }

    public int getShading () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getShading");
        return shading;
    }

    public void setShading (int shading) {
        this.shading = shading;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setShading", shading);
    }

    public int getThreshold () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThreshold");
        return threshold;
    }

    public int getThresholdHigh () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThresholdHigh");
        return thresholdHigh;
    }

    public int getThresholdLow () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThresholdLow");
        return thresholdLow;
    }

    public void setThreshold (int threshold, int high, int low) {
        this.threshold = threshold;
        this.thresholdHigh = high;
        this.thresholdLow = low;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setThreshold", threshold, high, low);
    }

    public boolean isSemiTransparentEnabled () {
        //remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "isSemiTransparentEnabled");
        return semiTransparentEnabled;
    }

    public void setSemiTransparentEnabled (boolean isEnabled) {
        this.semiTransparentEnabled = isEnabled;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setSemiTransparentEnabled", isEnabled);
    }

    public com.jblend.graphics.j3d.Texture getSphereMap () {
        // Hot path - called every figure render, do not log.
        return sphereMap;
    }

    public void setSphereMap (com.jblend.graphics.j3d.Texture sphereMap) {
        this.sphereMap = sphereMap;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setSphereMap", sphereMap);
    }
}
