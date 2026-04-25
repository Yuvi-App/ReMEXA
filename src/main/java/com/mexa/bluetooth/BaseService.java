package com.mexa.bluetooth;

public class BaseService {
    private final String seed1;
    private final String seed2;
    private String serviceId = "";
    private String serviceName = "";

    public BaseService () {
        this(null, null);
    }

    public BaseService (java.lang.String seed) throws java.lang.NullPointerException {
        this(seed, null);
    }

    public BaseService (java.lang.String seed1, java.lang.String seed2) throws java.lang.NullPointerException {
        this.seed1 = seed1;
        this.seed2 = seed2;
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "BaseService", seed1, seed2);
    }

    protected final void setServiceIdInternal(String serviceId) {
        this.serviceId = serviceId == null ? "" : serviceId;
    }

    protected final void setServiceNameInternal(String serviceName) {
        this.serviceName = serviceName == null ? "" : serviceName;
    }

    protected final String seed1() {
        return seed1;
    }

    protected final String seed2() {
        return seed2;
    }

    public boolean matches (com.mexa.bluetooth.BaseService svc) throws java.lang.NullPointerException {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "matches", svc);
        if (svc == null) {
            throw new NullPointerException("svc");
        }
        boolean seedMatches = normalizeSeed(seed1).equals(normalizeSeed(svc.seed1))
                && normalizeSeed(seed2).equals(normalizeSeed(svc.seed2));
        boolean idMatches = !serviceId.isEmpty() && serviceId.equals(svc.serviceId);
        return seedMatches || idMatches;
    }

    private static String normalizeSeed(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    public java.lang.String getServiceID () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "getServiceID");
        return serviceId;
    }

    public java.lang.String getServiceName () {
        remexa.probes.SdkStubSupport.log("com.mexa.bluetooth.BaseService", "getServiceName");
        return serviceName;
    }
}
