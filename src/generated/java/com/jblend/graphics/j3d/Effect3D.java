package com.jblend.graphics.j3d;

public class Effect3D {
    public static final int NORMAL_SHADING = 0;
    public static final int TOON_SHADING = 0;

    public Effect3D () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "Effect3D");
    }

    public Effect3D (com.jblend.graphics.j3d.Light light, int shading, boolean isEnabled, com.jblend.graphics.j3d.Texture sphereMap) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "Effect3D", light, shading, isEnabled, sphereMap);
    }


    public com.jblend.graphics.j3d.Light getLight () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getLight");
        return null;
    }

    public void setLight (com.jblend.graphics.j3d.Light light) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setLight", light);
    }

    public int getShading () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getShading");
        return 0;
    }

    public void setShading (int shading) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setShading", shading);
    }

    public int getThreshold () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThreshold");
        return 0;
    }

    public int getThresholdHigh () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThresholdHigh");
        return 0;
    }

    public int getThresholdLow () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getThresholdLow");
        return 0;
    }

    public void setThreshold (int threshold, int high, int low) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setThreshold", threshold, high, low);
    }

    public boolean isSemiTransparentEnabled () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "isSemiTransparentEnabled");
        return false;
    }

    public void setSemiTransparentEnabled (boolean isEnabled) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setSemiTransparentEnabled", isEnabled);
    }

    public com.jblend.graphics.j3d.Texture getSphereMap () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "getSphereMap");
        return null;
    }

    public void setSphereMap (com.jblend.graphics.j3d.Texture sphereMap) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Effect3D", "setSphereMap", sphereMap);
    }
}
