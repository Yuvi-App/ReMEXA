package com.j_phone.system;

public class ApplicationManager {
    public static final int F_MENU = 0;
    public static final int J_SKY_MENU = 0;
    public static final int MAIL_MENU = 0;
    public static final int WEB_MENU = 0;

    public static com.j_phone.system.ApplicationManager getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "getInstance");
        return null;
    }

    public void setPausedTransitMenu (int type) throws java.lang.IllegalArgumentException, java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "setPausedTransitMenu", type);
    }

    public void scheduleWakeup (int time) throws java.lang.IllegalArgumentException, java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "scheduleWakeup", time);
    }

    public void cancelWakeup () throws java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "cancelWakeup");
    }

    public int getWakeup () throws java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "getWakeup");
        return 0;
    }

    public void setWakeupListener (com.j_phone.system.WakeupListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "setWakeupListener", listener);
    }

    public void flushRMS () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "flushRMS");
    }
}
