package javax.microedition.lcdui;

import com.j_phone.system.DeviceControl;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.microedition.midlet.MIDlet;
import remexa.host.profile.DisplayMetrics;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.SdkStubSupport;

public final class Display {
    private static final ScheduledExecutorService ALERT_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "remexa-alert-timeout");
        thread.setDaemon(true);
        return thread;
    });

    private Displayable current;
    private final MIDlet midlet;
    private final Queue<Runnable> pendingSerialCallbacks = new ArrayDeque<>();
    private ScheduledFuture<?> pendingAlertTimeout;
    private boolean serialCallbackDrainScheduled;

    public Display(MIDlet midlet) {
        this.midlet = midlet;
    }

    public static Display getDisplay(MIDlet midlet) {
        SdkStubSupport.log(Display.class.getName(), "getDisplay", midlet);
        return MidletRuntime.getDisplay(midlet);
    }

    public Displayable getCurrent() {
        return current;
    }

    public void setCurrent(Displayable next) {
        if (next instanceof Alert alert) {
            showAlert(alert, current);
        } else {
            applyCurrentDisplayable(next);
        }
        SdkStubSupport.log(Display.class.getName(), "setCurrent", midlet, next == null ? null : next.getTitle());
    }

    public void setCurrent(Alert alert, Displayable next) {
        if (alert == null || next == null) {
            throw new NullPointerException("Alert and next displayable must be non-null.");
        }
        if (next instanceof Alert) {
            throw new IllegalArgumentException("nextDisplayable must not be an Alert.");
        }
        showAlert(alert, next);
        SdkStubSupport.log(Display.class.getName(), "setCurrent", alert, next);
    }

    public boolean isColor() {
        return true;
    }

    public int numColors() {
        return 65536;
    }

    public int numAlphaLevels() {
        return 256;
    }

    public boolean vibrate(int duration) {
        SdkStubSupport.log(Display.class.getName(), "vibrate", duration);
        if (duration <= 0) {
            DeviceControl.getDefaultDeviceControl().setDeviceActive(DeviceControl.VIBRATION, false);
            return false;
        }
        DeviceControl.getDefaultDeviceControl().setDeviceActive(DeviceControl.VIBRATION, true);
        return true;
    }

    public boolean flashBacklight(int duration) {
        SdkStubSupport.log(Display.class.getName(), "flashBacklight", duration);
        if (duration < 0) {
            throw new IllegalArgumentException("Backlight flash duration must not be negative.");
        }
        return MidletRuntime.flashBacklight(midlet, duration);
    }

    public void callSerially(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("Runnable must be non-null.");
        }
        synchronized (pendingSerialCallbacks) {
            pendingSerialCallbacks.add(runnable);
            if (serialCallbackDrainScheduled) {
                return;
            }
            serialCallbackDrainScheduled = true;
        }
        SwingUtilities.invokeLater(this::drainSerialCallbacks);
    }

    DisplayMetrics displayMetrics() {
        return MidletRuntime.getDisplayMetrics(midlet);
    }

    void dismissAlert(Alert alert, Command command) {
        if (alert == null || current != alert) {
            return;
        }
        cancelPendingAlertTimeout();
        var next = alert.nextDisplayable();
        alert.detachFromDisplay(this);
        applyCurrentDisplayable(next);
        SdkStubSupport.log(Display.class.getName(), "dismissAlert", alert.getTitle(), command == null ? null : command.getLabel());
    }

    private void showAlert(Alert alert, Displayable nextAfterDismissal) {
        cancelPendingAlertTimeout();
        alert.attachToDisplay(this, nextAfterDismissal);
        applyCurrentDisplayable(alert);
        var timeout = alert.getTimeout();
        if (timeout != Alert.FOREVER) {
            pendingAlertTimeout = ALERT_TIMEOUT_EXECUTOR.schedule(alert::fireTimeout, timeout, TimeUnit.MILLISECONDS);
        }
    }

    private void applyCurrentDisplayable(Displayable next) {
        cancelPendingAlertTimeout();
        var previous = current;
        current = next;
        if (previous instanceof Alert previousAlert && previousAlert != next) {
            previousAlert.detachFromDisplay(this);
        }
        MidletRuntime.bindDisplayable(midlet, next);
        MidletRuntime.setCurrentDisplayable(midlet, next);
        deactivateDisplayable(previous, next);
        if (next instanceof TextBox textBox) {
            textBox.attachDisplay(this, previous);
        }
        initializeDisplayable(next);
    }

    private void cancelPendingAlertTimeout() {
        if (pendingAlertTimeout == null) {
            return;
        }
        pendingAlertTimeout.cancel(false);
        pendingAlertTimeout = null;
    }

    private void drainSerialCallbacks() {
        Runnable callback;
        synchronized (pendingSerialCallbacks) {
            callback = pendingSerialCallbacks.poll();
            if (callback == null) {
                serialCallbackDrainScheduled = false;
                return;
            }
        }

        try {
            callback.run();
        } catch (Throwable throwable) {
            var message = "Display.callSerially callback failed";
            SdkStubSupport.log(Display.class.getName(), "callSerially", message, throwable);
            throw throwable;
        }

        synchronized (pendingSerialCallbacks) {
            if (pendingSerialCallbacks.isEmpty()) {
                serialCallbackDrainScheduled = false;
                return;
            }
        }

        // A few games implement their frame loop by re-posting themselves from
        // callSerially(). Yield between callbacks so Swing can process repaints
        // and input instead of letting one self-rescheduling runnable monopolize
        // the event thread.
        SwingUtilities.invokeLater(this::drainSerialCallbacks);
    }

    private static void initializeDisplayable(Displayable displayable) {
        if (displayable == null) {
            return;
        }
        DisplayableHostAccess.fireSizeChanged(displayable);
        displayable.fireShown();
        if (displayable instanceof TextBox textBox) {
            textBox.onShown();
        }
        if (displayable instanceof com.j_phone.amuse.ACanvas aCanvas) {
            aCanvas.attachHostGraphics();
            ((Canvas) aCanvas).fireShowNotify();
            aCanvas.startHostPaintLoop();
            return;
        }
        if (displayable instanceof Canvas canvas) {
            canvas.fireShowNotify();
            SwingUtilities.invokeLater(() -> {
                if (canvas.isShown()) {
                    canvas.repaint();
                }
            });
            return;
        }
        if (displayable instanceof Screen screen && !(displayable instanceof TextBox)) {
            SwingUtilities.invokeLater(() -> {
                if (screen.isShown()) {
                    screen.repaintHost();
                }
            });
        }
    }

    private static void deactivateDisplayable(Displayable previous, Displayable next) {
        if (previous == next) {
            return;
        }
        if (previous != null) {
            previous.fireHidden();
        }
        if (previous instanceof TextBox textBox) {
            textBox.detachDisplay();
        }
        if (previous instanceof Canvas canvas) {
            canvas.fireHideNotify();
        }
    }
}
