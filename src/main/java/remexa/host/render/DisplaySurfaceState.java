package remexa.host.render;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import remexa.host.profile.DisplayMetrics;

public final class DisplaySurfaceState {
    private DisplayMetrics displayMetrics;
    private BufferedImage displayImage;
    private BufferedImage virtualImage;
    private BufferedImage frameBuffer;

    public DisplaySurfaceState(DisplayMetrics displayMetrics) {
        this.displayMetrics = displayMetrics;
        this.displayImage = createSurface(displayMetrics.width(), displayMetrics.height());
        this.virtualImage = createSurface(displayMetrics.width(), displayMetrics.height());
    }

    public synchronized DisplayMetrics displayMetrics() {
        return displayMetrics;
    }

    public synchronized void updateDisplayMetrics(DisplayMetrics nextDisplayMetrics) {
        displayMetrics = nextDisplayMetrics;
        displayImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
        virtualImage = createSurface(nextDisplayMetrics.width(), nextDisplayMetrics.height());
    }

    public synchronized javax.microedition.lcdui.Graphics beginCanvasPaint(boolean spriteCanvas) {
        if (!spriteCanvas) {
            clear(displayImage);
            return new javax.microedition.lcdui.Graphics(displayImage.createGraphics(), displayImage.getWidth(), displayImage.getHeight());
        }
        ensureVirtualSurface();
        clear(virtualImage);
        return new javax.microedition.lcdui.Graphics(virtualImage.createGraphics(), virtualImage.getWidth(), virtualImage.getHeight());
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

    private static BufferedImage createSurface(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
    }
}
