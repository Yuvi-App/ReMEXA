package com.mexa.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public abstract class SessionBase {
    public static final int REPORT_NONE = 0;
    public static final int REPORT_ERROR = 1;
    public static final int REPORT_RESULT = 2;

    private final VirtualBluetoothRuntime.SessionHandle sessionHandle;

    protected SessionBase() {
        this(VirtualBluetoothRuntime.getInstance().createSession(new VirtualBluetoothRuntime.SessionCallbacks() { }));
    }

    protected SessionBase(VirtualBluetoothRuntime.SessionHandle sessionHandle) {
        this.sessionHandle = sessionHandle;
    }

    protected final VirtualBluetoothRuntime.SessionHandle sessionHandle() {
        return sessionHandle;
    }

    public boolean close (int connID) {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "close", connID);
        return sessionHandle.close(connID);
    }

    public int send (int[] connIDs, java.lang.String msg, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return sessionHandle.sendString(connIDs, msg);
    }

    public int send (int[] connIDs, byte[] msg, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "send", connIDs, msg, confLevel);
        return sessionHandle.sendBytes(connIDs, msg);
    }

    public int sendSignal (int[] connIDs, int signal, int confLevel) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "sendSignal", connIDs, signal, confLevel);
        return sessionHandle.sendSignal(connIDs, signal);
    }

    public void cleanAllMessage () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionBase", "cleanAllMessage");
    }
}
