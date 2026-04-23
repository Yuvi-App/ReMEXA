package remexa.host.runtime;

import com.j_phone.amuse.ACanvas;
import com.jblend.graphics.sprite.SpriteCanvas;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import remexa.host.jad.JadDescriptor;
import remexa.host.profile.DisplayMetrics;
import remexa.host.profile.LaunchProfile;
import remexa.host.render.DisplaySurfaceState;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class MidletRuntime {
    private static final DisplayMetrics DEFAULT_DISPLAY = new DisplayMetrics(240, 320, "MIDP default");
    private static final ThreadLocal<LaunchContext> CURRENT_CONTEXT = new ThreadLocal<>();
    private static final Map<MIDlet, LaunchContext> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<MIDlet, Display> DISPLAYS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Displayable, LaunchContext> DISPLAYABLES = Collections.synchronizedMap(new WeakHashMap<>());

    private MidletRuntime() {
    }

    public static void beginInstantiation(
            JadDescriptor descriptor,
            LaunchProfile launchProfile,
            ClassLoader classLoader,
            Consumer<DisplayMetrics> displayListener
    ) {
        CURRENT_CONTEXT.set(new LaunchContext(descriptor, launchProfile, classLoader, displayListener));
    }

    public static void endInstantiation() {
        CURRENT_CONTEXT.remove();
    }

    public static void attach(MIDlet midlet) {
        var context = CURRENT_CONTEXT.get();
        if (context != null) {
            CONTEXTS.put(midlet, context);
            DebugLog.log(LogCategory.MIDLET, MidletRuntime.class.getName(), "Attached MIDlet to " + context.descriptor().title());
        }
    }

    public static void detach(MIDlet midlet) {
        if (midlet == null) {
            return;
        }
        var context = CONTEXTS.remove(midlet);
        var display = DISPLAYS.remove(midlet);
        if (display != null) {
            var current = display.getCurrent();
            if (current != null) {
                DISPLAYABLES.remove(current);
            }
        }
        if (context != null) {
            context.shutdown();
            DebugLog.log(LogCategory.MIDLET, MidletRuntime.class.getName(), "Detached MIDlet from " + context.descriptor().title());
        }
    }

    public static String getAppProperty(MIDlet midlet, String key) {
        var context = CONTEXTS.get(midlet);
        if (context == null) {
            return null;
        }
        return context.descriptor().properties().get(key);
    }

    public static Display getDisplay(MIDlet midlet) {
        return DISPLAYS.computeIfAbsent(midlet, ignored -> new Display(midlet));
    }

    public static void bindDisplayable(MIDlet midlet, Displayable displayable) {
        var context = CONTEXTS.get(midlet);
        if (context != null && displayable != null) {
            DISPLAYABLES.put(displayable, context);
            context.surfaceFor(displayable);
        }
    }

    public static void setCurrentDisplayable(MIDlet midlet, Displayable displayable) {
        var context = CONTEXTS.get(midlet);
        if (context != null) {
            context.setCurrentDisplayable(displayable);
        }
    }

    public static DisplayMetrics getDisplayMetrics(Displayable displayable) {
        return contextFor(displayable)
                .map(LaunchContext::displayMetrics)
                .orElse(DEFAULT_DISPLAY);
    }

    public static DisplayMetrics getDisplayMetrics(MIDlet midlet) {
        var context = CONTEXTS.get(midlet);
        if (context == null) {
            context = CURRENT_CONTEXT.get();
        }
        return context == null ? DEFAULT_DISPLAY : context.displayMetrics();
    }

    public static void updateDisplayMetrics(Displayable displayable, DisplayMetrics displayMetrics) {
        var context = contextFor(displayable).orElse(CURRENT_CONTEXT.get());
        if (context == null) {
            return;
        }
        context.updateDisplayMetrics(displayMetrics);
        if (displayable != null) {
            context.surfaceFor(displayable).updateDisplayMetrics(displayMetrics);
        }
        DebugLog.log(
                LogCategory.UI,
                MidletRuntime.class.getName(),
                "Active display updated to " + displayMetrics.dimensions() + " via " + displayMetrics.source()
        );
    }

    public static int getDeviceStyle(Displayable displayable) {
        return contextFor(displayable)
                .map(context -> context.launchProfile().profile().deviceStyle())
                .orElse(com.j_phone.system.DeviceControl.STYLE_PORTRAIT);
    }

    public static int getDeviceStyle(MIDlet midlet) {
        var context = CONTEXTS.get(midlet);
        if (context == null) {
            context = CURRENT_CONTEXT.get();
        }
        return context == null
                ? com.j_phone.system.DeviceControl.STYLE_PORTRAIT
                : context.launchProfile().profile().deviceStyle();
    }

    public static void renderCanvas(Canvas canvas, Consumer<javax.microedition.lcdui.Graphics> renderer) {
        ensureThreadActive();
        var context = contextFor(canvas).orElse(CURRENT_CONTEXT.get());
        if (context == null) {
            return;
        }
        var surface = context.surfaceFor(canvas);
        var graphics = surface.beginCanvasPaint(canvas instanceof SpriteCanvas || canvas instanceof ACanvas);
        renderer.accept(graphics);
    }

    public static void createSpriteFrameBuffer(Displayable displayable, int width, int height) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).createFrameBuffer(width, height));
    }

    public static void disposeSpriteFrameBuffer(Displayable displayable) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).disposeFrameBuffer());
    }

    public static void spriteCopyArea(Displayable displayable, int sx, int sy, int width, int height, int tx, int ty) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).copyArea(sx, sy, width, height, tx, ty));
    }

    public static void spriteCopyFullScreen(Displayable displayable, int tx, int ty) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).copyFullScreen(tx, ty));
    }

    public static void spriteDrawFrameBuffer(Displayable displayable, int tx, int ty) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).drawFrameBuffer(tx, ty));
    }

    public static javax.microedition.lcdui.Graphics beginAmuseVirtualGraphics(Displayable displayable) {
        var context = contextFor(displayable).orElse(CURRENT_CONTEXT.get());
        if (context == null) {
            return null;
        }
        return context.surfaceFor(displayable).beginVirtualPaint();
    }

    public static void createAmuseFrameBuffer(Displayable displayable, int width, int height) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).createFrameBuffer(width, height));
    }

    public static void amuseCopyArea(Displayable displayable, int sx, int sy, int width, int height, int tx, int ty) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).copyArea(sx, sy, width, height, tx, ty));
    }

    public static void amuseScroll(Displayable displayable, int dx, int dy) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).copyFullScreen(dx, dy));
    }

    public static void amuseFlush(Displayable displayable, int tx, int ty) {
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).presentFrameBuffer(tx, ty));
    }

    public static void amuseDrawPattern(
            Displayable displayable,
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
        contextFor(displayable).ifPresent(context -> context.surfaceFor(displayable).drawIndexedPattern(
                palette,
                pattern,
                paletteOffset,
                transparent,
                toFrameBuffer,
                x,
                y,
                rotation,
                upsideDown,
                rightsideLeft
        ));
    }

    public static InputStream openResource(String resourceName) {
        ensureThreadActive();
        var normalizedName = resourceName == null ? "" : resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        var threadContext = Thread.currentThread().getContextClassLoader();
        if (threadContext != null) {
            var stream = threadContext.getResourceAsStream(normalizedName);
            if (stream != null) {
                return stream;
            }
        }

        var context = CURRENT_CONTEXT.get();
        if (context == null) {
            context = activeContext();
        }
        if (context == null) {
            return null;
        }
        return context.classLoader().getResourceAsStream(normalizedName);
    }

    public static BufferedImage currentFrameSnapshot() {
        var context = activeContext();
        if (context == null) {
            return null;
        }
        return context.currentFrameSnapshot();
    }

    public static Path appStorageRoot() {
        var context = activeContext();
        if (context == null) {
            throw new IllegalStateException("No active MIDlet context is available.");
        }
        return context.appStorageRoot();
    }

    public static Displayable currentDisplayable() {
        var context = activeContext();
        return context == null ? null : context.currentDisplayable();
    }

    public static void dispatchKeyPressed(int keyCode) {
        var displayable = currentDisplayable();
        if (displayable instanceof Canvas canvas) {
            canvas.fireKeyPressed(keyCode);
        }
    }

    public static void dispatchKeyReleased(int keyCode) {
        var displayable = currentDisplayable();
        if (displayable instanceof Canvas canvas) {
            canvas.fireKeyReleased(keyCode);
        }
    }

    public static void dispatchKeyRepeated(int keyCode) {
        var displayable = currentDisplayable();
        if (displayable instanceof Canvas canvas) {
            canvas.fireKeyRepeated(keyCode);
        }
    }

    public static void dispatchSoftKey(int index) {
        var displayable = currentDisplayable();
        if (displayable != null) {
            displayable.fireCommand(index);
        }
    }

    public static int currentDeviceKeyState() {
        ensureThreadActive();
        var displayable = currentDisplayable();
        if (displayable instanceof Canvas canvas) {
            return canvas.deviceKeyStateMask();
        }
        return 0;
    }

    public static void ensureThreadActive() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AppShutdownError();
        }
    }

    private static Optional<LaunchContext> contextFor(Displayable displayable) {
        var context = displayable == null ? null : DISPLAYABLES.get(displayable);
        if (context == null) {
            context = CURRENT_CONTEXT.get();
        }
        return Optional.ofNullable(context);
    }

    private static LaunchContext activeContext() {
        var current = CURRENT_CONTEXT.get();
        if (current != null) {
            return current;
        }
        synchronized (CONTEXTS) {
            for (var context : CONTEXTS.values()) {
                if (context != null) {
                    return context;
                }
            }
        }
        return null;
    }

    private static final class LaunchContext {
        private final JadDescriptor descriptor;
        private final LaunchProfile launchProfile;
        private final ClassLoader classLoader;
        private final Consumer<DisplayMetrics> displayListener;
        private final Path appStorageRoot;
        private final Map<Displayable, DisplaySurfaceState> surfaces = Collections.synchronizedMap(new WeakHashMap<>());
        private volatile DisplayMetrics displayMetrics;
        private volatile Displayable currentDisplayable;

        private LaunchContext(
                JadDescriptor descriptor,
                LaunchProfile launchProfile,
                ClassLoader classLoader,
                Consumer<DisplayMetrics> displayListener
        ) {
            this.descriptor = descriptor;
            this.launchProfile = launchProfile;
            this.classLoader = classLoader;
            this.displayListener = displayListener;
            this.appStorageRoot = descriptor.sourcePath().getParent()
                    .resolve(".remexa");
            this.displayMetrics = launchProfile.initialDisplay();
            if (displayListener != null) {
                displayListener.accept(this.displayMetrics);
            }
        }

        private JadDescriptor descriptor() {
            return descriptor;
        }

        private LaunchProfile launchProfile() {
            return launchProfile;
        }

        private ClassLoader classLoader() {
            return classLoader;
        }

        private Path appStorageRoot() {
            return appStorageRoot;
        }

        private DisplayMetrics displayMetrics() {
            return displayMetrics;
        }

        private void updateDisplayMetrics(DisplayMetrics nextDisplayMetrics) {
            displayMetrics = nextDisplayMetrics;
            if (displayListener != null) {
                displayListener.accept(nextDisplayMetrics);
            }
        }

        private DisplaySurfaceState surfaceFor(Displayable displayable) {
            return surfaces.computeIfAbsent(displayable, ignored -> new DisplaySurfaceState(displayMetrics));
        }

        private void setCurrentDisplayable(Displayable displayable) {
            currentDisplayable = displayable;
        }

        private BufferedImage currentFrameSnapshot() {
            var displayable = currentDisplayable;
            if (displayable == null) {
                return null;
            }
            var surface = surfaces.get(displayable);
            return surface == null ? null : surface.currentFrameSnapshot();
        }

        private Displayable currentDisplayable() {
            return currentDisplayable;
        }

        private void shutdown() {
            synchronized (surfaces) {
                surfaces.clear();
            }
            if (currentDisplayable != null) {
                DISPLAYABLES.remove(currentDisplayable);
                currentDisplayable = null;
            }
        }

    }

    private static final class AppShutdownError extends Error {
        private AppShutdownError() {
            super("MIDlet runtime is shutting down.");
        }
    }
}
