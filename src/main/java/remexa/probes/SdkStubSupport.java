package remexa.probes;

public final class SdkStubSupport {
    public static volatile boolean SDK_TRACE_ENABLED = LogSettings.isAnyEnabled();

    private SdkStubSupport() {
    }

    static void refreshTraceEnabled() {
        SDK_TRACE_ENABLED = LogSettings.isAnyEnabled();
    }

    public static void log(String owner, String member) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member);
    }

    public static void log(String owner, String member, Object arg0) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0);
    }

    public static void log(String owner, String member, Object arg0, Object arg1) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
            Object arg11) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
    }

    public static void log(String owner, String member, Object arg0, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
            Object arg11, Object arg12) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                arg12);
    }

    public static void log(String owner, String member, int arg0) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0);
    }

    public static void log(String owner, String member, int arg0, int arg1) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5, int arg6) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5, int arg6, int arg7) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public static void log(String owner, String member, int arg0, float arg1) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1);
    }

    public static void log(String owner, String member, int arg0, int arg1, Object arg2) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, Object arg3) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, Object arg4) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            Object arg5) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5, Object arg6) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5, int arg6, Object arg7) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public static void log(String owner, String member, int arg0, int arg1, int arg2, int arg3, int arg4,
            int arg5, int arg6, int arg7, Object arg8) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public static void log(String owner, String member, float arg0) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0);
    }

    public static void log(String owner, String member, float arg0, float arg1) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1);
    }

    public static void log(String owner, String member, float arg0, float arg1, float arg2) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2);
    }

    public static void log(String owner, String member, float arg0, float arg1, float arg2, float arg3) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3);
    }

    public static void log(String owner, String member, float arg0, float arg1, float arg2, float arg3,
            float arg4) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4);
    }

    public static void log(String owner, String member, boolean arg0) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0);
    }

    public static void log(String owner, String member, boolean arg0, boolean arg1) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1);
    }

    public static void log(String owner, String member, boolean arg0, boolean arg1, boolean arg2) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2);
    }

    public static void log(String owner, String member, boolean arg0, boolean arg1, boolean arg2, boolean arg3) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3);
    }

    public static void log(String owner, String member, short arg0, short arg1, short arg2, short arg3,
            short arg4) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arg0, arg1, arg2, arg3, arg4);
    }

    public static void log(String owner, String member, Object... arguments) {
        if (!shouldLog(owner, member)) {
            return;
        }
        DebugLog.sdkCall(owner, member, arguments);
    }

    private static boolean shouldLog(String owner, String member) {
        return SDK_TRACE_ENABLED && !isHighVolumeJ3dTrace(owner, member);
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
