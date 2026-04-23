package remexa.host;

import java.util.Locale;

public final class LaunchConfig {
    public static final String FONT_TYPE_PROPERTY = "remexa.fontType";
    public static final String JSKY_PHONE_TYPE_PROPERTY = "remexa.jskyPhoneType";
    public static final String HOST_SCALE_PROPERTY = "remexa.hostScale";
    public static final int MIN_HOST_SCALE = 1;
    public static final int MAX_HOST_SCALE = 5;

    private LaunchConfig() {
    }

    public enum FontType {
        BITMAP("bitmap", "Bitmap"),
        SYSTEM("system", "System (SJIS)");

        private final String id;
        private final String label;

        FontType(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() {
            return id;
        }

        @Override
        public String toString() {
            return label;
        }

        public static FontType fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var type : values()) {
                if (type.id.equals(normalized)) {
                    return type;
                }
            }
            return null;
        }

        public static FontType normalize(String candidate) {
            var type = fromId(candidate);
            return type == null ? BITMAP : type;
        }

        public static FontType resolveConfigured() {
            return normalize(System.getProperty(FONT_TYPE_PROPERTY, BITMAP.id));
        }
    }

    public static void applyFontType(FontType fontType) {
        var resolved = fontType == null ? FontType.BITMAP : fontType;
        System.setProperty(FONT_TYPE_PROPERTY, resolved.id());
    }

    public enum JskyPhoneType {
        GENERIC("generic", "JSKY-Generic", "JSKY-Generic"),
        J_SH53("j-sh53", "J-SH53", "J-SH53");

        private final String id;
        private final String label;
        private final String platformName;

        JskyPhoneType(String id, String label, String platformName) {
            this.id = id;
            this.label = label;
            this.platformName = platformName;
        }

        public String id() {
            return id;
        }

        public String platformName() {
            return platformName;
        }

        @Override
        public String toString() {
            return label;
        }

        public static JskyPhoneType fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var type : values()) {
                if (type.id.equals(normalized)
                        || type.platformName.toLowerCase(Locale.ROOT).equals(normalized)
                        || type.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return type;
                }
            }
            return null;
        }

        public static JskyPhoneType normalize(String candidate) {
            var type = fromId(candidate);
            return type == null ? GENERIC : type;
        }

        public static JskyPhoneType resolveConfigured() {
            return normalize(System.getProperty(JSKY_PHONE_TYPE_PROPERTY, GENERIC.id));
        }
    }

    public static void applyJskyPhoneType(JskyPhoneType jskyPhoneType) {
        var resolved = jskyPhoneType == null ? JskyPhoneType.GENERIC : jskyPhoneType;
        System.setProperty(JSKY_PHONE_TYPE_PROPERTY, resolved.id());
    }

    public static Integer parseHostScale(String candidate) {
        if (candidate == null) {
            return null;
        }
        var normalized = candidate.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("x")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            var parsed = Integer.parseInt(normalized);
            return isSupportedHostScale(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static int normalizeHostScale(String candidate) {
        var parsed = parseHostScale(candidate);
        return parsed == null ? 3 : parsed;
    }

    public static int resolveConfiguredHostScale() {
        return normalizeHostScale(System.getProperty(HOST_SCALE_PROPERTY, "3"));
    }

    public static void applyHostScale(Integer hostScale) {
        var resolved = hostScale == null ? 3 : clampHostScale(hostScale);
        System.setProperty(HOST_SCALE_PROPERTY, Integer.toString(resolved));
    }

    public static int clampHostScale(int hostScale) {
        return Math.max(MIN_HOST_SCALE, Math.min(MAX_HOST_SCALE, hostScale));
    }

    public static boolean isSupportedHostScale(int hostScale) {
        return hostScale >= MIN_HOST_SCALE && hostScale <= MAX_HOST_SCALE;
    }
}
