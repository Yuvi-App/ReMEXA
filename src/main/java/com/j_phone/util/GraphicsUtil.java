package com.j_phone.util;

import remexa.host.jblend.CanvasGraphics3D;

public class GraphicsUtil {
    private static final int RGB_MASK = 0x00FFFFFF;
    private static final int OPAQUE_ALPHA = 0xFF000000;
    private static final int MASK_GRID_SIZE = 4;

    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_MIRROR_ROT90 = 7;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR_ROT270 = 4;
    public static final int STRETCH_QUALITY_NORMAL = 0;
    public static final int STRETCH_QUALITY_LOW = 1;
    public static final int STRETCH_QUALITY_HIGH = 2;

    public static int getPixel (javax.microedition.lcdui.Graphics g, int x, int y) {
        if (g instanceof CanvasGraphics3D canvasGraphics) {
            canvasGraphics.flush();
            var backingImage = canvasGraphics.backingImage();
            if (backingImage != null) {
                int pixelX = x + g.getTranslateX();
                int pixelY = y + g.getTranslateY();
                if (pixelX >= 0
                        && pixelY >= 0
                        && pixelX < backingImage.getWidth()
                        && pixelY < backingImage.getHeight()) {
                    return backingImage.getRGB(pixelX, pixelY) & 0x00FFFFFF;
                }
            }
        }
        return 0;
    }

    public static void setPixel (javax.microedition.lcdui.Graphics g, int x, int y) {
        if (g == null) {
            return;
        }
        setPixel(g, x, y, g.getColor());
    }

    public static void setPixel (javax.microedition.lcdui.Graphics g, int x, int y, int color) {
        if (g == null) {
            return;
        }
        g.drawRGB(new int[]{OPAQUE_ALPHA | (color & RGB_MASK)}, 0, 1, x, y, 1, 1, true);
    }

    public static void drawRegion (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawRegion", g, src, x_src, y_src, width, height, transform, x_dest, y_dest, anchor);
        if (g == null || src == null) {
            return;
        }
        g.drawRegion(src, x_src, y_src, width, height, transform, x_dest, y_dest, anchor);
    }

    public static void drawRegion (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int width_dest, int height_dest, int anchor, int stretch_quality) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawRegion", g, src, x_src, y_src, width, height, transform, x_dest, y_dest, width_dest, height_dest, anchor, stretch_quality);
        if (g == null || src == null) {
            return;
        }
        g.drawRegion(src, x_src, y_src, width, height, transform, x_dest, y_dest, width_dest, height_dest, anchor);
    }

    public static void drawPseudoTransparentImage (javax.microedition.lcdui.Graphics g, javax.microedition.lcdui.Image src, int x_dest, int y_dest, int anchor, short mask_pattern, int element_size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.util.GraphicsUtil", "drawPseudoTransparentImage", g, src, x_dest, y_dest, anchor, mask_pattern, element_size);
        if (g == null || src == null) {
            return;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int mask = mask_pattern & 0xFFFF;
        if (mask == 0) {
            return;
        }
        if (mask == 0xFFFF) {
            g.drawImage(src, x_dest, y_dest, anchor);
            return;
        }

        int elementSize = Math.max(1, element_size);
        int[] pixels = new int[width * height];
        src.getRGB(pixels, 0, width, 0, 0, width, height);
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                if (!isMaskPixelVisible(mask, x, y, elementSize)) {
                    pixels[row + x] = 0;
                }
            }
        }
        g.drawRGB(pixels, 0, width, anchoredX(x_dest, anchor, width), anchoredY(y_dest, anchor, height), width, height, true);
    }

    private static boolean isMaskPixelVisible (int mask, int x, int y, int elementSize) {
        int maskX = (x / elementSize) % MASK_GRID_SIZE;
        int maskY = (y / elementSize) % MASK_GRID_SIZE;
        int bit = 15 - (maskY * MASK_GRID_SIZE + maskX);
        return ((mask >>> bit) & 1) != 0;
    }

    private static int anchoredX (int x, int anchor, int width) {
        if ((anchor & javax.microedition.lcdui.Graphics.RIGHT) != 0) {
            return x - width;
        }
        if ((anchor & javax.microedition.lcdui.Graphics.HCENTER) != 0) {
            return x - width / 2;
        }
        return x;
    }

    private static int anchoredY (int y, int anchor, int height) {
        if ((anchor & javax.microedition.lcdui.Graphics.BOTTOM) != 0) {
            return y - height;
        }
        if ((anchor & javax.microedition.lcdui.Graphics.VCENTER) != 0) {
            return y - height / 2;
        }
        return y;
    }
}
