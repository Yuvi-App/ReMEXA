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

    public static boolean fireScreenKeyPressed(Displayable displayable, int keyCode) {
        if (!(displayable instanceof Screen screen)) {
            return false;
        }
        screen.fireScreenKeyPressed(keyCode);
        return true;
    }

    public static boolean fireScreenKeyRepeated(Displayable displayable, int keyCode) {
        if (!(displayable instanceof Screen screen)) {
            return false;
        }
        screen.fireScreenKeyRepeated(keyCode);
        return true;
    }

    public static boolean repaintScreen(Displayable displayable) {
        if (!(displayable instanceof Screen screen)) {
            return false;
        }
        screen.repaintHost();
        return true;
    }
}
