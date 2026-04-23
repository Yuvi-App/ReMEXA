package remexa.host.jad;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record JadDescriptor(
        Path sourcePath,
        Map<String, String> properties,
        List<MidletEntry> midlets
) {
    public Optional<String> property(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    public String title() {
        return property("MIDlet-Name")
                .or(() -> property("AppName"))
                .or(() -> midlets.stream().findFirst().map(MidletEntry::name))
                .orElseGet(() -> sourcePath.getFileName().toString());
    }

    public Optional<String> entryClassName() {
        if (!midlets.isEmpty()) {
            return Optional.ofNullable(midlets.getFirst().className());
        }
        return property("AppClass")
                .or(() -> property("KVM-Class-Name"))
                .or(() -> property("Main-Class"));
    }

    public Optional<Path> resolveJarPath() {
        var configuredJar = property("MIDlet-Jar-URL")
                .or(() -> property("Jar-URL"))
                .or(() -> property("AppJar"))
                .flatMap(this::configuredJarPath);
        if (configuredJar.isPresent() && java.nio.file.Files.exists(configuredJar.get())) {
            return configuredJar;
        }

        var siblingFallback = siblingJarPath();
        if (siblingFallback.isPresent() && java.nio.file.Files.exists(siblingFallback.get())) {
            return siblingFallback;
        }

        return configuredJar.isPresent() ? configuredJar : siblingFallback;
    }

    private Optional<Path> configuredJarPath(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        var trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        var uriCandidate = toUri(trimmed);
        if (uriCandidate.isPresent() && uriCandidate.get().isAbsolute()) {
            var uri = uriCandidate.get();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                try {
                    return Optional.of(Path.of(uri).normalize());
                } catch (IllegalArgumentException exception) {
                    return Optional.empty();
                }
            }

            var fileName = fileNameFromUri(uri);
            if (fileName.isEmpty()) {
                return Optional.empty();
            }
            return resolveAgainstSourceParent(fileName);
        }

        try {
            var directPath = Path.of(trimmed);
            if (directPath.isAbsolute()) {
                return Optional.of(directPath.normalize());
            }
        } catch (InvalidPathException ignored) {
            return Optional.empty();
        }

        return resolveAgainstSourceParent(trimmed);
    }

    private Optional<Path> resolveAgainstSourceParent(String fileName) {
        var parent = sourcePath.getParent();
        if (parent == null) {
            try {
                return Optional.of(Path.of(fileName).normalize());
            } catch (InvalidPathException exception) {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(parent.resolve(fileName).normalize());
        } catch (InvalidPathException exception) {
            return Optional.empty();
        }
    }

    private static Optional<URI> toUri(String rawValue) {
        try {
            return Optional.of(URI.create(rawValue));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String fileNameFromUri(URI uri) {
        var path = uri.getPath();
        if (path == null || path.isBlank()) {
            return "";
        }
        var separator = path.lastIndexOf('/');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private Optional<Path> siblingJarPath() {
        var fileName = sourcePath.getFileName();
        if (fileName == null) {
            return Optional.empty();
        }

        var jadName = fileName.toString();
        var extensionIndex = jadName.lastIndexOf('.');
        var jarName = extensionIndex >= 0
                ? jadName.substring(0, extensionIndex) + ".jar"
                : jadName + ".jar";
        return Optional.of(sourcePath.getParent().resolve(jarName).normalize());
    }

    public Optional<String> iconPath() {
        return property("MIDlet-Icon")
                .or(() -> midlets.stream()
                        .map(MidletEntry::icon)
                        .filter(icon -> icon != null && !icon.isBlank())
                        .findFirst())
                .filter(icon -> !icon.isBlank());
    }

    public List<String> summaryLines() {
        var lines = new ArrayList<String>();
        lines.add("Title: " + title());
        property("MIDlet-Vendor").ifPresent(value -> lines.add("Vendor: " + value));
        property("MIDlet-Version").ifPresent(value -> lines.add("Version: " + value));
        property("MIDlet-Resident").ifPresent(value -> lines.add("Resident: " + value));
        iconPath().ifPresent(value -> lines.add("Icon: " + value));
        resolveJarPath().ifPresent(value -> lines.add("Jar: " + value));
        entryClassName().ifPresent(value -> lines.add("Entry: " + value));
        return List.copyOf(lines);
    }
}
