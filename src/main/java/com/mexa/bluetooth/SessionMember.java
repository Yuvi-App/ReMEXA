package com.mexa.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class SessionMember extends com.mexa.bluetooth.SessionBase {
    protected SessionMember() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "SessionMember");
    }

    public SessionMember (com.mexa.bluetooth.SessionListener listener) throws java.lang.NullPointerException {
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
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "SessionMember", listener);
    }


    public final int open (com.mexa.bluetooth.LocalService service) throws java.lang.SecurityException, java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        return sessionHandle().listen(service.serviceInfo());
    }

    public final int openSecured (com.mexa.bluetooth.LocalService service, boolean encrypt, boolean authorize) throws java.lang.SecurityException, java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "openSecured", service, encrypt, authorize);
        return open(service);
    }

    public java.lang.String getBluetoothAddress (int connID) {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionMember", "getBluetoothAddress", connID);
        return sessionHandle().getRemoteAddress(connID);
    }
}
