package remexa.host.input;

public record MotionPosture(
        int yaw,
        int roll,
        int pitch,
        int dynamicAccelerationX,
        int dynamicAccelerationY,
        int dynamicAccelerationZ,
        int staticAccelerationX,
        int staticAccelerationY,
        int staticAccelerationZ,
        int field
) {
    private static final MotionPosture NEUTRAL = new MotionPosture(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public static MotionPosture neutral() {
        return NEUTRAL;
    }

    public MotionPosture minus(MotionPosture neutral) {
        if (neutral == null) {
            return this;
        }
        return new MotionPosture(
                yaw - neutral.yaw,
                roll - neutral.roll,
                pitch - neutral.pitch,
                dynamicAccelerationX,
                dynamicAccelerationY,
                dynamicAccelerationZ,
                staticAccelerationX - neutral.staticAccelerationX,
                staticAccelerationY - neutral.staticAccelerationY,
                staticAccelerationZ - neutral.staticAccelerationZ,
                field
        );
    }
}
