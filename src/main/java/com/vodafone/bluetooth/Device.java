package com.vodafone.bluetooth;

public class Device {
    private final String bluetoothAddress;

    protected Device() {
        this("");
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "Device");
    }

    public Device(String bdaddr) throws NullPointerException, IllegalArgumentException {
        this.bluetoothAddress = bdaddr == null ? "" : bdaddr;
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "Device", bdaddr);
    }

    public final String getFriendlyName() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "getFriendlyName");
        return bluetoothAddress.isEmpty() ? "ReMEXA Device" : bluetoothAddress;
    }

    public final void startServiceSeek(BaseService[] svcList, SeekListener lsn)
            throws NullPointerException, IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "startServiceSeek", svcList, lsn);
        if (lsn == null) {
            throw new NullPointerException("lsn");
        }
        lsn.terminatedServiceSeek(this, SeekListener.SERVICE_NOT_FOUND);
    }

    public final boolean stopServiceSeek() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "stopServiceSeek");
        return true;
    }

    public final String getBluetoothAddress() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.Device", "getBluetoothAddress");
        return bluetoothAddress;
    }
}
