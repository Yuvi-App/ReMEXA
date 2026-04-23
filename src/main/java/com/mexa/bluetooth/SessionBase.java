package com.mexa.bluetooth;

public abstract class SessionBase {
    public static final int REPORT_NONE = 0;
    public static final int REPORT_ERROR = 0;
    public static final int REPORT_RESULT = 0;

    public boolean close (int connID) {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "close", connID);
        return false;
    }

    public int send (int[] connIDs, java.lang.String msg, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return 0;
    }

    public int send (int[] connIDs, byte[] msg, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return 0;
    }

    public int sendSignal (int[] connIDs, int signal, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "sendSignal", connIDs, signal, confLevel);
        return 0;
    }

    public void cleanAllMessage () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "cleanAllMessage");
    }
}
