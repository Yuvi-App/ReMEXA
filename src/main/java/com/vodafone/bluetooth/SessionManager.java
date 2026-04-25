package com.vodafone.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class SessionManager extends SessionBase {
    private final SessionListener listener;

    protected SessionManager() {
        this(null);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "SessionManager");
    }

    public SessionManager(SessionListener listener) throws NullPointerException {
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
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "SessionManager", listener);
    }

    public final int open(RemoteService service) throws SecurityException, NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "open", service);
        if (service == null) {
            throw new NullPointerException("service");
        }
        return sessionHandle().connect(service.serviceInfo());
    }

    public final int openSecured(RemoteService service, boolean encrypt)
            throws SecurityException, NullPointerException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.SessionManager", "openSecured", service, encrypt);
        return open(service);
    }
}
