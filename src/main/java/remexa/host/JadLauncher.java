package remexa.host;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import remexa.host.HostUiSettings;
import remexa.host.jad.JadDescriptor;
import remexa.host.jad.JadIconLoader;
import remexa.host.jad.JadManifestOverlay;
import remexa.host.jad.JadParser;
import remexa.host.jad.RecentJadsRepository;
import remexa.host.profile.LaunchProfileResolver;
import remexa.host.runtime.AppRuntime;
import remexa.host.runtime.LaunchException;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class JadLauncher {
    private final RecentJadsRepository recentJads = new RecentJadsRepository();
    private final AppRuntime runtime = new AppRuntime();
    private final boolean consoleLaunch;
    private final Boolean showHostDetailsOverride;
    private final Path captureFramePath;
    private final int captureDelayMs;
    private final boolean exitAfterCapture;

    public JadLauncher() {
        this(false, null, null, 0, false);
    }

    public JadLauncher(boolean consoleLaunch) {
        this(consoleLaunch, null, null, 0, false);
    }

    public JadLauncher(boolean consoleLaunch, Boolean showHostDetailsOverride) {
        this(consoleLaunch, showHostDetailsOverride, null, 0, false);
    }

    public JadLauncher(
            boolean consoleLaunch,
            Boolean showHostDetailsOverride,
            Path captureFramePath,
            int captureDelayMs,
            boolean exitAfterCapture
    ) {
        this.consoleLaunch = consoleLaunch;
        this.showHostDetailsOverride = showHostDetailsOverride;
        this.captureFramePath = captureFramePath;
        this.captureDelayMs = captureDelayMs;
        this.exitAfterCapture = exitAfterCapture;
    }

    public void launch(Path jadPath) {
        try {
            var descriptor = descriptorForLaunch(JadParser.parse(jadPath));
            recentJads.remember(descriptor);
            openFrame(descriptor);
        } catch (Exception exception) {
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Launch failed: " + exception.getMessage());
            if (consoleLaunch) {
                System.err.println("ReMEXA launch failed: " + exception.getMessage());
                exception.printStackTrace(System.err);
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        exception.getMessage(),
                        "ReMEXA Launch Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public RecentJadsRepository recentJads() {
        return recentJads;
    }

    private JadDescriptor descriptorForLaunch(JadDescriptor descriptor) throws LaunchException {
        var jarPath = descriptor.resolveJarPath();
        if (jarPath.isEmpty() || !Files.exists(jarPath.get())) {
            return descriptor;
        }
        try {
            return JadManifestOverlay.merge(descriptor, jarPath.get());
        } catch (Exception exception) {
            throw new LaunchException("Failed to read JAR manifest.", exception);
        }
    }

    private void openFrame(JadDescriptor descriptor) throws LaunchException {
        var launchProfile = LaunchProfileResolver.resolve(descriptor);
        var frame = new JadFrame(descriptor, launchProfile, showHostDetails());
        JadIconLoader.load(descriptor).ifPresent(frame::setAppIcon);
        var shutdownOnce = new AtomicBoolean();
        Thread.UncaughtExceptionHandler previousDefaultHandler = null;
        try {
            frame.showFrame();
            frame.updateStatus("Loading " + descriptor.title());
            var result = runtime.launch(descriptor, launchProfile, frame::updateDisplayMetrics, frame::requestTextInput);
            MidletRuntime.registerHostFrame(result.classLoader(), frame);
            previousDefaultHandler = installExitOnUncaughtException(frame, descriptor, result.classLoader());
            var restoredDefaultHandler = previousDefaultHandler;
            frame.setCloseHandler(() -> {
                restoreDefaultExceptionHandler(restoredDefaultHandler);
                MidletRuntime.unregisterHostFrame(result.classLoader());
                shutdownLaunch(result, shutdownOnce);
            });
            frame.updateStatus("Loaded " + result.entryClass());
            scheduleCaptureIfRequested(frame);
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Loaded entry class: " + result.entryClass());
        } catch (LaunchException exception) {
            restoreDefaultExceptionHandler(previousDefaultHandler);
            if (consoleLaunch) {
                frame.dispose();
            } else {
                frame.updateStatus("Launch failed");
            }
            throw exception;
        }
    }

    private void shutdownLaunch(remexa.host.runtime.LaunchResult result, AtomicBoolean shutdownOnce) {
        if (!shutdownOnce.compareAndSet(false, true)) {
            return;
        }

        DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Shutting down " + result.descriptor().title());
        runtime.shutdown(result);
        if (consoleLaunch) {
            System.exit(0);
        }
    }

    private boolean showHostDetails() {
        if (showHostDetailsOverride != null) {
            return showHostDetailsOverride;
        }
        return HostUiSettings.showHostDetails();
    }

    private void scheduleCaptureIfRequested(JadFrame frame) {
        if (captureFramePath == null) {
            return;
        }
        var timer = new Timer(Math.max(0, captureDelayMs), event -> {
            ((Timer) event.getSource()).stop();
            captureFrame(frame);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void captureFrame(JadFrame frame) {
        try {
            var snapshot = MidletRuntime.currentFrameSnapshot();
            if (snapshot == null) {
                throw new IllegalStateException("No rendered frame is available yet.");
            }
            var parent = captureFramePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(snapshot, "png", captureFramePath.toFile());
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Captured frame to " + captureFramePath.toAbsolutePath());
        } catch (Exception exception) {
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Frame capture failed: " + exception.getMessage());
            if (consoleLaunch) {
                System.err.println("ReMEXA frame capture failed: " + exception.getMessage());
                exception.printStackTrace(System.err);
            }
        }

        if (exitAfterCapture) {
            frame.dispose();
            System.exit(0);
        }
    }

    private Thread.UncaughtExceptionHandler installExitOnUncaughtException(
            JadFrame frame,
            JadDescriptor descriptor,
            ClassLoader appClassLoader
    ) {
        var previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (!MidletRuntime.isExpectedShutdownThrowable(throwable)
                    && appClassLoader != null
                    && thread.getContextClassLoader() == appClassLoader) {
                DebugLog.log(
                        LogCategory.HOST,
                        JadLauncher.class.getName(),
                        "Uncaught app exception in " + descriptor.title() + " on " + thread.getName() + ": " + throwable
                );
                frame.exitOnFatalException("uncaught app exception", throwable);
            }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });
        return previousHandler;
    }

    private void restoreDefaultExceptionHandler(Thread.UncaughtExceptionHandler handler) {
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }
}
