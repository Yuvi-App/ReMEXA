package org.recompile.mobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.microedition.io.Connector;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

/**
 * Minimal FreeJ2ME compatibility shim used by the SMAF decoder port.
 */
public final class Mobile {
    public static final int LOG_DEBUG = 0;
    public static final int LOG_INFO = 1;
    public static final int LOG_WARNING = 2;
    public static final int LOG_ERROR = 3;

    private static final String AUDIO_LOG_LEVEL_PROPERTY = "remexa.audioLogLevel";

    public static volatile int minLogLevel = parseConfiguredLogLevel();

    private Mobile() {
    }

    public static void log(int level, String message) {
        if (level < minLogLevel) {
            return;
        }
        DebugLog.log(LogCategory.AUDIO, "org.recompile.mobile.Mobile", prefix(level) + message);
    }

    public static void configureLogLevel(String level) {
        minLogLevel = parseLogLevel(level);
    }

    public static byte[] getMIDletResourceAsByteArray(String url) throws IOException {
        try (InputStream input = getMIDletResourceAsStream(url)) {
            if (input == null) {
                throw new IOException("Missing MIDlet resource: " + url);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    public static InputStream getMIDletResourceAsStream(String url) {
        String raw = url == null ? "" : url.trim();
        if (raw.regionMatches(true, 0, "jar://", 0, "jar://".length())) {
            try {
                return Connector.openInputStream(raw);
            } catch (IOException exception) {
                DebugLog.log(
                        LogCategory.AUDIO,
                        Mobile.class.getName(),
                        "Jar phrase resource fallback for " + raw + ": " + exception.getMessage()
                );
            }
        }

        String path = normalizeResourcePath(raw);
        InputStream input = MidletRuntime.openResource(path);
        if (input != null) {
            return input;
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        input = open(path, contextLoader);
        if (input != null) {
            return input;
        }
        input = open(path, Mobile.class.getClassLoader());
        if (input != null) {
            return input;
        }
        return Mobile.class.getResourceAsStream("/" + path);
    }

    private static InputStream open(String path, ClassLoader loader) {
        if (loader == null) {
            return null;
        }
        InputStream input = loader.getResourceAsStream(path);
        if (input != null) {
            return input;
        }
        return loader.getResourceAsStream("/" + path);
    }

    private static String normalizeResourcePath(String url) {
        String path = url == null ? "" : url.trim();
        if (path.regionMatches(true, 0, "jar://", 0, "jar://".length())) {
            path = path.substring("jar://".length());
            int separator = path.indexOf('/');
            if (separator >= 0 && separator < path.length() - 1) {
                path = path.substring(separator + 1);
            } else {
                path = "";
            }
        }
        if (path.startsWith("resource:")) {
            path = path.substring("resource:".length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private static int parseConfiguredLogLevel() {
        return parseLogLevel(System.getProperty(AUDIO_LOG_LEVEL_PROPERTY, "warn"));
    }

    private static int parseLogLevel(String value) {
        if (value == null) {
            return LOG_WARNING;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "debug" -> LOG_DEBUG;
            case "info" -> LOG_INFO;
            case "error" -> LOG_ERROR;
            case "warn", "warning" -> LOG_WARNING;
            default -> LOG_WARNING;
        };
    }

    private static String prefix(int level) {
        return switch (level) {
            case LOG_DEBUG -> "[smaf:debug] ";
            case LOG_INFO -> "[smaf:info] ";
            case LOG_WARNING -> "[smaf:warn] ";
            case LOG_ERROR -> "[smaf:error] ";
            default -> "[smaf] ";
        };
    }
}
