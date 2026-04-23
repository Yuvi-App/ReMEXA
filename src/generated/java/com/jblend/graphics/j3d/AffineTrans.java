package com.jblend.graphics.j3d;

public class AffineTrans {
    public int m00 = 0;
    public int m01 = 0;
    public int m02 = 0;
    public int m03 = 0;
    public int m10 = 0;
    public int m11 = 0;
    public int m12 = 0;
    public int m13 = 0;
    public int m20 = 0;
    public int m21 = 0;
    public int m22 = 0;
    public int m23 = 0;

    public AffineTrans () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "AffineTrans");
    }

    public AffineTrans (int[][] m) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "AffineTrans", m);
    }


    public void set (int[][] m) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "set", m);
    }

    public com.jblend.graphics.j3d.Vector3D transPoint (com.jblend.graphics.j3d.Vector3D src) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "transPoint", src);
        return null;
    }

    public void multiply (com.jblend.graphics.j3d.AffineTrans t) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "multiply", t);
    }

    public void multiply (com.jblend.graphics.j3d.AffineTrans t1, com.jblend.graphics.j3d.AffineTrans t2) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "multiply", t1, t2);
    }

    public void rotationX (int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "rotationX", a);
    }

    public void rotationY (int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "rotationY", a);
    }

    public void rotationZ (int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "rotationZ", a);
    }

    public void rotationV (com.jblend.graphics.j3d.Vector3D vec, int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "rotationV", vec, a);
    }

    public void setViewTrans (com.jblend.graphics.j3d.Vector3D position, com.jblend.graphics.j3d.Vector3D look, com.jblend.graphics.j3d.Vector3D up) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.AffineTrans", "setViewTrans", position, look, up);
    }
}
