package com.mexa.bluetooth;

public interface SessionListener {
    public static final int SIGNAL_START = 0;
    public static final int SIGNAL_END = 0;
    public static final int SIGNAL_PAUSE = 0;
    public static final int SIGNAL_WAIT = 0;
    public static final int SIGNAL_REJECT = 0;
    public static final int CONN_OPENED = 0;
    public static final int CONN_CLOSED = 0;
    public static final int CONN_FAILED = 0;
    public static final int SUCCESS = 0;
    public static final int ERROR_NO_CONNECTION = 0;
    public static final int ERROR_GOT_NACK = 0;
    public static final int ERROR_ACK_TIMEOUT = 0;

    public void gotConnectionStatus (int connID, int status);
    public void gotMemberList (int[] connID);
    public void gotMessage (int connID, java.lang.String msg);
    public void gotMessage (int connID, byte[] msg);
    public void gotSignal (int connID, int signal);
    public void gotResult (int msgID, int[] connIDs, int[] results);}
