package com.vodafone.bluetooth;

public class BaseService {
    private final String seed1;
    private final String seed2;
    private String serviceId = "";
    private String serviceName = "";

    public BaseService() {
        this(null, null);
    }

    public BaseService(String seed) throws NullPointerException {
        this(seed, null);
    }

    public BaseService(String seed1, String seed2) throws NullPointerException {
        this.seed1 = seed1;
        this.seed2 = seed2;
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BaseService", "BaseService", seed1, seed2);
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

    public boolean matches(BaseService svc) throws NullPointerException {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BaseService", "matches", svc);
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

    public String getServiceID() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BaseService", "getServiceID");
        return serviceId;
    }

    public String getServiceName() {
        remexa.probes.SdkStubSupport.log("com.vodafone.bluetooth.BaseService", "getServiceName");
        return serviceName;
    }
}
