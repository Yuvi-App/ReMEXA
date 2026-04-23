package com.mexa.opgl;

public class ByteBuffer extends com.mexa.opgl.Buffer {
    public static com.mexa.opgl.ByteBuffer allocateDirect (int size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "allocateDirect", size);
        return null;
    }

    public static com.mexa.opgl.ByteBuffer allocateDirect (com.mexa.opgl.ByteBuffer buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "allocateDirect", buffer);
        return null;
    }

    public byte[] get (int srcIndex, byte[] buf, int dstIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "get", srcIndex, buf, dstIndex, length);
        return null;
    }

    public void put (int dstIndex, byte[] buf, int srcIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "put", dstIndex, buf, srcIndex, length);
    }
}
