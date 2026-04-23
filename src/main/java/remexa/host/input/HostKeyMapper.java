package remexa.host.input;

import java.awt.event.KeyEvent;
import javax.microedition.lcdui.Canvas;

public final class HostKeyMapper {
    private HostKeyMapper() {
    }

    public static int toPhoneKeyCode(int awtKeyCode) {
        return switch (awtKeyCode) {
            case KeyEvent.VK_UP, KeyEvent.VK_KP_UP -> Canvas.KEYCODE_UP;
            case KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT -> Canvas.KEYCODE_LEFT;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT -> Canvas.KEYCODE_RIGHT;
            case KeyEvent.VK_DOWN, KeyEvent.VK_KP_DOWN -> Canvas.KEYCODE_DOWN;
            case KeyEvent.VK_ENTER -> Canvas.KEYCODE_FIRE;
            case KeyEvent.VK_A, KeyEvent.VK_F1 -> Canvas.SOFT1;
            case KeyEvent.VK_S, KeyEvent.VK_F2 -> Canvas.SOFT2;
            case KeyEvent.VK_NUMPAD7 -> '1';
            case KeyEvent.VK_NUMPAD8 -> '2';
            case KeyEvent.VK_NUMPAD9 -> '3';
            case KeyEvent.VK_NUMPAD4 -> '4';
            case KeyEvent.VK_NUMPAD5 -> '5';
            case KeyEvent.VK_NUMPAD6 -> '6';
            case KeyEvent.VK_NUMPAD1 -> '7';
            case KeyEvent.VK_NUMPAD2 -> '8';
            case KeyEvent.VK_NUMPAD3 -> '9';
            case KeyEvent.VK_NUMPAD0 -> '0';
            case KeyEvent.VK_MULTIPLY -> '*';
            case KeyEvent.VK_NUMBER_SIGN, KeyEvent.VK_DIVIDE, KeyEvent.VK_DECIMAL -> '#';
            default -> Integer.MIN_VALUE;
        };
    }

    public static int toSoftKeyIndex(int awtKeyCode) {
        return switch (awtKeyCode) {
            case KeyEvent.VK_A, KeyEvent.VK_F1 -> 0;
            case KeyEvent.VK_S, KeyEvent.VK_F2 -> 1;
            default -> -1;
        };
    }
}
