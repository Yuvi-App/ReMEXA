package com.jblend.graphics.j3d;

import remexa.host.j3d.FixedPoint;

public class AffineTrans {
    public int m00 = FixedPoint.ONE;
    public int m01 = 0;
    public int m02 = 0;
    public int m03 = 0;
    public int m10 = 0;
    public int m11 = FixedPoint.ONE;
    public int m12 = 0;
    public int m13 = 0;
    public int m20 = 0;
    public int m21 = 0;
    public int m22 = FixedPoint.ONE;
    public int m23 = 0;

    public AffineTrans () {
    }

    public AffineTrans (int[][] m) {
        set(m);
    }


    public void set (int[][] m) {
        if (m == null || m.length < 3 || m[0].length < 4 || m[1].length < 4 || m[2].length < 4) {
            throw new IllegalArgumentException("Matrix must be 3x4");
        }
        m00 = m[0][0];
        m01 = m[0][1];
        m02 = m[0][2];
        m03 = m[0][3];
        m10 = m[1][0];
        m11 = m[1][1];
        m12 = m[1][2];
        m13 = m[1][3];
        m20 = m[2][0];
        m21 = m[2][1];
        m22 = m[2][2];
        m23 = m[2][3];
    }

    public com.jblend.graphics.j3d.Vector3D transPoint (com.jblend.graphics.j3d.Vector3D src) {
        if (src == null) {
            throw new NullPointerException();
        }
        return new Vector3D(
                FixedPoint.mul(src.x, m00) + FixedPoint.mul(src.y, m01) + FixedPoint.mul(src.z, m02) + m03,
                FixedPoint.mul(src.x, m10) + FixedPoint.mul(src.y, m11) + FixedPoint.mul(src.z, m12) + m13,
                FixedPoint.mul(src.x, m20) + FixedPoint.mul(src.y, m21) + FixedPoint.mul(src.z, m22) + m23
        );
    }

    public void multiply (com.jblend.graphics.j3d.AffineTrans t) {
        multiply(this, t);
    }

    public void multiply (com.jblend.graphics.j3d.AffineTrans t1, com.jblend.graphics.j3d.AffineTrans t2) {
        if (t1 == null || t2 == null) {
            throw new NullPointerException();
        }
        int a00 = FixedPoint.mul(t1.m00, t2.m00) + FixedPoint.mul(t1.m01, t2.m10) + FixedPoint.mul(t1.m02, t2.m20);
        int a01 = FixedPoint.mul(t1.m00, t2.m01) + FixedPoint.mul(t1.m01, t2.m11) + FixedPoint.mul(t1.m02, t2.m21);
        int a02 = FixedPoint.mul(t1.m00, t2.m02) + FixedPoint.mul(t1.m01, t2.m12) + FixedPoint.mul(t1.m02, t2.m22);
        int a03 = FixedPoint.mul(t1.m00, t2.m03) + FixedPoint.mul(t1.m01, t2.m13) + FixedPoint.mul(t1.m02, t2.m23) + t1.m03;
        int a10 = FixedPoint.mul(t1.m10, t2.m00) + FixedPoint.mul(t1.m11, t2.m10) + FixedPoint.mul(t1.m12, t2.m20);
        int a11 = FixedPoint.mul(t1.m10, t2.m01) + FixedPoint.mul(t1.m11, t2.m11) + FixedPoint.mul(t1.m12, t2.m21);
        int a12 = FixedPoint.mul(t1.m10, t2.m02) + FixedPoint.mul(t1.m11, t2.m12) + FixedPoint.mul(t1.m12, t2.m22);
        int a13 = FixedPoint.mul(t1.m10, t2.m03) + FixedPoint.mul(t1.m11, t2.m13) + FixedPoint.mul(t1.m12, t2.m23) + t1.m13;
        int a20 = FixedPoint.mul(t1.m20, t2.m00) + FixedPoint.mul(t1.m21, t2.m10) + FixedPoint.mul(t1.m22, t2.m20);
        int a21 = FixedPoint.mul(t1.m20, t2.m01) + FixedPoint.mul(t1.m21, t2.m11) + FixedPoint.mul(t1.m22, t2.m21);
        int a22 = FixedPoint.mul(t1.m20, t2.m02) + FixedPoint.mul(t1.m21, t2.m12) + FixedPoint.mul(t1.m22, t2.m22);
        int a23 = FixedPoint.mul(t1.m20, t2.m03) + FixedPoint.mul(t1.m21, t2.m13) + FixedPoint.mul(t1.m22, t2.m23) + t1.m23;
        m00 = a00;
        m01 = a01;
        m02 = a02;
        m03 = a03;
        m10 = a10;
        m11 = a11;
        m12 = a12;
        m13 = a13;
        m20 = a20;
        m21 = a21;
        m22 = a22;
        m23 = a23;
    }

