package remexa.host.profile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import remexa.host.jad.JadDescriptor;
import remexa.host.LaunchConfig;

public final class LaunchProfileResolver {
    private static final List<String> DISPLAY_KEYS = List.of(
            "MIDlet-Display-Size",
            "MIDlet-DisplaySize",
            "MIDlet-Screen-Size",
            "MIDlet-ScreenSize",
            "Display-Size",
            "DisplaySize",
            "Screen-Size",
            "Canvas-Size",
            "App-Display-Size",
            "AppSize",
            "MIDxlet-Display-Size",
            "MIDxlet-DisplaySize",
            "MIDxlet-Screen-Size",
            "MIDxlet-ScreenSize"
    );
    private static final List<String> PLATFORM_KEYS = List.of(
            "microedition.platform",
            "Platform"
    );
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)\\D+(\\d+)");

    private LaunchProfileResolver() {
    }

    public static LaunchProfile resolve(JadDescriptor descriptor) {
        var profile = resolveProfile(descriptor);
        var initialDisplay = resolveDisplayMetrics(descriptor, profile)
                .orElse(profile.fallbackDisplay());
        return new LaunchProfile(profile, initialDisplay);
    }

    private static AppProfile resolveProfile(JadDescriptor descriptor) {
        var ocl = descriptor.property("MIDlet-OCL").orElse("");
        if (isJskyFamily(ocl)) {
            var normalizedOcl = primaryOclToken(ocl);
            var platform = resolveDeclaredPlatform(descriptor).orElse("");
            if (looksLikeVodafonePlatform(platform) || looksLikeVodafoneVendor(descriptor)) {
                return AppProfile.vodafone(normalizedOcl, resolveVodafonePhoneType(platform));
            }
            if (looksLikeVodafoneOcl(normalizedOcl)) {
                return AppProfile.vodafone(normalizedOcl, resolveVodafonePhoneType(platform));
            }
            return AppProfile.jsky(normalizedOcl, resolveJskyPhoneType(platform));
        }
        return AppProfile.generic();
    }

    private static boolean isJskyFamily(String ocl) {
        if (ocl == null) {
            return false;
        }
        var normalized = ocl.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("JSCL-") || normalized.startsWith("JOCL-");
    }

    private static String primaryOclToken(String ocl) {
        if (ocl == null) {
            return "";
        }
        var separator = ocl.indexOf(',');
        return separator >= 0 ? ocl.substring(0, separator).trim() : ocl.trim();
    }

    private static boolean looksLikeVodafoneOcl(String ocl) {
        if (ocl == null || ocl.isBlank()) {
            return false;
        }
        var normalized = ocl.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.startsWith("JSCL-")) {
            return false;
        }
        var version = normalized.substring("JSCL-".length()).trim();
        return compareVersion(version, "1.2.0") >= 0;
    }

    private static int compareVersion(String left, String right) {
        var leftParts = left.split("[^0-9]+");
        var rightParts = right.split("[^0-9]+");
        var maxLength = Math.max(leftParts.length, rightParts.length);
        for (var index = 0; index < maxLength; index++) {
            var leftValue = parseVersionPart(leftParts, index);
            var rightValue = parseVersionPart(rightParts, index);
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        var part = parts[index];
        if (part == null || part.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Optional<String> resolveDeclaredPlatform(JadDescriptor descriptor) {
        for (var key : PLATFORM_KEYS) {
            var value = descriptor.property(key)
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty());
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static boolean looksLikeVodafonePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return false;
        }
        var normalized = platform.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("V") || normalized.contains("VODAFONE");
    }

    private static boolean looksLikeVodafoneVendor(JadDescriptor descriptor) {
        return descriptor.property("MIDlet-Vendor")
                .map(String::trim)
                .map(value -> value.toUpperCase(java.util.Locale.ROOT))
                .filter(value -> value.contains("VODAFONE") || value.contains("SOFTBANK"))
                .isPresent();
    }

    private static LaunchConfig.JskyPhoneType resolveJskyPhoneType(String platform) {
        var declared = LaunchConfig.JskyPhoneType.fromId(platform);
        return declared == null ? LaunchConfig.JskyPhoneType.resolveConfigured() : declared;
    }

    private static LaunchConfig.VodafonePhoneType resolveVodafonePhoneType(String platform) {
        var declared = LaunchConfig.VodafonePhoneType.fromId(platform);
        return declared == null ? LaunchConfig.VodafonePhoneType.resolveConfigured() : declared;
    }

    private static Optional<DisplayMetrics> resolveDisplayMetrics(JadDescriptor descriptor, AppProfile profile) {
        for (var key : DISPLAY_KEYS) {
            var parsed = descriptor.property(key).flatMap(value -> parseDisplayMetrics(value, key));
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<DisplayMetrics> parseDisplayMetrics(String rawValue, String key) {
        var matcher = SIZE_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return Optional.empty();
        }
        var width = Integer.parseInt(matcher.group(1));
        var height = Integer.parseInt(matcher.group(2));
        return Optional.of(new DisplayMetrics(width, height, "JAD " + key));
    }
}
