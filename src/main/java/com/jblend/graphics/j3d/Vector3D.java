package com.jblend.graphics.j3d;

public class Vector3D {
    public int x = 0;
    public int y = 0;
    public int z = 0;

    public Vector3D () {
    }

    public Vector3D (int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public int getX () {
        return x;
    }

    public int getY () {
        return y;
    }

    public int getZ () {
        return z;
    }

    public void setX (int x) {
        this.x = x;
    }

    public void setY (int y) {
        this.y = y;
    }

    public void setZ (int z) {
        this.z = z;
    }

    public void set (int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void unit () {
        long lengthSquared = (long) x * (long) x + (long) y * (long) y + (long) z * (long) z;
        int length = (int) Math.round(Math.sqrt(lengthSquared));
        if (length == 0) {
            throw new ArithmeticException();
        }
        x = (int) ((((long) x) << 12) / length);
        y = (int) ((((long) y) << 12) / length);
        z = (int) ((((long) z) << 12) / length);
    }

    public int innerProduct (com.jblend.graphics.j3d.Vector3D v) {
        return innerProduct(this, v);
    }

    public void outerProduct (com.jblend.graphics.j3d.Vector3D v) {
        int nextX = this.y * v.z - this.z * v.y;
        int nextY = this.z * v.x - this.x * v.z;
        int nextZ = this.x * v.y - this.y * v.x;
        this.x = nextX;
        this.y = nextY;
        this.z = nextZ;
    }

    public static int innerProduct (com.jblend.graphics.j3d.Vector3D v1, com.jblend.graphics.j3d.Vector3D v2) {
        if (v1 == null || v2 == null) {
            throw new NullPointerException();
        }
        long result = (long) v1.x * (long) v2.x
                + (long) v1.y * (long) v2.y
                + (long) v1.z * (long) v2.z;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }

    public static com.jblend.graphics.j3d.Vector3D outerProduct (com.jblend.graphics.j3d.Vector3D v1, com.jblend.graphics.j3d.Vector3D v2) {
        if (v1 == null || v2 == null) {
            throw new NullPointerException();
        }
        return new Vector3D(
                v1.y * v2.z - v1.z * v2.y,
                v1.z * v2.x - v1.x * v2.z,
                v1.x * v2.y - v1.y * v2.x
        );
    }
}
