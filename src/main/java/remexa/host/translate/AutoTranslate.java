package remexa.host.translate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import remexa.host.LaunchConfig;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class AutoTranslate {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ExecutorService TRANSLATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "remexa-auto-translate");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, String> READY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> PENDING_TEXT = new ConcurrentHashMap<>();
    private static final Pattern TRANSLATED_TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
    private static final long RETRY_DELAY_MS = 60_000L;
    private static volatile String activeConfigurationKey = "";

    private AutoTranslate() {
    }

    public static String translateForRender(String source) {
        if (source == null || source.isBlank()) {
            return source == null ? "" : source;
        }
        refreshConfigurationState();
        if (!LaunchConfig.resolveConfiguredLiveTranslationEnabled()) {
            return source;
        }
        var apiKey = LaunchConfig.resolveConfiguredDeepLApiKey();
        if (apiKey.isBlank()) {
            return source;
        }
        if (!looksJapanese(source) || isProbablyFragment(source)) {
            return source;
        }

        var cached = READY_CACHE.get(source);
        if (cached != null) {
            return cached;
        }

        var now = System.currentTimeMillis();
        var pendingUntil = PENDING_TEXT.get(source);
        if (pendingUntil != null && pendingUntil > now) {
            return source;
        }

        PENDING_TEXT.put(source, now + RETRY_DELAY_MS);
        TRANSLATION_EXECUTOR.submit(() -> translateAsync(source, apiKey));
        return source;
    }

    private static void refreshConfigurationState() {
        var configurationKey = LaunchConfig.resolveConfiguredLiveTranslationEnabled()
                + "|" + LaunchConfig.DeepLApiPlan.resolveConfigured().id()
                + "|" + LaunchConfig.TranslationTargetLanguage.resolveConfigured().code()
                + "|" + LaunchConfig.resolveConfiguredDeepLApiKey();
        if (configurationKey.equals(activeConfigurationKey)) {
            return;
        }
        synchronized (AutoTranslate.class) {
            if (configurationKey.equals(activeConfigurationKey)) {
                return;
            }
            READY_CACHE.clear();
            PENDING_TEXT.clear();
            activeConfigurationKey = configurationKey;
        }
    }

    private static void translateAsync(String source, String apiKey) {
        try {
            var translated = requestDeepLTranslation(source, apiKey);
            if (translated == null || translated.isBlank()) {
                return;
            }
            var normalized = translated.strip();
            READY_CACHE.put(source, normalized);
            PENDING_TEXT.remove(source);
            if (!normalized.equals(source)) {
                DebugLog.log(
                        LogCategory.UI,
                        AutoTranslate.class.getName(),
                        "Translated text: " + compact(source) + " -> " + compact(normalized)
                );
            }
            requestActiveCanvasRepaint();
        } catch (Exception exception) {
            DebugLog.log(
                    LogCategory.UI,
                    AutoTranslate.class.getName(),
                    "Auto-translate failed for " + compact(source) + ": " + exception.getMessage()
            );
        }
    }

    private static String requestDeepLTranslation(String source, String apiKey) throws Exception {
        var body = "text=" + encode(source)
                + "&source_lang=JA"
                + "&target_lang=" + encode(LaunchConfig.TranslationTargetLanguage.resolveConfigured().code())
                + "&preserve_formatting=1";
        var request = HttpRequest.newBuilder(URI.create(LaunchConfig.DeepLApiPlan.resolveConfigured().endpoint()))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "DeepL-Auth-Key " + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DeepL returned HTTP " + response.statusCode() + ": " + compact(response.body()));
        }
        return parseTranslatedText(response.body());
    }

    private static String parseTranslatedText(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        var matcher = TRANSLATED_TEXT_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static String unescapeJsonString(String value) {
        var builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            var ch = value.charAt(index);
            if (ch != '\\' || index + 1 >= value.length()) {
                builder.append(ch);
                continue;
            }
            var escaped = value.charAt(++index);
            switch (escaped) {
                case '"', '\\', '/' -> builder.append(escaped);
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        return builder.toString();
                    }
                    var hex = value.substring(index + 1, index + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                }
                default -> builder.append(escaped);
            }
        }
        return builder.toString();
    }

    private static void requestActiveCanvasRepaint() {
        var displayable = MidletRuntime.currentDisplayable();
        if (!(displayable instanceof javax.microedition.lcdui.Canvas canvas) || !canvas.isShown()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                canvas.repaint();
            } catch (Throwable ignored) {
                // Best-effort refresh.
            }
        });
    }

    private static boolean isProbablyFragment(String source) {
        return source.codePointCount(0, source.length()) <= 1;
    }

    private static boolean looksJapanese(String source) {
        for (int offset = 0; offset < source.length(); ) {
            var codePoint = source.codePointAt(offset);
            offset += Character.charCount(codePoint);
            var block = Character.UnicodeBlock.of(codePoint);
            if (block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                return true;
            }
        }
        return false;
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        var compacted = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (compacted.length() <= 120) {
            return compacted;
        }
        return compacted.substring(0, 117) + "...";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
