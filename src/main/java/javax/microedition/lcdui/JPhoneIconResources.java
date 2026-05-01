package javax.microedition.lcdui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

final class JPhoneIconResources {
    private static final String RESOURCE_ROOT = "/remexa/images/jphone_icons/";
    private static final Map<Integer, Optional<BufferedImage>> CACHE = new ConcurrentHashMap<>();

    private JPhoneIconResources() {
    }

    static BufferedImage imageFor(int codePoint) {
        return CACHE.computeIfAbsent(codePoint, JPhoneIconResources::loadImage)
                .orElse(null);
    }

    static int scaledWidthFor(int codePoint, int targetHeight) {
        if (targetHeight <= 0) {
            return -1;
        }
        var image = imageFor(codePoint);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return -1;
        }
        return Math.max(1, Math.round((float) image.getWidth() * targetHeight / image.getHeight()));
    }

    static boolean hasIcon(int codePoint) {
        return imageFor(codePoint) != null;
    }

    private static Optional<BufferedImage> loadImage(int codePoint) {
        for (var resourceName : resourceNamesFor(codePoint)) {
            try (var input = JPhoneIconResources.class.getResourceAsStream(resourceName)) {
                if (input == null) {
                    continue;
                }
                var decoded = ImageIO.read(input);
                if (decoded != null) {
                    return Optional.of(decoded);
                }
            } catch (IOException ignored) {
                // Best-effort optional asset loading.
            }
        }
        return Optional.empty();
    }

    private static String[] resourceNamesFor(int codePoint) {
        var upper = String.format(Locale.ROOT, "u%04X", codePoint);
        var lower = String.format(Locale.ROOT, "u%04x", codePoint);
        return new String[]{
                RESOURCE_ROOT + upper + ".png",
                RESOURCE_ROOT + lower + ".png",
                RESOURCE_ROOT + upper + ".gif",
                RESOURCE_ROOT + lower + ".gif"
        };
    }
}
