package remexa.host.render;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import remexa.host.jblend.CanvasGraphics3D;
import remexa.host.profile.DisplayMetrics;

public final class DisplaySurfaceState {
    private static final int SPRITE_SCRATCH_MARGIN = 8;

    private DisplayMetrics displayMetrics;
    private BufferedImage displayImage;
    private BufferedImage virtualImage;
    private BufferedImage frameBuffer;
    private Graphics2D displayGraphicsDelegate;
    private Graphics2D canvasGraphicsDelegate;
    private Graphics2D frameBufferGraphicsDelegate;
    private javax.microedition.lcdui.Graphics canvasGraphics;
    private int[] virtualCopyPixels;
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
        disposeDisplayGraphics();
        disposeCanvasGraphics();
        displayMetrics = nextDisplayMetrics;
        displayImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
        virtualImage = createVirtualSurface();
    }

    public synchronized javax.microedition.lcdui.Graphics beginCanvasPaint() {
        return beginCachedVirtualPaint();
    }

    public synchronized javax.microedition.lcdui.Graphics beginVirtualPaint() {
        return beginCachedVirtualPaint();
    }

    public synchronized void createFrameBuffer(int width, int height) {
        disposeFrameBufferGraphics();
        frameBuffer = createTransparentSurface(width, height);
        ensureVirtualSurface();
    }

    public synchronized void disposeFrameBuffer() {
        disposeFrameBufferGraphics();
        frameBuffer = null;
    }

    public synchronized void copyArea(int sx, int sy, int width, int height, int tx, int ty) {
        if (frameBuffer == null) {
            return;
        }
        var graphics = frameBufferGraphics();
        resetBlitGraphics(graphics, frameBuffer);
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
    }

    public synchronized void copyFullScreen(int tx, int ty) {
        ensureVirtualSurface();
        copyImageWithinVirtualSurface(tx, ty);
    }

    public synchronized void drawFrameBuffer(int tx, int ty) {
        if (frameBuffer == null) {
            return;
        }
        markFrameRendered();
        var graphics = displayGraphics();
        resetBlitGraphics(graphics, displayImage);
        graphics.drawImage(frameBuffer, tx, ty, null);
        clearTransparent(frameBuffer);
    }

    public synchronized void presentFrameBuffer(int tx, int ty) {
        var source = frameBuffer == null ? virtualImage : frameBuffer;
        if (source == null) {
            return;
        }
        markFrameRendered();
        var graphics = displayGraphics();
        resetBlitGraphics(graphics, displayImage);
        graphics.drawImage(source, tx, ty, null);
        if (frameBuffer != null) {
            clearTransparent(frameBuffer);
        }
    }

    public synchronized void presentCanvas() {
        ensureVirtualSurface();
        clear(displayImage);
        var graphics = displayGraphics();
        resetBlitGraphics(graphics, displayImage);
        graphics.drawImage(virtualImage, 0, 0, null);
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
        var targetPixels = pixels(target);
        int targetWidth = target.getWidth();
        int targetHeight = target.getHeight();
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
                drawPatternPixel(
                        targetPixels,
                        targetWidth,
                        targetHeight,
                        x,
                        y,
                        sampleX,
                        sampleY,
                        normalizedRotation,
                        upsideDown,
                        rightsideLeft,
                        argb
                );
            }
        }
    }

    private static void drawPatternPixel(
            int[] targetPixels,
            int targetWidth,
            int targetHeight,
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
        if (drawX < 0 || drawY < 0 || drawX >= targetWidth || drawY >= targetHeight) {
            return;
        }
        targetPixels[drawY * targetWidth + drawX] = argb;
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
            disposeCanvasGraphics();
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

    private Graphics2D displayGraphics() {
        if (displayGraphicsDelegate == null) {
            displayGraphicsDelegate = displayImage.createGraphics();
        }
        return displayGraphicsDelegate;
    }

    private Graphics2D frameBufferGraphics() {
        if (frameBufferGraphicsDelegate == null) {
            frameBufferGraphicsDelegate = frameBuffer.createGraphics();
        }
        return frameBufferGraphicsDelegate;
    }

    private javax.microedition.lcdui.Graphics beginCachedVirtualPaint() {
        ensureVirtualSurface();
        ensureCanvasGraphics();
        canvasGraphics.resetState();
        return canvasGraphics;
    }

    private void disposeCanvasGraphics() {
        canvasGraphics = null;
        if (canvasGraphicsDelegate != null) {
            canvasGraphicsDelegate.dispose();
            canvasGraphicsDelegate = null;
        }
    }

    private void disposeDisplayGraphics() {
        if (displayGraphicsDelegate != null) {
            displayGraphicsDelegate.dispose();
            displayGraphicsDelegate = null;
        }
    }

    private void disposeFrameBufferGraphics() {
        if (frameBufferGraphicsDelegate != null) {
            frameBufferGraphicsDelegate.dispose();
            frameBufferGraphicsDelegate = null;
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
        System.arraycopy(pixels(source), 0, pixels(copy), 0, source.getWidth() * source.getHeight());
        return copy;
    }

    private static void clear(BufferedImage image) {
        Arrays.fill(pixels(image), 0xFF000000);
    }

    private static void clearTransparent(BufferedImage image) {
        Arrays.fill(pixels(image), 0);
    }

    private void copyImageWithinVirtualSurface(int tx, int ty) {
        int width = virtualImage.getWidth();
        int height = virtualImage.getHeight();
        int size = width * height;
        if (virtualCopyPixels == null || virtualCopyPixels.length != size) {
            virtualCopyPixels = new int[size];
        }
        var target = pixels(virtualImage);
        System.arraycopy(target, 0, virtualCopyPixels, 0, size);

        int left = Math.max(0, tx);
        int top = Math.max(0, ty);
        int right = Math.min(width, tx + width);
        int bottom = Math.min(height, ty + height);
        if (left >= right || top >= bottom) {
            return;
        }
        int copyWidth = right - left;
        int sourceX = left - tx;
        for (int y = top; y < bottom; y++) {
            int sourceY = y - ty;
            System.arraycopy(virtualCopyPixels, sourceY * width + sourceX, target, y * width + left, copyWidth);
        }
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private static void resetBlitGraphics(Graphics2D graphics, BufferedImage target) {
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setClip(0, 0, target.getWidth(), target.getHeight());
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
