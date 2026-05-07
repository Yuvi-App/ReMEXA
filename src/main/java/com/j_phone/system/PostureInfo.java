package com.j_phone.system;

import remexa.host.input.MotionPosture;
import remexa.host.runtime.MidletRuntime;

public class PostureInfo {
    private final MotionPosture[] postures;

    public PostureInfo() {
        this(MidletRuntime.currentMotionPosture());
    }

    PostureInfo(MotionPosture posture) {
        this(new MotionPosture[]{posture});
    }

    PostureInfo(MotionPosture[] postures) {
        if (postures == null) {
            this.postures = new MotionPosture[0];
            return;
        }
        this.postures = new MotionPosture[postures.length];
        for (int index = 0; index < postures.length; index++) {
            this.postures[index] = postures[index] == null ? MotionPosture.neutral() : postures[index];
        }
    }

    public int getCount () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getCount");
        return postures.length;
    }

    public int[] getYaw (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getYaw", offset, length);
        return values(offset, length, MotionPosture::yaw);
    }

    public int[] getRoll (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getRoll", offset, length);
        return values(offset, length, MotionPosture::roll);
    }

    public int[] getPitch (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getPitch", offset, length);
        return values(offset, length, MotionPosture::pitch);
    }

    public int[] getDynamicAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationX", offset, length);
        return values(offset, length, MotionPosture::dynamicAccelerationX);
    }

    public int[] getDynamicAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationY", offset, length);
        return values(offset, length, MotionPosture::dynamicAccelerationY);
    }

    public int[] getDynamicAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getDynamicAccelerationZ", offset, length);
        return values(offset, length, MotionPosture::dynamicAccelerationZ);
    }

    public int[] getStaticAccelerationX (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationX", offset, length);
        return values(offset, length, MotionPosture::staticAccelerationX);
    }

    public int[] getStaticAccelerationY (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationY", offset, length);
        return values(offset, length, MotionPosture::staticAccelerationY);
    }

    public int[] getStaticAccelerationZ (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getStaticAccelerationZ", offset, length);
        return values(offset, length, MotionPosture::staticAccelerationZ);
    }

    public int[] getField (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.PostureInfo", "getField", offset, length);
        return values(offset, length, MotionPosture::field);
    }

    private int[] values(int offset, int length, java.util.function.ToIntFunction<MotionPosture> valueAccessor) {
        validateRange(offset, length);
        var values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = valueAccessor.applyAsInt(postures[offset + index]);
        }
        return values;
    }

    private void validateRange(int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset and length must be non-negative");
        }
        if (offset > postures.length || length > postures.length - offset) {
            throw new ArrayIndexOutOfBoundsException(
                    "offset=" + offset + ", length=" + length + ", count=" + postures.length
            );
        }
    }
}
