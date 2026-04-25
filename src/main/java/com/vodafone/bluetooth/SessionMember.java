package com.vodafone.bluetooth;

public class SessionMember extends SessionBase {
    private final SessionListener listener;

    protected SessionMember() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "SessionMember");
    }

    public SessionMember(SessionListener listener) throws NullPointerException {
        this.listener = listener;
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "SessionMember", listener);
    }

    public final int open(LocalService service)
            throws SecurityException, NullPointerException, IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        return 0;
    }

    public final int openSecured(LocalService service, boolean encrypt, boolean authorize)
            throws SecurityException, NullPointerException, IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log(
                "com.vodafone.bluetooth.SessionMember",
                "openSecured",
                service,
                encrypt,
                authorize
        );
        return open(service);
    }

    public String getBluetoothAddress(int connID) {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "getBluetoothAddress", connID);
        return "";
    }
}
