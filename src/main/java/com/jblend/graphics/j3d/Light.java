package com.jblend.graphics.j3d;

public class Light {
    private Vector3D direction = new Vector3D(0, 0, 4096);
    private int dirIntensity = 4096;
    private int ambIntensity;

    public Light () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "Light");
    }

    public Light (com.jblend.graphics.j3d.Vector3D dir, int dirIntensity, int ambIntensity) {
        this.direction = dir == null ? new Vector3D(0, 0, 4096) : dir;
        this.dirIntensity = dirIntensity;
        this.ambIntensity = ambIntensity;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "Light", dir, dirIntensity, ambIntensity);
    }


    public int getDirIntensity () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "getDirIntensity");
        return dirIntensity;
    }

    public void setDirIntensity (int dirIntensity) {
        this.dirIntensity = dirIntensity;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "setDirIntensity", dirIntensity);
    }

    public int getAmbIntensity () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "getAmbIntensity");
        return ambIntensity;
    }

    public void setAmbIntensity (int ambIntensity) {
        this.ambIntensity = ambIntensity;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "setAmbIntensity", ambIntensity);
    }

    public com.jblend.graphics.j3d.Vector3D getDirection () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "getDirection");
        return direction;
    }

    public void setDirection (com.jblend.graphics.j3d.Vector3D dir) {
        this.direction = dir == null ? new Vector3D(0, 0, 4096) : dir;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Light", "setDirection", dir);
    }
}
