package remexa.host.jad;

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
                .map(value -> sourcePath.getParent().resolve(value).normalize());
        if (configuredJar.isPresent() && java.nio.file.Files.exists(configuredJar.get())) {
            return configuredJar;
        }

        var siblingFallback = siblingJarPath();
        if (siblingFallback.isPresent() && java.nio.file.Files.exists(siblingFallback.get())) {
            return siblingFallback;
        }

        return configuredJar.isPresent() ? configuredJar : siblingFallback;
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
