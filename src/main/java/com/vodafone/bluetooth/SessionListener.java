package com.vodafone.bluetooth;

public interface SessionListener {
    int SIGNAL_START = 0;
    int SIGNAL_END = 1;
    int SIGNAL_PAUSE = 2;
    int SIGNAL_WAIT = 3;
    int SIGNAL_REJECT = 4;
    int CONN_OPENED = 5;
    int CONN_CLOSED = 6;
    int CONN_FAILED = 7;
    int SUCCESS = 0;
    int ERROR_NO_CONNECTION = 1;
    int ERROR_GOT_NACK = 2;
    int ERROR_ACK_TIMEOUT = 3;

    void gotConnectionStatus(int connID, int status);

    void gotMemberList(int[] connID);

    void gotMessage(int connID, String msg);

    void gotMessage(int connID, byte[] msg);

    void gotSignal(int connID, int signal);

    void gotResult(int msgID, int[] connIDs, int[] results);
}
