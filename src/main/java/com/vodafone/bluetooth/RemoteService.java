package com.vodafone.bluetooth;

import remexa.bluetooth.VirtualBluetoothRuntime;

public class RemoteService extends BaseService {
    private final Device device;
    private final VirtualBluetoothRuntime.ServiceInfo serviceInfo;

    public RemoteService() {
        this(null, new VirtualBluetoothRuntime.ServiceInfo("", "", "", "", "", ""));
    }

    public RemoteService(Device device) {
        this(device, new VirtualBluetoothRuntime.ServiceInfo("", "", "", "", "", ""));
    }

    RemoteService(Device device, VirtualBluetoothRuntime.ServiceInfo serviceInfo) {
        super(serviceInfo.seed1(), serviceInfo.seed2());
        this.device = device;
        this.serviceInfo = serviceInfo;
        setServiceIdInternal(serviceInfo.serviceId());
        setServiceNameInternal(serviceInfo.serviceName());
    }

    public final Device getDevice() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.RemoteService", "getDevice");
        return device;
    }

    VirtualBluetoothRuntime.ServiceInfo serviceInfo() {
        return serviceInfo;
    }
}
