package com.j_phone.amuse.j3d;

public class AffineTrans extends com.jblend.graphics.j3d.AffineTrans {
    public AffineTrans () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "AffineTrans");
    }

    public AffineTrans (int[][] m) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "AffineTrans", m);
    }


    public com.j_phone.amuse.j3d.Vector3D transPoint (com.j_phone.amuse.j3d.Vector3D src) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "transPoint", src);
        return null;
    }

    public void multiply (com.j_phone.amuse.j3d.AffineTrans t) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "multiply", t);
    }

    public void multiply (com.j_phone.amuse.j3d.AffineTrans t1, com.j_phone.amuse.j3d.AffineTrans t2) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "multiply", t1, t2);
    }

    public void rotationV (com.j_phone.amuse.j3d.Vector3D vec, int a) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "rotationV", vec, a);
    }

    public void setViewTrans (com.j_phone.amuse.j3d.Vector3D position, com.j_phone.amuse.j3d.Vector3D look, com.j_phone.amuse.j3d.Vector3D up) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.AffineTrans", "setViewTrans", position, look, up);
    }
}
