package remexa.audio.smaf;

import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogSettings;

public final class SmafDebug {
    public enum Level {
        DEBUG,
        INFO
    }

    private static final String AUDIO_LOG_LEVEL_PROPERTY = "remexa.audioLogLevel";

    private SmafDebug() {
    }

    public static boolean isEnabled(String channel, Level level) {
        return LogSettings.isEnabled(LogCategory.AUDIO) && level.ordinal() >= configuredLevel().ordinal();
    }

    public static void debug(String channel, String message) {
        if (isEnabled(channel, Level.DEBUG)) {
            DebugLog.log(LogCategory.AUDIO, source(channel), message);
        }
    }

    public static void info(String channel, String message) {
        if (isEnabled(channel, Level.INFO)) {
            DebugLog.log(LogCategory.AUDIO, source(channel), message);
        }
    }

    private static Level configuredLevel() {
        String configured = System.getProperty(AUDIO_LOG_LEVEL_PROPERTY, "warn");
        return "debug".equalsIgnoreCase(configured) ? Level.DEBUG : Level.INFO;
    }

    private static String source(String channel) {
        return channel == null || channel.isBlank()
                ? "remexa.audio.smaf"
                : "remexa.audio.smaf." + channel;
    }
}
