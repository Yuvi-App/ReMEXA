package com.mexa.opgl;

public abstract class Buffer {
    public int length () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "length");
        return 0;
    }

    public void setBounds (int offset, int length) {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "setBounds", offset, length);
    }

    public void resetBounds () {
        remexa.probes.SdkStubSupport.log("com.mexa.opgl.Buffer", "resetBounds");
    }
}
