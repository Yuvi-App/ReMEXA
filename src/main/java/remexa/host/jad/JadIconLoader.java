package remexa.host.jad;

import java.awt.Image;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class JadIconLoader {
    private JadIconLoader() {
    }

    public static Optional<Image> load(JadDescriptor descriptor) {
        var jarPath = descriptor.resolveJarPath();
        var iconPath = descriptor.iconPath();
        if (jarPath.isEmpty() || iconPath.isEmpty()) {
            return Optional.empty();
        }

        var resolvedJar = jarPath.get();
        if (!Files.exists(resolvedJar)) {
            return Optional.empty();
        }

        var normalizedIconPath = normalizeJarEntryPath(iconPath.get());
        try (var jarFile = new JarFile(resolvedJar.toFile())) {
            var entry = findEntry(jarFile, normalizedIconPath);
            if (entry == null) {
                DebugLog.log(
                        LogCategory.HOST,
                        JadIconLoader.class.getName(),
                        "JAD icon not found in jar: " + normalizedIconPath
                );
                return Optional.empty();
            }

            try (InputStream input = jarFile.getInputStream(entry)) {
                var image = ImageIO.read(input);
                if (image == null) {
                    DebugLog.log(
                            LogCategory.HOST,
                            JadIconLoader.class.getName(),
                            "Unsupported JAD icon format: " + normalizedIconPath
                    );
                    return Optional.empty();
                }
                DebugLog.log(
                        LogCategory.HOST,
                        JadIconLoader.class.getName(),
                        "Loaded JAD icon from jar: " + normalizedIconPath
                );
                return Optional.of(image);
            }
        } catch (Exception exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    JadIconLoader.class.getName(),
                    "Failed to load JAD icon: " + exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private static JarEntry findEntry(JarFile jarFile, String normalizedIconPath) {
        var direct = jarFile.getJarEntry(normalizedIconPath);
        if (direct != null) {
            return direct;
        }

        var lowerCaseNeedle = normalizedIconPath.toLowerCase(java.util.Locale.ROOT);
        return jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().toLowerCase(java.util.Locale.ROOT).equals(lowerCaseNeedle))
                .findFirst()
                .orElse(null);
    }

    private static String normalizeJarEntryPath(String iconPath) {
        var normalized = iconPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
