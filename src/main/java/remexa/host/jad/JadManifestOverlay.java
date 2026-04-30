package remexa.host.jad;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

public final class JadManifestOverlay {
    private static final List<String> DISPLAY_SIZE_KEYS = List.of(
            "MIDlet-Display-Size",
            "MIDlet-DisplaySize",
            "MIDlet-Screen-Size",
            "MIDlet-ScreenSize",
            "Display-Size",
            "DisplaySize",
            "Screen-Size",
            "Canvas-Size",
            "App-Display-Size",
            "AppSize",
            "MIDxlet-Display-Size",
            "MIDxlet-DisplaySize",
            "MIDxlet-Screen-Size",
            "MIDxlet-ScreenSize"
    );

    private JadManifestOverlay() {
    }

    public static JadDescriptor merge(JadDescriptor descriptor, Path jarPath) throws IOException {
        try (var jarFile = new JarFile(jarPath.toFile())) {
            var manifest = jarFile.getManifest();
            if (manifest == null) {
                return descriptor;
            }

            var mergedProperties = new LinkedHashMap<String, String>();
            mergedProperties.putAll(descriptor.properties());
            for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
                var key = String.valueOf(entry.getKey());
                var value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (isOverriddenByJad(key, descriptor.properties())) {
                    continue;
                }
                mergedProperties.put(key, value.toString());
            }

            return new JadDescriptor(descriptor.sourcePath(), Map.copyOf(mergedProperties), descriptor.midlets());
        }
    }

    private static boolean isOverriddenByJad(String manifestKey, Map<String, String> jadProperties) {
        if (jadProperties.containsKey(manifestKey)) {
            return true;
        }

        for (var jadKey : jadProperties.keySet()) {
            if (jadKey.equalsIgnoreCase(manifestKey)) {
                return true;
            }
        }

        return belongsToSameAliasGroup(manifestKey, jadProperties, DISPLAY_SIZE_KEYS);
    }

    private static boolean belongsToSameAliasGroup(String manifestKey, Map<String, String> jadProperties, List<String> aliasGroup) {
        if (!containsIgnoreCase(aliasGroup, manifestKey)) {
            return false;
        }
        for (var jadKey : jadProperties.keySet()) {
            if (containsIgnoreCase(aliasGroup, jadKey)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> keys, String candidate) {
        for (var key : keys) {
            if (key.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
