package com.mexa.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class RemoteService extends com.mexa.bluetooth.BaseService {
    private final Device device;
    private final VirtualBluetoothRuntime.ServiceInfo serviceInfo;

    public RemoteService() {
        this(null, new VirtualBluetoothRuntime.ServiceInfo("", "", "", "", "", ""));
    }

    RemoteService(Device device, VirtualBluetoothRuntime.ServiceInfo serviceInfo) {
        super(serviceInfo.seed1(), serviceInfo.seed2());
        this.device = device;
        this.serviceInfo = serviceInfo;
        setServiceIdInternal(serviceInfo.serviceId());
        setServiceNameInternal(serviceInfo.serviceName());
    }

    public final com.mexa.bluetooth.Device getDevice () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.RemoteService", "getDevice");
        return device;
    }

    VirtualBluetoothRuntime.ServiceInfo serviceInfo() {
        return serviceInfo;
    }
}
