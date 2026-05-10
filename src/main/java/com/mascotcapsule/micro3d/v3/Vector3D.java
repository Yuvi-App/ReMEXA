package com.mascotcapsule.micro3d.v3;

public class Vector3D extends com.jblend.graphics.j3d.Vector3D {
    public Vector3D() {
        super();
    }

    public Vector3D(int x, int y, int z) {
        super(x, y, z);
    }

    public Vector3D(Vector3D value) {
        this(value.x, value.y, value.z);
    }

    public void set(Vector3D value) {
        if (value == null) {
            throw new NullPointerException();
        }
        set(value.x, value.y, value.z);
    }

    public static int innerProduct(Vector3D v1, Vector3D v2) {
        return com.jblend.graphics.j3d.Vector3D.innerProduct(v1, v2);
    }

    public static Vector3D outerProduct(Vector3D v1, Vector3D v2) {
        com.jblend.graphics.j3d.Vector3D result = com.jblend.graphics.j3d.Vector3D.outerProduct(v1, v2);
        return new Vector3D(result.x, result.y, result.z);
    }
}
