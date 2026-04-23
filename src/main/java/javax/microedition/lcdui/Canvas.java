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
    public static final int SOFT1 = -6;
    public static final int SOFT2 = -7;
    public static final int SOFT3 = -8;
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
        if (pressedKeys.contains(UP)) {
            state |= 0x1000;
        }
        if (pressedKeys.contains(LEFT)) {
            state |= 0x2000;
        }
        if (pressedKeys.contains(RIGHT)) {
            state |= 0x4000;
        }
        if (pressedKeys.contains(DOWN)) {
            state |= 0x8000;
        }
        if (containsAnyKey(FIRE, (int) '\n')) {
            state |= 0x10000;
        }
        if (pressedKeys.contains(SOFT1)) {
            state |= 0x20000;
        }
        if (pressedKeys.contains(SOFT2)) {
            state |= 0x40000;
        }
        if (pressedKeys.contains(SOFT3)) {
            state |= 0x80000;
        }
        if (pressedKeys.contains((int) '9')) {
            state |= 0x0200;
        }
        if (pressedKeys.contains((int) '8')) {
            state |= 0x0100;
        }
        if (pressedKeys.contains((int) '7')) {
            state |= 0x0080;
        }
        if (pressedKeys.contains((int) '6')) {
            state |= 0x0040;
        }
        if (pressedKeys.contains((int) '5')) {
            state |= 0x0020;
        }
        if (pressedKeys.contains((int) '4')) {
            state |= 0x0010;
        }
        if (pressedKeys.contains((int) '3')) {
            state |= 0x0008;
        }
        if (pressedKeys.contains((int) '2')) {
            state |= 0x0004;
        }
        if (pressedKeys.contains((int) '1')) {
            state |= 0x0002;
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
