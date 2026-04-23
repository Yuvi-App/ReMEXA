package com.j_phone.io;

public interface PhoneConnection extends javax.microedition.io.Connection {
    public static final int BEFORE_CALLING = 0;
    public static final int NORMAL = 0;
    public static final int CANCEL_BY_MYSELF = 0;
    public static final int LINE_ENGAGED = 0;
    public static final int OFF_LINE = 0;
    public static final int OUT_OF_SERVICE = 0;
    public static final int FORMAT_ERROR = 0;
    public static final int ERROR = 0;

    public void connect () throws java.io.IOException;
    public int getOffLineCause () throws java.io.IOException;}
