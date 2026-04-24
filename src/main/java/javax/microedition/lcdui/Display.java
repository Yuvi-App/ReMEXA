package javax.microedition.lcdui;

import com.j_phone.system.DeviceControl;
import javax.microedition.midlet.MIDlet;
import remexa.host.profile.DisplayMetrics;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.SdkStubSupport;

public final class Display {
    private Displayable current;
    private final MIDlet midlet;

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
        var previous = current;
        current = next;
        MidletRuntime.bindDisplayable(midlet, next);
        MidletRuntime.setCurrentDisplayable(midlet, next);
        deactivateDisplayable(previous, next);
        initializeDisplayable(next);
        SdkStubSupport.log(Display.class.getName(), "setCurrent", midlet, next == null ? null : next.getTitle());
    }

    public void setCurrent(Alert alert, Displayable next) {
        var previous = current;
        current = next;
        MidletRuntime.bindDisplayable(midlet, next);
        MidletRuntime.setCurrentDisplayable(midlet, next);
        deactivateDisplayable(previous, next);
        initializeDisplayable(next);
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

    private static void initializeDisplayable(Displayable displayable) {
        if (displayable == null) {
            return;
        }
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
