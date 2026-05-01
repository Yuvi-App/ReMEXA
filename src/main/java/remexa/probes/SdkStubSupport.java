package remexa.probes;

public final class SdkStubSupport {
    private SdkStubSupport() {
    }

    public static void log(String owner, String member, Object... arguments) {
        if (isHighVolumeJ3dTrace(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arguments);
    }

    private static boolean isHighVolumeJ3dTrace(String owner, String member) {
        if (owner == null || member == null || !owner.startsWith("com.mexa.opgl.")) {
            return false;
        }
        if (!(owner.endsWith("Buffer")
                || owner.endsWith("ByteBuffer")
                || owner.endsWith("ShortBuffer")
                || owner.endsWith("FloatBuffer")
                || owner.endsWith("IntBuffer"))) {
            return false;
        }
        return switch (member) {
            case "allocateDirect", "put", "get", "setBounds" -> true;
            default -> false;
        };
    }
}
