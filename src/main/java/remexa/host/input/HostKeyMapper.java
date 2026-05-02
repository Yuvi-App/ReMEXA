package remexa.host.input;

import java.awt.event.KeyEvent;
import javax.microedition.lcdui.Canvas;

public final class HostKeyMapper {
    public static final int NO_MAPPING = Integer.MIN_VALUE;
    public static final int JSKY_LEFT_SOFT_AWT_KEY = KeyEvent.VK_A;
    public static final int JSKY_RIGHT_SOFT_AWT_KEY = KeyEvent.VK_S;
    public static final int JSKY_CENTER_AWT_KEY = KeyEvent.VK_ENTER;
    public static final int JSKY_LEFT_SOFT_KEY = Canvas.JPHONE_SOFT_LEFT;
    public static final int JSKY_RIGHT_SOFT_KEY = Canvas.JPHONE_SOFT_RIGHT;
    public static final int JSKY_CENTER_KEY = Canvas.FIRE;
    public static final int GENERIC_LEFT_SOFT_KEY = Canvas.SOFT1;
    public static final int GENERIC_RIGHT_SOFT_KEY = Canvas.SOFT2;

    private HostKeyMapper() {
    }

    public static int toPhoneKeyCode(int awtKeyCode) {
        return toPhoneKeyCode(awtKeyCode, InputProfile.GENERIC);
    }

    public static int toPhoneKeyCode(int awtKeyCode, InputProfile inputProfile) {
        var profile = inputProfile == null ? InputProfile.GENERIC : inputProfile;
        var directionalKeyCode = toDirectionalKeyCode(awtKeyCode, profile);
        if (directionalKeyCode != NO_MAPPING) {
            return directionalKeyCode;
        }

        var sharedKeyCode = toSharedPhoneKeyCode(awtKeyCode, profile);
        if (sharedKeyCode != NO_MAPPING) {
            return sharedKeyCode;
        }

        return toNumpadPhoneKeyCode(awtKeyCode, profile);
    }

    public static int toSoftKeyIndex(int awtKeyCode) {
        return toSoftKeyIndex(awtKeyCode, InputProfile.GENERIC);
    }

    public static int toSoftKeyIndex(int awtKeyCode, InputProfile inputProfile) {
        return switch (awtKeyCode) {
            case JSKY_LEFT_SOFT_AWT_KEY, KeyEvent.VK_F1 -> 0;
            case JSKY_RIGHT_SOFT_AWT_KEY, KeyEvent.VK_F2 -> 1;
            default -> -1;
        };
    }

    private static int toDirectionalKeyCode(int awtKeyCode, InputProfile inputProfile) {
        if (inputProfile.usesJPhoneKeyCodes()) {
            return switch (awtKeyCode) {
                // J-Phone family titles frequently compare raw keyPressed values
                // against the MIDP game-action constants directly.
                case KeyEvent.VK_UP -> Canvas.UP;
                case KeyEvent.VK_LEFT -> Canvas.LEFT;
                case KeyEvent.VK_RIGHT -> Canvas.RIGHT;
                case KeyEvent.VK_DOWN -> Canvas.DOWN;
                case JSKY_CENTER_AWT_KEY -> JSKY_CENTER_KEY;
                default -> NO_MAPPING;
            };
        }
        return switch (awtKeyCode) {
            // Generic MIDP titles usually expect device key codes from keyPressed.
            case KeyEvent.VK_UP, KeyEvent.VK_KP_UP -> Canvas.KEYCODE_UP;
            case KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT -> Canvas.KEYCODE_LEFT;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT -> Canvas.KEYCODE_RIGHT;
            case KeyEvent.VK_DOWN, KeyEvent.VK_KP_DOWN -> Canvas.KEYCODE_DOWN;
            case KeyEvent.VK_ENTER -> Canvas.KEYCODE_FIRE;
            default -> NO_MAPPING;
        };
    }

    private static int toSharedPhoneKeyCode(int awtKeyCode, InputProfile inputProfile) {
        return switch (awtKeyCode) {
            case KeyEvent.VK_A, KeyEvent.VK_F1 ->
                    inputProfile.usesJPhoneKeyCodes() ? JSKY_LEFT_SOFT_KEY : GENERIC_LEFT_SOFT_KEY;
            case KeyEvent.VK_S, KeyEvent.VK_F2 ->
                    inputProfile.usesJPhoneKeyCodes() ? JSKY_RIGHT_SOFT_KEY : GENERIC_RIGHT_SOFT_KEY;
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
            case KeyEvent.VK_MULTIPLY -> '*';
            case KeyEvent.VK_NUMBER_SIGN, KeyEvent.VK_DIVIDE, KeyEvent.VK_DECIMAL -> '#';
            default -> NO_MAPPING;
        };
    }

    private static int toNumpadPhoneKeyCode(int awtKeyCode, InputProfile inputProfile) {
        if (inputProfile.usesJPhoneKeyCodes()) {
            return switch (awtKeyCode) {
                case KeyEvent.VK_NUMPAD0 -> '0';
                case KeyEvent.VK_NUMPAD7 -> '1';
                case KeyEvent.VK_KP_UP, KeyEvent.VK_NUMPAD8 -> '2';
                case KeyEvent.VK_NUMPAD9 -> '3';
                case KeyEvent.VK_KP_LEFT, KeyEvent.VK_NUMPAD4 -> '4';
                case KeyEvent.VK_NUMPAD5 -> '5';
                case KeyEvent.VK_KP_RIGHT, KeyEvent.VK_NUMPAD6 -> '6';
                case KeyEvent.VK_NUMPAD1 -> '7';
                case KeyEvent.VK_KP_DOWN, KeyEvent.VK_NUMPAD2 -> '8';
                case KeyEvent.VK_NUMPAD3 -> '9';
                default -> NO_MAPPING;
            };
        }
        return switch (awtKeyCode) {
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
            default -> NO_MAPPING;
        };
    }
}
