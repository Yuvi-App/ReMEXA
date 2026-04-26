package com.mexa.opgl;

public abstract class Buffer {
    private final int length;
    private int boundOffset;
    private int boundLength;

    protected Buffer(int length) {
        this.length = length;
        this.boundOffset = 0;
        this.boundLength = length;
    }

    public int length () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "length");
        return length;
    }

    public void setBounds (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "setBounds", offset, length);
        if (offset < 0 || offset >= this.length || length <= 0 || offset + length > this.length) {
            throw new IndexOutOfBoundsException();
        }
        this.boundOffset = offset;
        this.boundLength = length;
    }

    public void resetBounds () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "resetBounds");
        boundOffset = 0;
        boundLength = length;
    }

    protected final void validateRange(int index, int arrayLength, int otherIndex, int copyLength) {
        if (copyLength < 0
                || index < 0
                || index > length
                || index + copyLength > length
                || otherIndex < 0
                || otherIndex > arrayLength
                || otherIndex + copyLength > arrayLength) {
            throw new IndexOutOfBoundsException();
        }
    }

    final int boundedOffset() {
        return boundOffset;
    }

    final int boundedLength() {
        return boundLength;
    }
}
