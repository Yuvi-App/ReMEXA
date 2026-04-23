package javax.microedition.lcdui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class BitmapFontBackend implements FontBackend {
    private static final String RESOURCE_ROOT = "/remexa/fonts/bitmap/";
    private static final int[] SUPPORTED_SIZES = {8, 10, 12, 16, 20, 24, 30};
    private static final int SPACE = 0x0020;
    private static final int IDEOGRAPHIC_SPACE = 0x3000;
    private static final int REPLACEMENT_CHARACTER = 0xFFFD;
    private static final java.awt.Font PLACEHOLDER_FONT =
            new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN, 12);
    private static final Map<Integer, Strike> STRIKES = loadStrikes();
    private static final int MAX_RENDER_CACHE_ENTRIES = 256;
    private static final Map<RenderKey, BufferedImage> RENDER_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<RenderKey, BufferedImage> eldest) {
            return size() > MAX_RENDER_CACHE_ENTRIES;
        }
    };

    private final Strike strike;

    private BitmapFontBackend(Strike strike) {
        this.strike = strike;
    }

    static BitmapFontBackend create(int requestedSize) {
        if (STRIKES.isEmpty()) {
            return null;
        }
        var strike = STRIKES.get(selectSize(mapLogicalSize(requestedSize)));
        return strike == null ? null : new BitmapFontBackend(strike);
    }

    @Override
    public int getHeight() {
        return strike.lineHeight();
    }

    @Override
    public int stringWidth(String value) {
        var normalized = Font.normalizeText(value);
        if (normalized.isEmpty()) {
            return 0;
        }
        var width = 0;
        for (var line : normalized.split("\\n", -1)) {
            width = Math.max(width, lineWidth(line));
        }
        return width;
    }

    @Override
    public int getAscent() {
        return strike.baseline();
    }

    @Override
    public int getDescent() {
        return strike.descent();
    }

    @Override
    public java.awt.Font awtFont() {
        return PLACEHOLDER_FONT;
    }

    @Override
    public void drawString(Graphics2D graphics, String text, int x, int baselineY, int argbColor) {
        var image = rendered(Font.normalizeText(text), argbColor);
        if (image != null) {
            graphics.drawImage(image, x, baselineY - strike.baseline(), null);
        }
    }

    private BufferedImage rendered(String value, int argbColor) {
        if (value.isEmpty()) {
            return null;
        }
        var key = new RenderKey(strike, argbColor, value);
        synchronized (RENDER_CACHE) {
            var cached = RENDER_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        var image = draw(value, argbColor);
        if (image == null) {
            return null;
        }
        synchronized (RENDER_CACHE) {
            RENDER_CACHE.put(key, image);
        }
        return image;
    }

    private BufferedImage draw(String text, int argbColor) {
        var lines = text.split("\\n", -1);
        var width = 0;
        for (var line : lines) {
            width = Math.max(width, lineWidth(line));
        }
        var height = lines.length * strike.lineHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            var cursorX = 0;
            var cursorY = lineIndex * strike.lineHeight();
            var codePoints = lines[lineIndex].codePoints().toArray();
            for (var codePoint : codePoints) {
                var advance = advanceFor(codePoint);
                var glyphIndex = glyphIndexFor(codePoint);
                if (glyphIndex >= 0) {
                    blitGlyph(pixels, width, height, cursorX, cursorY, glyphIndex, argbColor, advance);
                }
                cursorX += advance;
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private void blitGlyph(
            int[] target,
            int targetWidth,
            int targetHeight,
            int dx,
            int dy,
            int glyphIndex,
            int argbColor,
            int visibleWidth
    ) {
        var glyphOffset = glyphIndex * strike.bytesPerGlyph();
        for (var row = 0; row < strike.height(); row++) {
            var targetY = dy + row;
            if (targetY < 0 || targetY >= targetHeight) {
                continue;
            }
            var rowOffset = glyphOffset + row * strike.bytesPerRow();
            for (var byteIndex = 0; byteIndex < strike.bytesPerRow(); byteIndex++) {
                var bits = strike.glyphData()[rowOffset + byteIndex] & 0xFF;
                if (bits == 0) {
                    continue;
                }
                for (var bit = 0; bit < 8; bit++) {
                    if ((bits & (0x80 >>> bit)) == 0) {
                        continue;
                    }
                    var targetX = dx + byteIndex * 8 + bit;
                    if (targetX - dx >= visibleWidth || targetX < 0 || targetX >= targetWidth) {
                        continue;
                    }
                    target[targetY * targetWidth + targetX] = argbColor;
                }
            }
        }
    }

    private int lineWidth(String line) {
        var width = 0;
        for (var codePoint : line.codePoints().toArray()) {
            width += advanceFor(codePoint);
        }
        return width;
    }

    private int advanceFor(int codePoint) {
        var effective = effectiveCodePoint(codePoint);
        if (effective == SPACE) {
            return strike.halfAdvance();
        }
        if (effective == IDEOGRAPHIC_SPACE) {
            return strike.fullAdvance();
        }
        return isHalfWidth(effective) ? strike.halfAdvance() : strike.fullAdvance();
    }

    private int glyphIndexFor(int codePoint) {
        var effective = effectiveCodePoint(codePoint);
        if (effective == SPACE || effective == IDEOGRAPHIC_SPACE) {
            return -1;
        }
        return strike.codePointToGlyph().getOrDefault(effective, strike.questionMarkIndex());
    }

    private int effectiveCodePoint(int codePoint) {
        if (codePoint == SPACE || codePoint == IDEOGRAPHIC_SPACE) {
            return codePoint;
        }
        if (strike.codePointToGlyph().containsKey(codePoint)) {
            return codePoint;
        }
        return REPLACEMENT_CHARACTER;
    }

    private static boolean isHalfWidth(int codePoint) {
        return codePoint <= 0x00FF || (codePoint >= 0xFF61 && codePoint <= 0xFFDC);
    }

    private static int mapLogicalSize(int requestedSize) {
        return switch (requestedSize) {
            case Font.SIZE_SMALL -> 10;
            case Font.SIZE_LARGE -> 16;
            default -> 12;
        };
    }

    private static int selectSize(int requestedSize) {
        if (requestedSize <= SUPPORTED_SIZES[0]) {
            return SUPPORTED_SIZES[0];
        }
        var selected = SUPPORTED_SIZES[0];
        for (var size : SUPPORTED_SIZES) {
            if (requestedSize < size) {
                break;
            }
            selected = size;
        }
        return selected;
    }

    private static Map<Integer, Strike> loadStrikes() {
        try {
            var codePoints = loadCodePoints();
            Map<Integer, Strike> strikes = new HashMap<>();
            for (var height : SUPPORTED_SIZES) {
                var glyphData = readResource("glyphs-" + height + ".dat");
                var width = deriveWidth(height);
                var bytesPerRow = width / 8;
                var bytesPerGlyph = bytesPerRow * height;
                if ((glyphData.length % bytesPerGlyph) != 0) {
                    throw new IOException("Unexpected glyph file length for height " + height);
                }
                var glyphCount = glyphData.length / bytesPerGlyph;
                if (glyphCount > codePoints.length) {
                    throw new IOException("Glyph table for height " + height + " exceeds code-point table");
                }
                Map<Integer, Integer> codePointToGlyph = new HashMap<>(glyphCount * 2);
                for (var i = 0; i < glyphCount; i++) {
                    codePointToGlyph.put(codePoints[i], i);
                }
                var questionMarkIndex = codePointToGlyph.getOrDefault(REPLACEMENT_CHARACTER, -1);
                strikes.put(
                        height,
                        new Strike(
                                height,
                                width,
                                bytesPerRow,
                                bytesPerGlyph,
                                glyphData,
                                codePointToGlyph,
                                questionMarkIndex,
                                inferredBaseline(height),
                                inferredDescent(height),
                                height
                        )
                );
            }
            return strikes;
        } catch (IOException exception) {
            System.err.println("ReMEXA: bitmap font resources unavailable, falling back to system font: " + exception.getMessage());
            return Map.of();
        }
    }

    private static int[] loadCodePoints() throws IOException {
        var data = readResource("code-points.dat");
        if ((data.length & 1) != 0) {
            throw new IOException("Invalid code-point table length");
        }
        var codePoints = new int[data.length / 2];
        for (int i = 0, source = 0; i < codePoints.length; i++) {
            var low = data[source++] & 0xFF;
            var high = data[source++] & 0xFF;
            codePoints[i] = low | (high << 8);
        }
        return codePoints;
    }

    private static byte[] readResource(String name) throws IOException {
        try (InputStream input = BitmapFontBackend.class.getResourceAsStream(RESOURCE_ROOT + name)) {
            if (input == null) {
                throw new IOException("Missing bitmap font resource: " + name);
            }
            return input.readAllBytes();
        }
    }

    private static int deriveWidth(int height) {
        return ((height + 7) / 8) * 8;
    }

    private static int inferredBaseline(int height) {
        return height - inferredDescent(height);
    }

    private static int inferredDescent(int height) {
        return 2;
    }

    private record Strike(
            int height,
            int width,
            int bytesPerRow,
            int bytesPerGlyph,
            byte[] glyphData,
            Map<Integer, Integer> codePointToGlyph,
            int questionMarkIndex,
            int baseline,
            int descent,
            int lineHeight
    ) {
        private Strike {
            glyphData = Arrays.copyOf(glyphData, glyphData.length);
            codePointToGlyph = Map.copyOf(codePointToGlyph);
        }

        int fullAdvance() {
            return height;
        }

        int halfAdvance() {
            return height / 2;
        }
    }

    private record RenderKey(Strike strike, int argbColor, String text) {
    }
}
