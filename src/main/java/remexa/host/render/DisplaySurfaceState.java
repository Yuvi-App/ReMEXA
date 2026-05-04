package remexa.host.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import remexa.host.jblend.CanvasGraphics3D;
import remexa.host.profile.DisplayMetrics;

public final class DisplaySurfaceState {
    private static final int SPRITE_SCRATCH_MARGIN = 8;

    private DisplayMetrics displayMetrics;
    private BufferedImage displayImage;
    private BufferedImage virtualImage;
    private BufferedImage frameBuffer;
    private Graphics2D canvasGraphicsDelegate;
    private javax.microedition.lcdui.Graphics canvasGraphics;
    private long renderedFrameCount;

    public DisplaySurfaceState(DisplayMetrics displayMetrics) {
        this.displayMetrics = displayMetrics;
        this.displayImage = createSurface(displayMetrics.width(), displayMetrics.height());
        this.virtualImage = createVirtualSurface();
    }

    public synchronized DisplayMetrics displayMetrics() {
        return displayMetrics;
    }

    public synchronized void updateDisplayMetrics(DisplayMetrics nextDisplayMetrics) {
        disposeCanvasGraphics();
        displayMetrics = nextDisplayMetrics;
        displayImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
        virtualImage = createVirtualSurface();
    }

    public synchronized javax.microedition.lcdui.Graphics beginCanvasPaint(boolean spriteCanvas) {
        ensureVirtualSurface();
        if (!spriteCanvas) {
            ensureCanvasGraphics();
            canvasGraphics.resetState();
            return canvasGraphics;
        }
        return new CanvasGraphics3D(
                virtualImage.createGraphics(),
                virtualImage.getWidth(),
                virtualImage.getHeight(),
                true,
                virtualImage,
                true
        );
    }

    public synchronized javax.microedition.lcdui.Graphics beginVirtualPaint() {
        ensureVirtualSurface();
        return new CanvasGraphics3D(
                virtualImage.createGraphics(),
                virtualImage.getWidth(),
                virtualImage.getHeight(),
                true,
                virtualImage,
                true
        );
    }

    public synchronized void createFrameBuffer(int width, int height) {
        frameBuffer = createTransparentSurface(width, height);
        ensureVirtualSurface();
    }

    public synchronized void disposeFrameBuffer() {
        frameBuffer = null;
    }

    public synchronized void copyArea(int sx, int sy, int width, int height, int tx, int ty) {
        if (frameBuffer == null) {
            return;
        }
        var graphics = frameBuffer.createGraphics();
        try {
            graphics.drawImage(
                    virtualImage,
                    tx,
                    ty,
                    tx + width,
                    ty + height,
                    sx,
                    sy,
                    sx + width,
                    sy + height,
                    null
            );
        } finally {
            graphics.dispose();
        }
    }

