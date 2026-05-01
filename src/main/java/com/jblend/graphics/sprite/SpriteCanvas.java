package com.jblend.graphics.sprite;

public abstract class SpriteCanvas extends javax.microedition.lcdui.Canvas {
    private static final int PALETTE_BANK_SIZE = 32;
    private static final int PATTERN_MASK = 0x00FF;
    private static final int TRANSFORM_MASK = 0x0700;
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
        encoded |= encodeTransform(rotation, isUpsideDown, isRightsideLeft);
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
        var transform = decodeTransform(command & TRANSFORM_MASK);
        int patternNo = command & PATTERN_MASK;
        return new CharacterCommand(
                offset,
                transparent,
                transform.rotation(),
                transform.upsideDown(),
                transform.rightsideLeft(),
                patternNo
        );
    }

    private static int encodeTransform(int rotation, boolean upsideDown, boolean rightsideLeft) {
        int[][] samples = {
                {0, 0},
                {7, 0},
                {0, 7}
        };
        int[][] target = new int[samples.length][2];
        for (int i = 0; i < samples.length; i++) {
            int x = rotateX(samples[i][0], samples[i][1], rotation);
            int y = rotateY(samples[i][0], samples[i][1], rotation);
            if (upsideDown) {
                y = 7 - y;
            }
            if (rightsideLeft) {
                x = 7 - x;
            }
            target[i][0] = x;
            target[i][1] = y;
        }
        for (int transformCode = 0; transformCode < 8; transformCode++) {
            if (matchesCanonicalTransform(transformCode, samples, target)) {
                return transformCode << 8;
            }
        }
        throw new IllegalArgumentException("Unsupported SpriteCanvas transform.");
    }

    private static boolean matchesCanonicalTransform(int transformCode, int[][] samples, int[][] target) {
        for (int i = 0; i < samples.length; i++) {
            int[] transformed = applyCanonicalTransform(transformCode, samples[i][0], samples[i][1]);
            if (transformed[0] != target[i][0] || transformed[1] != target[i][1]) {
                return false;
            }
        }
        return true;
    }

    private static CharacterTransform decodeTransform(int encodedTransform) {
        return switch (encodedTransform) {
            case 0x000 -> new CharacterTransform(0, false, false);
            case 0x100 -> new CharacterTransform(0, false, true);
            case 0x200 -> new CharacterTransform(0, true, false);
            case 0x300 -> new CharacterTransform(2, false, false);
            case 0x400 -> new CharacterTransform(1, false, false);
            case 0x500 -> new CharacterTransform(3, false, false);
            case 0x600 -> new CharacterTransform(1, false, true);
            case 0x700 -> new CharacterTransform(1, true, false);
            default -> throw new IllegalArgumentException("Unsupported SpriteCanvas transform: 0x" + Integer.toHexString(encodedTransform));
        };
    }

    private static int[] applyCanonicalTransform(int transformCode, int x, int y) {
        return switch (transformCode) {
            case 0 -> new int[] {x, y};
            case 1 -> new int[] {7 - x, y};
            case 2 -> new int[] {x, 7 - y};
            case 3 -> new int[] {7 - x, 7 - y};
            case 4 -> new int[] {7 - y, x};
            case 5 -> new int[] {y, 7 - x};
            case 6 -> new int[] {y, x};
            case 7 -> new int[] {7 - y, 7 - x};
            default -> throw new IllegalArgumentException("Unsupported SpriteCanvas canonical transform.");
        };
    }

    private static int rotateX(int x, int y, int rotation) {
        return switch (rotation & 0x3) {
            case 1 -> 7 - y;
            case 2 -> 7 - x;
            case 3 -> y;
            default -> x;
        };
    }

    private static int rotateY(int x, int y, int rotation) {
        return switch (rotation & 0x3) {
            case 1 -> x;
            case 2 -> 7 - y;
            case 3 -> 7 - x;
            default -> y;
        };
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

    private record CharacterTransform(
            int rotation,
            boolean upsideDown,
            boolean rightsideLeft
    ) {
    }
}
