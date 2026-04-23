package com.mexa.bluetooth;

public class SessionManager extends com.mexa.bluetooth.SessionBase {
    protected SessionManager() {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "SessionManager");
    }

    public SessionManager (com.mexa.bluetooth.SessionListener listener) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "SessionManager", listener);
    }


    public final int open (com.mexa.bluetooth.RemoteService service) throws java.lang.SecurityException, java.lang.NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "open", service);
        return 0;
    }

    public final int openSecured (com.mexa.bluetooth.RemoteService service, boolean encrypt) throws java.lang.SecurityException, java.lang.NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "openSecured", service, encrypt);
        return 0;
    }
}
