package remexa.host;

import java.util.Locale;

public final class LaunchConfig {
    public static final String FONT_TYPE_PROPERTY = "remexa.fontType";

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
}
