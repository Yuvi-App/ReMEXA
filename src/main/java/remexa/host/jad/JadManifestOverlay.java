package remexa.host.jad;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

public final class JadManifestOverlay {
    private JadManifestOverlay() {
    }

    public static JadDescriptor merge(JadDescriptor descriptor, Path jarPath) throws IOException {
        try (var jarFile = new JarFile(jarPath.toFile())) {
            var manifest = jarFile.getManifest();
            if (manifest == null) {
                return descriptor;
            }

            var mergedProperties = new LinkedHashMap<String, String>();
            for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
                var key = String.valueOf(entry.getKey());
                var value = entry.getValue();
                if (value == null) {
                    continue;
                }
                mergedProperties.put(key, value.toString());
            }
            mergedProperties.putAll(descriptor.properties());

            return new JadDescriptor(descriptor.sourcePath(), Map.copyOf(mergedProperties), descriptor.midlets());
        }
    }
}
