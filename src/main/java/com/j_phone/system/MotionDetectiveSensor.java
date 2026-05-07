package com.j_phone.system;

import remexa.host.input.MotionPosture;
import remexa.host.runtime.MidletRuntime;

public class MotionDetectiveSensor {
    public static final int POSTURE_INFO = 1;
    public static final int KEY_COMPATIBLE = 2;
    public static final int KEY_SENSOR = 3;
    public static final int CYCLE_20 = 1;
    public static final int CYCLE_40 = 2;
    public static final int CYCLE_60 = 3;
    public static final int CYCLE_80 = 4;
    public static final int CYCLE_100 = 5;
    private static final int MAX_STACK_COUNT = 64;
    private static final com.j_phone.system.MotionDetectiveSensor DEFAULT = new com.j_phone.system.MotionDetectiveSensor();
    private final Object lock = new Object();
    private volatile int state;
    private volatile int cycleMs = 20;
    private int stackCount;
    private long lastSampleNanos;
    private volatile MotionPosture neutralPosture = MotionPosture.neutral();

    public static final com.j_phone.system.MotionDetectiveSensor getDefaultMotionDetectiveSensor () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getDefaultMotionDetectiveSensor");
        return DEFAULT;
    }

    public void startSensor (int type, int cycle) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "startSensor", type, cycle);
        MidletRuntime.ensureThreadActive();
        validateType(type);
        if (type == POSTURE_INFO) {
            validateCycle(cycle);
        }
        MidletRuntime.noteMotionApiUsage("MotionDetectiveSensor.startSensor");
        synchronized (lock) {
            state = type;
            cycleMs = type == POSTURE_INFO ? cycleMillis(cycle) : 20;
            stackCount = 0;
            lastSampleNanos = System.nanoTime();
        }
    }

    public void stopSensor () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "stopSensor");
        synchronized (lock) {
            state = 0;
            stackCount = 0;
            lastSampleNanos = 0L;
        }
    }

    public com.j_phone.system.PostureInfo getPostureInfoLatest () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getPostureInfoLatest");
        updateStackCount();
        return new com.j_phone.system.PostureInfo(currentPosture());
    }

    public com.j_phone.system.PostureInfo getPostureInfoStack (int num) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getPostureInfoStack", num);
        if (num < 0) {
            throw new IllegalArgumentException("num must be non-negative");
        }
        var count = 0;
        synchronized (lock) {
            updateStackCountLocked();
            count = Math.min(num, stackCount);
            stackCount -= count;
        }
        var postures = new MotionPosture[count];
        var posture = currentPosture();
        java.util.Arrays.fill(postures, posture);
        return new com.j_phone.system.PostureInfo(postures);
    }

    public int getStackCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getStackCount");
        synchronized (lock) {
            updateStackCountLocked();
            return state == POSTURE_INFO ? stackCount : 0;
        }
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getState");
        return state;
    }

    public void setNeutralPosition () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "setNeutralPosition");
        neutralPosture = MidletRuntime.currentMotionPosture();
        clearStack();
    }

    public void setDefaultNeutralPosition () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "setDefaultNeutralPosition");
        neutralPosture = MotionPosture.neutral();
        clearStack();
    }

    private MotionPosture currentPosture() {
        if (state != POSTURE_INFO) {
            return MotionPosture.neutral();
        }
        return MidletRuntime.currentMotionPosture().minus(neutralPosture);
    }

    private void updateStackCount() {
        synchronized (lock) {
            updateStackCountLocked();
        }
    }

    private void updateStackCountLocked() {
        if (state != POSTURE_INFO || lastSampleNanos == 0L) {
            return;
        }
        var now = System.nanoTime();
        var elapsedNanos = now - lastSampleNanos;
        var cycleNanos = cycleMs * 1_000_000L;
        if (elapsedNanos < cycleNanos) {
            return;
        }
        var samples = (int) Math.min(
                MAX_STACK_COUNT,
                elapsedNanos / cycleNanos
        );
        stackCount = Math.min(MAX_STACK_COUNT, stackCount + samples);
        lastSampleNanos += samples * cycleNanos;
    }

    private void clearStack() {
        synchronized (lock) {
            stackCount = 0;
            lastSampleNanos = state == POSTURE_INFO ? System.nanoTime() : 0L;
        }
    }

    private static void validateType(int type) {
        if (type != POSTURE_INFO && type != KEY_COMPATIBLE && type != KEY_SENSOR) {
            throw new IllegalArgumentException("Unsupported motion sensor type: " + type);
        }
    }

    private static void validateCycle(int cycle) {
        if (cycle < CYCLE_20 || cycle > CYCLE_100) {
            throw new IllegalArgumentException("Unsupported motion sensor cycle: " + cycle);
        }
    }

    private static int cycleMillis(int cycle) {
        return switch (cycle) {
            case CYCLE_20 -> 20;
            case CYCLE_40 -> 40;
            case CYCLE_60 -> 60;
            case CYCLE_80 -> 80;
            case CYCLE_100 -> 100;
            default -> throw new IllegalArgumentException("Unsupported motion sensor cycle: " + cycle);
        };
    }
}
