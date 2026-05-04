package com.j_phone.amuse;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ACanvas extends javax.microedition.lcdui.Canvas implements com.jblend.ui.SequenceInterface {
    private static final int PALETTE_BANK_SIZE = 32;
    private static final int PATTERN_MASK = 0x00FF;
    private static final int ROTATION_MASK = 0x0C00;
    private static final int UPSIDE_DOWN_MASK = 0x0200;
    private static final int RIGHTSIDE_LEFT_MASK = 0x0100;
    private static final int TRANSPARENT_MASK = 0x1000;
    private static final int OFFSET_MASK = 0xE000;

    private final int[] palette;
    private final byte[][] patterns;
    private final AtomicBoolean hostPaintLoopStarted = new AtomicBoolean();
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
        var metrics = remexa.host.runtime.MidletRuntime.getDisplayMetrics((javax.microedition.lcdui.Displayable) null);
        if (fw < 0 || fh < 0) {
            throw new IllegalArgumentException("ACanvas framebuffer size must not be negative.");
        }
        int resolvedWidth = fw == 0 ? metrics.width() : fw;
        int resolvedHeight = fh == 0 ? metrics.height() : fh;
        palette = new int[numPalettes];
        patterns = new byte[numPatterns][];
        // The ACanvas framebuffer is a separate off-screen surface. The real
        // handset display size remains whatever the active profile/JAD chose,
        // and titles can flush the framebuffer into that screen at arbitrary
        // coordinates. Updating the display metrics here breaks games that
        // probe getWidth()/getHeight() before the canvas is shown.
        remexa.host.runtime.MidletRuntime.createAmuseFrameBuffer(this, resolvedWidth, resolvedHeight);
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
        if (offset < 0 || offset > 7) {
            throw new IllegalArgumentException("ACanvas palette offset must be between 0 and 7.");
        }
        if (rotation < 0 || rotation > 3) {
            throw new IllegalArgumentException("ACanvas rotation must be between 0 and 3.");
        }
        if (patternNo < 0 || patternNo > 255) {
            throw new IllegalArgumentException("ACanvas pattern number must be between 0 and 255.");
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

    @Override
    public void repaint() {
        if (this instanceof Runnable && isHostPaintInProgress()) {
            return;
        }
        if (deferRepaintIfPainting(this::repaint)) {
            return;
        }
        if (hostGraphics == null) {
            attachHostGraphics();
            return;
        }
        beginHostPaint();
        try {
            paint(hostGraphics);
        } finally {
            endHostPaint();
        }
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
    }

    public final void startHostPaintLoop() {
        attachHostGraphics();
        if (hostGraphics == null) {
            return;
        }
        // Some ACanvas titles own their repaint loop via Runnable. They still
        // expect one initial paint after setCurrent() to seed cached Graphics
        // fields, but a persistent auto paint-loop races the app-managed loop.
        if (this instanceof Runnable) {
            runHostPaintFrame();
            return;
        }
        if (!hostPaintLoopStarted.compareAndSet(false, true)) {
            return;
        }
        var paintThread = new Thread(this::runHostPaintFrame, "remexa-acanvas-paint-" + getClass().getName());
        paintThread.setContextClassLoader(getClass().getClassLoader());
        paintThread.setDaemon(true);
        paintThread.start();
    }

    private void runHostPaintFrame() {
        beginHostPaint();
        try {
            hostGraphics.resetState();
            paint(hostGraphics);
        } catch (Throwable throwable) {
            if (!remexa.host.runtime.MidletRuntime.isExpectedShutdownThrowable(throwable)) {
                rethrowUnchecked(throwable);
            }
        } finally {
            endHostPaint();
            hostPaintLoopStarted.set(false);
        }
    }

    private static void rethrowUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(throwable);
    }

    private byte[] requirePattern(int patternNo) {
        if (patternNo < 0 || patternNo >= patterns.length || patterns[patternNo] == null) {
            throw new IllegalArgumentException("Unknown ACanvas pattern: " + patternNo);
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
