package remexa.host.jad;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class JadParser {
    private JadParser() {
    }

    public static JadDescriptor parse(Path path) throws IOException {
        var properties = parseProperties(path);
        var midlets = parseMidlets(properties);
        DebugLog.log(LogCategory.JAD, JadParser.class.getName(), "Parsed JAD: " + path.toAbsolutePath());
        return new JadDescriptor(path.toAbsolutePath(), properties, midlets);
    }

    private static Map<String, String> parseProperties(Path path) throws IOException {
        var content = Files.readString(path, detectCharset(path));
        var logicalLines = unfoldLines(content.lines().toList());
        var properties = new LinkedHashMap<String, String>();
        for (var line : logicalLines) {
            if (line.isBlank()) {
                continue;
            }
            var separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            var key = line.substring(0, separator).trim();
            var value = line.substring(separator + 1).trim();
            properties.put(key, value);
        }
        return Map.copyOf(properties);
    }

    private static Charset detectCharset(Path path) throws IOException {
        var bytes = Files.readAllBytes(path);
        var utf8 = StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        if (!utf8.contains("\uFFFD")) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName("Shift_JIS");
    }

    private static List<String> unfoldLines(List<String> lines) {
        var result = new ArrayList<String>();
        String current = null;
        for (var line : lines) {
            if (line.startsWith(" ")) {
                current = current == null ? line.trim() : current + line.substring(1);
                continue;
            }
            if (current != null) {
                result.add(current);
            }
            current = line;
        }
        if (current != null) {
            result.add(current);
        }
        return result;
    }

    private static List<MidletEntry> parseMidlets(Map<String, String> properties) {
        var entries = new ArrayList<MidletEntry>();
        for (var entry : properties.entrySet()) {
            if (!entry.getKey().startsWith("MIDlet-")) {
                continue;
            }
            var suffix = entry.getKey().substring("MIDlet-".length());
            if (!suffix.chars().allMatch(Character::isDigit)) {
                continue;
            }
            var index = Integer.parseInt(suffix);
            var parts = entry.getValue().split(",", 3);
            var name = parts.length > 0 ? parts[0].trim() : "MIDlet-" + index;
            var icon = parts.length > 1 ? parts[1].trim() : "";
            var className = parts.length > 2 ? parts[2].trim() : "";
            entries.add(new MidletEntry(index, name, icon, className));
        }
        entries.sort(java.util.Comparator.comparingInt(MidletEntry::index));
        return List.copyOf(entries);
    }
}
