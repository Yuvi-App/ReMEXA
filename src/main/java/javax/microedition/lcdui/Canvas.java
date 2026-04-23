package javax.microedition.lcdui;

import java.util.HashSet;
import java.util.Set;
import remexa.host.runtime.MidletRuntime;

public abstract class Canvas extends Displayable {
    public static final int UP = -1;
    public static final int DOWN = -2;
    public static final int LEFT = -3;
    public static final int RIGHT = -4;
    public static final int FIRE = -5;
    private final Set<Integer> pressedKeys = new HashSet<>();

    protected abstract void paint(Graphics graphics);

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    public int getWidth() {
        return MidletRuntime.getDisplayMetrics(this).width();
    }

    public int getHeight() {
        return MidletRuntime.getDisplayMetrics(this).height();
    }

    public void repaint() {
        MidletRuntime.renderCanvas(this, graphics -> {
            try {
                paint(graphics);
            } finally {
                graphics.dispose();
            }
        });
    }

    public void serviceRepaints() {
    }

    public final void fireKeyPressed(int keyCode) {
        pressedKeys.add(keyCode);
        keyPressed(keyCode);
    }

    public final void fireKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        keyReleased(keyCode);
    }

    public final void fireKeyRepeated(int keyCode) {
        pressedKeys.add(keyCode);
        keyRepeated(keyCode);
    }

    public final int deviceKeyStateMask() {
        var state = 0;
        if (containsAnyKey((int) '2', UP)) {
            state |= 0x1000;
        }
        if (containsAnyKey((int) '4', LEFT)) {
            state |= 0x2000;
            state |= 0x0010;
        }
        if (containsAnyKey((int) '6', RIGHT)) {
            state |= 0x4000;
            state |= 0x0040;
        }
        if (containsAnyKey((int) '8', DOWN)) {
            state |= 0x8000;
        }
        if (containsAnyKey((int) '5', FIRE, (int) '\n')) {
            state |= 0x10000;
            state |= 0x0020;
        }
        if (pressedKeys.contains((int) '*')) {
            state |= 0x0400;
        }
        if (pressedKeys.contains((int) '#')) {
            state |= 0x0800;
        }
        if (pressedKeys.contains((int) '0')) {
            state |= 0x0001;
        }
        return state;
    }

    private boolean containsAnyKey(int... keyCodes) {
        for (var keyCode : keyCodes) {
            if (pressedKeys.contains(keyCode)) {
                return true;
            }
        }
        return false;
    }
}
