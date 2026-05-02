package com.j_phone.util;

public class Vector2D {
    private static final int FRACTION_BITS = 16;
    private static final int ONE = 1 << FRACTION_BITS;

    private int x;
    private int y;

    public Vector2D(FixedPoint x, FixedPoint y) {
        this.x = x == null ? 0 : encode(x);
        this.y = y == null ? 0 : encode(y);
    }

    public Vector2D(int x, int y) {
        this.x = x << FRACTION_BITS;
        this.y = y << FRACTION_BITS;
    }

    public Vector2D() {
        this(0, 0);
    }

    public void add(Vector2D vector) {
        if (vector == null) {
            return;
        }
        x += vector.x;
        y += vector.y;
    }

    public void add(int x, int y) {
        this.x += x << FRACTION_BITS;
        this.y += y << FRACTION_BITS;
    }

    public void subtract(Vector2D vector) {
        if (vector == null) {
            return;
        }
        x -= vector.x;
        y -= vector.y;
    }

    public void subtract(int x, int y) {
        this.x -= x << FRACTION_BITS;
        this.y -= y << FRACTION_BITS;
    }

    public void normalize() {
        if (x == 0 && y == 0) {
            return;
        }

        double xd = x / (double) ONE;
        double yd = y / (double) ONE;
        double length = Math.hypot(xd, yd);
        if (length == 0.0d) {
            x = 0;
            y = 0;
            return;
        }

        x = (int) Math.round((xd / length) * ONE);
        y = (int) Math.round((yd / length) * ONE);
    }

    public static FixedPoint innerProduct(Vector2D v1, Vector2D v2) {
        if (v1 == null || v2 == null) {
            return new FixedPoint(0);
        }
        long raw = ((long) v1.x * v2.x + (long) v1.y * v2.y) >> FRACTION_BITS;
        return new FixedPoint(saturate(raw));
    }

    public static FixedPoint outerProduct(Vector2D v1, Vector2D v2) {
        if (v1 == null || v2 == null) {
            return new FixedPoint(0);
        }
        long raw = ((long) v1.x * v2.y - (long) v1.y * v2.x) >> FRACTION_BITS;
        return new FixedPoint(saturate(raw));
    }

    public void setValue(int x, int y) {
        this.x = x << FRACTION_BITS;
        this.y = y << FRACTION_BITS;
    }

    public void setValue(FixedPoint x, FixedPoint y) {
        this.x = x == null ? 0 : encode(x);
        this.y = y == null ? 0 : encode(y);
    }

    public FixedPoint getX() {
        return new FixedPoint(x);
    }

    public FixedPoint getY() {
        return new FixedPoint(y);
    }

    public Vector2D clone() {
        Vector2D clone = new Vector2D();
        clone.x = x;
        clone.y = y;
        return clone;
    }

    private static int encode(FixedPoint value) {
        return (value.getInteger() << FRACTION_BITS) + value.getDecimal();
    }

    private static int saturate(long raw) {
        if (raw > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (raw < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) raw;
    }
}
