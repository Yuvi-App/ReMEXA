package com.j_phone.amuse;

public abstract class ACanvas extends javax.microedition.lcdui.Canvas implements com.jblend.ui.SequenceInterface {
    private static final int PALETTE_BANK_SIZE = 32;
    private static final java.util.Map<java.lang.Short, CharacterCommand> COMMANDS =
            java.util.Collections.synchronizedMap(new java.util.HashMap<>());
    private static short nextCommandId = 1;

    private final int[] palette;
    private final byte[][] patterns;
    private javax.microedition.lcdui.Graphics hostGraphics;

    protected ACanvas() {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "ACanvas");
        var metrics = remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null);
        palette = new int[256];
        patterns = new byte[256][];
        remexa.host.runtime.MidletRuntime.createAmuseFrameBuffer(this, metrics.width(), metrics.height());
    }

    public ACanvas (int numPalettes, int numPatterns, int fw, int fh) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "ACanvas", numPalettes, numPatterns, fw, fh);
        if (numPalettes < 1 || numPalettes > 256 || numPatterns < 1 || numPatterns > 256) {
            throw new IllegalArgumentException("ACanvas palette and pattern counts must be between 1 and 256.");
        }
        if (fw <= 0 || fh <= 0) {
            throw new IllegalArgumentException("ACanvas framebuffer size must be positive.");
        }
        palette = new int[numPalettes];
        patterns = new byte[numPatterns][];
        remexa.host.runtime.MidletRuntime.createAmuseFrameBuffer(this, fw, fh);
    }


    public final javax.microedition.lcdui.Ticker getTicker () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getTicker");
        return super.getTicker();
    }

    public final java.lang.String getTitle () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getTitle");
        return super.getTitle();
    }

    public final void setTicker (javax.microedition.lcdui.Ticker ticker) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setTicker", ticker);
        super.setTicker(ticker);
    }

    public final void setTitle (java.lang.String title) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setTitle", title);
        super.setTitle(title);
    }

    public static int getVirtualWidth () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getVirtualWidth");
        return remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).width();
    }

    public static int getVirtualHeight () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "getVirtualHeight");
        return remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null).height();
    }

    public void setPalette (int index, int palette) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setPalette", index, palette);
        this.palette[index] = palette;
    }

    public void setPattern (int index, byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "setPattern", index, data);
        if (data == null || data.length != 64) {
            throw new IllegalArgumentException("ACanvas patterns must be 8x8 indexed pixels.");
        }
        patterns[index] = data.clone();
    }

    public static short createCharacterCommand (int offset, boolean transparent, int rotation, boolean isUpsideDown, boolean isRightsideLeft, int patternNo) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "createCharacterCommand", offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo);
        synchronized (COMMANDS) {
            short id = nextCommandId++;
            COMMANDS.put(id, new CharacterCommand(offset, transparent, rotation, isUpsideDown, isRightsideLeft, patternNo));
            return id;
        }
    }

    public void drawSpriteChar (short command, short x, short y) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "drawSpriteChar", command, x, y);
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
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "drawBackground", command, x, y);
        var resolved = requireCommand(command);
        remexa.host.runtime.MidletRuntime.amuseDrawPattern(
                this,
                palette,
                requirePattern(resolved.patternNo),
                resolved.offset * PALETTE_BANK_SIZE,
                resolved.transparent,
                false,
                x * 8,
                y * 8,
                resolved.rotation,
                resolved.upsideDown,
                resolved.rightsideLeft
        );
    }

    public void copyArea (int sx, int sy, int fw, int fh, int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "copyArea", sx, sy, fw, fh, tx, ty);
        remexa.host.runtime.MidletRuntime.amuseCopyArea(this, sx, sy, fw, fh, tx, ty);
    }

    public void scroll (int dx, int dy) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "scroll", dx, dy);
        remexa.host.runtime.MidletRuntime.amuseScroll(this, dx, dy);
    }

    public void flush (int tx, int ty) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "flush", tx, ty);
        remexa.host.runtime.MidletRuntime.amuseFlush(this, tx, ty);
    }

    public final void sequenceStart () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "sequenceStart");
    }

    public final void sequenceStop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.ACanvas", "sequenceStop");
    }

    public final void attachHostGraphics() {
        if (hostGraphics != null) {
            return;
        }
        hostGraphics = remexa.host.runtime.MidletRuntime.beginAmuseVirtualGraphics(this);
        if (hostGraphics != null) {
            paint(hostGraphics);
        }
    }

    private byte[] requirePattern(int patternNo) {
        if (patternNo < 0 || patternNo >= patterns.length || patterns[patternNo] == null) {
            throw new IllegalArgumentException("Unknown ACanvas pattern: " + patternNo);
        }
        return patterns[patternNo];
    }

    private static CharacterCommand requireCommand(short id) {
        var command = COMMANDS.get(id);
        if (command == null) {
            throw new IllegalArgumentException("Unknown ACanvas command: " + id);
        }
        return command;
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
