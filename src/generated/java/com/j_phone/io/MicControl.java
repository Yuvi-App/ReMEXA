package com.j_phone.io;

public class MicControl {
    public static final int MIC = 0;
    public static final int PITCH_SCANNING = 0;

    public static com.j_phone.io.MicControl getInstance () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getInstance");
        return null;
    }

    public int getState () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getState");
        return 0;
    }

    public void enable () throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "enable");
    }

    public void disable () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "disable");
    }

    public int getMaxVolume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getMaxVolume");
        return 0;
    }

    public int getVolume () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getVolume");
        return 0;
    }

    public void setVolume (int volume) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setVolume", volume);
    }

    public int getEchoMaxLevel () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getEchoMaxLevel");
        return 0;
    }

    public int getEchoLevel () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getEchoLevel");
        return 0;
    }

    public void setEchoLevel (int level) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setEchoLevel", level);
    }

    public int getScanInterval () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "getScanInterval");
        return 0;
    }

    public void startPitchScan (com.j_phone.io.PitchScanData scandata) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "startPitchScan", scandata);
    }

    public void stopPitchScan () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "stopPitchScan");
    }

    public static void setMicControlListener (com.j_phone.io.MicControlListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.MicControl", "setMicControlListener", listener);
    }
}
