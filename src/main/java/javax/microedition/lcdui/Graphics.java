package javax.microedition.lcdui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    private final Graphics2D delegate;
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final boolean disposable;
    private Font font = Font.getDefaultFont();
    private int argbColor = 0xFF000000;
    private int translateX;
    private int translateY;

    public Graphics(Graphics2D delegate, int surfaceWidth, int surfaceHeight) {
        this(delegate, surfaceWidth, surfaceHeight, true);
    }

    public Graphics(Graphics2D delegate, int surfaceWidth, int surfaceHeight, boolean disposable) {
        this.delegate = delegate;
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.disposable = disposable;
        this.delegate.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        this.delegate.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        resetState();
    }

    public void setColor(int rgb) {
        argbColor = 0xFF000000 | (rgb & 0x00FFFFFF);
        delegate.setColor(new Color(argbColor, true));
    }

    public void setColor(int red, int green, int blue) {
        argbColor = 0xFF000000 | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
        delegate.setColor(new Color(argbColor, true));
    }

    public int getColor() {
        return argbColor & 0x00FFFFFF;
    }

    public void fillRect(int x, int y, int width, int height) {
        delegate.fillRect(x + translateX, y + translateY, width, height);
    }

    public void drawRect(int x, int y, int width, int height) {
        delegate.drawRect(x + translateX, y + translateY, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.drawRoundRect(x + translateX, y + translateY, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.fillRoundRect(x + translateX, y + translateY, width, height, arcWidth, arcHeight);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width < 0 || height < 0) {
            return;
        }
        delegate.drawArc(x + translateX, y + translateY, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width <= 0 || height <= 0) {
            return;
        }
        delegate.fillArc(x + translateX, y + translateY, width, height, startAngle, arcAngle);
    }

    public void drawOval(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        delegate.drawOval(x + translateX, y + translateY, width, height);
    }

    public void fillOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        delegate.fillOval(x + translateX, y + translateY, width, height);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        var polygon = new Polygon(
                new int[]{x1 + translateX, x2 + translateX, x3 + translateX},
                new int[]{y1 + translateY, y2 + translateY, y3 + translateY},
                3
        );
        delegate.fillPolygon(polygon);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        delegate.drawLine(x1 + translateX, y1 + translateY, x2 + translateX, y2 + translateY);
    }

    public void drawString(String string, int x, int y, int anchor) {
        var text = string == null ? "" : string;
        var drawX = anchoredX(x + translateX, anchor, font.stringWidth(text));
        var drawY = anchoredY(y + translateY, anchor, font.getAscent(), font.getHeight());
        font.drawString(delegate, text, drawX, drawY, argbColor);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        drawString(String.valueOf(character), x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        if (data == null || length <= 0) {
            return;
        }
        drawString(new String(data, offset, length), x, y, anchor);
    }

    public void drawSubstring(String string, int offset, int len, int x, int y, int anchor) {
        if (string == null || len <= 0 || offset >= string.length()) {
            return;
        }
        int safeOffset = Math.max(0, offset);
        int safeEnd = Math.min(string.length(), safeOffset + Math.max(0, len));
        drawString(string.substring(safeOffset, safeEnd), x, y, anchor);
    }

    public void drawImage(Image image, int x, int y, int anchor) {
        if (image == null) {
            return;
        }
        var drawX = anchoredX(x + translateX, anchor, image.getWidth());
        var drawY = anchoredYForImage(y + translateY, anchor, image.getHeight());
        delegate.drawImage(image.awtImage(), drawX, drawY, null);
    }

    public void setClip(int x, int y, int width, int height) {
        delegate.setClip(x + translateX, y + translateY, width, height);
    }

    public void clipRect(int x, int y, int width, int height) {
        var next = new Rectangle(x + translateX, y + translateY, width, height);
        var existing = delegate.getClipBounds();
        delegate.setClip(existing == null ? next : existing.intersection(next));
    }

    public void setFont(Font font) {
        this.font = font == null ? Font.getDefaultFont() : font;
        delegate.setFont(this.font.awtFont());
    }

    public Font getFont() {
        return font;
    }

    public int getClipX() {
        var bounds = clipBounds();
        return bounds.x - translateX;
    }

    public int getClipY() {
        var bounds = clipBounds();
        return bounds.y - translateY;
    }

    public int getClipWidth() {
        var bounds = clipBounds();
        return bounds.width;
    }

    public int getClipHeight() {
        var bounds = clipBounds();
        return bounds.height;
    }

    public void dispose() {
        if (disposable) {
            delegate.dispose();
        }
    }

    public void translate(int x, int y) {
        translateX += x;
        translateY += y;
    }

    public int getTranslateX() {
        return translateX;
    }

    public int getTranslateY() {
        return translateY;
    }

    public void resetState() {
        font = Font.getDefaultFont();
        argbColor = 0xFF000000;
        translateX = 0;
        translateY = 0;
        delegate.setClip(0, 0, surfaceWidth, surfaceHeight);
        delegate.setFont(font.awtFont());
        delegate.setColor(new Color(argbColor, true));
    }

    private Rectangle clipBounds() {
        var bounds = delegate.getClipBounds();
        if (bounds == null) {
            return new Rectangle(0, 0, surfaceWidth, surfaceHeight);
        }
        return bounds;
    }

    private int anchoredX(int x, int anchor, int width) {
        if ((anchor & RIGHT) != 0) {
            return x - width;
        }
        if ((anchor & HCENTER) != 0) {
            return x - width / 2;
        }
        return x;
    }

    private int anchoredY(int y, int anchor, int ascent, int height) {
        if ((anchor & BASELINE) != 0) {
            return y;
        }
        if ((anchor & BOTTOM) != 0) {
            return y - height + ascent;
        }
        if ((anchor & VCENTER) != 0) {
            return y - height / 2 + ascent;
        }
        return y + ascent;
    }

    private int anchoredYForImage(int y, int anchor, int height) {
        if ((anchor & BOTTOM) != 0) {
            return y - height;
        }
        if ((anchor & VCENTER) != 0) {
            return y - height / 2;
        }
        return y;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
