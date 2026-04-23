package remexa.host.profile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import remexa.host.jad.JadDescriptor;

public final class LaunchProfileResolver {
    private static final List<String> DISPLAY_KEYS = List.of(
            "MIDlet-Display-Size",
            "MIDlet-Screen-Size",
            "Display-Size",
            "DisplaySize",
            "Screen-Size",
            "Canvas-Size",
            "App-Display-Size",
            "AppSize"
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
        if (ocl.startsWith("JSCL-")) {
            return AppProfile.jsky(ocl);
        }
        return AppProfile.generic();
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
