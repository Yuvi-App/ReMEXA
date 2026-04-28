package javax.microedition.lcdui;

import com.j_phone.system.DeviceControl;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private ScheduledFuture<?> pendingAlertTimeout;

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

    public boolean vibrate(int duration) {
        SdkStubSupport.log(Display.class.getName(), "vibrate", duration);
        if (duration <= 0) {
            DeviceControl.getDefaultDeviceControl().setDeviceActive(DeviceControl.VIBRATION, false);
            return false;
        }
        DeviceControl.getDefaultDeviceControl().setDeviceActive(DeviceControl.VIBRATION, true);
        return true;
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
        initializeDisplayable(next);
    }

    private void cancelPendingAlertTimeout() {
        if (pendingAlertTimeout == null) {
            return;
        }
        pendingAlertTimeout.cancel(false);
        pendingAlertTimeout = null;
    }

    private static void initializeDisplayable(Displayable displayable) {
        if (displayable == null) {
            return;
        }
        DisplayableHostAccess.fireSizeChanged(displayable);
        displayable.fireShown();
        if (displayable instanceof com.j_phone.amuse.ACanvas aCanvas) {
            aCanvas.attachHostGraphics();
            ((Canvas) aCanvas).fireShowNotify();
            return;
        }
        if (displayable instanceof Canvas canvas) {
            canvas.fireShowNotify();
            canvas.repaint();
        }
    }

    private static void deactivateDisplayable(Displayable previous, Displayable next) {
        if (previous == next) {
            return;
        }
        if (previous != null) {
            previous.fireHidden();
        }
        if (previous instanceof Canvas canvas) {
            canvas.fireHideNotify();
        }
    }
}
