package remexa.host.jad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AppDescriptorLoader {
    private AppDescriptorLoader() {
    }

    public static boolean supports(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        var name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jad") || name.endsWith(".kjx");
    }

    public static JadDescriptor load(Path path) throws IOException {
        if (!supports(path)) {
            throw new IOException("Only .jad and .kjx files can be opened in ReMEXA.");
        }
        var descriptor = path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".kjx")
                ? KjxParser.parse(path)
                : JadParser.parse(path);
        var jarPath = descriptor.resolveJarPath();
        if (jarPath.isEmpty() || !Files.exists(jarPath.get())) {
            return descriptor;
        }
        try {
            return JadManifestOverlay.merge(descriptor, jarPath.get());
        } catch (IOException exception) {
            throw new IOException("Failed to read JAR manifest.", exception);
        }
    }
}
