package com.mexa.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import remexa.bluetooth.VirtualBluetoothRuntime;

public class Device {
    private final VirtualBluetoothRuntime.DeviceInfo deviceInfo;
    private final String bluetoothAddress;
    private final String friendlyName;

    protected Device() {
        this(new VirtualBluetoothRuntime.DeviceInfo("", "ReMEXA Device"));
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "Device");
    }

    public Device (java.lang.String bdaddr) throws java.lang.NullPointerException, java.lang.IllegalArgumentException {
        this(new VirtualBluetoothRuntime.DeviceInfo(bdaddr == null ? "" : bdaddr, bdaddr == null ? "ReMEXA Device" : bdaddr));
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "Device", bdaddr);
    }

    Device(VirtualBluetoothRuntime.DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.bluetoothAddress = deviceInfo.address();
        this.friendlyName = deviceInfo.friendlyName().isBlank() ? "ReMEXA Device" : deviceInfo.friendlyName();
    }

    public final java.lang.String getFriendlyName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "getFriendlyName");
        return friendlyName;
    }

    public final void startServiceSeek (com.mexa.bluetooth.BaseService[] svcList, com.mexa.bluetooth.SeekListener lsn) throws java.lang.NullPointerException, java.lang.IllegalStateException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "startServiceSeek", svcList, lsn);
        if (lsn == null) {
            throw new NullPointerException("lsn");
        }
        try {
            var discovered = VirtualBluetoothRuntime.getInstance().discoverServices(deviceInfo);
            var matches = new ArrayList<RemoteService>();
            for (var serviceInfo : discovered) {
                var remoteService = new RemoteService(this, serviceInfo);
                if (svcList == null || svcList.length == 0) {
                    matches.add(remoteService);
                    continue;
                }
                for (var filter : svcList) {
                    if (filter != null && filter.matches(remoteService)) {
                        matches.add(remoteService);
                        break;
                    }
                }
            }
            if (!matches.isEmpty()) {
                lsn.foundService(matches.toArray(RemoteService[]::new));
                lsn.terminatedServiceSeek(this, SeekListener.COMPLETED);
                return;
            }
            lsn.terminatedServiceSeek(this, SeekListener.SERVICE_NOT_FOUND);
        } catch (IOException exception) {
            lsn.terminatedServiceSeek(this, SeekListener.ERROR);
            throw exception;
        }
    }

    public final boolean stopServiceSeek () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "stopServiceSeek");
        return true;
    }

    public final java.lang.String getBluetoothAddress () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.Device", "getBluetoothAddress");
        return bluetoothAddress;
    }
}
