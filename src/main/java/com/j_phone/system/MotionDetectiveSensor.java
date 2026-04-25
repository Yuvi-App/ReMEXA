package com.j_phone.system;

public class MotionDetectiveSensor {
    public static final int POSTURE_INFO = 0;
    public static final int KEY_COMPATIBLE = 0;
    public static final int KEY_SENSOR = 0;
    public static final int CYCLE_20 = 0;
    public static final int CYCLE_40 = 0;
    public static final int CYCLE_60 = 0;
    public static final int CYCLE_80 = 0;
    public static final int CYCLE_100 = 0;
    private static final com.j_phone.system.MotionDetectiveSensor DEFAULT = new com.j_phone.system.MotionDetectiveSensor();
    private volatile boolean active;

    public static final com.j_phone.system.MotionDetectiveSensor getDefaultMotionDetectiveSensor () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getDefaultMotionDetectiveSensor");
        return DEFAULT;
    }

    public void startSensor (int type, int cycle) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "startSensor", type, cycle);
        active = true;
    }

    public void stopSensor () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "stopSensor");
        active = false;
    }

    public com.j_phone.system.PostureInfo getPostureInfoLatest () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getPostureInfoLatest");
        return new com.j_phone.system.PostureInfo();
    }

    public com.j_phone.system.PostureInfo getPostureInfoStack (int num) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getPostureInfoStack", num);
        return new com.j_phone.system.PostureInfo();
    }

    public int getStackCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getStackCount");
        return 0;
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "getState");
        return active ? 1 : 0;
    }

    public void setNeutralPosition () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "setNeutralPosition");
    }

    public void setDefaultNeutralPosition () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.MotionDetectiveSensor", "setDefaultNeutralPosition");
    }
}
