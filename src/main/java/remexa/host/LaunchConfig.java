package remexa.host;

import java.util.Locale;

public final class LaunchConfig {
    public static final String FONT_TYPE_PROPERTY = "remexa.fontType";
    public static final String JSKY_PHONE_TYPE_PROPERTY = "remexa.jskyPhoneType";
    public static final String VODAFONE_PHONE_TYPE_PROPERTY = "remexa.vodafonePhoneType";
    public static final String MEXA_PHONE_TYPE_PROPERTY = "remexa.mexaPhoneType";
    public static final String SMAF_SYNTH_PROPERTY = "remexa.smafSynth";
    public static final String HOST_SCALE_PROPERTY = "remexa.hostScale";
    public static final String BLUETOOTH_BACKEND_PROPERTY = "remexa.bluetoothBackend";
    public static final String BLUETOOTH_ROLE_PROPERTY = "remexa.bluetoothRole";
    public static final String BLUETOOTH_LOCAL_NAME_PROPERTY = "remexa.bluetoothLocalName";
    public static final String BLUETOOTH_REMOTE_HOST_PROPERTY = "remexa.bluetoothRemoteHost";
    public static final String BLUETOOTH_PORT_PROPERTY = "remexa.bluetoothPort";
    public static final int MIN_HOST_SCALE = 1;
    public static final int MAX_HOST_SCALE = 5;
    public static final int DEFAULT_BLUETOOTH_PORT = 23024;
    public static final int MIN_BLUETOOTH_PORT = 1;
    public static final int MAX_BLUETOOTH_PORT = 65535;

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

    public enum VodafonePhoneType {
        GENERIC("generic", "Vodafone Generic", "Vodafone-Generic"),
        V604SH("v604sh", "V604SH", "V604SH");

        private final String id;
        private final String label;
        private final String platformName;

