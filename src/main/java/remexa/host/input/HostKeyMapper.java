package remexa.host.input;

import java.awt.event.KeyEvent;
import javax.microedition.lcdui.Canvas;

public final class HostKeyMapper {
    private HostKeyMapper() {
    }

    public static int toPhoneKeyCode(int awtKeyCode) {
        return toPhoneKeyCode(awtKeyCode, false);
    }

    public static int toPhoneKeyCode(int awtKeyCode, boolean jPhoneDirectionalLayout) {
        if (jPhoneDirectionalLayout) {
            return switch (awtKeyCode) {
                case KeyEvent.VK_UP -> Canvas.UP;
                case KeyEvent.VK_LEFT -> Canvas.LEFT;
                case KeyEvent.VK_RIGHT -> Canvas.RIGHT;
                case KeyEvent.VK_DOWN -> Canvas.DOWN;
                case KeyEvent.VK_ENTER -> Canvas.FIRE;
                case KeyEvent.VK_A, KeyEvent.VK_F1 -> Canvas.SOFT1;
                case KeyEvent.VK_S, KeyEvent.VK_F2 -> Canvas.SOFT2;
                case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> '0';
                case KeyEvent.VK_1, KeyEvent.VK_NUMPAD7 -> '1';
                case KeyEvent.VK_2, KeyEvent.VK_KP_UP, KeyEvent.VK_NUMPAD8 -> '2';
                case KeyEvent.VK_3, KeyEvent.VK_NUMPAD9 -> '3';
                case KeyEvent.VK_4, KeyEvent.VK_KP_LEFT, KeyEvent.VK_NUMPAD4 -> '4';
                case KeyEvent.VK_5, KeyEvent.VK_NUMPAD5 -> '5';
                case KeyEvent.VK_6, KeyEvent.VK_KP_RIGHT, KeyEvent.VK_NUMPAD6 -> '6';
                case KeyEvent.VK_7, KeyEvent.VK_NUMPAD1 -> '7';
                case KeyEvent.VK_8, KeyEvent.VK_KP_DOWN, KeyEvent.VK_NUMPAD2 -> '8';
                case KeyEvent.VK_9, KeyEvent.VK_NUMPAD3 -> '9';
                case KeyEvent.VK_MULTIPLY -> '*';
                case KeyEvent.VK_NUMBER_SIGN, KeyEvent.VK_DIVIDE, KeyEvent.VK_DECIMAL -> '#';
                default -> Integer.MIN_VALUE;
            };
        }
        return switch (awtKeyCode) {
            // Some MIDP titles compare raw keyPressed values against the Canvas action
            // constants directly instead of using KEYCODE_* device codes.
            case KeyEvent.VK_UP, KeyEvent.VK_KP_UP -> Canvas.UP;
            case KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT -> Canvas.LEFT;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT -> Canvas.RIGHT;
            case KeyEvent.VK_DOWN, KeyEvent.VK_KP_DOWN -> Canvas.DOWN;
            case KeyEvent.VK_ENTER -> Canvas.FIRE;
            case KeyEvent.VK_A, KeyEvent.VK_F1 -> Canvas.SOFT1;
            case KeyEvent.VK_S, KeyEvent.VK_F2 -> Canvas.SOFT2;
            case KeyEvent.VK_0 -> '0';
            case KeyEvent.VK_1 -> '1';
            case KeyEvent.VK_2 -> '2';
            case KeyEvent.VK_3 -> '3';
            case KeyEvent.VK_4 -> '4';
            case KeyEvent.VK_5 -> '5';
            case KeyEvent.VK_6 -> '6';
            case KeyEvent.VK_7 -> '7';
            case KeyEvent.VK_8 -> '8';
            case KeyEvent.VK_9 -> '9';
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
