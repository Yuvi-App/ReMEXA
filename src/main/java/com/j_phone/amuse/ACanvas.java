package com.j_phone.amuse;

public abstract class ACanvas extends javax.microedition.lcdui.Canvas implements com.jblend.ui.SequenceInterface {
    protected ACanvas() {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "ACanvas");
    }

    public ACanvas (int numPalettes, int numPatterns, int fw, int fh) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "ACanvas", numPalettes, numPatterns, fw, fh);
    }


    public final javax.microedition.lcdui.Ticker getTicker () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getTicker");
        return null;
    }

    public final java.lang.String getTitle () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getTitle");
        return "";
    }

    public final void setTicker (javax.microedition.lcdui.Ticker ticker) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setTicker", ticker);
    }

    public final void setTitle (java.lang.String title) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setTitle", title);
    }

    public static int getVirtualWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getVirtualWidth");
        return 0;
    }

    public static int getVirtualHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getVirtualHeight");
        return 0;
    }

    public void setPalette (int index, int palette) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setPalette", index, palette);
    }

    public void setPattern (int index, byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setPattern", index, data);
    }

    public static short createCharacterCommand (int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "createCharacterCommand", offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo);
        return (short) 0;
    }

    public void drawSpriteChar (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "drawSpriteChar", command, x, y);
    }

    public void drawBackground (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "drawBackground", command, x, y);
    }

    public void copyArea (int sx, int sy, int fw, int fh, int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "copyArea", sx, sy, fw, fh, tx, ty);
    }

    public void scroll (int dx, int dy) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "scroll", dx, dy);
    }

    public void flush (int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "flush", tx, ty);
    }

    public final void sequenceStart () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "sequenceStart");
    }

    public final void sequenceStop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "sequenceStop");
    }
}
