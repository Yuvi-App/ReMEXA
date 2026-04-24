package remexa.host.profile;

import java.util.Map;
import java.util.Set;

public record AppProfile(
        String id,
        String displayName,
        DisplayMetrics fallbackDisplay,
        int deviceStyle,
        Map<String, String> systemProperties
) {
    private static final Set<String> MANAGED_SYSTEM_PROPERTY_KEYS = Set.of(
            "Platform",
            "microedition.platform",
            "jscl.system.mannermode",
            "jscl.system.offlinemode",
            "jscl.system.javasetting.volume",
            "jscl.system.javasetting.vibration",
            "jscl.system.wakeupmode",
            "jscl.system.display.colordepth",
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
            "jscl.supports.suspend_javaexecution"
    );

    public static AppProfile generic() {
        return new AppProfile(
                "generic-midp",
                "Generic MIDP",
                new DisplayMetrics(240, 320, "Generic fallback"),
                0,
                Map.of()
        );
    }

    public static AppProfile jsky(String oclVersion, remexa.host.LaunchConfig.JskyPhoneType phoneType) {
        var normalizedVersion = oclVersion == null || oclVersion.isBlank() ? "JSCL" : oclVersion;
        var resolvedPhoneType = phoneType == null ? remexa.host.LaunchConfig.JskyPhoneType.GENERIC : phoneType;
        return new AppProfile(
                "jsky-" + normalizedVersion.toLowerCase() + "-" + resolvedPhoneType.id(),
                "JSKY / " + normalizedVersion + " / " + resolvedPhoneType.platformName(),
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
                fallbackDisplay,
                com.j_phone.system.DeviceControl.STYLE_PORTRAIT,
                jsclSystemProperties(resolvedPhoneType.platformName())
        );
    }

    public static Set<String> managedSystemPropertyKeys() {
        return MANAGED_SYSTEM_PROPERTY_KEYS;
    }

    private static Map<String, String> jsclSystemProperties(String platformName) {
        return Map.ofEntries(
                Map.entry("Platform", platformName),
                Map.entry("microedition.platform", platformName),
                Map.entry("jscl.system.mannermode", "false"),
                Map.entry("jscl.system.offlinemode", "false"),
                Map.entry("jscl.system.javasetting.volume", "5"),
                Map.entry("jscl.system.javasetting.vibration", "1"),
                Map.entry("jscl.system.wakeupmode", "1"),
                Map.entry("jscl.system.display.colordepth", "565"),
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
}





















































