package remexa.host.profile;

import java.util.Map;
import java.util.Set;
import java.util.Locale;
import remexa.host.input.InputProfile;

public record AppProfile(
        String id,
        String displayName,
        InputProfile inputProfile,
        DisplayMetrics fallbackDisplay,
        int deviceStyle,
        Map<String, String> systemProperties
) {
    private static final Set<String> MANAGED_SYSTEM_PROPERTY_KEYS = Set.of(
            "Platform",
            "microedition.locale",
            "microedition.platform",
            "microedition.configuration",
            "microedition.profiles",
            "microedition.m3g.version",
            "jscl.system.mannermode",
            "jscl.system.offlinemode",
            "jscl.system.javasetting.volume",
            "jscl.system.javasetting.vibration",
            "jscl.system.wakeupmode",
            "jscl.system.btswitchsetting",
            "jscl.system.btjavasetting",
            "jscl.system.btvisibilitysetting",
            "jscl.system.display.colordepth",
            "jscl.system.e-fep_startposition",
            "jscl.supports.subdisplay",
            "jscl.supports.subdisplay.dualdraw",
            "jscl.supports.external_storage",
            "jscl.supports.barcode",
            "jscl.supports.irda",
            "jscl.supports.remote_control",
            "jscl.supports.voice_recognition",
            "jscl.supports.tv",
            "jscl.supports.tv_reserve",
            "jscl.supports.karaoke",
            "jscl.supports.msensor",
            "jscl.supports.serial",
            "jscl.supports.suspend_javaexecution",
            "mexa.system.resumemode",
            "mexa.supports.irsimple",
            "mexa.supports.maxobexsize",
            "mexa.supports.transmissionrate",
            "mexa.network.configuration"
    );

    public static AppProfile generic() {
        return new AppProfile(
                "generic-midp",
                "Generic MIDP",
                InputProfile.GENERIC,
                new DisplayMetrics(240, 320, "Generic fallback"),
                0,
                Map.of(
                        "microedition.locale", configuredLocale(),
                        "microedition.m3g.version", "1.1"
                )
        );
    }

    public static AppProfile jsky(String oclVersion, remexa.host.LaunchConfig.JskyPhoneType phoneType) {
        var normalizedVersion = oclVersion == null || oclVersion.isBlank() ? "JSCL" : oclVersion;
        var resolvedPhoneType = phoneType == null ? remexa.host.LaunchConfig.JskyPhoneType.GENERIC : phoneType;
        return new AppProfile(
                "jsky-" + normalizedVersion.toLowerCase() + "-" + resolvedPhoneType.id(),
                "JSKY / " + normalizedVersion + " / " + resolvedPhoneType.platformName(),
                InputProfile.JSKY,
                new DisplayMetrics(120, 130, "JSKY fallback"),
                com.j_phone.system.DeviceControl.STYLE_PORTRAIT,
                jsclSystemProperties(resolvedPhoneType.platformName())
        );
    }

    public static AppProfile vodafone(String oclVersion, remexa.host.LaunchConfig.VodafonePhoneType phoneType) {
        var normalizedVersion = oclVersion == null || oclVersion.isBlank() ? "JSCL" : oclVersion;
        var resolvedPhoneType = phoneType == null ? remexa.host.LaunchConfig.VodafonePhoneType.GENERIC : phoneType;
        var fallbackDisplay = resolvedPhoneType == remexa.host.LaunchConfig.VodafonePhoneType.V604SH
                ? new DisplayMetrics(240, 320, "Vodafone V604SH fallback")
                : new DisplayMetrics(240, 320, "Vodafone fallback");
        return new AppProfile(
                "vodafone-" + normalizedVersion.toLowerCase() + "-" + resolvedPhoneType.id(),
                "Vodafone / " + normalizedVersion + " / " + resolvedPhoneType.platformName(),
                InputProfile.VODAFONE,
                fallbackDisplay,
                com.j_phone.system.DeviceControl.STYLE_PORTRAIT,
                jsclSystemProperties(resolvedPhoneType.platformName())
        );
    }

    public static AppProfile mexa(String oclVersion, remexa.host.LaunchConfig.MexaPhoneType phoneType) {
        var normalizedVersion = oclVersion == null || oclVersion.isBlank() ? "JSCL" : oclVersion;
        var resolvedPhoneType = phoneType == null ? remexa.host.LaunchConfig.MexaPhoneType.GENERIC : phoneType;
        var fallbackDisplay = resolvedPhoneType == remexa.host.LaunchConfig.MexaPhoneType.SHARP_930SH
                ? new DisplayMetrics(240, 400, "MEXA 930SH fallback")
                : new DisplayMetrics(240, 400, "MEXA fallback");
        return new AppProfile(
                "mexa-" + normalizedVersion.toLowerCase() + "-" + resolvedPhoneType.id(),
                "MEXA / " + normalizedVersion + " / " + resolvedPhoneType.platformName(),
                InputProfile.MEXA,
                fallbackDisplay,
                com.j_phone.system.DeviceControl.STYLE_PORTRAIT,
                mexaSystemProperties(resolvedPhoneType.platformName())
        );
    }

    public static Set<String> managedSystemPropertyKeys() {
        return MANAGED_SYSTEM_PROPERTY_KEYS;
    }

    public AppProfile withSystemProperties(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return this;
        }
        var merged = new java.util.LinkedHashMap<String, String>(systemProperties);
        merged.putAll(overrides);
        return new AppProfile(id, displayName, inputProfile, fallbackDisplay, deviceStyle, Map.copyOf(merged));
    }

    private static Map<String, String> jsclSystemProperties(String platformName) {
        return Map.ofEntries(
                Map.entry("Platform", platformName),
                Map.entry("microedition.locale", configuredLocale()),
                Map.entry("microedition.platform", platformName),
                Map.entry("microedition.configuration", "CLDC-1.0"),
                Map.entry("microedition.profiles", "MIDP-1.0"),
                Map.entry("microedition.m3g.version", "1.1"),
                Map.entry("jscl.system.mannermode", "false"),
                Map.entry("jscl.system.offlinemode", "false"),
                Map.entry("jscl.system.javasetting.volume", "5"),
                Map.entry("jscl.system.javasetting.vibration", "1"),
                Map.entry("jscl.system.wakeupmode", "1"),
                Map.entry("jscl.system.btswitchsetting", "false"),
                Map.entry("jscl.system.btjavasetting", "false"),
                Map.entry("jscl.system.btvisibilitysetting", "false"),
                Map.entry("jscl.system.display.colordepth", "565"),
                Map.entry("jscl.system.e-fep_startposition", "0"),
                Map.entry("jscl.supports.subdisplay", "false"),
                Map.entry("jscl.supports.subdisplay.dualdraw", "false"),
                Map.entry("jscl.supports.external_storage", "false"),
                Map.entry("jscl.supports.barcode", "0"),
                Map.entry("jscl.supports.irda", "false"),
                Map.entry("jscl.supports.remote_control", "false"),
                Map.entry("jscl.supports.voice_recognition", "false"),
                Map.entry("jscl.supports.tv", "false"),
                Map.entry("jscl.supports.tv_reserve", "0"),
                Map.entry("jscl.supports.karaoke", "false"),
                Map.entry("jscl.supports.msensor", "false"),
                Map.entry("jscl.supports.serial", "false"),
                Map.entry("jscl.supports.suspend_javaexecution", "false")
        );
    }

    private static Map<String, String> mexaSystemProperties(String platformName) {
        var properties = new java.util.LinkedHashMap<String, String>(jsclSystemProperties(platformName));
        properties.put("mexa.system.resumemode", "0");
        properties.put("mexa.supports.irsimple", "0");
        properties.put("mexa.supports.maxobexsize", "0");
        properties.put("mexa.supports.transmissionrate", "false");
        properties.put("mexa.network.configuration", "0");
        return Map.copyOf(properties);
    }

    private static String configuredLocale() {
        var configured = System.getProperty("remexa.microedition.locale");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }

        var locale = Locale.getDefault();
        var language = locale.getLanguage();
        if (language == null || language.isBlank()) {
            return "en";
        }
        var country = locale.getCountry();
        if (country == null || country.isBlank()) {
            return language;
        }
        return language + "-" + country;
    }
}





















































