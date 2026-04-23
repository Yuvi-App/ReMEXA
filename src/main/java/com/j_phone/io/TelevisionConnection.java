package com.j_phone.io;

public interface TelevisionConnection extends javax.microedition.io.Connection {
    public static final int GROUND_WAVE_ANALOG = 0;
    public static final int CATV = 0;
    public static final int GROUND_WAVE_DIGITAL = 0;
    public static final int STATE_READY = 0;
    public static final int STATE_UNSUPPORTED = 0;
    public static final int STATE_LITTLE_BATTERY = 0;
    public static final int STATE_TV_OFF = 0;
    public static final int STATE_RECORDING = 0;
    public static final int STATE_IMPOSSIBLE = 0;

    public int getState (int category);
    public void activate (int category, int channel) throws java.io.IOException;
    public int getLastCategory () throws java.io.IOException;
    public int getLastChannel () throws java.io.IOException;
    public int getAreaInfo () throws java.io.IOException;
    public int getReserveDataCount (int type) throws java.io.IOException;
    public int getReserveDataMaxCount (int type) throws java.io.IOException;
    public void addReserveData (com.j_phone.io.TelevisionReserveData data) throws java.io.IOException;
    public void removeReserveData (com.j_phone.io.TelevisionReserveData data) throws java.io.IOException;
    public com.j_phone.io.TelevisionReserveData getReserveData (int number) throws java.io.IOException;
    public com.j_phone.io.TelevisionReserveData createReserveData () throws java.io.IOException;
    public void setTelevisionReserveDataListener (com.j_phone.io.TelevisionReserveDataListener listener);}
