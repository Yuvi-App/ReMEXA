package com.vodafone.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class LocalService extends BaseService {
    public LocalService() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.LocalService", "LocalService");
    }

    public LocalService(String seed) throws NullPointerException {
        super(seed);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.LocalService", "LocalService", seed);
    }

    public LocalService(String seed1, String seed2) throws NullPointerException {
        super(seed1, seed2);
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.LocalService", "LocalService", seed1, seed2);
    }

    public final void setServiceID(String uuidStr) throws IllegalArgumentException, NumberFormatException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.LocalService", "setServiceID", uuidStr);
        setServiceIdInternal(uuidStr);
    }

    public final void setServiceName(String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.LocalService", "setServiceName", name);
        setServiceNameInternal(name);
    }

    VirtualBluetoothRuntime.ServiceInfo serviceInfo() {
        return new VirtualBluetoothRuntime.ServiceInfo(
                "",
                "",
                getServiceID(),
                getServiceName(),
                seed1(),
                seed2()
        );
    }
}
