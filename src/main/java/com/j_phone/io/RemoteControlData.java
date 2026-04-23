package com.j_phone.io;

public class RemoteControlData {
    public static final int OUTPUT_PPM_HIGH_LOW = 0;
    public static final int OUTPUT_PPM_LOW_HIGH = 0;
    public static final int OUTPUT_MANCHESTER = 0;

    public RemoteControlData () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "RemoteControlData");
    }


    public void setPulse (int leader_on, int leader_off, int trailer_on) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "setPulse", leader_on, leader_off, trailer_on);
    }

    public void setLogicalPulse (int output, int data0_on, int data0_off, int data1_on, int data1_off) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "setLogicalPulse", output, data0_on, data0_off, data1_on, data1_off);
    }

    public void setCarrier (int on, int off) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "setCarrier", on, off);
    }

    public void setData (int length, byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "setData", length, data);
    }

    public void setRepeat (int time, int count) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.RemoteControlData", "setRepeat", time, count);
    }
}
