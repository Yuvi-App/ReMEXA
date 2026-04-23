package com.j_phone.util;

public class GraphicsUtil {
    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 0;
    public static final int TRANS_ROT180 = 0;
    public static final int TRANS_ROT270 = 0;
    public static final int TRANS_MIRROR = 0;
    public static final int TRANS_MIRROR_ROT90 = 0;
    public static final int TRANS_MIRROR_ROT180 = 0;
    public static final int TRANS_MIRROR_ROT270 = 0;
    public static final int STRETCH_QUALITY_NORMAL = 0;
    public static final int STRETCH_QUALITY_LOW = 0;
    public static final int STRETCH_QUALITY_HIGH = 0;

    public static int getPixel (javax.microedition.lcdui.Graphics g, int x, int y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "getPixel", g, x, y);
        return 0;
    }

    public static void setPixel (javax.microedition.lcdui.Graphics g, int x, int y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "setPixel", g, x, y);
    }

    public static void setPixel (javax.microedition.lcdui.Graphics g, int x, int y, int color) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "setPixel", g, x, y, color);
    }

    public static void drawRegion (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawRegion", g, src, x_src, y_src, width, height, transform, x_dest, y_dest, anchor);
    }

    public static void drawRegion (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int width_dest, int height_dest, int anchor, int stretch_quality) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawRegion", g, src, x_src, y_src, width, height, transform, x_dest, y_dest, width_dest, height_dest, anchor, stretch_quality);
    }

    public static void drawPseudoTransparentImage (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_dest, int y_dest, int anchor, short mask_pattern, int element_size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawPseudoTransparentImage", g, src, x_dest, y_dest, anchor, mask_pattern, element_size);
    }
}
