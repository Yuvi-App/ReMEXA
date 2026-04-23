package com.jblend.micro.io;

public final class SerialConnection  implements javax.microedition.io.StreamConnection {
    public static final int PORT_IDLE_ALIVE = 0;
    public static final int PORT_OPENED = 0;
    public static final int PORT_DISCONNECTED = 0;

    protected SerialConnection() {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "SerialConnection");
    }

    public SerialConnection (java.lang.String URI, int mode, boolean timeouts) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "SerialConnection", URI, mode, timeouts);
    }


    public static int getPortState (java.lang.String portName) {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getPortState", portName);
        return 0;
    }

    public java.io.InputStream openInputStream () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "openInputStream");
        return null;
    }

    public java.io.DataInputStream openDataInputStream () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "openDataInputStream");
        return null;
    }

    public java.io.OutputStream openOutputStream () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "openOutputStream");
        return null;
    }

    public java.io.DataOutputStream openDataOutputStream () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "openDataOutputStream");
        return null;
    }

    public void close () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "close");
    }

    public int getBaudrate () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getBaudrate");
        return 0;
    }

    public int setBaudrate (int baudrate) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "setBaudrate", baudrate);
        return 0;
    }

    public int getTxBufferSize () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getTxBufferSize");
        return 0;
    }

    public int getTxBufferFree () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getTxBufferFree");
        return 0;
    }

    public int getRxBufferSize () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getRxBufferSize");
        return 0;
    }

    public int getRxBufferFree () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.io.SerialConnection", "getRxBufferFree");
        return 0;
    }
}
