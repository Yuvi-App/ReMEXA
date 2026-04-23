package remexa.probes;

import java.util.EnumMap;
import java.util.Map;
import remexa.settings.RemexaPreferences;

public final class LogSettings {
    private LogSettings() {
    }

    public static boolean isEnabled(LogCategory category) {
        return RemexaPreferences.log().getBoolean(key(category), true);
    }

    public static void setEnabled(LogCategory category, boolean enabled) {
        RemexaPreferences.log().putBoolean(key(category), enabled);
    }

    public static boolean areAllEnabled() {
        for (var category : LogCategory.values()) {
            if (!isEnabled(category)) {
                return false;
            }
        }
        return true;
    }

    public static void setAllEnabled(boolean enabled) {
        for (var category : LogCategory.values()) {
            setEnabled(category, enabled);
        }
    }

    public static Map<LogCategory, Boolean> loadAll() {
        var values = new EnumMap<LogCategory, Boolean>(LogCategory.class);
        for (var category : LogCategory.values()) {
            values.put(category, isEnabled(category));
        }
        return Map.copyOf(values);
    }

    private static String key(LogCategory category) {
        return RemexaPreferences.LOG_ENABLED_PREFIX + category.name();
    }
}
