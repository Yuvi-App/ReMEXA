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
        return metrics().stringWidth(normalized);
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
        graphics.drawString(normalized, x, baselineY);
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
