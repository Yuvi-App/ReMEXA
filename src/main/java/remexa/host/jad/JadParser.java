package remexa.host.jad;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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
        var bytes = Files.readAllBytes(path);
        var utf8 = parseCandidate(bytes, StandardCharsets.UTF_8);
        if (utf8.strict() && looksLikeValidJad(utf8.properties())) {
            return utf8.properties();
        }

        var candidates = List.of(
                utf8,
                parseCandidate(bytes, Charset.forName("windows-31j")),
                parseCandidate(bytes, Charset.forName("Shift_JIS"))
        );

        var bestStrict = candidates.stream()
                .filter(Candidate::strict)
                .max(java.util.Comparator.comparingInt(candidate -> score(candidate.properties())));
        if (bestStrict.isPresent()) {
            return bestStrict.get().properties();
        }

        return candidates.stream()
                .max(java.util.Comparator.comparingInt(candidate -> score(candidate.properties())))
                .map(Candidate::properties)
                .orElseGet(Map::of);
    }

    private static String decode(byte[] bytes, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static Candidate parseCandidate(byte[] bytes, Charset charset) {
        var strict = decodeStrict(bytes, charset);
        var text = strict.orElseGet(() -> decode(bytes, charset));
        return new Candidate(charset, parseProperties(text), strict.isPresent());
    }

    private static java.util.Optional<String> decodeStrict(byte[] bytes, Charset charset) {
        var decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
            return java.util.Optional.of(decoded.toString());
        } catch (CharacterCodingException exception) {
            return java.util.Optional.empty();
        }
    }

    private static Map<String, String> parseProperties(String content) {
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
            var key = stripBom(line.substring(0, separator).trim());
            var value = line.substring(separator + 1).trim();
            properties.put(key, value);
        }
        return Map.copyOf(properties);
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

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
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

    private static int score(Map<String, String> properties) {
        var score = 0;
        if (properties.containsKey("MIDlet-1")) {
            score += 20;
        }
        if (properties.containsKey("MIDlet-Name")) {
            score += 10;
        }
        if (properties.containsKey("MIDlet-Jar-URL")) {
            score += 10;
        }
        score += parseMidlets(properties).size() * 20;
        score += scoreTextQuality(properties.get("MIDlet-Name"));
        return score;
    }

    private static int scoreTextQuality(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        var score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\uFFFD') {
                score -= 10;
            } else if ((ch >= '\u3040' && ch <= '\u30FF') || (ch >= '\uFF61' && ch <= '\uFF9F')) {
                score += 2;
            } else if ((ch >= '\u3400' && ch <= '\u4DBF') || (ch >= '\u4E00' && ch <= '\u9FFF')) {
                score += 1;
            } else if (ch >= '\u0000' && ch <= '\u007F') {
                score += 0;
            } else if ((ch >= '\u00C0' && ch <= '\u024F') || (ch >= '\u0300' && ch <= '\u036F')) {
                score -= 1;
            }
        }
        return score;
    }

    private static boolean looksLikeValidJad(Map<String, String> properties) {
        if (properties.isEmpty()) {
            return false;
        }
        if (properties.containsKey("MIDlet-Name") || properties.containsKey("MIDlet-1")) {
            return true;
        }
        return properties.containsKey("MIDlet-Jar-URL")
                || properties.containsKey("AppClass")
                || properties.containsKey("Main-Class");
    }

    private record Candidate(Charset charset, Map<String, String> properties, boolean strict) {
    }
}
