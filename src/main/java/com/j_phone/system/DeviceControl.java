package com.j_phone.system;

public class DeviceControl {
    public static final int BATTERY = 0;
    public static final int FIELD_INTENSITY = 0;
    public static final int KEY_STATE = 0;
    public static final int VIBRATION = 0;
    public static final int BACK_LIGHT = 0;
    public static final int EIGHT_DIRECTIONS = 0;
    public static final int FLIP_STATE = 0;
    public static final int MEMORY_CARD = 0;
    public static final int SPEAKER_STATE = 0;
    public static final int ENHANCED_KEY_STATE = 0;
    public static final int FLIP_OPENED = 0;
    public static final int FLIP_CLOSED = 0;
    public static final int MEMORY_CARD_OFF = 0;
    public static final int MEMORY_CARD_WRITABLE = 0;
    public static final int MEMORY_CARD_WRITE_PROTECTED = 0;
    public static final int MEMORY_CARD_READ_ONLY = 0;
    public static final int NEW_ARRIVAL_STATE_CALL = 0;
    public static final int NEW_ARRIVAL_STATE_MAIL = 0;
    public static final int SPEAKER_INTERNAL = 0;
    public static final int SPEAKER_EXTERNAL = 0;
    public static final int RAB_GPRS = 0;
    public static final int RAB_R99 = 0;
    public static final int RAB_HSDPA_C6 = 0;
    public static final int RAB_HSDPA_C7 = 0;
    public static final int RAB_NULL = 0;
    public static final int RAB_MEASUREMENT = 0;
    public static final int RAB_INDEFINITE = 0;
    public static final int STYLE_PORTRAIT = 0;
    public static final int STYLE_LANDSCAPE = 0;

    public static final com.j_phone.system.DeviceControl getDefaultDeviceControl () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getDefaultDeviceControl");
        return null;
    }

    public int getDeviceState (int deviceNo) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getDeviceState", deviceNo);
        return 0;
    }

    public boolean isDeviceActive (int deviceNo) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "isDeviceActive", deviceNo);
        return false;
    }

    public boolean setDeviceActive (int deviceNo, boolean active) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setDeviceActive", deviceNo, active);
        return false;
    }

    public void blink (int lighting, int extinction, int repeat) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "blink", lighting, extinction, repeat);
    }

    public boolean setKeyRepeatState (int key, boolean state) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setKeyRepeatState", key, state);
        return false;
    }

    public boolean getKeyRepeatState (int key) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getKeyRepeatState", key);
        return false;
    }

    public int getLatitude () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getLatitude");
        return 0;
    }

    public int getLongitude () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getLongitude");
        return 0;
    }

    public java.lang.String getPlaceName () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getPlaceName");
        return "";
    }

    public void updateLocationInfo () throws java.lang.RuntimeException {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "updateLocationInfo");
    }

    public int getNewArrivalState () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getNewArrivalState");
        return 0;
    }

    public java.lang.String getWakeupParam (javax.microedition.midlet.MIDlet midlet, java.lang.String name) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getWakeupParam", midlet, name);
        return "";
    }

    public java.lang.String getMyTelNumber () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getMyTelNumber");
        return "";
    }

    public java.lang.String getIMEI () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getIMEI");
        return "";
    }

    public int getTransmissionRate () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getTransmissionRate");
        return 0;
    }

    public int getStyle () {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "getStyle");
        return 0;
    }

    public static void setMailListener (com.j_phone.system.MailListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setMailListener", listener);
    }

    public static void setScheduledAlarmListener (com.j_phone.system.ScheduledAlarmListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setScheduledAlarmListener", listener);
    }

    public static void setTelephonyListener (com.j_phone.system.TelephonyListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setTelephonyListener", listener);
    }

    public static void setRingStateListener (com.j_phone.system.RingStateListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setRingStateListener", listener);
    }

    public static void setBodyOpenListener (com.j_phone.system.BodyOpenListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setBodyOpenListener", listener);
    }

    public static void setLocationUpdateListener (com.j_phone.system.LocationUpdateListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setLocationUpdateListener", listener);
    }

    public static void setPhoneStateListener (com.j_phone.system.PhoneStateListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setPhoneStateListener", listener);
    }

    public static void setMemoryCardListener (com.j_phone.system.MemoryCardListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setMemoryCardListener", listener);
    }

    public static void setSpeakerStateListener (com.j_phone.system.SpeakerStateListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setSpeakerStateListener", listener);
    }

    public static void setStyleChangedListener (com.j_phone.system.StyleChangedListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.system.DeviceControl", "setStyleChangedListener", listener);
    }
}
