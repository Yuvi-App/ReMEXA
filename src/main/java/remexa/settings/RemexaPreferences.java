package remexa.settings;

import java.util.prefs.Preferences;

public final class RemexaPreferences {
    public static final String ROOT_PATH = "remexa";

    public static final String UI_NODE = "ui";
    public static final String DEBUG_NODE = "debug";
    public static final String LOG_NODE = "log";
    public static final String RECENT_JADS_NODE = "recent-jads";

    public static final String SHOW_HOST_DETAILS_KEY = "showHostDetails";
    public static final String FONT_TYPE_KEY = "fontType";
    public static final String JSKY_PHONE_TYPE_KEY = "jskyPhoneType";
    public static final String VODAFONE_PHONE_TYPE_KEY = "vodafonePhoneType";
    public static final String MEXA_PHONE_TYPE_KEY = "mexaPhoneType";
    public static final String SMAF_SYNTH_TYPE_KEY = "smafSynthType";
    public static final String HOST_SCALE_KEY = "hostScale";
    public static final String FRAME_RATE_KEY = "frameRate";
    public static final String TOUCH_CONTROLS_ENABLED_KEY = "touchControlsEnabled";
    public static final String FLASH_BACKLIGHT_ENABLED_KEY = "flashBacklightEnabled";
    public static final String LIVE_TRANSLATION_ENABLED_KEY = "liveTranslationEnabled";
    public static final String DEEPL_API_PLAN_KEY = "deeplApiPlan";
    public static final String DEEPL_API_KEY_KEY = "deeplApiKey";
    public static final String DEEPL_TARGET_LANGUAGE_KEY = "deeplTargetLanguage";
    public static final String BLUETOOTH_BACKEND_KEY = "bluetoothBackend";
    public static final String BLUETOOTH_ROLE_KEY = "bluetoothRole";
    public static final String BLUETOOTH_LOCAL_NAME_KEY = "bluetoothLocalName";
    public static final String BLUETOOTH_REMOTE_HOST_KEY = "bluetoothRemoteHost";
    public static final String BLUETOOTH_PORT_KEY = "bluetoothPort";
    public static final String DUMP_RMS_KEY = "dumpRms";
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

    public static Preferences debug() {
        return ROOT.node(DEBUG_NODE);
    }

    public static Preferences recentJads() {
        return ROOT.node(RECENT_JADS_NODE);
    }
}
