package javax.microedition.lcdui;

import java.util.HashSet;
import java.util.Set;
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
    private final Set<Integer> pressedKeys = new HashSet<>();
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

    protected void sizeChanged(int width, int height) {
    }

    public int getWidth() {
        return MidletRuntime.getDisplayMetrics(this).width();
    }

    public int getHeight() {
        return MidletRuntime.getDisplayMetrics(this).height();
    }

    public boolean hasPointerEvents() {
        return false;
    }

    public boolean hasPointerMotionEvents() {
        return false;
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
        if (containsAnyKey(KEYCODE_UP, UP)) {
            state |= 0x1000;
        }
        if (containsAnyKey(KEYCODE_LEFT, LEFT)) {
            state |= 0x2000;
        }
        if (containsAnyKey(KEYCODE_RIGHT, RIGHT)) {
            state |= 0x4000;
        }
        if (containsAnyKey(KEYCODE_DOWN, DOWN)) {
            state |= 0x8000;
        }
        if (containsAnyKey(KEYCODE_FIRE, (int) '\n', FIRE)) {
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

    final void fireShowNotify() {
        if (shown) {
            return;
        }
        shown = true;
        showNotify();
    }

    final void fireHideNotify() {
        if (!shown) {
            return;
        }
        shown = false;
        hideNotify();
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
