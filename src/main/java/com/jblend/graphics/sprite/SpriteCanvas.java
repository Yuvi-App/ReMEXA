package com.jblend.graphics.sprite;

public abstract class SpriteCanvas extends javax.microedition.lcdui.Canvas {
    private static final int PALETTE_BANK_SIZE = 32;
    private static final int PATTERN_MASK = 0x00FF;
    private static final int ROTATION_MASK = 0x0C00;
    private static final int UPSIDE_DOWN_MASK = 0x0200;
    private static final int RIGHTSIDE_LEFT_MASK = 0x0100;
    private static final int TRANSPARENT_MASK = 0x1000;
    private static final int OFFSET_MASK = 0xE000;

    private final int[] palette;
    private final byte[][] patterns;

    protected SpriteCanvas() {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "SpriteCanvas");
        palette = new int[256];
        patterns = new byte[256][];
    }

    public SpriteCanvas (int numPalettes, int numPatterns) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "SpriteCanvas", numPalettes, numPatterns);
        if (numPalettes < 1 || numPalettes > 256 || numPatterns < 1 || numPatterns > 256) {
            throw new IllegalArgumentException("SpriteCanvas palette and pattern counts must be between 1 and 256.");
        }
        palette = new int[numPalettes];
        patterns = new byte[numPatterns][];
    }



    public final javax.microedition.lcdui.Ticker getTicker () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getTicker");
        return super.getTicker();
    }

    public final java.lang.String getTitle () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "getTitle");
        return super.getTitle();
    }

    public final void setTicker (javax.microedition.lcdui.Ticker ticker) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setTicker", ticker);
        super.setTicker(ticker);
    }

    public final void setTitle (java.lang.String title) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setTitle", title);
        super.setTitle(title);
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
        this.palette[index] = palette;
    }

    public void setPattern (int index, byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "setPattern", index, data);
        if (data == null || data.length != 64) {
            throw new IllegalArgumentException("SpriteCanvas patterns must be 8x8 indexed pixels.");
        }
        patterns[index] = data.clone();
    }

    public static short createCharacterCommand (int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "createCharacterCommand", offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo);
        if (offset < 0 || offset > 7) {
            throw new IllegalArgumentException("SpriteCanvas palette offset must be between 0 and 7.");
        }
        if (rotation < 0 || rotation > 3) {
            throw new IllegalArgumentException("SpriteCanvas rotation must be between 0 and 3.");
        }
        if (patternNo < 0 || patternNo > 255) {
            throw new IllegalArgumentException("SpriteCanvas pattern number must be between 0 and 255.");
        }
        int encoded = patternNo & PATTERN_MASK;
        encoded |= (rotation & 0x3) << 10;
        if (isUpsideDown) {
            encoded |= UPSIDE_DOWN_MASK;
        }
        if (isRightsideLeft) {
            encoded |= RIGHTSIDE_LEFT_MASK;
        }
        if (transparent) {
            encoded |= TRANSPARENT_MASK;
        }
        encoded |= (offset & 0x7) << 13;
        return (short) encoded;
    }

    public void drawSpriteChar (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "drawSpriteChar", command, x, y);
        var resolved = requireCommand(command);
        remexa.host.runtime.MidletRuntime.amuseDrawPattern(
                this,
                palette,
                requirePattern(resolved.patternNo),
                resolved.offset * PALETTE_BANK_SIZE,
                resolved.transparent,
                true,
                x,
                y,
                resolved.rotation,
                resolved.upsideDown,
                resolved.rightsideLeft
        );
    }

    public void drawBackground (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.sprite.SpriteCanvas", "drawBackground", command, x, y);
        var resolved = requireCommand(command);
        remexa.host.runtime.MidletRuntime.amuseDrawPattern(
                this,
                palette,
                requirePattern(resolved.patternNo),
                resolved.offset * PALETTE_BANK_SIZE,
                false,
                false,
                x * 8,
                y * 8,
                resolved.rotation,
                resolved.upsideDown,
                resolved.rightsideLeft
        );
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

    private byte[] requirePattern(int patternNo) {
        if (patternNo < 0 || patternNo >= patterns.length || patterns[patternNo] == null) {
            throw new IllegalArgumentException("Unknown SpriteCanvas pattern: " + patternNo);
        }
        return patterns[patternNo];
    }

    private static CharacterCommand requireCommand(short encodedCommand) {
        int command = encodedCommand & 0xFFFF;
        int offset = (command & OFFSET_MASK) >>> 13;
        boolean transparent = (command & TRANSPARENT_MASK) != 0;
        int patternNo = command & PATTERN_MASK;
        return new CharacterCommand(
                offset,
                transparent,
                (command & ROTATION_MASK) >>> 10,
                (command & UPSIDE_DOWN_MASK) != 0,
                (command & RIGHTSIDE_LEFT_MASK) != 0,
                patternNo
        );
    }

    private record CharacterCommand(
            int offset,
            boolean transparent,
            int rotation,
            boolean upsideDown,
            boolean rightsideLeft,
            int patternNo
    ) {
    }

}
