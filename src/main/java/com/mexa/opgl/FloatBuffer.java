package com.mexa.opgl;

public class FloatBuffer extends com.mexa.opgl.Buffer {
    private final float[] data;

    private FloatBuffer(float[] data) {
        super(data.length);
        this.data = data;
    }

    public static com.mexa.opgl.FloatBuffer allocateDirect (int size) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.FloatBuffer", "allocateDirect", size);
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        return new FloatBuffer(new float[size]);
    }

    public static com.mexa.opgl.FloatBuffer allocateDirect (com.mexa.opgl.FloatBuffer buffer) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.FloatBuffer", "allocateDirect", buffer);
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        float[] copy = new float[buffer.boundedLength()];
        System.arraycopy(buffer.data, buffer.boundedOffset(), copy, 0, copy.length);
        return new FloatBuffer(copy);
    }

    public float[] get (int srcIndex, float[] buf, int dstIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.FloatBuffer", "get", srcIndex, buf, dstIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(srcIndex, buf.length, dstIndex, length);
        System.arraycopy(data, srcIndex, buf, dstIndex, length);
        return buf;
    }

    public void put (int dstIndex, float[] buf, int srcIndex, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.FloatBuffer", "put", dstIndex, buf, srcIndex, length);
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        validateRange(dstIndex, buf.length, srcIndex, length);
        System.arraycopy(buf, srcIndex, data, dstIndex, length);
    }
}
