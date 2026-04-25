package com.vodafone.bluetooth;

public class SessionManager extends SessionBase {
    private final SessionListener listener;

    protected SessionManager() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "SessionManager");
    }

    public SessionManager(SessionListener listener) throws NullPointerException {
        this.listener = listener;
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "SessionManager", listener);
    }

    public final int open(RemoteService service) throws SecurityException, NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        throw new java.io.IOException("Bluetooth transport is not implemented yet.");
    }

    public final int openSecured(RemoteService service, boolean encrypt)
            throws SecurityException, NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "openSecured", service, encrypt);
        return open(service);
    }
}
