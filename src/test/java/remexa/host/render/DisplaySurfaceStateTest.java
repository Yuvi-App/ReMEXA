package remexa.host.render;

import javax.microedition.lcdui.Font;
import org.junit.Test;
import remexa.host.jblend.CanvasGraphics3D;
import remexa.host.profile.DisplayMetrics;

import static org.junit.Assert.*;

public class DisplaySurfaceStateTest {
    @Test
    public void framebufferGrowthPreservesGraphicsStateAndVirtualPixels() {
        var surface = new DisplaySurfaceState(new DisplayMetrics(240, 260, "test"));
        var graphics = surface.beginVirtualPaint();
        graphics.setColor(0xff0000);
        graphics.fillRect(0, 0, 2, 2);
        var font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL);
        graphics.setFont(font);
        graphics.setColor(0x00ff00);
        graphics.translate(3, 5);
        graphics.setClip(4, 6, 8, 10);
        // ReMEXA grows its internal virtual surface to include sprite scratch
        // space. Apps retaining Graphics must keep drawing to that surface.
        surface.createFrameBuffer(240, 240);
        assertSame(font, graphics.getFont());
        assertEquals(0x00ff00, graphics.getColor());
        assertEquals(3, graphics.getTranslateX());
        assertEquals(5, graphics.getTranslateY());
        assertEquals(4, graphics.getClipX());
        assertEquals(6, graphics.getClipY());
        assertEquals(8, graphics.getClipWidth());
        assertEquals(10, graphics.getClipHeight());
        graphics.fillRect(0, 0, 30, 30);
        surface.copyArea(0, 0, 30, 30, 0, 0);
        surface.drawFrameBuffer(0, 0);
        var frame = surface.currentFrameSnapshot();
        assertEquals(0xffff0000, frame.getRGB(0, 0));
        assertEquals(0xff00ff00, frame.getRGB(7, 11));
        assertEquals(0xff000000, frame.getRGB(6, 11));
        assertEquals(0xff000000, frame.getRGB(15, 11));
        assertSame(graphics, surface.beginVirtualPaint());
    }

    @Test
    public void displayResizeRebindsRetainedGraphicsAndIts3dBackingImage() {
        var surface = new DisplaySurfaceState(new DisplayMetrics(16, 16, "test"));
        var graphics = (CanvasGraphics3D) surface.beginVirtualPaint();
        var oldImage = graphics.backingImage();
        surface.updateDisplayMetrics(new DisplayMetrics(32, 40, "resized"));
        assertNotSame(oldImage, graphics.backingImage());
        assertEquals(32, graphics.backingImage().getWidth());
        assertEquals(40, graphics.backingImage().getHeight());
        assertEquals(32, graphics.getClipWidth());
        assertEquals(40, graphics.getClipHeight());
        graphics.setColor(0x0000ff);
        graphics.fillRect(31, 39, 1, 1);
        surface.presentCanvas();
        assertEquals(0xff0000ff, surface.currentFrameSnapshot().getRGB(31, 39));
        assertSame(graphics, surface.beginCanvasPaint());
    }
}
