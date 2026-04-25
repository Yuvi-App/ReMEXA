package com.vodafone.bluetooth;

public class RemoteService extends BaseService {
    private final Device device;

    public RemoteService() {
        this(null);
    }

    public RemoteService(Device device) {
        this.device = device;
    }

    public final Device getDevice() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.RemoteService", "getDevice");
        return device;
    }
}
