package com.j_phone.io;

public interface MicControlListener {
    public static final int VOLUME_CHANGED = 0;
    public static final int ECHO_CHANGED = 0;
    public static final int PITCHSCAN_START = 0;
    public static final int PITCHSCAN_STOP = 0;
    public static final int PITCHSCANDATA_OVERFLOW = 0;
    public static final int SWITCH_ON = 0;
    public static final int SWITCH_OFF = 0;

    public void eventOccurred (int event, long time);}
