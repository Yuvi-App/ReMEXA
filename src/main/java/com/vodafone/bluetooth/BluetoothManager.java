package com.vodafone.bluetooth;

public class BluetoothManager {
    private static final BluetoothManager INSTANCE = new BluetoothManager();

    public static BluetoothManager getInstance() throws IllegalStateException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "getInstance");
        return INSTANCE;
    }

    public final String getFriendlyName() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "getFriendlyName");
        return "ReMEXA";
    }

    public final int getMaxDevices() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "getMaxDevices");
        return 4;
    }

    public final void startDeviceSeek(SeekListener lsn)
            throws NullPointerException, IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "startDeviceSeek", lsn);
        if (lsn == null) {
            throw new NullPointerException("lsn");
        }
        lsn.terminatedDeviceSeek(SeekListener.COMPLETED);
    }

    public final boolean stopDeviceSeek() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "stopDeviceSeek");
        return true;
    }

    public final void registerPushRequest(String appID, String sender, String content)
            throws NullPointerException, IllegalArgumentException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "registerPushRequest", appID, sender, content);
    }

    public String[] getPushRequest() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BluetoothManager", "getPushRequest");
        return new String[0];
    }
}
