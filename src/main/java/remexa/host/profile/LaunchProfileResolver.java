package remexa.host.profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import remexa.host.input.InputProfile;
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
    private static final List<String> API_KEYS = List.of(
            "MIDlet-OCL",
            "MIDxlet-API",
            "MIDlet-API"
    );
    private static final List<String> MICROEDITION_PROFILE_KEYS = List.of(
            "MicroEdition-Profile",
            "microedition.profile"
    );
    private static final List<String> MICROEDITION_CONFIGURATION_KEYS = List.of(
            "MicroEdition-Configuration",
            "microedition.configuration"
    );
    private static final List<String> WIDESCREEN_KEYS = List.of(
            "MIDxlet-WideScreen",
            "MIDlet-WideScreen"
    );
    private static final Map<String, String> JSCL_CAPABILITY_OVERRIDES = Map.ofEntries(
            Map.entry("MIDxlet-MSensor", "jscl.supports.msensor"),
            Map.entry("MIDlet-MSensor", "jscl.supports.msensor")
    );
    private static final List<String> JPHONE_API_MARKERS = List.of("com/j_phone/", "com.j_phone.");
    private static final List<String> VODAFONE_API_MARKERS = List.of("com/vodafone/", "com.vodafone.");
    private static final List<String> MEXA_API_MARKERS = List.of("com/mexa/", "com.mexa.");
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)\\D+(\\d+)");

    private LaunchProfileResolver() {
    }

    public static LaunchProfile resolve(JadDescriptor descriptor) {
        var profile = resolveProfile(descriptor);
        var wideScreen = resolveWideScreen(descriptor);
        var initialDisplay = resolveDisplayMetrics(descriptor, profile)
                .map(displayMetrics -> applyWideScreen(displayMetrics, wideScreen))
                .orElseGet(() -> applyWideScreen(profile.fallbackDisplay(), wideScreen));
        return new LaunchProfile(profile, initialDisplay, shouldRotateInputForWideScreen(profile, wideScreen));
    }

    private static boolean shouldRotateInputForWideScreen(AppProfile profile, boolean wideScreen) {
        return wideScreen
                && LaunchConfig.resolveConfiguredRotateWidescreenKeysEnabled()
                && profile.inputProfile() == InputProfile.MEXA;
    }

    private static AppProfile resolveProfile(JadDescriptor descriptor) {
        var platform = resolveDeclaredPlatform(descriptor).orElse("");
        var ocl = resolveDeclaredApi(descriptor).orElse("");
        var normalizedOcl = primaryOclToken(ocl);
        var apiHints = scanJarApiHints(descriptor);
        if (hasMidxletProperties(descriptor) || apiHints.mexaApi() || looksLikeMexaOcl(normalizedOcl)) {
            return applyDescriptorCapabilityOverrides(
                    descriptor,
                    AppProfile.mexa(profileApiLabel(normalizedOcl, "MEXA-API"), resolveMexaPhoneType(platform))
            );
        }
        if (isJskyFamily(ocl) || apiHints.jPhoneApi() || apiHints.vodafoneApi()) {
            AppProfile profile;
            if (looksLikeMexaPlatform(platform) || looksLikeMexaVendor(descriptor) || looksLikeMexaOcl(normalizedOcl)) {
                profile = AppProfile.mexa(profileApiLabel(normalizedOcl, "MEXA-API"), resolveMexaPhoneType(platform));
            } else if (looksLikeVodafonePlatform(platform) || looksLikeVodafoneVendor(descriptor) || apiHints.vodafoneApi()) {
                profile = AppProfile.vodafone(profileApiLabel(normalizedOcl, "VODAFONE-API"), resolveVodafonePhoneType(platform));
            } else if (looksLikeVodafoneOcl(normalizedOcl)) {
                profile = AppProfile.vodafone(profileApiLabel(normalizedOcl, "VODAFONE-API"), resolveVodafonePhoneType(platform));
            } else {
                profile = AppProfile.jsky(profileApiLabel(normalizedOcl, "JPHONE-API"), resolveJskyPhoneType(platform));
            }
            return applyDescriptorCapabilityOverrides(descriptor, profile);
        }
        if (looksLikeMexaPlatform(platform) || looksLikeMexaVendor(descriptor)) {
            return applyDescriptorCapabilityOverrides(
                    descriptor,
                    AppProfile.mexa(profileApiLabel(normalizedOcl, "MEXA-API"), resolveMexaPhoneType(platform))
            );
        }
        if (looksLikeVodafonePlatform(platform) || looksLikeVodafoneVendor(descriptor)) {
            return applyDescriptorCapabilityOverrides(
                    descriptor,
                    AppProfile.vodafone(profileApiLabel(normalizedOcl, "VODAFONE-API"), resolveVodafonePhoneType(platform))
            );
        }
        return AppProfile.generic();
    }

    private static String profileApiLabel(String normalizedOcl, String fallback) {
        if (normalizedOcl == null || normalizedOcl.isBlank()) {
            return fallback;
        }
        return normalizedOcl;
    }

    private static ApiHints scanJarApiHints(JadDescriptor descriptor) {
        var jarPath = descriptor.resolveJarPath();
        if (jarPath.isEmpty() || !Files.exists(jarPath.get())) {
            return ApiHints.NONE;
        }
        var jPhoneApi = false;
        var vodafoneApi = false;
        var mexaApi = false;
        try (var jarFile = new JarFile(jarPath.get().toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (var inputStream = jarFile.getInputStream(entry)) {
                    var classBytes = inputStream.readAllBytes();
                    jPhoneApi |= containsAnyMarker(classBytes, JPHONE_API_MARKERS);
                    vodafoneApi |= containsAnyMarker(classBytes, VODAFONE_API_MARKERS);
                    mexaApi |= containsAnyMarker(classBytes, MEXA_API_MARKERS);
                }
                if (jPhoneApi && vodafoneApi && mexaApi) {
                    break;
                }
            }
        } catch (IOException | SecurityException ignored) {
            return ApiHints.NONE;
        }
        return new ApiHints(jPhoneApi, vodafoneApi, mexaApi);
    }

    private static boolean containsAnyMarker(byte[] bytes, List<String> markers) {
        for (var marker : markers) {
            if (containsBytes(bytes, marker.getBytes(StandardCharsets.ISO_8859_1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBytes(byte[] bytes, byte[] marker) {
        if (bytes == null || marker == null || marker.length == 0 || bytes.length < marker.length) {
            return false;
        }
        var limit = bytes.length - marker.length;
        for (var index = 0; index <= limit; index++) {
            var matched = true;
            for (var markerIndex = 0; markerIndex < marker.length; markerIndex++) {
                if (bytes[index + markerIndex] != marker[markerIndex]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMidxletProperties(JadDescriptor descriptor) {
        for (var key : descriptor.properties().keySet()) {
            if (key != null && key.regionMatches(true, 0, "MIDxlet", 0, "MIDxlet".length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJskyFamily(String ocl) {
        if (ocl == null) {
            return false;
        }
        var normalized = ocl.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("JSCL-") || normalized.startsWith("JOCL-");
    }

    private static Optional<String> resolveDeclaredApi(JadDescriptor descriptor) {
        for (var key : API_KEYS) {
            var value = descriptor.property(key)
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty());
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static boolean resolveWideScreen(JadDescriptor descriptor) {
        for (var key : WIDESCREEN_KEYS) {
            var wideScreen = descriptor.property(key)
                    .flatMap(LaunchProfileResolver::parseDescriptorBoolean);
            if (wideScreen.isPresent()) {
                return wideScreen.get();
            }
        }
        return false;
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

    private static boolean looksLikeMexaOcl(String ocl) {
        if (ocl == null || ocl.isBlank()) {
            return false;
        }
        var normalized = ocl.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MEXA")) {
            return true;
        }
        if (!normalized.startsWith("JSCL-")) {
            return false;
        }
        var version = normalized.substring("JSCL-".length()).trim();
        return compareVersion(version, "1.3.2") > 0;
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
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> value.contains("VODAFONE") || value.contains("SOFTBANK"))
                .isPresent();
    }

    private static boolean looksLikeMexaPlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return false;
        }
        var normalized = platform.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("MEXA")) {
            return true;
        }
        for (var phoneType : LaunchConfig.MexaPhoneType.values()) {
            if (normalized.contains(phoneType.platformName().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeMexaVendor(JadDescriptor descriptor) {
        return descriptor.property("MIDlet-Vendor")
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> value.contains("MEXA"))
                .isPresent();
    }

    private static AppProfile applyDescriptorCapabilityOverrides(JadDescriptor descriptor, AppProfile profile) {
        var overrides = new LinkedHashMap<String, String>();
        resolveMicroEditionProperty(descriptor, MICROEDITION_PROFILE_KEYS)
                .ifPresent(value -> overrides.put("microedition.profiles", normalizeMicroEditionValue(value)));
        resolveMicroEditionProperty(descriptor, MICROEDITION_CONFIGURATION_KEYS)
                .ifPresent(value -> overrides.put("microedition.configuration", normalizeMicroEditionValue(value)));
        for (var entry : JSCL_CAPABILITY_OVERRIDES.entrySet()) {
            descriptor.property(entry.getKey())
                    .flatMap(LaunchProfileResolver::parseDescriptorBoolean)
                    .ifPresent(value -> overrides.put(entry.getValue(), Boolean.toString(value)));
        }
        return profile.withSystemProperties(overrides);
    }

    private static Optional<String> resolveMicroEditionProperty(JadDescriptor descriptor, List<String> keys) {
        for (var key : keys) {
            var value = descriptor.property(key)
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty());
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static String normalizeMicroEditionValue(String value) {
        if (value == null) {
            return "";
        }
        var trimmed = value.trim();
        if (trimmed.endsWith(".0")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }
        return trimmed;
    }

    private static Optional<Boolean> parseDescriptorBoolean(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        return switch (rawValue.trim().toUpperCase(Locale.ROOT)) {
            case "Y", "YES", "TRUE", "1", "ON" -> Optional.of(true);
            case "N", "NO", "FALSE", "0", "OFF" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static LaunchConfig.JskyPhoneType resolveJskyPhoneType(String platform) {
        var declared = LaunchConfig.JskyPhoneType.fromId(platform);
        return declared == null ? LaunchConfig.JskyPhoneType.resolveConfigured() : declared;
    }

    private static LaunchConfig.VodafonePhoneType resolveVodafonePhoneType(String platform) {
        var declared = LaunchConfig.VodafonePhoneType.fromId(platform);
        return declared == null ? LaunchConfig.VodafonePhoneType.resolveConfigured() : declared;
    }

    private static LaunchConfig.MexaPhoneType resolveMexaPhoneType(String platform) {
        var declared = LaunchConfig.MexaPhoneType.fromId(platform);
        if (declared != null) {
            return declared;
        }
        if (platform != null) {
            var normalized = platform.trim().toUpperCase(Locale.ROOT);
            if (normalized.contains(LaunchConfig.MexaPhoneType.SHARP_930SH.platformName().toUpperCase(Locale.ROOT))) {
                return LaunchConfig.MexaPhoneType.SHARP_930SH;
            }
        }
        return LaunchConfig.MexaPhoneType.resolveConfigured();
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
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new DisplayMetrics(width, height, "JAD " + key));
    }

    private static DisplayMetrics applyWideScreen(DisplayMetrics displayMetrics, boolean wideScreen) {
        if (!wideScreen) {
            return displayMetrics;
        }
        return new DisplayMetrics(
                displayMetrics.height(),
                displayMetrics.width(),
                displayMetrics.source() + " (WideScreen)"
        );
    }

    private record ApiHints(boolean jPhoneApi, boolean vodafoneApi, boolean mexaApi) {
        private static final ApiHints NONE = new ApiHints(false, false, false);
    }
}
