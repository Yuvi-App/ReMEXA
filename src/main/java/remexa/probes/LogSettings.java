package remexa.probes;

import java.util.EnumMap;
import java.util.Map;
import remexa.settings.RemexaPreferences;

public final class LogSettings {
    private static final boolean DEFAULT_LOG_ENABLED = false;
    private static final LogCategory[] CATEGORIES = LogCategory.values();
    private static final boolean[] ENABLED = loadEnabledStates();

    private LogSettings() {
    }

    public static boolean isEnabled(LogCategory category) {
        return ENABLED[category.ordinal()];
    }

    public static void setEnabled(LogCategory category, boolean enabled) {
        ENABLED[category.ordinal()] = enabled;
        RemexaPreferences.log().putBoolean(key(category), enabled);
        SdkStubSupport.refreshTraceEnabled();
    }

    public static boolean areAllEnabled() {
        for (boolean enabled : ENABLED) {
            if (!enabled) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAnyEnabled() {
        for (boolean enabled : ENABLED) {
            if (enabled) {
                return true;
            }
        }
        return false;
    }

    public static void setAllEnabled(boolean enabled) {
        for (var category : CATEGORIES) {
            ENABLED[category.ordinal()] = enabled;
            RemexaPreferences.log().putBoolean(key(category), enabled);
        }
        SdkStubSupport.refreshTraceEnabled();
    }

    public static Map<LogCategory, Boolean> loadAll() {
        var values = new EnumMap<LogCategory, Boolean>(LogCategory.class);
        for (var category : CATEGORIES) {
            values.put(category, isEnabled(category));
        }
        return Map.copyOf(values);
    }

    private static boolean[] loadEnabledStates() {
        var enabled = new boolean[CATEGORIES.length];
        var preferences = RemexaPreferences.log();
        for (var category : CATEGORIES) {
            enabled[category.ordinal()] = preferences.getBoolean(key(category), DEFAULT_LOG_ENABLED);
        }
        return enabled;
    }

    private static String key(LogCategory category) {
        return RemexaPreferences.LOG_ENABLED_PREFIX + category.name();
    }
}
