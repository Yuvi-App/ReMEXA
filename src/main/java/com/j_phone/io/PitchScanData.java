package com.j_phone.io;

public class PitchScanData {
    protected PitchScanData() {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "PitchScanData");
    }

    public PitchScanData (int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "PitchScanData", size);
    }


    public int[] getPitch (int offset, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getPitch", offset, size);
        return null;
    }

    public int[] getVoice (int offset, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getVoice", offset, size);
        return null;
    }

    public int getCurrentIndex () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.PitchScanData", "getCurrentIndex");
        return 0;
    }
}
