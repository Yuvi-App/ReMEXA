package com.j_phone.io;

public interface MicControlListener {
    public static final int VOLUME_CHANGED = 1;
    public static final int ECHO_CHANGED = 2;
    public static final int PITCHSCAN_START = 3;
    public static final int PITCHSCAN_STOP = 4;
    public static final int PITCHSCANDATA_OVERFLOW = 5;
    public static final int SWITCH_ON = 6;
    public static final int SWITCH_OFF = 7;

    public void eventOccurred (int event, long time);}
