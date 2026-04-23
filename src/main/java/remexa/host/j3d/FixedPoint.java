package remexa.host.j3d;

public final class FixedPoint {
    public static final int ONE = 4096;

    private FixedPoint() {
    }

    public static int mul(int left, int right) {
        return (int) (((long) left * (long) right + 2048L) >> 12);
    }

    public static int mulTrunc(int left, int right) {
        return (int) (((long) left * (long) right) >> 12);
    }

    public static int sqrt(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative sqrt");
        }
        return (int) Math.round(Math.sqrt(value));
    }

    public static int sin(int angle) {
        return (int) Math.round(Math.sin(angle * Math.PI / 2048.0d) * ONE);
    }

    public static int cos(int angle) {
        return sin(angle + 1024);
    }
}
