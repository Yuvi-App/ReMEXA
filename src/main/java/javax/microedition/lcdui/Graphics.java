package javax.microedition.lcdui;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import remexa.host.translate.AutoTranslate;

public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0;
    public static final int DOTTED = 1;
    private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();
    private final Graphics2D delegate;
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final boolean disposable;
    private Font font = Font.getDefaultFont();
    private int argbColor = 0xFF000000;
    private int cachedColorArgb;
    private Color cachedColor;
    private int translateX;
    private int translateY;
    private int strokeStyle = SOLID;
    private BufferedImage drawRegionSourceScratch;
    private Graphics2D drawRegionSourceGraphics;
    private BufferedImage drawRegionTransformScratch;
    private Graphics2D drawRegionTransformGraphics;
    private BufferedImage drawRgbScratch;
    private int[] drawRgbPixels;

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
        setDelegateColor(argbColor);
    }

    public void setColor(int red, int green, int blue) {
        validateColorComponent(red);
        validateColorComponent(green);
        validateColorComponent(blue);
        argbColor = 0xFF000000 | (red << 16) | (green << 8) | blue;
        setDelegateColor(argbColor);
    }

    public int getColor() {
        return argbColor & 0x00FFFFFF;
    }

    public int getRedComponent() {
        return (argbColor >>> 16) & 0xFF;
    }

    public int getGreenComponent() {
        return (argbColor >>> 8) & 0xFF;
    }

    public int getBlueComponent() {
        return argbColor & 0xFF;
    }

    public int getGrayScale() {
        return (getRedComponent() * 299 + getGreenComponent() * 587 + getBlueComponent() * 114 + 500) / 1000;
    }

    public void setGrayScale(int value) {
        validateColorComponent(value);
        setColor(value, value, value);
    }

    public int getDisplayColor(int color) {
        return color & 0x00FFFFFF;
    }

    public void setStrokeStyle(int style) {
        if (style != SOLID && style != DOTTED) {
            throw new IllegalArgumentException("Unsupported stroke style: " + style);
        }
        strokeStyle = style;
    }

    public int getStrokeStyle() {
        return strokeStyle;
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
        var text = AutoTranslate.translateForRender(string == null ? "" : string);
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

    public void drawImage(Image image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0) {
            return;
        }
        delegate.drawImage(image.awtImage(), x + translateX, y + translateY, width, height, null);
    }

    public void drawRegion(Image image, int xSrc, int ySrc, int width, int height, int transform, int xDest, int yDest, int anchor) {
        int drawWidth = swapsAxes(transform) ? height : width;
        int drawHeight = swapsAxes(transform) ? width : height;
        drawRegion(image, xSrc, ySrc, width, height, transform, xDest, yDest, drawWidth, drawHeight, anchor);
    }

    public void drawRegion(Image image, int xSrc, int ySrc, int width, int height, int transform, int xDest, int yDest, int widthDest, int heightDest, int anchor) {
        if (image == null || width <= 0 || height <= 0 || widthDest <= 0 || heightDest <= 0) {
            return;
        }

        BufferedImage sourceRegion = prepareDrawRegionSource(width, height);
        int clipLeft = Math.max(0, xSrc);
        int clipTop = Math.max(0, ySrc);
        int clipRight = Math.min(image.getWidth(), xSrc + width);
        int clipBottom = Math.min(image.getHeight(), ySrc + height);
        if (clipLeft < clipRight && clipTop < clipBottom) {
            int destLeft = clipLeft - xSrc;
            int destTop = clipTop - ySrc;
            drawRegionSourceGraphics.drawImage(
                    image.awtImage(),
                    destLeft,
                    destTop,
                    destLeft + (clipRight - clipLeft),
                    destTop + (clipBottom - clipTop),
                    clipLeft,
                    clipTop,
                    clipRight,
                    clipBottom,
                    null
            );
        }

        BufferedImage transformed = transform == 0 ? sourceRegion : transformRegion(sourceRegion, transform);
        int drawWidth = widthDest;
        int drawHeight = heightDest;
        int drawX = anchoredX(xDest + translateX, anchor, drawWidth);
        int drawY = anchoredYForImage(yDest + translateY, anchor, drawHeight);
        delegate.drawImage(transformed, drawX, drawY, drawWidth, drawHeight, null);
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        if (rgbData == null) {
            throw new NullPointerException("rgbData");
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage image = prepareDrawRgbScratch(width, height);
        if (processAlpha) {
            for (int row = 0; row < height; row++) {
                int rowStart = offset + row * scanlength;
                System.arraycopy(rgbData, rowStart, drawRgbPixels, row * width, width);
            }
        } else {
            int sourceRowStart = offset;
            int targetIndex = 0;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    drawRgbPixels[targetIndex++] = rgbData[sourceRowStart + column] | 0xFF000000;
                }
                sourceRowStart += scanlength;
            }
        }
        delegate.drawImage(image, x + translateX, y + translateY, null);
    }

    public void copyArea(int xSrc, int ySrc, int width, int height, int xDest, int yDest, int anchor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int sourceX = xSrc + translateX;
        int sourceY = ySrc + translateY;
        int targetX = anchoredX(xDest + translateX, anchor, width);
        int targetY = anchoredYForImage(yDest + translateY, anchor, height);
        delegate.copyArea(sourceX, sourceY, width, height, targetX - sourceX, targetY - sourceY);
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
            disposeDrawRegionScratch();
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
        strokeStyle = SOLID;
        delegate.setClip(0, 0, surfaceWidth, surfaceHeight);
        delegate.setFont(font.awtFont());
        setDelegateColor(argbColor);
    }

    public void clearSurface(int argbColor) {
        var previousComposite = delegate.getComposite();
        var previousClip = delegate.getClip();
        var previousColor = delegate.getColor();
        try {
            delegate.setComposite(AlphaComposite.Src);
            delegate.setClip(0, 0, surfaceWidth, surfaceHeight);
            delegate.setColor(cachedColor(argbColor));
            delegate.fillRect(0, 0, surfaceWidth, surfaceHeight);
        } finally {
            delegate.setComposite(previousComposite);
            delegate.setClip(previousClip);
            delegate.setColor(previousColor);
        }
    }

    private Rectangle clipBounds() {
        var bounds = delegate.getClipBounds();
        if (bounds == null) {
            return new Rectangle(0, 0, surfaceWidth, surfaceHeight);
        }
        return bounds;
    }

    private void setDelegateColor(int argb) {
        delegate.setColor(cachedColor(argb));
    }

    private Color cachedColor(int argb) {
        if (cachedColor == null || cachedColorArgb != argb) {
            cachedColorArgb = argb;
            cachedColor = new Color(argb, true);
        }
        return cachedColor;
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

    private static void validateColorComponent(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Color component out of range: " + value);
        }
    }

    private BufferedImage prepareDrawRegionSource(int width, int height) {
        if (drawRegionSourceScratch == null
                || drawRegionSourceScratch.getWidth() != width
                || drawRegionSourceScratch.getHeight() != height) {
            if (drawRegionSourceGraphics != null) {
                drawRegionSourceGraphics.dispose();
            }
            drawRegionSourceScratch = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            drawRegionSourceGraphics = drawRegionSourceScratch.createGraphics();
            drawRegionSourceGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
        }
        clearScratch(drawRegionSourceGraphics, width, height);
        return drawRegionSourceScratch;
    }

    private BufferedImage prepareDrawRegionTransform(int width, int height) {
        if (drawRegionTransformScratch == null
                || drawRegionTransformScratch.getWidth() != width
                || drawRegionTransformScratch.getHeight() != height) {
            if (drawRegionTransformGraphics != null) {
                drawRegionTransformGraphics.dispose();
            }
            drawRegionTransformScratch = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            drawRegionTransformGraphics = drawRegionTransformScratch.createGraphics();
            drawRegionTransformGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
        }
        clearScratch(drawRegionTransformGraphics, width, height);
        return drawRegionTransformScratch;
    }

    private void clearScratch(Graphics2D graphics, int width, int height) {
        graphics.setTransform(IDENTITY_TRANSFORM);
        graphics.setClip(0, 0, width, height);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private BufferedImage prepareDrawRgbScratch(int width, int height) {
        if (drawRgbScratch == null
                || drawRgbScratch.getWidth() != width
                || drawRgbScratch.getHeight() != height) {
            drawRgbScratch = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            drawRgbPixels = ((DataBufferInt) drawRgbScratch.getRaster().getDataBuffer()).getData();
        }
        return drawRgbScratch;
    }

    private void disposeDrawRegionScratch() {
        if (drawRegionSourceGraphics != null) {
            drawRegionSourceGraphics.dispose();
            drawRegionSourceGraphics = null;
        }
        if (drawRegionTransformGraphics != null) {
            drawRegionTransformGraphics.dispose();
            drawRegionTransformGraphics = null;
        }
        drawRegionSourceScratch = null;
        drawRegionTransformScratch = null;
    }

    private BufferedImage transformRegion(BufferedImage image, int transform) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int targetWidth = swapsAxes(transform) ? sourceHeight : sourceWidth;
        int targetHeight = swapsAxes(transform) ? sourceWidth : sourceHeight;
        BufferedImage transformed = prepareDrawRegionTransform(targetWidth, targetHeight);
        drawRegionTransformGraphics.transform(transformMatrix(transform, sourceWidth, sourceHeight));
        drawRegionTransformGraphics.drawImage(image, 0, 0, null);
        return transformed;
    }

    private static boolean swapsAxes(int transform) {
        return switch (transform) {
            case 4, 5, 6, 7 -> true;
            default -> false;
        };
    }

    private static AffineTransform transformMatrix(int transform, int width, int height) {
        return switch (transform) {
            case 1 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 5 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 6 -> new AffineTransform(0, -1, 1, 0, 0, width);
            case 7 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            default -> new AffineTransform();
        };
    }
}