    public synchronized void copyFullScreen(int tx, int ty) {
        ensureVirtualSurface();
        var copy = copyOf(virtualImage);
        var graphics = virtualImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(copy, tx, ty, null);
        } finally {
            graphics.dispose();
        }
    }

    public synchronized void drawFrameBuffer(int tx, int ty) {
        if (frameBuffer == null) {
            return;
        }
        markFrameRendered();
        var graphics = displayImage.createGraphics();
        try {
            graphics.drawImage(frameBuffer, tx, ty, null);
        } finally {
            graphics.dispose();
        }
        clearTransparent(frameBuffer);
    }

    public synchronized void presentFrameBuffer(int tx, int ty) {
        var source = frameBuffer == null ? virtualImage : frameBuffer;
        if (source == null) {
            return;
        }
        markFrameRendered();
        var graphics = displayImage.createGraphics();
        try {
            graphics.drawImage(source, tx, ty, null);
        } finally {
            graphics.dispose();
        }
        if (frameBuffer != null) {
            clearTransparent(frameBuffer);
        }
    }

    public synchronized void presentCanvas() {
        ensureVirtualSurface();
        clear(displayImage);
        var graphics = displayImage.createGraphics();
        try {
            graphics.drawImage(virtualImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
    }

    public synchronized void drawIndexedPattern(
            int[] palette,
            byte[] pattern,
            int paletteOffset,
            boolean transparent,
            boolean toFrameBuffer,
            int x,
            int y,
            int rotation,
            boolean upsideDown,
            boolean rightsideLeft
    ) {
        if (pattern == null || pattern.length != 64) {
            return;
        }
        var target = toFrameBuffer ? ensureFrameBuffer() : ensureVirtualSurfaceAndGet();
        var normalizedRotation = Math.floorMod(rotation, 4);
        for (int sampleY = 0; sampleY < 8; sampleY++) {
            for (int sampleX = 0; sampleX < 8; sampleX++) {
                var rawPaletteIndex = pattern[sampleY * 8 + sampleX] & 0xFF;
                if (transparent && rawPaletteIndex == 0) {
                    continue;
                }
                var paletteIndex = (paletteOffset + rawPaletteIndex) & 0xFF;
                var argb = resolvePaletteColor(palette, paletteIndex, rawPaletteIndex, transparent);
                if (((argb >>> 24) & 0xFF) == 0) {
                    continue;
                }
                drawPatternPixel(target, x, y, sampleX, sampleY, normalizedRotation, upsideDown, rightsideLeft, argb);
            }
        }
    }

    private static void drawPatternPixel(
            BufferedImage target,
            int x,
            int y,
            int sampleX,
            int sampleY,
            int rotation,
            boolean upsideDown,
            boolean rightsideLeft,
            int argb
    ) {
        int transformedX = rotateX(sampleX, sampleY, rotation);
        int transformedY = rotateY(sampleX, sampleY, rotation);
        if (upsideDown) {
            transformedY = 7 - transformedY;
        }
        if (rightsideLeft) {
            transformedX = 7 - transformedX;
        }
        var drawX = x + transformedX;
        var drawY = y + transformedY;
        if (drawX < 0 || drawY < 0 || drawX >= target.getWidth() || drawY >= target.getHeight()) {
            return;
        }
        target.setRGB(drawX, drawY, argb);
    }

    public synchronized BufferedImage currentFrameSnapshot() {
        return copyOf(displayImage);
    }

    public synchronized void markFrameRendered() {
        renderedFrameCount++;
    }

    public synchronized long renderedFrameCount() {
        return renderedFrameCount;
    }

    private void ensureVirtualSurface() {
        if (virtualImage == null
                || virtualImage.getWidth() != virtualSurfaceWidth()
                || virtualImage.getHeight() != virtualSurfaceHeight()) {
            virtualImage = createVirtualSurface();
        }
    }

    private BufferedImage ensureVirtualSurfaceAndGet() {
        ensureVirtualSurface();
        return virtualImage;
    }

    private void ensureCanvasGraphics() {
        if (canvasGraphics != null
                && virtualImage != null
                && virtualImage.getWidth() == virtualSurfaceWidth()
                && virtualImage.getHeight() == virtualSurfaceHeight()) {
            return;
        }
        disposeCanvasGraphics();
        canvasGraphicsDelegate = virtualImage.createGraphics();
        canvasGraphics = new CanvasGraphics3D(
                canvasGraphicsDelegate,
                virtualImage.getWidth(),
                virtualImage.getHeight(),
                false,
                virtualImage,
                true
        );
    }

    private void disposeCanvasGraphics() {
        canvasGraphics = null;
        if (canvasGraphicsDelegate != null) {
            canvasGraphicsDelegate.dispose();
            canvasGraphicsDelegate = null;
        }
    }

    private BufferedImage ensureFrameBuffer() {
        if (frameBuffer == null) {
            frameBuffer = createTransparentSurface(displayMetrics.width(), displayMetrics.height());
        }
        return frameBuffer;
    }

    private BufferedImage createVirtualSurface() {
        return createSurface(virtualSurfaceWidth(), virtualSurfaceHeight());
    }

    private int virtualSurfaceWidth() {
        if (frameBuffer == null) {
            return displayMetrics.width();
        }
        return Math.max(displayMetrics.width(), frameBuffer.getWidth() + SPRITE_SCRATCH_MARGIN * 2);
    }

    private int virtualSurfaceHeight() {
        if (frameBuffer == null) {
            return displayMetrics.height();
        }
        return Math.max(displayMetrics.height(), frameBuffer.getHeight() + SPRITE_SCRATCH_MARGIN * 2);
    }

    private static BufferedImage createSurface(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        clear(image);
        return image;
    }

    private static BufferedImage createTransparentSurface(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        clearTransparent(image);
        return image;
    }

    private static BufferedImage copyOf(BufferedImage source) {
        var copy = createSurface(source.getWidth(), source.getHeight());
        var graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static void clear(BufferedImage image) {
        var graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
    }

    private static void clearTransparent(BufferedImage image) {
        var graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
    }

    private static int resolvePaletteColor(int[] palette, int paletteIndex, int rawPaletteIndex, boolean transparent) {
        int color;
        if (palette != null && paletteIndex >= 0 && paletteIndex < palette.length) {
            color = palette[paletteIndex];
        } else {
            var shade = paletteIndex & 0xFF;
            color = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
        }
        if ((color >>> 24) == 0) {
            if (transparent && rawPaletteIndex == 0) {
                return 0;
            }
            color |= 0xFF000000;
        }
        return color;
    }

    private static int rotateX(int x, int y, int rotation) {
        return switch (rotation) {
            case 1 -> 7 - y;
            case 2 -> 7 - x;
            case 3 -> y;
            default -> x;
        };
    }

    private static int rotateY(int x, int y, int rotation) {
        return switch (rotation) {
            case 1 -> x;
            case 2 -> 7 - y;
            case 3 -> 7 - x;
            default -> y;
        };
    }
}
