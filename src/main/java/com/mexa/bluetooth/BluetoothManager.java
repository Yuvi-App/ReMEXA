package com.mexa.bluetooth;

public class BluetoothManager {
    public static final com.mexa.bluetooth.BluetoothManager getInstance () throws java.lang.IllegalStateException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getInstance");
        return null;
    }

    public final java.lang.String getFriendlyName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getFriendlyName");
        return "";
    }

    public final int getMaxDevices () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getMaxDevices");
        return 0;
    }

    public final void startDeviceSeek (com.mexa.bluetooth.SeekListener lsn) throws java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "startDeviceSeek", lsn);
    }

    public final boolean stopDeviceSeek () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "stopDeviceSeek");
        return false;
    }

    public final void registerPushRequest (java.lang.String appID, java.lang.String sender, java.lang.String content) throws java.lang.NullPointerException, java.lang.IllegalArgumentException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "registerPushRequest", appID, sender, content);
    }

    public java.lang.String[] getPushRequest () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getPushRequest");
        return null;
    }
}
