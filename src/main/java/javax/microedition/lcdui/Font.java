package javax.microedition.lcdui;

import remexa.host.LaunchConfig;

public final class Font {
    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;

    private final FontBackend backend;
    private final int face;
    private final int style;
    private final int size;

    private Font(FontBackend backend, int face, int style, int size) {
        this.backend = backend;
        this.face = face;
        this.style = style;
        this.size = size;
    }

    public static Font getDefaultFont() {
        return getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
    }

    public static Font getFont(int face, int style, int size) {
        FontBackend backend = switch (LaunchConfig.FontType.resolveConfigured()) {
            case BITMAP -> BitmapFontBackend.create(size);
            case SYSTEM -> null;
        };
        if (backend == null) {
            backend = SystemFontBackend.create(face, style, size);
        }
        return new Font(backend, face, style, size);
    }

    public int getFace() {
        return face;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public boolean isPlain() {
        return style == STYLE_PLAIN;
    }

    public boolean isBold() {
        return (style & STYLE_BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & STYLE_ITALIC) != 0;
    }

    public boolean isUnderlined() {
        return (style & STYLE_UNDERLINED) != 0;
    }

    public int getHeight() {
        return backend.getHeight();
    }

    public int getBaselinePosition() {
        return backend.getAscent();
    }

    public int stringWidth(String value) {
        return backend.stringWidth(value);
    }

    public int charWidth(char ch) {
        return stringWidth(String.valueOf(ch));
    }

    public int charsWidth(char[] data, int offset, int length) {
        if (data == null || length <= 0) {
            return 0;
        }
        return stringWidth(new String(data, offset, length));
    }

    public int substringWidth(String value, int offset, int length) {
        if (value == null || length <= 0 || offset >= value.length()) {
            return 0;
        }
        int safeOffset = Math.max(0, offset);
        int safeEnd = Math.min(value.length(), safeOffset + Math.max(0, length));
        return stringWidth(value.substring(safeOffset, safeEnd));
    }

    java.awt.Font awtFont() {
        return backend.awtFont();
    }

    int getAscent() {
        return backend.getAscent();
    }

    int getDescent() {
        return backend.getDescent();
    }

    void drawString(java.awt.Graphics2D graphics, String text, int x, int baselineY, int argbColor) {
        backend.drawString(graphics, text, x, baselineY, argbColor);
    }

    static String normalizeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("\r", "");
    }
}