    public void rotationX (int a) {
        int cos = FixedPoint.cos(a);
        int sin = FixedPoint.sin(a);
        m00 = FixedPoint.ONE;
        m01 = 0;
        m02 = 0;
        m10 = 0;
        m11 = cos;
        m12 = -sin;
        m20 = 0;
        m21 = sin;
        m22 = cos;
    }

    public void rotationY (int a) {
        int cos = FixedPoint.cos(a);
        int sin = FixedPoint.sin(a);
        m00 = cos;
        m01 = 0;
        m02 = sin;
        m10 = 0;
        m11 = FixedPoint.ONE;
        m12 = 0;
        m20 = -sin;
        m21 = 0;
        m22 = cos;
    }

    public void rotationZ (int a) {
        int cos = FixedPoint.cos(a);
        int sin = FixedPoint.sin(a);
        m00 = cos;
        m01 = -sin;
        m02 = 0;
        m10 = sin;
        m11 = cos;
        m12 = 0;
        m20 = 0;
        m21 = 0;
        m22 = FixedPoint.ONE;
    }

    public void rotationV (com.jblend.graphics.j3d.Vector3D vec, int a) {
        if (vec == null) {
            throw new NullPointerException();
        }
        Vector3D axis = new Vector3D(vec.x, vec.y, vec.z);
        axis.unit();
        int cos = FixedPoint.cos(a);
        int sin = FixedPoint.sin(a);
        int nc = FixedPoint.ONE - cos;
        int x = axis.x;
        int y = axis.y;
        int z = axis.z;
        m00 = cos + FixedPoint.mul(FixedPoint.mul(x, x), nc);
        m01 = FixedPoint.mul(FixedPoint.mul(x, y), nc) - FixedPoint.mul(z, sin);
        m02 = FixedPoint.mul(FixedPoint.mul(x, z), nc) + FixedPoint.mul(y, sin);
        m10 = FixedPoint.mul(FixedPoint.mul(y, x), nc) + FixedPoint.mul(z, sin);
        m11 = cos + FixedPoint.mul(FixedPoint.mul(y, y), nc);
        m12 = FixedPoint.mul(FixedPoint.mul(y, z), nc) - FixedPoint.mul(x, sin);
        m20 = FixedPoint.mul(FixedPoint.mul(z, x), nc) - FixedPoint.mul(y, sin);
        m21 = FixedPoint.mul(FixedPoint.mul(z, y), nc) + FixedPoint.mul(x, sin);
        m22 = cos + FixedPoint.mul(FixedPoint.mul(z, z), nc);
    }

    public void setViewTrans (com.jblend.graphics.j3d.Vector3D position, com.jblend.graphics.j3d.Vector3D look, com.jblend.graphics.j3d.Vector3D up) {
        if (position == null || look == null || up == null) {
            throw new NullPointerException();
        }
        // JSCL's `look` parameter represents the view direction vector rather than
        // an absolute target point. Some Vodafone titles, including SD Gundam's
        // scrolling battle camera, pass an offset direction directly here.
        Vector3D forward = new Vector3D(look.x, look.y, look.z);
        forward.unit();
        Vector3D side = Vector3D.outerProduct(forward, up);
        side.unit();
        Vector3D actualUp = Vector3D.outerProduct(side, forward);
        actualUp.unit();
        m00 = side.x;
        m01 = side.y;
        m02 = side.z;
        m03 = -viewTranslation(side, position);
        m10 = actualUp.x;
        m11 = actualUp.y;
        m12 = actualUp.z;
        m13 = -viewTranslation(actualUp, position);
        m20 = forward.x;
        m21 = forward.y;
        m22 = forward.z;
        m23 = -viewTranslation(forward, position);
    }

    private static int viewTranslation(Vector3D basis, Vector3D point) {
        return FixedPoint.mul(basis.x, point.x)
                + FixedPoint.mul(basis.y, point.y)
                + FixedPoint.mul(basis.z, point.z);
    }
}