        VodafonePhoneType(String id, String label, String platformName) {
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

        public static VodafonePhoneType fromId(String candidate) {
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

        public static VodafonePhoneType normalize(String candidate) {
            var type = fromId(candidate);
            return type == null ? GENERIC : type;
        }

        public static VodafonePhoneType resolveConfigured() {
            return normalize(System.getProperty(VODAFONE_PHONE_TYPE_PROPERTY, GENERIC.id));
        }
    }

    public static void applyVodafonePhoneType(VodafonePhoneType vodafonePhoneType) {
        var resolved = vodafonePhoneType == null ? VodafonePhoneType.GENERIC : vodafonePhoneType;
        System.setProperty(VODAFONE_PHONE_TYPE_PROPERTY, resolved.id());
    }

    public enum MexaPhoneType {
        GENERIC("generic", "MEXA Generic", "MEXA-Generic"),
        SHARP_930SH("930sh", "930SH", "930SH");

        private final String id;
        private final String label;
        private final String platformName;

        MexaPhoneType(String id, String label, String platformName) {
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

        public static MexaPhoneType fromId(String candidate) {
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

        public static MexaPhoneType normalize(String candidate) {
            var type = fromId(candidate);
            return type == null ? GENERIC : type;
        }

        public static MexaPhoneType resolveConfigured() {
            return normalize(System.getProperty(MEXA_PHONE_TYPE_PROPERTY, GENERIC.id));
        }
    }

    public static void applyMexaPhoneType(MexaPhoneType mexaPhoneType) {
        var resolved = mexaPhoneType == null ? MexaPhoneType.GENERIC : mexaPhoneType;
        System.setProperty(MEXA_PHONE_TYPE_PROPERTY, resolved.id());
    }

    public enum SmafSynthType {
        AUTO("auto", "Auto"),
        MA3("ma3", "MA3"),
        MA5("ma5", "MA5"),
        LEGACY("legacy", "Legacy FueTrek"),
        MIDI("midi", "Host MIDI");

        private final String id;
        private final String label;

        SmafSynthType(String id, String label) {
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

        public static SmafSynthType fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            if ("fuetrek".equals(normalized)) {
                return LEGACY;
            }
            for (var type : values()) {
                if (type.id.equals(normalized) || type.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return type;
                }
            }
            return null;
        }

        public static SmafSynthType normalize(String candidate) {
            var type = fromId(candidate);
            return type == null ? AUTO : type;
        }

        public static SmafSynthType resolveConfigured() {
            return normalize(System.getProperty(SMAF_SYNTH_PROPERTY, AUTO.id));
        }
    }

    public static void applySmafSynthType(SmafSynthType synthType) {
        var resolved = synthType == null ? SmafSynthType.AUTO : synthType;
        System.setProperty(SMAF_SYNTH_PROPERTY, resolved.id());
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

    public enum BluetoothBackend {
        OFF("off", "Off"),
        VIRTUAL_IP("virtual-ip", "Remote Over IP");

        private final String id;
        private final String label;

        BluetoothBackend(String id, String label) {
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

        public static BluetoothBackend fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var backend : values()) {
                if (backend.id.equals(normalized) || backend.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return backend;
                }
            }
            return null;
        }

        public static BluetoothBackend normalize(String candidate) {
            var backend = fromId(candidate);
            return backend == null ? OFF : backend;
        }

        public static BluetoothBackend resolveConfigured() {
            return normalize(System.getProperty(BLUETOOTH_BACKEND_PROPERTY, OFF.id));
        }
    }

    public static void applyBluetoothBackend(BluetoothBackend backend) {
        var resolved = backend == null ? BluetoothBackend.OFF : backend;
        System.setProperty(BLUETOOTH_BACKEND_PROPERTY, resolved.id());
    }

    public enum BluetoothRole {
        HOST("host", "Host"),
        CLIENT("client", "Client");

        private final String id;
        private final String label;

        BluetoothRole(String id, String label) {
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

        public static BluetoothRole fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var role : values()) {
                if (role.id.equals(normalized) || role.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return role;
                }
            }
            return null;
        }

        public static BluetoothRole normalize(String candidate) {
            var role = fromId(candidate);
            return role == null ? HOST : role;
        }

        public static BluetoothRole resolveConfigured() {
            return normalize(System.getProperty(BLUETOOTH_ROLE_PROPERTY, HOST.id));
        }
    }

    public static void applyBluetoothRole(BluetoothRole role) {
        var resolved = role == null ? BluetoothRole.HOST : role;
        System.setProperty(BLUETOOTH_ROLE_PROPERTY, resolved.id());
    }

    public static String normalizeBluetoothLocalName(String candidate) {
        if (candidate == null) {
            return "ReMEXA";
        }
        var normalized = candidate.trim();
        return normalized.isEmpty() ? "ReMEXA" : normalized;
    }

    public static String resolveConfiguredBluetoothLocalName() {
        return normalizeBluetoothLocalName(System.getProperty(BLUETOOTH_LOCAL_NAME_PROPERTY, "ReMEXA"));
    }

    public static void applyBluetoothLocalName(String localName) {
        System.setProperty(BLUETOOTH_LOCAL_NAME_PROPERTY, normalizeBluetoothLocalName(localName));
    }

    public static String normalizeBluetoothRemoteHost(String candidate) {
        if (candidate == null) {
            return "127.0.0.1";
        }
        var normalized = candidate.trim();
        return normalized.isEmpty() ? "127.0.0.1" : normalized;
    }

    public static String resolveConfiguredBluetoothRemoteHost() {
        return normalizeBluetoothRemoteHost(System.getProperty(BLUETOOTH_REMOTE_HOST_PROPERTY, "127.0.0.1"));
    }

    public static void applyBluetoothRemoteHost(String host) {
        System.setProperty(BLUETOOTH_REMOTE_HOST_PROPERTY, normalizeBluetoothRemoteHost(host));
    }

    public static Integer parseBluetoothPort(String candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            var parsed = Integer.parseInt(candidate.trim());
            return isSupportedBluetoothPort(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static int normalizeBluetoothPort(String candidate) {
        var parsed = parseBluetoothPort(candidate);
        return parsed == null ? DEFAULT_BLUETOOTH_PORT : parsed;
    }

    public static int resolveConfiguredBluetoothPort() {
        return normalizeBluetoothPort(System.getProperty(BLUETOOTH_PORT_PROPERTY, Integer.toString(DEFAULT_BLUETOOTH_PORT)));
    }

    public static void applyBluetoothPort(Integer port) {
        var resolved = port == null ? DEFAULT_BLUETOOTH_PORT : clampBluetoothPort(port);
        System.setProperty(BLUETOOTH_PORT_PROPERTY, Integer.toString(resolved));
    }

    public static int clampBluetoothPort(int port) {
        return Math.max(MIN_BLUETOOTH_PORT, Math.min(MAX_BLUETOOTH_PORT, port));
    }

    public static boolean isSupportedBluetoothPort(int port) {
        return port >= MIN_BLUETOOTH_PORT && port <= MAX_BLUETOOTH_PORT;
    }
}
