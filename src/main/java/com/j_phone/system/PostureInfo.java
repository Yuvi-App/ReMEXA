package com.j_phone.system;

public class PostureInfo {
    public int getCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getCount");
        return 1;
    }

    public int[] getYaw (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getYaw", offset, length);
        return zeroes(length);
    }

    public int[] getRoll (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getRoll", offset, length);
        return zeroes(length);
    }

    public int[] getPitch (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getPitch", offset, length);
        return zeroes(length);
    }

    public int[] getDynamicAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationX", offset, length);
        return zeroes(length);
    }

    public int[] getDynamicAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationY", offset, length);
        return zeroes(length);
    }

    public int[] getDynamicAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationZ", offset, length);
        return zeroes(length);
    }

    public int[] getStaticAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationX", offset, length);
        return zeroes(length);
    }

    public int[] getStaticAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationY", offset, length);
        return zeroes(length);
    }

    public int[] getStaticAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationZ", offset, length);
        return zeroes(length);
    }

    public int[] getField (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getField", offset, length);
        return zeroes(length);
    }

    private static int[] zeroes(int length) {
        return new int[Math.max(1, length)];
    }
}
