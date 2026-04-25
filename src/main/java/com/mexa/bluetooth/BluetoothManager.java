package com.mexa.bluetooth;

import java.io.IOException;
import remexa.bluetooth.VirtualBluetoothRuntime;

public class BluetoothManager {
    private static final BluetoothManager INSTANCE = new BluetoothManager();

    public static final com.mexa.bluetooth.BluetoothManager getInstance () throws java.lang.IllegalStateException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getInstance");
        return INSTANCE;
    }

    public final java.lang.String getFriendlyName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getFriendlyName");
        return VirtualBluetoothRuntime.getInstance().localFriendlyName();
    }

    public final int getMaxDevices () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getMaxDevices");
        return 4;
    }

    public final void startDeviceSeek (com.mexa.bluetooth.SeekListener lsn) throws java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "startDeviceSeek", lsn);
        if (lsn == null) {
            throw new NullPointerException("lsn");
        }
        try {
            var device = new Device(VirtualBluetoothRuntime.getInstance().discoverRemoteDevice());
            lsn.foundDevice(device, 512);
            lsn.terminatedDeviceSeek(SeekListener.COMPLETED);
        } catch (IOException exception) {
            lsn.terminatedDeviceSeek(SeekListener.ERROR);
            throw exception;
        }
    }

    public final boolean stopDeviceSeek () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "stopDeviceSeek");
        return true;
    }

    public final void registerPushRequest (java.lang.String appID, java.lang.String sender, java.lang.String content) throws java.lang.NullPointerException, java.lang.IllegalArgumentException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "registerPushRequest", appID, sender, content);
    }

    public java.lang.String[] getPushRequest () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BluetoothManager", "getPushRequest");
        return new String[0];
    }
}
