package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import remexa.host.runtime.MidletRuntime;

public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 0x0002;
    public static final int LEFT_PRESSED = 0x0004;
    public static final int RIGHT_PRESSED = 0x0020;
    public static final int DOWN_PRESSED = 0x0040;
    public static final int FIRE_PRESSED = 0x0100;
    public static final int GAME_A_PRESSED = 0x0200;
    public static final int GAME_B_PRESSED = 0x0400;
    public static final int GAME_C_PRESSED = 0x0800;
    public static final int GAME_D_PRESSED = 0x1000;

    private final boolean suppressKeyEvents;
    private Image backBuffer;
    private int backBufferWidth = -1;
    private int backBufferHeight = -1;
    private int currentKeyState;
    private int latchedKeyState;

    protected GameCanvas(boolean suppressKeyEvents) {
        this.suppressKeyEvents = suppressKeyEvents;
    }

    public Graphics getGraphics() {
        ensureBackBuffer();
        return backBuffer.getGraphics();
    }

    public void flushGraphics() {
        flushGraphics(0, 0, getWidth(), getHeight());
    }

    public void flushGraphics(int x, int y, int width, int height) {
        if (!isShown() || width < 1 || height < 1) {
            return;
        }
        ensureBackBuffer();
        MidletRuntime.renderCanvas(this, graphics -> graphics.drawImage(backBuffer, 0, 0, Graphics.LEFT | Graphics.TOP));
    }

    public int getKeyStates() {
        if (!isShown()) {
            return 0;
        }
        var state = currentKeyState | latchedKeyState;
        latchedKeyState = 0;
        return state;
    }

    @Override
    public void paint(Graphics graphics) {
        if (graphics == null) {
            throw new NullPointerException("graphics");
        }
        ensureBackBuffer();
        graphics.drawImage(backBuffer, 0, 0, Graphics.LEFT | Graphics.TOP);
    }

    protected final boolean suppressKeyEvents() {
        return suppressKeyEvents;
    }

    @Override
    protected void keyStateChanged(int keyCode, boolean pressed) {
        int stateBit = stateBitFor(keyCode);
        if (stateBit == 0) {
            return;
        }
        if (pressed) {
            currentKeyState |= stateBit;
            latchedKeyState |= stateBit;
        } else {
            currentKeyState &= ~stateBit;
        }
    }

    private void ensureBackBuffer() {
        var width = Math.max(1, getWidth());
        var height = Math.max(1, getHeight());
        if (backBuffer != null && backBufferWidth == width && backBufferHeight == height) {
            return;
        }
        backBuffer = Image.createImage(width, height);
        var graphics = backBuffer.getGraphics();
        graphics.setColor(0xFFFFFF);
        graphics.fillRect(0, 0, width, height);
        backBufferWidth = width;
        backBufferHeight = height;
    }

    private int stateBitFor(int keyCode) {
        return switch (getGameAction(keyCode)) {
            case UP -> UP_PRESSED;
            case LEFT -> LEFT_PRESSED;
            case RIGHT -> RIGHT_PRESSED;
            case DOWN -> DOWN_PRESSED;
            case FIRE -> FIRE_PRESSED;
            case GAME_A -> GAME_A_PRESSED;
            case GAME_B -> GAME_B_PRESSED;
            case GAME_C -> GAME_C_PRESSED;
            case GAME_D -> GAME_D_PRESSED;
            default -> switch (keyCode) {
                case '7' -> GAME_A_PRESSED;
                case '9' -> GAME_B_PRESSED;
                case '*' -> GAME_C_PRESSED;
                case '#' -> GAME_D_PRESSED;
                default -> 0;
            };
        };
    }
}
