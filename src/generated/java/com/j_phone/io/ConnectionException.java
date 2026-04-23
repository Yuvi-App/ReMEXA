package com.j_phone.io;

public class ConnectionException extends java.io.IOException {
    public static final int ILLEGAL_STATE = 0;
    public static final int RESOURCE_BUSY = 0;
    public static final int TIMEOUT = 0;
    public static final int USER_ABORT = 0;
    public static final int OBEX_ERROR = 0;
    public static final int UNDEFINED = 0;
    public static final int STATUS_FIRST = 0;
    public static final int STATUS_LAST = 0;

    public ConnectionException () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.ConnectionException", "ConnectionException");
    }

    public ConnectionException (int stat) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.ConnectionException", "ConnectionException", stat);
    }

    public ConnectionException (int status, java.lang.String msg) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.ConnectionException", "ConnectionException", status, msg);
    }


    public int getStatus () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.ConnectionException", "getStatus");
        return 0;
    }
}
