package javax.microedition.io;

public final class PushRegistry {
    private PushRegistry() {
    }

    public static String[] listConnections(boolean available) {
        remexa.probes.SdkStubSupport.log("javax.microedition.io.PushRegistry", "listConnections", available);
        return new String[0];
    }
}
