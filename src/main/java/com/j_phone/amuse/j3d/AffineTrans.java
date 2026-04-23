package com.j_phone.amuse.j3d;

public class AffineTrans extends com.jblend.graphics.j3d.AffineTrans {
    public AffineTrans () {
    }

    public AffineTrans (int[][] m) {
        super(m);
    }


    public com.j_phone.amuse.j3d.Vector3D transPoint (com.j_phone.amuse.j3d.Vector3D src) {
        com.jblend.graphics.j3d.Vector3D result = super.transPoint(src);
        return new Vector3D(result.x, result.y, result.z);
    }

    public void multiply (com.j_phone.amuse.j3d.AffineTrans t) {
        super.multiply(t);
    }

    public void multiply (com.j_phone.amuse.j3d.AffineTrans t1, com.j_phone.amuse.j3d.AffineTrans t2) {
        super.multiply(t1, t2);
    }

    public void rotationV (com.j_phone.amuse.j3d.Vector3D vec, int a) {
        super.rotationV(vec, a);
    }

    public void setViewTrans (com.j_phone.amuse.j3d.Vector3D position, com.j_phone.amuse.j3d.Vector3D look, com.j_phone.amuse.j3d.Vector3D up) {
        super.setViewTrans(position, look, up);
    }
}
