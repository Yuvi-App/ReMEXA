package com.mexa.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class SessionManager extends com.mexa.bluetooth.SessionBase {
    protected SessionManager() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "SessionManager");
    }

    public SessionManager (com.mexa.bluetooth.SessionListener listener) throws java.lang.NullPointerException {
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
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "SessionManager", listener);
    }


    public final int open (com.mexa.bluetooth.RemoteService service) throws java.lang.SecurityException, java.lang.NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        return sessionHandle().connect(service.serviceInfo());
    }

    public final int openSecured (com.mexa.bluetooth.RemoteService service, boolean encrypt) throws java.lang.SecurityException, java.lang.NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.SessionManager", "openSecured", service, encrypt);
        return open(service);
    }
}
