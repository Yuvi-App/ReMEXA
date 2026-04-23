package remexa.settings;

import java.util.prefs.Preferences;

public final class RemexaPreferences {
    public static final String ROOT_PATH = "remexa";

    public static final String UI_NODE = "ui";
    public static final String LOG_NODE = "log";
    public static final String RECENT_JADS_NODE = "recent-jads";

    public static final String SHOW_HOST_DETAILS_KEY = "showHostDetails";
    public static final String FONT_TYPE_KEY = "fontType";
    public static final String LOG_ENABLED_PREFIX = "enabled.";
    public static final String RECENT_ENTRY_PREFIX = "entry.";

    private static final Preferences ROOT = Preferences.userRoot().node(ROOT_PATH);

    private RemexaPreferences() {
    }

    public static Preferences ui() {
        return ROOT.node(UI_NODE);
    }

    public static Preferences log() {
        return ROOT.node(LOG_NODE);
    }

    public static Preferences recentJads() {
        return ROOT.node(RECENT_JADS_NODE);
    }
}
