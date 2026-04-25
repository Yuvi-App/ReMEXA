package com.vodafone.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class SessionMember extends SessionBase {
    private final SessionListener listener;

    protected SessionMember() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "SessionMember");
    }

    public SessionMember(SessionListener listener) throws NullPointerException {
        super(VirtualBluetoothRuntime.getInstance().createSession(new VirtualBluetoothRuntime.SessionCallbacks() {
            @Override
            public void onConnectionStatus(int connId, int status) {
                if (listener != null) {
                    listener.gotConnectionStatus(connId, status);
                }
            }

            @Override
            public void onMemberList(int[] connIds) {
                if (listener != null) {
                    listener.gotMemberList(connIds);
                }
            }

            @Override
            public void onStringMessage(int connId, String message) {
                if (listener != null) {
                    listener.gotMessage(connId, message);
                }
            }

            @Override
            public void onByteMessage(int connId, byte[] message) {
                if (listener != null) {
                    listener.gotMessage(connId, message);
                }
            }

            @Override
            public void onSignal(int connId, int signal) {
                if (listener != null) {
                    listener.gotSignal(connId, signal);
                }
            }

            @Override
            public void onResult(int messageId, int[] connIds, int[] results) {
                if (listener != null) {
                    listener.gotResult(messageId, connIds, results);
                }
            }
        }));
        this.listener = listener;
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "SessionMember", listener);
    }

    public final int open(LocalService service)
            throws SecurityException, NullPointerException, IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionMember", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        return sessionHandle().listen(service.serviceInfo());
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
        return sessionHandle().getRemoteAddress(connID);
    }
}
