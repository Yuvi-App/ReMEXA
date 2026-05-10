package com.mascotcapsule.micro3d.v3;

public class AffineTrans extends com.jblend.graphics.j3d.AffineTrans {
    public AffineTrans() {
        super();
    }

    public AffineTrans(AffineTrans value) {
        set(value);
    }

    public AffineTrans(int[] values) {
        set(values);
    }

    public AffineTrans(int[][] values) {
        super(values);
    }

    public AffineTrans(int[] values, int offset) {
        set(values, offset);
    }

    public AffineTrans(
            int m00, int m01, int m02, int m03,
            int m10, int m11, int m12, int m13,
            int m20, int m21, int m22, int m23
    ) {
        set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
    }

    public final void get(int[] values) {
        get(values, 0);
    }

    public final void get(int[] values, int offset) {
        if (values == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || values.length - offset < 12) {
            throw new IllegalArgumentException();
        }
        values[offset++] = m00;
        values[offset++] = m01;
        values[offset++] = m02;
        values[offset++] = m03;
        values[offset++] = m10;
        values[offset++] = m11;
        values[offset++] = m12;
        values[offset++] = m13;
        values[offset++] = m20;
        values[offset++] = m21;
        values[offset++] = m22;
        values[offset] = m23;
    }

    public final void lookAt(Vector3D position, Vector3D look, Vector3D up) {
        setViewTrans(position, look, up);
    }

    public final void mul(AffineTrans value) {
        multiply(value);
    }

    public final void mul(AffineTrans left, AffineTrans right) {
        multiply(left, right);
    }

    public final void set(AffineTrans value) {
        if (value == null) {
            throw new NullPointerException();
        }
        set(value.m00, value.m01, value.m02, value.m03,
                value.m10, value.m11, value.m12, value.m13,
                value.m20, value.m21, value.m22, value.m23);
    }

    public final void set(int[] values) {
        set(values, 0);
    }

    public final void set(int[] values, int offset) {
        if (values == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || values.length - offset < 12) {
            throw new IllegalArgumentException();
        }
        set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3],
                values[offset + 4], values[offset + 5], values[offset + 6], values[offset + 7],
                values[offset + 8], values[offset + 9], values[offset + 10], values[offset + 11]);
    }

    public final void set(
            int m00, int m01, int m02, int m03,
            int m10, int m11, int m12, int m13,
            int m20, int m21, int m22, int m23
    ) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
    }

    public final void setIdentity() {
        set(4096, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 4096, 0);
    }

    public final void setRotation(Vector3D axis, int angle) {
        rotationV(axis, angle);
    }

    public final void setRotationX(int angle) {
        rotationX(angle);
    }

    public final void setRotationY(int angle) {
        rotationY(angle);
    }

    public final void setRotationZ(int angle) {
        rotationZ(angle);
    }

    public final Vector3D transform(Vector3D value) {
        com.jblend.graphics.j3d.Vector3D result = transPoint(value);
        return new Vector3D(result.x, result.y, result.z);
    }
}
