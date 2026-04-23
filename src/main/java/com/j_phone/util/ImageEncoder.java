package com.j_phone.util;

public class ImageEncoder {
    public static final int FORMAT_PNG = 0;
    public static final int FORMAT_JPEG = 0;
    public static final int SIZE_6KB = 0;
    public static final int SIZE_12KB = 0;
    public static final int SIZE_30KB = 0;
    public static final int QUALITY_FINE = 0;
    public static final int QUALITY_NORMAL = 0;

    public static com.j_phone.util.ImageEncoder createEncoder (int format) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.ImageEncoder", "createEncoder", format);
        return null;
    }

    public void setJpegOption (int option) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.ImageEncoder", "setJpegOption", option);
    }

    public byte[] encodeOffscreen (javax.microedition.lcdui.Image src, int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.ImageEncoder", "encodeOffscreen", src, x, y, width, height);
        return null;
    }
}
