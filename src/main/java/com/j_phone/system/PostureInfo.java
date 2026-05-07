package com.j_phone.system;

import remexa.host.input.MotionPosture;
import remexa.host.runtime.MidletRuntime;

public class PostureInfo {
    private final MotionPosture posture;

    public PostureInfo() {
        this(MidletRuntime.currentMotionPosture());
    }

    PostureInfo(MotionPosture posture) {
        this.posture = posture == null ? MotionPosture.neutral() : posture;
    }

    public int getCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getCount");
        return 1;
    }

    public int[] getYaw (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getYaw", offset, length);
        return values(length, posture.yaw());
    }

    public int[] getRoll (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getRoll", offset, length);
        return values(length, posture.roll());
    }

    public int[] getPitch (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getPitch", offset, length);
        return values(length, posture.pitch());
    }

    public int[] getDynamicAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationX", offset, length);
        return values(length, posture.dynamicAccelerationX());
    }

    public int[] getDynamicAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationY", offset, length);
        return values(length, posture.dynamicAccelerationY());
    }

    public int[] getDynamicAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationZ", offset, length);
        return values(length, posture.dynamicAccelerationZ());
    }

    public int[] getStaticAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationX", offset, length);
        return values(length, posture.staticAccelerationX());
    }

    public int[] getStaticAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationY", offset, length);
        return values(length, posture.staticAccelerationY());
    }

    public int[] getStaticAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationZ", offset, length);
        return values(length, posture.staticAccelerationZ());
    }

    public int[] getField (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getField", offset, length);
        return values(length, posture.field());
    }

    private static int[] values(int length, int value) {
        var values = new int[Math.max(1, length)];
        java.util.Arrays.fill(values, value);
        return values;
    }
}
