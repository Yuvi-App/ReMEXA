package com.mexa.bluetooth;

public class Device {
    protected Device() {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "Device");
    }

    public Device (java.lang.String bdaddr) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "Device", bdaddr);
    }


    public final java.lang.String getFriendlyName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "getFriendlyName");
        return "";
    }

    public final void startServiceSeek (com.mexa.bluetooth.BaseService[] svcList, com.mexa.bluetooth.SeekListener lsn) throws java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "startServiceSeek", svcList, lsn);
    }

    public final boolean stopServiceSeek () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "stopServiceSeek");
        return false;
    }

    public final java.lang.String getBluetoothAddress () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "getBluetoothAddress");
        return "";
    }
}
