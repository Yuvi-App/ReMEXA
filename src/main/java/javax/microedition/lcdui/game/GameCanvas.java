package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import remexa.host.runtime.MidletRuntime;

public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 0x0001;
    public static final int DOWN_PRESSED = 0x0002;
    public static final int LEFT_PRESSED = 0x0004;
    public static final int RIGHT_PRESSED = 0x0008;
    public static final int FIRE_PRESSED = 0x0010;
    public static final int GAME_A_PRESSED = 0x0020;
    public static final int GAME_B_PRESSED = 0x0040;
    public static final int GAME_C_PRESSED = 0x0080;
    public static final int GAME_D_PRESSED = 0x0100;

    private final boolean suppressKeyEvents;
    private Image backBuffer;
    private Graphics backBufferGraphics;
    private int backBufferWidth = -1;
    private int backBufferHeight = -1;

    protected GameCanvas(boolean suppressKeyEvents) {
        this.suppressKeyEvents = suppressKeyEvents;
    }

    public Graphics getGraphics() {
        ensureBackBuffer();
        return backBufferGraphics;
    }

    public void flushGraphics() {
        flushGraphics(0, 0, getWidth(), getHeight());
    }

    public void flushGraphics(int x, int y, int width, int height) {
        ensureBackBuffer();
        MidletRuntime.renderCanvas(this, graphics -> graphics.drawImage(backBuffer, 0, 0, Graphics.LEFT | Graphics.TOP));
    }

    public int getKeyStates() {
        var state = 0;
        if (containsAnyPressedKey(KEYCODE_UP, UP, (int) '2')) {
            state |= UP_PRESSED;
        }
        if (containsAnyPressedKey(KEYCODE_DOWN, DOWN, (int) '8')) {
            state |= DOWN_PRESSED;
        }
        if (containsAnyPressedKey(KEYCODE_LEFT, LEFT, (int) '4')) {
            state |= LEFT_PRESSED;
        }
        if (containsAnyPressedKey(KEYCODE_RIGHT, RIGHT, (int) '6')) {
            state |= RIGHT_PRESSED;
        }
        if (containsAnyPressedKey(KEYCODE_FIRE, FIRE, (int) '\n', (int) '5')) {
            state |= FIRE_PRESSED;
        }
        if (containsAnyPressedKey((int) '7')) {
            state |= GAME_A_PRESSED;
        }
        if (containsAnyPressedKey((int) '9')) {
            state |= GAME_B_PRESSED;
        }
        if (containsAnyPressedKey((int) '*')) {
            state |= GAME_C_PRESSED;
        }
        if (containsAnyPressedKey((int) '#')) {
            state |= GAME_D_PRESSED;
        }
        return state;
    }

    protected final boolean suppressKeyEvents() {
        return suppressKeyEvents;
    }

    private void ensureBackBuffer() {
        var width = Math.max(1, getWidth());
        var height = Math.max(1, getHeight());
        if (backBuffer != null && backBufferWidth == width && backBufferHeight == height) {
            return;
        }
        backBuffer = Image.createImage(width, height);
        backBufferGraphics = backBuffer.getGraphics();
        backBufferWidth = width;
        backBufferHeight = height;
    }
}
