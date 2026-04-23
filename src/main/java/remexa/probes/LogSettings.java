package remexa.probes;

import java.util.EnumMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class LogSettings {
    private static final Preferences PREFERENCES = Preferences.userRoot().node("remexa/log-settings");

    private LogSettings() {
    }

    public static boolean isEnabled(LogCategory category) {
        return PREFERENCES.getBoolean(key(category), true);
    }

    public static void setEnabled(LogCategory category, boolean enabled) {
        PREFERENCES.putBoolean(key(category), enabled);
    }

    public static Map<LogCategory, Boolean> loadAll() {
        var values = new EnumMap<LogCategory, Boolean>(LogCategory.class);
        for (var category : LogCategory.values()) {
            values.put(category, isEnabled(category));
        }
        return Map.copyOf(values);
    }

    private static String key(LogCategory category) {
        return "enabled." + category.name();
    }
}
