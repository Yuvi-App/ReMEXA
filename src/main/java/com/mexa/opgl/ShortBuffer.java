package com.mexa.opgl;

public class ShortBuffer extends com.mexa.opgl.Buffer {
    private final short[] data;

    private ShortBuffer(short[] data) {
        super(data.length);
        this.data = data;
    }

    public static com.mexa.opgl.ShortBuffer allocateDirect (int size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ShortBuffer", "allocateDirect", size);
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        return new ShortBuffer(new short[size]);
    }

    public static com.mexa.opgl.ShortBuffer allocateDirect (com.mexa.opgl.ShortBuffer buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ShortBuffer", "allocateDirect", buffer);
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        short[] copy = new short[buffer.boundedLength()];
        System.arraycopy(buffer.data, buffer.boundedOffset(), copy, 0, copy.length);
        return new ShortBuffer(copy);
    }

    public short[] get (int srcIndex, short[] buf, int dstIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ShortBuffer", "get", srcIndex, buf, dstIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(srcIndex, buf.length, dstIndex, length);
        System.arraycopy(data, srcIndex, buf, dstIndex, length);
        return buf;
    }

    public void put (int dstIndex, short[] buf, int srcIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.ShortBuffer", "put", dstIndex, buf, srcIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(dstIndex, buf.length, srcIndex, length);
        System.arraycopy(buf, srcIndex, data, dstIndex, length);
    }

    short[] rawData() {
        return data;
    }
}
