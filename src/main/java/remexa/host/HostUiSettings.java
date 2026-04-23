package remexa.host;

import remexa.settings.RemexaPreferences;

public final class HostUiSettings {
    private HostUiSettings() {
    }

    public static boolean showHostDetails() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.SHOW_HOST_DETAILS_KEY, false);
    }

    public static void setShowHostDetails(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.SHOW_HOST_DETAILS_KEY, enabled);
    }

    public static LaunchConfig.FontType fontType() {
        return LaunchConfig.FontType.normalize(
                RemexaPreferences.ui().get(RemexaPreferences.FONT_TYPE_KEY, LaunchConfig.FontType.BITMAP.id())
        );
    }

    public static void setFontType(LaunchConfig.FontType fontType) {
        var resolved = fontType == null ? LaunchConfig.FontType.BITMAP : fontType;
        RemexaPreferences.ui().put(RemexaPreferences.FONT_TYPE_KEY, resolved.id());
    }

    public static LaunchConfig.JskyPhoneType jskyPhoneType() {
        return LaunchConfig.JskyPhoneType.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.JSKY_PHONE_TYPE_KEY,
                        LaunchConfig.JskyPhoneType.GENERIC.id()
                )
        );
    }

    public static void setJskyPhoneType(LaunchConfig.JskyPhoneType jskyPhoneType) {
        var resolved = jskyPhoneType == null ? LaunchConfig.JskyPhoneType.GENERIC : jskyPhoneType;
        RemexaPreferences.ui().put(RemexaPreferences.JSKY_PHONE_TYPE_KEY, resolved.id());
    }
}
