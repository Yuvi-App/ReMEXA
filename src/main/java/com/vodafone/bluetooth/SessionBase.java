package com.vodafone.bluetooth;

public abstract class SessionBase {
    public static final int REPORT_NONE = 0;
    public static final int REPORT_ERROR = 1;
    public static final int REPORT_RESULT = 2;

    public boolean close(int connID) {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionBase", "close", connID);
        return true;
    }

    public int send(int[] connIDs, String msg, int confLevel)
            throws NullPointerException, IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return 0;
    }

    public int send(int[] connIDs, byte[] msg, int confLevel)
            throws NullPointerException, IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return 0;
    }

    public int sendSignal(int[] connIDs, int signal, int confLevel)
            throws NullPointerException, IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionBase", "sendSignal", connIDs, signal, confLevel);
        return 0;
    }

    public void cleanAllMessage() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionBase", "cleanAllMessage");
    }
}
