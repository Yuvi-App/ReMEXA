package com.j_phone.util;

public class FixedPoint {
    private static final int FRACTION_BITS = 16;
    private static final int ONE = 1 << FRACTION_BITS;
    private static final long MIN_RAW = Integer.MIN_VALUE;
    private static final long MAX_RAW = Integer.MAX_VALUE;

    private int value;
    private boolean infinite;

    public FixedPoint() {
        this(0);
    }

    public FixedPoint(int value) {
        this.value = value;
        this.infinite = false;
    }

    private FixedPoint(int value, boolean infinite) {
        this.value = value;
        this.infinite = infinite;
    }

    public int getInteger() {
        if (value >= 0) {
            return value >> FRACTION_BITS;
        }
        return -((int) ((-(long) value) >> FRACTION_BITS));
    }

    public int getDecimal() {
        return value - (getInteger() << FRACTION_BITS);
    }

    public void setValue(int value) {
        this.value = value;
        this.infinite = false;
    }

    public com.j_phone.util.FixedPoint add(com.j_phone.util.FixedPoint n) {
        return add(n.value);
    }

    public com.j_phone.util.FixedPoint add(int n) {
        return setSaturated((long) value + n);
    }

    public com.j_phone.util.FixedPoint subtract(com.j_phone.util.FixedPoint n) {
        return subtract(n.value);
    }

    public com.j_phone.util.FixedPoint subtract(int n) {
        return setSaturated((long) value - n);
    }

    public com.j_phone.util.FixedPoint multiply(com.j_phone.util.FixedPoint n) {
        return multiply(n.value);
    }

    public com.j_phone.util.FixedPoint multiply(int n) {
        return setSaturated(((long) value * n) >> FRACTION_BITS);
    }

    public com.j_phone.util.FixedPoint divide(com.j_phone.util.FixedPoint n) {
        return divide(n.value);
    }

    public com.j_phone.util.FixedPoint divide(int n) {
        if (n == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return setSaturated((((long) value) << FRACTION_BITS) / n);
    }

    public com.j_phone.util.FixedPoint sin(com.j_phone.util.FixedPoint r) {
        return setFromDouble(Math.sin(r.toDouble()));
    }

    public com.j_phone.util.FixedPoint cos(com.j_phone.util.FixedPoint r) {
        return setFromDouble(Math.cos(r.toDouble()));
    }

    public com.j_phone.util.FixedPoint tan(com.j_phone.util.FixedPoint r) {
        return setFromDouble(Math.tan(r.toDouble()));
    }

    public com.j_phone.util.FixedPoint asin(com.j_phone.util.FixedPoint v) {
        double input = v.toDouble();
        if (input < -1.0 || input > 1.0) {
            throw new ArithmeticException("asin domain");
        }
        return setFromDouble(Math.asin(input));
    }

    public com.j_phone.util.FixedPoint acos(com.j_phone.util.FixedPoint v) {
        double input = v.toDouble();
        if (input < -1.0 || input > 1.0) {
            throw new ArithmeticException("acos domain");
        }
        return setFromDouble(Math.acos(input));
    }

    public com.j_phone.util.FixedPoint atan(com.j_phone.util.FixedPoint v) {
        return setFromDouble(Math.atan(v.toDouble()));
    }

    public com.j_phone.util.FixedPoint sqrt() {
        if (value < 0) {
            throw new ArithmeticException("sqrt domain");
        }
        return setFromDouble(Math.sqrt(toDouble()));
    }

    public com.j_phone.util.FixedPoint inverse() {
        if (value == 0) {
            throw new ArithmeticException("inverse of zero");
        }
        return setFromDouble(1.0 / toDouble());
    }

    public com.j_phone.util.FixedPoint pow() {
        return setSaturated(((long) value * value) >> FRACTION_BITS);
    }

    public boolean isInfinite() {
        return infinite;
    }

    public com.j_phone.util.FixedPoint clone() {
        return new com.j_phone.util.FixedPoint(value, infinite);
    }

    public static com.j_phone.util.FixedPoint getPI() {
        return fromDouble(Math.PI);
    }

    public static com.j_phone.util.FixedPoint getMaximum() {
        return new com.j_phone.util.FixedPoint(Integer.MAX_VALUE, false);
    }

    public static com.j_phone.util.FixedPoint getMinimum() {
        return new com.j_phone.util.FixedPoint(Integer.MIN_VALUE, false);
    }

    private double toDouble() {
        return value / (double) ONE;
    }

    private com.j_phone.util.FixedPoint setFromDouble(double number) {
        if (Double.isNaN(number)) {
            throw new ArithmeticException("NaN");
        }
        if (Double.isInfinite(number)) {
            value = number > 0.0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            infinite = true;
            return this;
        }
        return setSaturated(Math.round(number * ONE));
    }

    private com.j_phone.util.FixedPoint setSaturated(long rawValue) {
        if (rawValue > MAX_RAW) {
            value = Integer.MAX_VALUE;
            infinite = true;
        } else if (rawValue < MIN_RAW) {
            value = Integer.MIN_VALUE;
            infinite = true;
        } else {
            value = (int) rawValue;
            infinite = false;
        }
        return this;
    }

    private static com.j_phone.util.FixedPoint fromDouble(double number) {
        com.j_phone.util.FixedPoint result = new com.j_phone.util.FixedPoint();
        return result.setFromDouble(number);
    }
}
