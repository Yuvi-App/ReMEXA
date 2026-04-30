package com.mexa.opgl;

public class ByteBuffer extends com.mexa.opgl.Buffer {
    private final byte[] data;

    private ByteBuffer(byte[] data) {
        super(data.length);
        this.data = data;
    }

    public static com.mexa.opgl.ByteBuffer allocateDirect (int size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "allocateDirect", size);
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        return new ByteBuffer(new byte[size]);
    }

    public static com.mexa.opgl.ByteBuffer allocateDirect (com.mexa.opgl.ByteBuffer buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "allocateDirect", buffer);
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        byte[] copy = new byte[buffer.boundedLength()];
        System.arraycopy(buffer.data, buffer.boundedOffset(), copy, 0, copy.length);
        return new ByteBuffer(copy);
    }

    public byte[] get (int srcIndex, byte[] buf, int dstIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "get", srcIndex, buf, dstIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(srcIndex, buf.length, dstIndex, length);
        System.arraycopy(data, srcIndex, buf, dstIndex, length);
        return buf;
    }

    public void put (int dstIndex, byte[] buf, int srcIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ByteBuffer", "put", dstIndex, buf, srcIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(dstIndex, buf.length, srcIndex, length);
        System.arraycopy(buf, srcIndex, data, dstIndex, length);
    }

    byte[] rawData() {
        return data;
    }
}
