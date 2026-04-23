package com.mexa.bluetooth;

public class BaseService {
    public BaseService () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "BaseService");
    }

    public BaseService (java.lang.String seed) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "BaseService", seed);
    }

    public BaseService (java.lang.String seed1, java.lang.String seed2) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "BaseService", seed1, seed2);
    }


    public boolean matches (com.mexa.bluetooth.BaseService svc) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "matches", svc);
        return false;
    }

    public java.lang.String getServiceID () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "getServiceID");
        return "";
    }

    public java.lang.String getServiceName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "getServiceName");
        return "";
    }
}
