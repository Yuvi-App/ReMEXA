package com.j_phone.io;

public interface CameraConnection extends com.j_phone.io.OpticalDeviceConnection {
    public static final int CHKTYPE_FORMAT_JPEG = 0;
    public static final int CHKTYPE_FORMAT_PNG = 0;
    public static final int QUALITY_NORMAL = 0;
    public static final int QUALITY_FINE = 0;
    public static final int QUALITY_SUPERFINE = 0;
    public static final int FORMAT_JPEG = 0;
    public static final int FORMAT_PNG = 0;

    public boolean isSupported (int chkType) throws java.lang.IllegalArgumentException;
    public int countAvailablePictureSizes ();
    public int getPictureWidth (int sizeId) throws java.lang.IllegalArgumentException;
    public int getPictureHeight (int sizeId) throws java.lang.IllegalArgumentException;
    public void setPictureSize (int sizeId) throws java.lang.IllegalArgumentException;
    public void setPictureQuality (int quality) throws java.lang.IllegalArgumentException;
    public void setPictureFormat (int format) throws java.lang.IllegalArgumentException;
    public void setPictureFrame (java.lang.String frameFileName);
    public void setPictureFrame (byte[] bytes);
    public void capture () throws java.io.IOException;
    public java.lang.String getFileName ();}
