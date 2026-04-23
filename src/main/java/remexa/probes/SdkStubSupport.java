package remexa.probes;

public final class SdkStubSupport {
    private SdkStubSupport() {
    }

    public static void log(String owner, String member, Object... arguments) {
        DebugLog.sdkCall(owner, member, arguments);
    }
}
