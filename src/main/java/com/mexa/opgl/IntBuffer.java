package com.mexa.opgl;

public class IntBuffer extends com.mexa.opgl.Buffer {
    private final int[] data;

    private IntBuffer(int[] data) {
        super(data.length);
        this.data = data;
    }

    public static com.mexa.opgl.IntBuffer allocateDirect (int size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.IntBuffer", "allocateDirect", size);
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        return new IntBuffer(new int[size]);
    }

    public static com.mexa.opgl.IntBuffer allocateDirect (com.mexa.opgl.IntBuffer buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.IntBuffer", "allocateDirect", buffer);
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        int[] copy = new int[buffer.boundedLength()];
        System.arraycopy(buffer.data, buffer.boundedOffset(), copy, 0, copy.length);
        return new IntBuffer(copy);
    }

    public int[] get (int srcIndex, int[] buf, int dstIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.IntBuffer", "get", srcIndex, buf, dstIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(srcIndex, buf.length, dstIndex, length);
        System.arraycopy(data, srcIndex, buf, dstIndex, length);
        return buf;
    }

    public void put (int dstIndex, int[] buf, int srcIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.IntBuffer", "put", dstIndex, buf, srcIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(dstIndex, buf.length, srcIndex, length);
        System.arraycopy(buf, srcIndex, data, dstIndex, length);
    }
}
