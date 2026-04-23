package com.jblend.graphics.sprite;

public abstract class SpriteCanvas extends javax.microedition.lcdui.Canvas {
    protected SpriteCanvas() {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "SpriteCanvas");
    }

    public SpriteCanvas (int numPalettes, int numPatterns) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "SpriteCanvas", numPalettes, numPatterns);
    }


    public final javax.microedition.lcdui.Ticker getTicker () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getTicker");
        return null;
    }

    public final java.lang.String getTitle () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getTitle");
        return "";
    }

    public final void setTicker (javax.microedition.lcdui.Ticker ticker) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setTicker", ticker);
    }

    public final void setTitle (java.lang.String title) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setTitle", title);
    }

    public void createFrameBuffer (int fw, int fh) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "createFrameBuffer", fw, fh);
        remexa.host.runtime.MidletRuntime.updateDisplayMetrics(
                this,
                new remexa.host.profile.DisplayMetrics(fw, fh, "SpriteCanvas.createFrameBuffer")
        );
        remexa.host.runtime.MidletRuntime.createSpriteFrameBuffer(this, fw, fh);
    }

    public void disposeFrameBuffer () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "disposeFrameBuffer");
        remexa.host.runtime.MidletRuntime.disposeSpriteFrameBuffer(this);
    }

    public static int getVirtualWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getVirtualWidth");
        return remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).width();
    }

    public static int getVirtualHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getVirtualHeight");
        return remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).height();
    }

    public void setPalette (int index, int palette) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setPalette", index, palette);
    }

    public void setPattern (int index, byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setPattern", index, data);
    }

    public static short createCharacterCommand (int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "createCharacterCommand", offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo);
        return (short) 0;
    }

    public void drawSpriteChar (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "drawSpriteChar", command, x, y);
    }

    public void drawBackground (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "drawBackground", command, x, y);
    }

    public void copyArea (int sx, int sy, int fw, int fh, int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "copyArea", sx, sy, fw, fh, tx, ty);
        remexa.host.runtime.MidletRuntime.spriteCopyArea(this, sx, sy, fw, fh, tx, ty);
    }

    public void copyFullScreen (int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "copyFullScreen", tx, ty);
        remexa.host.runtime.MidletRuntime.spriteCopyFullScreen(this, tx, ty);
    }

    public void drawFrameBuffer (int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "drawFrameBuffer", tx, ty);
        remexa.host.runtime.MidletRuntime.spriteDrawFrameBuffer(this, tx, ty);
    }
}
