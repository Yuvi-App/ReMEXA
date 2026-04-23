package com.jblend.io;

public final class InflateInputStream extends java.io.InputStream {
    protected InflateInputStream() {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "InflateInputStream");
    }

    public InflateInputStream (java.io.InputStream in) {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "InflateInputStream", in);
    }


    public int read () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "read");
        return 0;
    }

    public int read (byte[] b, int off, int len) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "read", b, off, len);
        return 0;
    }

    public int read (byte[] b) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "read", b);
        return 0;
    }

    public void close () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.io.InflateInputStream", "close");
    }
}
