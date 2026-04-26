package javax.microedition.lcdui;

import remexa.host.profile.DisplayMetrics;
import remexa.host.runtime.MidletRuntime;

public final class DisplayableHostAccess {
    private DisplayableHostAccess() {
    }

    public static void fireSizeChanged(Displayable displayable) {
        if (displayable == null) {
            return;
        }
        fireSizeChanged(displayable, MidletRuntime.getDisplayMetrics(displayable));
    }

    public static void fireSizeChanged(Displayable displayable, DisplayMetrics displayMetrics) {
        if (displayable == null || displayMetrics == null) {
            return;
        }
        displayable.fireSizeChanged(displayMetrics.width(), displayMetrics.height());
    }
}
