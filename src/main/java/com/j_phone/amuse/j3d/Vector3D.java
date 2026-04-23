package com.j_phone.amuse.j3d;

public class Vector3D extends com.jblend.graphics.j3d.Vector3D {
    public Vector3D () {
    }

    public Vector3D (int x, int y, int z) {
        super(x, y, z);
    }


    public int innerProduct (com.j_phone.amuse.j3d.Vector3D v) {
        return super.innerProduct(v);
    }

    public void outerProduct (com.j_phone.amuse.j3d.Vector3D v) {
        super.outerProduct(v);
    }

    public static int innerProduct (com.j_phone.amuse.j3d.Vector3D v1, com.j_phone.amuse.j3d.Vector3D v2) {
        return com.jblend.graphics.j3d.Vector3D.innerProduct(v1, v2);
    }

    public static com.j_phone.amuse.j3d.Vector3D outerProduct (com.j_phone.amuse.j3d.Vector3D v1, com.j_phone.amuse.j3d.Vector3D v2) {
        com.jblend.graphics.j3d.Vector3D result = com.jblend.graphics.j3d.Vector3D.outerProduct(v1, v2);
        return new Vector3D(result.x, result.y, result.z);
    }
}
