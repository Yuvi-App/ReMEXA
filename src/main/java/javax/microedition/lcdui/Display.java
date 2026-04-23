package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;
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
        current = next;
        SdkStubSupport.log(Display.class.getName(), "setCurrent", midlet, next == null ? null : next.getTitle());
    }

    public void setCurrent(Alert alert, Displayable next) {
        current = next;
        SdkStubSupport.log(Display.class.getName(), "setCurrent", alert, next);
    }

    public boolean isColor() {
        return true;
    }

    public int numColors() {
        return 65536;
    }
}
