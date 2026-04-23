package com.mexa.bluetooth;

public class SessionMember extends com.mexa.bluetooth.SessionBase {
    protected SessionMember() {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "SessionMember");
    }

    public SessionMember (com.mexa.bluetooth.SessionListener listener) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "SessionMember", listener);
    }


    public final int open (com.mexa.bluetooth.LocalService service) throws java.lang.SecurityException, java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "open", service);
        return 0;
    }

    public final int openSecured (com.mexa.bluetooth.LocalService service, boolean encrypt, boolean authorize) throws java.lang.SecurityException, java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "openSecured", service, encrypt, authorize);
        return 0;
    }

    public java.lang.String getBluetoothAddress (int connID) {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "getBluetoothAddress", connID);
        return "";
    }
}
