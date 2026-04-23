package javax.microedition.lcdui;

public class Font {
    public static final int FACE_SYSTEM = 0;
    public static final int STYLE_PLAIN = 0;
    public static final int SIZE_MEDIUM = 0;

    private static final Font DEFAULT = new Font();

    public static Font getDefaultFont() {
        return DEFAULT;
    }

    public static Font getFont(int face, int style, int size) {
        return DEFAULT;
    }
}
