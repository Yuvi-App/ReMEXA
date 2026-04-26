package com.j_phone.phonedata;

public class CallInfo {
    private static final com.j_phone.phonedata.CallInfo INSTANCE = new com.j_phone.phonedata.CallInfo();

    private CallInfo () {
    }

    public static com.j_phone.phonedata.CallInfo getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.CallInfo", "getInstance");
        return INSTANCE;
    }

    public int getLastTime () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.CallInfo", "getLastTime");
        return 0;
    }

    public int getLastCharge () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.CallInfo", "getLastCharge");
        return 0;
    }

    public int getAmountTime () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.CallInfo", "getAmountTime");
        return 0;
    }

    public int getAmountCharge () {
        remexa.probes.SdkStubSupport.log("com.j_phone.phonedata.CallInfo", "getAmountCharge");
        return 0;
    }
}
