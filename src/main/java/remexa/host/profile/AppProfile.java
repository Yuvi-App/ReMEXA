package remexa.host.profile;

import java.util.Map;

public record AppProfile(
        String id,
        String displayName,
        DisplayMetrics fallbackDisplay,
        int deviceStyle,
        Map<String, String> systemProperties
) {
    public static AppProfile generic() {
        return new AppProfile(
                "generic-midp",
                "Generic MIDP",
                new DisplayMetrics(240, 320, "Generic fallback"),
                0,
                Map.of()
        );
    }

    public static AppProfile jsky(String oclVersion) {
        var normalizedVersion = oclVersion == null || oclVersion.isBlank() ? "JSCL" : oclVersion;
        return new AppProfile(
                "jsky-" + normalizedVersion.toLowerCase(),
                "JSKY / " + normalizedVersion,
                new DisplayMetrics(120, 128, "JSKY fallback"),
                com.j_phone.system.DeviceControl.STYLE_PORTRAIT,
                Map.ofEntries(
                        Map.entry("Platform", "JSKY-Generic"),
                        Map.entry("microedition.platform", "JSKY-Generic"),
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
                        Map.entry("jscl.supports.suspend_javaexecution", "false")
                )
        );
    }
}
