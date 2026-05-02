package com.j_phone.system;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class ApplicationManager {
    public static final int F_MENU = 0;
    public static final int J_SKY_MENU = 1;
    public static final int MAIL_MENU = 2;
    public static final int WEB_MENU = 3;

    private static final com.j_phone.system.ApplicationManager INSTANCE = new com.j_phone.system.ApplicationManager();

    private int pausedTransitMenu = F_MENU;
    private long wakeupDeadlineMillis;
    private com.j_phone.system.WakeupListener wakeupListener;

    private ApplicationManager() {
    }

    public static com.j_phone.system.ApplicationManager getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "getInstance");
        return INSTANCE;
    }

    public void setPausedTransitMenu (int type) throws java.lang.IllegalArgumentException, java.lang.RuntimeException {
        validatePausedTransitMenu(type);
        this.pausedTransitMenu = type;
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "setPausedTransitMenu", type);
    }

    public void scheduleWakeup (int time) throws java.lang.IllegalArgumentException, java.lang.RuntimeException {
        if (time < 0) {
            throw new IllegalArgumentException("time must be >= 0");
        }
        this.wakeupDeadlineMillis = System.currentTimeMillis() + (time * 1000L);
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "scheduleWakeup", time);
    }

    public void cancelWakeup () throws java.lang.RuntimeException {
        this.wakeupDeadlineMillis = 0L;
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "cancelWakeup");
    }

    public int getWakeup () throws java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "getWakeup");
        if (wakeupDeadlineMillis <= 0L) {
            return 0;
        }
        long remainingMillis = wakeupDeadlineMillis - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            wakeupDeadlineMillis = 0L;
            return 0;
        }
        return (int) ((remainingMillis + 999L) / 1000L);
    }

    public void setWakeupListener (com.j_phone.system.WakeupListener listener) {
        this.wakeupListener = listener;
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "setWakeupListener", listener);
    }

    public void flushRMS () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.ApplicationManager", "flushRMS");
        try {
            RecordStore.flushAll();
        } catch (RecordStoreException exception) {
            throw new RuntimeException("Failed to flush RMS", exception);
        }
    }

    public int pausedTransitMenu() {
        return pausedTransitMenu;
    }

    public com.j_phone.system.WakeupListener wakeupListener() {
        return wakeupListener;
    }

    private static void validatePausedTransitMenu(int type) {
        if (type != F_MENU && type != J_SKY_MENU && type != MAIL_MENU && type != WEB_MENU) {
            throw new IllegalArgumentException("Unsupported paused transit menu: " + type);
        }
    }
}
