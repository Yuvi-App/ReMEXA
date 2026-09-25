package com.jblend.graphics.sprite;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import javax.swing.SwingUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import remexa.host.jad.JadDescriptor;
import remexa.host.profile.AppProfile;
import remexa.host.profile.DisplayMetrics;
import remexa.host.profile.LaunchProfile;
import remexa.host.runtime.MidletRuntime;

import static org.junit.Assert.*;

public class SpriteCanvasLifecycleTest {
    private MIDlet midlet;

    @Before
    public void attachRuntime() {
        var descriptor = new JadDescriptor(Path.of("target/sprite-lifecycle/test.jad"), Map.of(), List.of());
        var profile = new LaunchProfile(AppProfile.generic(), new DisplayMetrics(240, 260, "test"), false);
        MidletRuntime.beginInstantiation(descriptor, profile, getClass().getClassLoader(), null);
        midlet = new MIDlet() {
            protected void startApp() { }
            protected void pauseApp() { }
            protected void destroyApp(boolean unconditional) { }
        };
    }

    @After
    public void detachRuntime() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        MidletRuntime.detach(midlet);
        MidletRuntime.endInstantiation();
    }

    @Test
    public void initialSpritePaintCompletesBeforeSetCurrentReturns() throws Exception {
        var canvas = new CachedGraphicsCanvas();
        canvas.createFrameBuffer(240, 240);
        var release = holdEventThread();
        try {
            Display.getDisplay(midlet).setCurrent(canvas);
            assertTrue(canvas.isShown());
            assertNotNull("The app can draw immediately after showing its sprite canvas", canvas.cached);
            assertTrue(canvas.shownAtPaint);
            canvas.cached.setColor(0xff0000);
            canvas.cached.fillRect(0, 0, 4, 4);
            canvas.copyArea(0, 0, 4, 4, 0, 0);
            canvas.drawFrameBuffer(0, 0);
            assertEquals(0xffff0000, MidletRuntime.currentFrameSnapshot().getRGB(0, 0));
        } finally {
            release.countDown();
        }
    }

    @Test
    public void frameBufferAllocationDoesNotResizeTheDisplayOrInvalidateCachedGraphics() throws Exception {
        var canvas = new CachedGraphicsCanvas();
        SwingUtilities.invokeAndWait(() -> Display.getDisplay(midlet).setCurrent(canvas));
        canvas.serviceRepaints();
        assertNotNull(canvas.cached);
        var original = canvas.cached;
        original.setColor(0xff0000);
        original.fillRect(0, 0, 4, 4);
        for (int size : new int[] {240, 120, 240}) {
            canvas.createFrameBuffer(size, size);
            assertEquals(240, canvas.getWidth());
            assertEquals(260, canvas.getHeight());
            canvas.copyArea(0, 0, 4, 4, 0, 0);
            canvas.drawFrameBuffer(0, 0);
            assertEquals("Virtual pixels survive framebuffer replacement", 0xffff0000,
                    MidletRuntime.currentFrameSnapshot().getRGB(0, 0));
            original.setColor(0x00ff00);
            original.fillRect(4, 0, 4, 4);
            canvas.copyArea(4, 0, 4, 4, 4, 0);
            canvas.drawFrameBuffer(0, 0);
            assertEquals("The retained Graphics still targets the virtual screen", 0xff00ff00,
                    MidletRuntime.currentFrameSnapshot().getRGB(4, 0));
            canvas.disposeFrameBuffer();
        }
    }

    @Test
    public void ordinaryCanvasInitialPaintRemainsAsynchronous() throws Exception {
        var paints = new AtomicInteger();
        var canvas = new Canvas() {
            protected void paint(Graphics graphics) { paints.incrementAndGet(); }
        };
        var release = holdEventThread();
        try {
            Display.getDisplay(midlet).setCurrent(canvas);
            assertEquals(0, paints.get());
        } finally {
            release.countDown();
        }
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(1, paints.get());
    }

    @Test
    public void serviceRepaintsWaitsForAnAlreadyRunningPaint() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var completed = new CountDownLatch(1);
        var paints = new AtomicInteger();
        var canvas = new Canvas() {
            protected void paint(Graphics graphics) {
                entered.countDown();
                await(release);
                paints.incrementAndGet();
            }
        };
        Display.getDisplay(midlet).setCurrent(canvas);
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var waiter = new Thread(() -> {
            canvas.serviceRepaints();
            completed.countDown();
        });
        waiter.start();
        try {
            assertFalse("serviceRepaints must wait for paint to finish", completed.await(200, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            waiter.join(5000);
        }
        assertFalse(waiter.isAlive());
        assertEquals(1, paints.get());
        assertEquals(0, completed.getCount());
    }

    @Test
    public void repaintRequestedDuringPaintIsServicedWithoutReentrantPainting() throws Exception {
        var paints = new AtomicInteger();
        var canvas = new Canvas() {
            protected void paint(Graphics graphics) {
                if (paints.incrementAndGet() == 1) {
                    repaint();
                    serviceRepaints();
                    assertEquals("Painting must not recursively enter the app callback", 1, paints.get());
                }
            }
        };
        // Keep the queued Swing drain behind this operation so the assertion
        // verifies serviceRepaints itself consumes the repaint from paint().
        SwingUtilities.invokeAndWait(() -> {
            Display.getDisplay(midlet).setCurrent(canvas);
            canvas.serviceRepaints();
            canvas.serviceRepaints();
            assertEquals(2, paints.get());
        });
    }

    @Test
    public void failedPaintReleasesTheCanvasForLaterRepaints() throws Exception {
        var paints = new AtomicInteger();
        var failure = new IllegalStateException("test paint failure");
        var canvas = new Canvas() {
            protected void paint(Graphics graphics) {
                if (paints.incrementAndGet() == 1) throw failure;
            }
        };
        SwingUtilities.invokeAndWait(() -> {
            Display.getDisplay(midlet).setCurrent(canvas);
            assertSame(failure, assertThrows(IllegalStateException.class, canvas::serviceRepaints));
            canvas.repaint();
            canvas.serviceRepaints();
            assertEquals(2, paints.get());
        });
    }

    private static CountDownLatch holdEventThread() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            entered.countDown();
            await(release);
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        return release;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test thread");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class CachedGraphicsCanvas extends SpriteCanvas {
        private Graphics cached;
        private boolean shownAtPaint;

        protected void paint(Graphics graphics) {
            if (cached == null) cached = graphics;
            shownAtPaint = isShown();
        }
    }
}
