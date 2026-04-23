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
        return property("MIDlet-Jar-URL")
                .or(() -> property("Jar-URL"))
                .or(() -> property("AppJar"))
                .map(value -> sourcePath.getParent().resolve(value).normalize());
    }

    public List<String> summaryLines() {
        var lines = new ArrayList<String>();
        lines.add("Title: " + title());
        property("MIDlet-Vendor").ifPresent(value -> lines.add("Vendor: " + value));
        property("MIDlet-Version").ifPresent(value -> lines.add("Version: " + value));
        property("MIDlet-Resident").ifPresent(value -> lines.add("Resident: " + value));
        resolveJarPath().ifPresent(value -> lines.add("Jar: " + value));
        entryClassName().ifPresent(value -> lines.add("Entry: " + value));
        return List.copyOf(lines);
    }
}
