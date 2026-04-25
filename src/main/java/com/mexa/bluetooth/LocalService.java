package com.mexa.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class LocalService extends com.mexa.bluetooth.BaseService {
    public LocalService () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.LocalService", "LocalService");
    }

    public LocalService (java.lang.String seed) throws java.lang.NullPointerException {
        super(seed);
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.LocalService", "LocalService", seed);
    }

    public LocalService (java.lang.String seed1, java.lang.String seed2) throws java.lang.NullPointerException {
        super(seed1, seed2);
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.LocalService", "LocalService", seed1, seed2);
    }


    public final void setServiceID (java.lang.String uuidStr) throws java.lang.IllegalArgumentException, java.lang.NumberFormatException, java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.LocalService", "setServiceID", uuidStr);
        setServiceIdInternal(uuidStr);
    }

    public final void setServiceName (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.LocalService", "setServiceName", name);
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
