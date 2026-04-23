package com.j_phone.io;

public interface BarCodeConnection extends com.j_phone.io.OpticalDeviceConnection {
    public static final int CHKTYPE_JANCODE = 0;
    public static final int CHKTYPE_CONTINUOUS_READ = 0;
    public static final int READTYPE_JANCODE_SINGLE = 0;
    public static final int READTYPE_JANCODE_CONTINUOUS = 0;
    public static final int READTYPE_QRCODE_SINGLE = 0;
    public static final int READTYPE_QRCODE_CONTINUOUS = 0;
    public static final int READTYPE_JANQR_SINGLE = 0;
    public static final int JAN_CODE = 0;
    public static final int QR_CODE = 0;
    public static final int EXT_QR_CODE = 0;

    public boolean isSupported (int chkType) throws java.lang.IllegalArgumentException;
    public void setBarCodeReaderType (int readType) throws java.lang.IllegalArgumentException;
    public void capture () throws java.io.IOException;
    public int count ();
    public int getType (int index) throws java.lang.IllegalArgumentException;
    public byte[] getBytes (int index) throws java.lang.IllegalArgumentException;}
