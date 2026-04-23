package com.j_phone.io;

public interface VideoConnection extends com.j_phone.io.OpticalDeviceConnection {
    public static final int TYPE_MPEG4 = 0;
    public static final int TYPE_OTHER = 0;
    public static final int TYPE_MP4 = 0;
    public static final int MODE_MAIL = 0;
    public static final int MODE_FREE = 0;

    public int countAvailableVideoSizes ();
    public int getVideoWidth (int sizeId);
    public int getVideoHeight (int sizeId);
    public void setVideoSize (int sizeId);
    public void setVideoMode (int mode);
    public java.lang.String getFileName ();
    public int getFileFormat ();
    public boolean isSupported (int chkType);
    public void capture () throws java.io.IOException;}
