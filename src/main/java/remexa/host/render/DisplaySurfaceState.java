package remexa.host.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import remexa.host.jblend.CanvasGraphics3D;
import remexa.host.profile.DisplayMetrics;

public final class DisplaySurfaceState {
    private DisplayMetrics displayMetrics;
    private BufferedImage displayImage;
    private BufferedImage virtualImage;
    private BufferedImage frameBuffer;
    private Graphics2D canvasGraphicsDelegate;
    private javax.microedition.lcdui.Graphics canvasGraphics;

    public DisplaySurfaceState(DisplayMetrics displayMetrics) {
        this.displayMetrics = displayMetrics;
        this.displayImage = createSurface(displayMetrics.width(), displayMetrics.height());
        this.virtualImage = createSurface(displayMetrics.width(), displayMetrics.height());
    }

    public synchronized DisplayMetrics displayMetrics() {
        return displayMetrics;
    }

    public synchronized void updateDisplayMetrics(DisplayMetrics nextDisplayMetrics) {
        disposeCanvasGraphics();
        displayMetrics = nextDisplayMetrics;
        displayImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
        virtualImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
    }

    public synchronized javax.microedition.lcdui.Graphics beginCanvasPaint(boolean spriteCanvas) {
        ensureVirtualSurface();
        if (!spriteCanvas) {
            ensureCanvasGraphics();
            canvasGraphics.resetState();
            return canvasGraphics;
        }
        return new CanvasGraphics3D(virtualImage.createGraphics(), virtualImage.getWidth(), virtualImage.getHeight(), true);
    }

    public synchronized javax.microedition.lcdui.Graphics beginVirtualPaint() {
        ensureVirtualSurface();
        return new CanvasGraphics3D(virtualImage.createGraphics(), virtualImage.getWidth(), virtualImage.getHeight(), true);
    }

    public synchronized void createFrameBuffer(int width, int height) {
        frameBuffer = createSurface(width, height);
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
        var graphics = displayImage.createGraphics();
        try {
            graphics.drawImage(frameBuffer, tx, ty, null);
        } finally {
            graphics.dispose();
        }
    }

    public synchronized void presentFrameBuffer(int tx, int ty) {
        clear(displayImage);
        var source = frameBuffer == null ? virtualImage : frameBuffer;
        if (source == null) {
            return;
        }
        var graphics = displayImage.createGraphics();
        try {
            graphics.drawImage(source, tx, ty, null);
        } finally {
            graphics.dispose();
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
                var paletteIndex = paletteOffset + rawPaletteIndex;
                var argb = resolvePaletteColor(palette, paletteIndex, rawPaletteIndex, transparent);
                if (((argb >>> 24) & 0xFF) == 0) {
                    continue;
                }
                int transformedX = rotateX(sampleX, sampleY, normalizedRotation);
                int transformedY = rotateY(sampleX, sampleY, normalizedRotation);
                if (upsideDown) {
                    transformedY = 7 - transformedY;
                }
                if (rightsideLeft) {
                    transformedX = 7 - transformedX;
                }
                var drawX = x + transformedX;
                var drawY = y + transformedY;
                if (drawX < 0 || drawY < 0 || drawX >= target.getWidth() || drawY >= target.getHeight()) {
                    continue;
                }
                target.setRGB(drawX, drawY, argb);
            }
        }
    }

    public synchronized BufferedImage currentFrameSnapshot() {
        return copyOf(displayImage);
    }

    private void ensureVirtualSurface() {
        if (virtualImage == null
                || virtualImage.getWidth() != displayMetrics.width()
                || virtualImage.getHeight() != displayMetrics.height()) {
            virtualImage = createSurface(displayMetrics.width(), displayMetrics.height());
        }
    }

    private BufferedImage ensureVirtualSurfaceAndGet() {
        ensureVirtualSurface();
        return virtualImage;
    }

    private void ensureCanvasGraphics() {
        if (canvasGraphics != null
                && virtualImage != null
                && virtualImage.getWidth() == displayMetrics.width()
                && virtualImage.getHeight() == displayMetrics.height()) {
            return;
        }
        disposeCanvasGraphics();
        canvasGraphicsDelegate = virtualImage.createGraphics();
        canvasGraphics = new CanvasGraphics3D(
                canvasGraphicsDelegate,
                virtualImage.getWidth(),
                virtualImage.getHeight(),
                false
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
            frameBuffer = createSurface(displayMetrics.width(), displayMetrics.height());
        }
        return frameBuffer;
    }

    private static BufferedImage createSurface(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        clear(image);
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
