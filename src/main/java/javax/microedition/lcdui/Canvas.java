package javax.microedition.lcdui;

import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import remexa.host.runtime.LegacyRuntimeSupport;
import remexa.host.runtime.MidletRuntime;

public abstract class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int DOWN = 6;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    public static final int KEYCODE_UP = -1;
    public static final int KEYCODE_DOWN = -2;
    public static final int KEYCODE_LEFT = -3;
    public static final int KEYCODE_RIGHT = -4;
    public static final int KEYCODE_FIRE = -5;
    public static final int SOFT1 = -6;
    public static final int SOFT2 = -7;
    public static final int SOFT3 = -8;
    public static final int JPHONE_SOFT_LEFT = -21;
    public static final int JPHONE_SOFT_RIGHT = -22;
    public static final int JPHONE_SOFT_CENTER = -23;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean fullScreenMode;
    private boolean paintInProgress;
    private boolean repaintQueued;
    private boolean shown;

    protected abstract void paint(Graphics graphics);

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }

    protected void keyStateChanged(int keyCode, boolean pressed) {
    }

    protected void sizeChanged(int width, int height) {
    }

    public int getWidth() {
        return MidletRuntime.getDisplayMetrics(this).width();
    }

    public int getHeight() {
        return MidletRuntime.getDisplayMetrics(this).height();
    }

    public boolean hasPointerEvents() {
        return MidletRuntime.queryPointerEventsAvailable(this);
    }

    public boolean hasPointerMotionEvents() {
        return MidletRuntime.queryPointerMotionEventsAvailable(this);
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    public boolean isShown() {
        return shown;
    }

    public void setFullScreenMode(boolean mode) {
        if (fullScreenMode == mode) {
            return;
        }
        fullScreenMode = mode;
        if (!isShown()) {
            return;
        }

        // Some handsets signal a mode transition even when the usable pixel
        // bounds stay the same. A few games wait on this callback after asking
        // for fullscreen, so force it instead of letting duplicate dimensions
        // be coalesced.
        fireSizeChanged(getWidth(), getHeight(), true);
        repaint();
    }

    public void repaint() {
        if (deferRepaintIfPainting(this::repaint)) {
            return;
        }
        MidletRuntime.renderCanvas(this, graphics -> {
            beginHostPaint();
            try {
                paint(graphics);
            } finally {
                endHostPaint();
                graphics.dispose();
            }
        });
    }

    public void repaint(int x, int y, int width, int height) {
        if (deferRepaintIfPainting(() -> repaint(x, y, width, height))) {
            return;
        }
        MidletRuntime.renderCanvas(this, graphics -> {
            beginHostPaint();
            try {
                graphics.setClip(x, y, width, height);
                paint(graphics);
            } finally {
                endHostPaint();
                graphics.dispose();
            }
        });
    }

    public void serviceRepaints() {
        MidletRuntime.serviceCanvasRepaints(this);
    }

    public int getGameAction(int keyCode) {
        return switch (keyCode) {
            case KEYCODE_UP, '2', UP -> UP;
            case KEYCODE_LEFT, '4', LEFT -> LEFT;
            case KEYCODE_RIGHT, '6', RIGHT -> RIGHT;
            case KEYCODE_DOWN, '8', DOWN -> DOWN;
            case KEYCODE_FIRE, '\n', '5', FIRE -> FIRE;
            default -> 0;
        };
    }

    public int getKeyCode(int gameAction) {
        return switch (gameAction) {
            case UP -> KEYCODE_UP;
            case LEFT -> KEYCODE_LEFT;
            case RIGHT -> KEYCODE_RIGHT;
            case DOWN -> KEYCODE_DOWN;
            case FIRE -> KEYCODE_FIRE;
            default -> 0;
        };
    }

    public String getKeyName(int keyCode) {
        return switch (keyCode) {
            case KEYCODE_UP, UP -> "Up";
            case KEYCODE_LEFT, LEFT -> "Left";
            case KEYCODE_RIGHT, RIGHT -> "Right";
            case KEYCODE_DOWN, DOWN -> "Down";
            case KEYCODE_FIRE, '\n', FIRE -> "Select";
            case SOFT1 -> "Soft1";
            case SOFT2 -> "Soft2";
            case SOFT3 -> "Soft3";
            case JPHONE_SOFT_LEFT -> "SoftLeft";
            case JPHONE_SOFT_RIGHT -> "SoftRight";
            case JPHONE_SOFT_CENTER -> "SoftCenter";
            case '0' -> "0";
            case '1' -> "1";
            case '2' -> "2";
            case '3' -> "3";
            case '4' -> "4";
            case '5' -> "5";
            case '6' -> "6";
            case '7' -> "7";
            case '8' -> "8";
            case '9' -> "9";
            case '*' -> "*";
            case '#' -> "#";
            default -> Integer.toString(keyCode);
        };
    }

    public final void fireKeyPressed(int keyCode) {
        pressedKeys.add(keyCode);
        keyStateChanged(keyCode, true);
        keyPressed(keyCode);
    }

    public final void fireKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        keyStateChanged(keyCode, false);
        keyReleased(keyCode);
    }

    public final void fireKeyRepeated(int keyCode) {
        pressedKeys.add(keyCode);
        keyStateChanged(keyCode, true);
        keyRepeated(keyCode);
    }

    public final void fireKeyStateChanged(int keyCode, boolean pressed) {
        if (pressed) {
            pressedKeys.add(keyCode);
        } else {
            pressedKeys.remove(keyCode);
        }
        keyStateChanged(keyCode, pressed);
    }

    public final void firePointerPressed(int x, int y) {
        pointerPressed(x, y);
    }

    public final void firePointerReleased(int x, int y) {
        pointerReleased(x, y);
    }

    public final void firePointerDragged(int x, int y) {
        pointerDragged(x, y);
    }

    public final int deviceKeyStateMask() {
        return deviceKeyStateMask(false);
    }

    public final int deviceKeyStateMask(boolean eightDirectionsEnabled) {
        // MIDP allows directional game actions to be mapped either to dedicated
        // navigation keys or to the phone keypad (2/4/6/8, 5 for FIRE).
        var up = containsAnyKey(KEYCODE_UP, UP, (int) '2');
        var left = containsAnyKey(KEYCODE_LEFT, LEFT, (int) '4');
        var right = containsAnyKey(KEYCODE_RIGHT, RIGHT, (int) '6');
        var down = containsAnyKey(KEYCODE_DOWN, DOWN, (int) '8');
        boolean upRight = eightDirectionsEnabled && up && right;
        boolean upLeft = eightDirectionsEnabled && up && left;
        boolean downRight = eightDirectionsEnabled && down && right;
        boolean downLeft = eightDirectionsEnabled && down && left;
        var state = 0;
        if (upRight) {
            state |= 0x100000;
        }
        if (upLeft) {
            state |= 0x200000;
        }
        if (downRight) {
            state |= 0x400000;
        }
        if (downLeft) {
            state |= 0x800000;
        }
        if (!upRight && !upLeft) {
            if (up) {
                state |= 0x1000;
            }
        }
        if (!upLeft && !downLeft) {
            if (left) {
                state |= 0x2000;
            }
        }
        if (!upRight && !downRight) {
            if (right) {
                state |= 0x4000;
            }
        }
        if (!downLeft && !downRight) {
            if (down) {
                state |= 0x8000;
            }
        }
        if (containsAnyKey(KEYCODE_FIRE, (int) '\n', FIRE, (int) '5')) {
            state |= 0x10000;
        }
        // Raw J-Phone key presses arrive as -21/-22/-23 for the physical
        // left/right/center softkeys. Some titles poll DeviceControl.KEY_STATE
        // directly instead of handling keyPressed callbacks, so expose those raw
        // keys through the handset bitfield as well.
        if (pressedKeys.contains(JPHONE_SOFT_LEFT)) {
            state |= 0x20000;
        }
        if (pressedKeys.contains(JPHONE_SOFT_RIGHT)) {
            state |= 0x40000;
        }
        if (pressedKeys.contains(JPHONE_SOFT_CENTER)) {
            state |= 0x80000;
        }
        // MIDP softkey constants are kept for generic profiles and any titles
        // that synthesize SOFT1/SOFT2/SOFT3 rather than raw handset codes.
        if (pressedKeys.contains(SOFT1)) {
            state |= 0x40000;
        }
        if (pressedKeys.contains(SOFT2)) {
            state |= 0x20000;
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

    protected final void beginHostPaint() {
        synchronized (this) {
            paintInProgress = true;
        }
    }

    protected final void endHostPaint() {
        synchronized (this) {
            paintInProgress = false;
        }
    }

    protected final boolean isHostPaintInProgress() {
        synchronized (this) {
            return paintInProgress;
        }
    }

    protected final boolean deferRepaintIfPainting(Runnable repaintAction) {
        synchronized (this) {
            if (!paintInProgress) {
                return false;
            }
            if (repaintQueued) {
                return true;
            }
            repaintQueued = true;
        }
        SwingUtilities.invokeLater(() -> {
            synchronized (Canvas.this) {
                repaintQueued = false;
            }
            repaintAction.run();
        });
        return true;
    }

    public final int phoneKeyStateMask(boolean eightDirectionsEnabled) {
        var up = containsAnyKey(KEYCODE_UP, UP, (int) '2');
        var left = containsAnyKey(KEYCODE_LEFT, LEFT, (int) '4');
        var right = containsAnyKey(KEYCODE_RIGHT, RIGHT, (int) '6');
        var down = containsAnyKey(KEYCODE_DOWN, DOWN, (int) '8');
        var fire = containsAnyKey(KEYCODE_FIRE, (int) '\n', FIRE, (int) '5');
        var upRight = pressedKeys.contains((int) '9');
        var upLeft = pressedKeys.contains((int) '7');
        var downLeft = pressedKeys.contains((int) '1');
        var downRight = pressedKeys.contains((int) '3');

        if (eightDirectionsEnabled) {
            if (up && right) {
                upRight = true;
            }
            if (up && left) {
                upLeft = true;
            }
            if (down && left) {
                downLeft = true;
            }
            if (down && right) {
                downRight = true;
            }
            if (upRight || upLeft) {
                up = false;
            }
            if (upLeft || downLeft) {
                left = false;
            }
            if (upRight || downRight) {
                right = false;
            }
            if (downLeft || downRight) {
                down = false;
            }
        }

        var state = 0;
        if (upRight) {
            state |= 0x0200;
        }
        if (down) {
            state |= 0x0100;
        }
        if (upLeft) {
            state |= 0x0080;
        }
        if (right) {
            state |= 0x0040;
        }
        if (fire) {
            state |= 0x0020;
        }
        if (left) {
            state |= 0x0010;
        }
        if (downRight) {
            state |= 0x0008;
        }
        if (up) {
            state |= 0x0004;
        }
        if (downLeft) {
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

    final void fireShowNotify() {
        if (shown) {
            return;
        }
        shown = true;
        showNotify();
        LegacyRuntimeSupport.publishLegacyState();
    }

    final void fireHideNotify() {
        if (!shown) {
            return;
        }
        shown = false;
        hideNotify();
    }

    protected final boolean containsAnyPressedKey(int... keyCodes) {
        return containsAnyKey(keyCodes);
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
