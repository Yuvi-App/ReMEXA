package javax.microedition.lcdui;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Locale;

final class SystemFontBackend implements FontBackend {
    private static final BufferedImage METRICS_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private final java.awt.Font awtFont;

    private SystemFontBackend(java.awt.Font awtFont) {
        this.awtFont = awtFont;
    }

    static SystemFontBackend create(int face, int style, int size) {
        var family = resolveFamily(face);
        var awtStyle = java.awt.Font.PLAIN;
        if ((style & Font.STYLE_BOLD) != 0) {
            awtStyle |= java.awt.Font.BOLD;
        }
        if ((style & Font.STYLE_ITALIC) != 0) {
            awtStyle |= java.awt.Font.ITALIC;
        }
        var awtSize = switch (size) {
            case Font.SIZE_SMALL -> 8;
            case Font.SIZE_LARGE -> 14;
            default -> 11;
        };
        return new SystemFontBackend(new java.awt.Font(family, awtStyle, awtSize));
    }

    @Override
    public int getHeight() {
        return metrics().getAscent() + metrics().getDescent();
    }

    @Override
    public int stringWidth(String value) {
        var normalized = Font.normalizeText(value);
        if (normalized.isEmpty()) {
            return 0;
        }
        var metrics = metrics();
        var width = 0;
        for (var line : normalized.split("\\n", -1)) {
            width = Math.max(width, lineWidth(metrics, line));
        }
        return width;
    }

    @Override
    public int getAscent() {
        return metrics().getAscent();
    }

    @Override
    public int getDescent() {
        return metrics().getDescent();
    }

    @Override
    public java.awt.Font awtFont() {
        return awtFont;
    }

    @Override
    public void drawString(Graphics2D graphics, String text, int x, int baselineY, int argbColor) {
        var normalized = Font.normalizeText(text);
        if (normalized.isEmpty()) {
            return;
        }
        graphics.setFont(awtFont);
        graphics.setColor(new java.awt.Color(argbColor, true));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (!containsInlineIcons(normalized)) {
            graphics.drawString(normalized, x, baselineY);
            return;
        }

        var metrics = metrics();
        var lines = normalized.split("\\n", -1);
        var lineHeight = getHeight();
        var lineTop = baselineY - getAscent();
        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            var baseline = baselineY + lineIndex * lineHeight;
            var cursorX = x;
            var segment = new StringBuilder();
            var line = lines[lineIndex];
            for (var offset = 0; offset < line.length(); ) {
                var codePoint = line.codePointAt(offset);
                offset += Character.charCount(codePoint);
                var icon = JPhoneIconResources.imageFor(codePoint);
                var iconWidth = iconWidthFor(codePoint, lineHeight);
                if (icon != null && iconWidth > 0) {
                    if (!segment.isEmpty()) {
                        var textSegment = segment.toString();
                        graphics.drawString(textSegment, cursorX, baseline);
                        cursorX += metrics.stringWidth(textSegment);
                        segment.setLength(0);
                    }
                    graphics.drawImage(icon, cursorX, lineTop + lineIndex * lineHeight, iconWidth, lineHeight, null);
                    cursorX += iconWidth;
                    continue;
                }
                segment.appendCodePoint(codePoint);
            }
            if (!segment.isEmpty()) {
                graphics.drawString(segment.toString(), cursorX, baseline);
            }
        }
    }

    private int lineWidth(java.awt.FontMetrics metrics, String line) {
        var width = 0;
        for (var offset = 0; offset < line.length(); ) {
            var codePoint = line.codePointAt(offset);
            offset += Character.charCount(codePoint);
            var iconWidth = iconWidthFor(codePoint, getHeight());
            if (iconWidth > 0) {
                width += iconWidth;
                continue;
            }
            width += metrics.stringWidth(new String(Character.toChars(codePoint)));
        }
        return width;
    }

    private boolean containsInlineIcons(String value) {
        for (var offset = 0; offset < value.length(); ) {
            var codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (JPhoneIconResources.hasIcon(codePoint)) {
                return true;
            }
        }
        return false;
    }

    private int iconWidthFor(int codePoint, int targetHeight) {
        return JPhoneIconResources.scaledWidthFor(codePoint, targetHeight);
    }

    private java.awt.FontMetrics metrics() {
        var graphics = METRICS_IMAGE.createGraphics();
        try {
            graphics.setFont(awtFont);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            return graphics.getFontMetrics();
        } finally {
            graphics.dispose();
        }
    }

    private static String resolveFamily(int face) {
        String[] preferred = switch (face) {
            case Font.FACE_MONOSPACE -> new String[]{"MS Gothic", "MS PGothic", "Yu Gothic UI", java.awt.Font.MONOSPACED};
            case Font.FACE_PROPORTIONAL -> new String[]{"MS UI Gothic", "MS Gothic", "Yu Gothic UI", java.awt.Font.SANS_SERIF};
            default -> new String[]{"MS Gothic", "MS UI Gothic", "Yu Gothic UI", java.awt.Font.DIALOG};
        };
        var available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
        for (var candidate : preferred) {
            for (var installed : available) {
                if (installed.equalsIgnoreCase(candidate)) {
                    return installed;
                }
            }
        }
        return preferred[preferred.length - 1];
    }
}
