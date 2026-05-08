package remexa.host;

import java.util.Locale;

public final class LaunchConfig {
    public static final String FONT_TYPE_PROPERTY = "remexa.fontType";
    public static final String JSKY_PHONE_TYPE_PROPERTY = "remexa.jskyPhoneType";
    public static final String VODAFONE_PHONE_TYPE_PROPERTY = "remexa.vodafonePhoneType";
    public static final String MEXA_PHONE_TYPE_PROPERTY = "remexa.mexaPhoneType";
    public static final String SMAF_SYNTH_PROPERTY = "remexa.smafSynth";
    public static final String HOST_SCALE_PROPERTY = "remexa.hostScale";
    public static final String DISABLE_DPI_SCALING_PROPERTY = "remexa.disableDpiScaling";
    public static final String FRAME_INTERVAL_PROPERTY = "remexa.frameIntervalMs";
    public static final String TOUCH_CONTROLS_PROPERTY = "remexa.touchControls";
    public static final String MOTION_CONTROLS_PROPERTY = "remexa.motionControls";
    public static final String MOTION_SENSITIVITY_PROPERTY = "remexa.motionSensitivity";
    public static final String MOTION_TRACKING_MODE_PROPERTY = "remexa.motionTrackingMode";
    public static final String FLASH_BACKLIGHT_PROPERTY = "remexa.flashBacklight";
    public static final String FPS_OVERLAY_PROPERTY = "remexa.fpsOverlay";
    public static final String CAMERA_INPUT_MODE_PROPERTY = "remexa.cameraInputMode";
    public static final String LIVE_TRANSLATION_PROPERTY = "remexa.liveTranslation";
    public static final String DEEPL_API_PLAN_PROPERTY = "remexa.deeplApiPlan";
    public static final String DEEPL_API_KEY_PROPERTY = "remexa.deeplApiKey";
    public static final String DEEPL_TARGET_LANGUAGE_PROPERTY = "remexa.deeplTargetLanguage";
    public static final String BLUETOOTH_BACKEND_PROPERTY = "remexa.bluetoothBackend";
    public static final String BLUETOOTH_ROLE_PROPERTY = "remexa.bluetoothRole";
    public static final String BLUETOOTH_LOCAL_NAME_PROPERTY = "remexa.bluetoothLocalName";
    public static final String BLUETOOTH_REMOTE_HOST_PROPERTY = "remexa.bluetoothRemoteHost";
    public static final String BLUETOOTH_PORT_PROPERTY = "remexa.bluetoothPort";
    public static final int MIN_HOST_SCALE = 1;
    public static final int MAX_HOST_SCALE = 5;
    public static final int DEFAULT_MOTION_SENSITIVITY_PERCENT = 100;
    public static final int MIN_MOTION_SENSITIVITY_PERCENT = 25;
    public static final int MAX_MOTION_SENSITIVITY_PERCENT = 300;
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
        MA7("ma7", "MA7"),
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

    public static boolean resolveConfiguredDisableDpiScaling() {
        return Boolean.parseBoolean(System.getProperty(DISABLE_DPI_SCALING_PROPERTY, Boolean.FALSE.toString()));
    }

    public static void applyDisableDpiScaling(Boolean disabled) {
        System.setProperty(DISABLE_DPI_SCALING_PROPERTY, Boolean.toString(disabled != null && disabled));
    }

    public enum FrameRateOption {
        UNCAPPED("uncapped", "Uncapped", 0),
        FPS_5("5", "5 FPS", 200),
        FPS_10("10", "10 FPS", 100),
        FPS_15("15", "15 FPS", 67),
        FPS_20("20", "20 FPS", 50),
        FPS_30("30", "30 FPS", 33),
        FPS_60("60", "60 FPS", 17);

        private final String id;
        private final String label;
        private final int frameIntervalMs;

        FrameRateOption(String id, String label, int frameIntervalMs) {
            this.id = id;
            this.label = label;
            this.frameIntervalMs = frameIntervalMs;
        }

        public String id() {
            return id;
        }

        public int frameIntervalMs() {
            return frameIntervalMs;
        }

        @Override
        public String toString() {
            return label;
        }

        public static FrameRateOption fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var option : values()) {
                if (option.id.equals(normalized) || option.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return option;
                }
            }
            return null;
        }

        public static FrameRateOption fromFrameIntervalMs(int frameIntervalMs) {
            for (var option : values()) {
                if (option.frameIntervalMs == frameIntervalMs) {
                    return option;
                }
            }
            return frameIntervalMs <= 0 ? UNCAPPED : FPS_20;
        }

        public static FrameRateOption normalize(String candidate) {
            var option = fromId(candidate);
            return option == null ? UNCAPPED : option;
        }

        public static FrameRateOption resolveConfigured() {
            var configured = System.getProperty(FRAME_INTERVAL_PROPERTY);
            if (configured != null && !configured.isBlank()) {
                var parsed = parseFrameRateOption(configured);
                if (parsed != null) {
                    return parsed;
                }
            }
            return UNCAPPED;
        }
    }

    public static FrameRateOption parseFrameRateOption(String candidate) {
        if (candidate == null) {
            return null;
        }
        var option = FrameRateOption.fromId(candidate);
        if (option != null) {
            return option;
        }
        try {
            var parsed = Integer.parseInt(candidate.trim());
            return FrameRateOption.fromFrameIntervalMs(parsed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static void applyFrameRateOption(FrameRateOption option) {
        var resolved = option == null ? FrameRateOption.UNCAPPED : option;
        System.setProperty(FRAME_INTERVAL_PROPERTY, Integer.toString(resolved.frameIntervalMs()));
    }

    public static long resolveConfiguredFrameIntervalNanos() {
        return (long) FrameRateOption.resolveConfigured().frameIntervalMs() * 1_000_000L;
    }

    public static boolean resolveConfiguredTouchControlsEnabled() {
        return Boolean.parseBoolean(System.getProperty(TOUCH_CONTROLS_PROPERTY, Boolean.FALSE.toString()));
    }

    public static void applyTouchControlsEnabled(Boolean enabled) {
        System.setProperty(TOUCH_CONTROLS_PROPERTY, Boolean.toString(enabled != null && enabled));
    }

    public static boolean resolveConfiguredMotionControlsEnabled() {
        return Boolean.parseBoolean(System.getProperty(MOTION_CONTROLS_PROPERTY, Boolean.FALSE.toString()));
    }

    public static void applyMotionControlsEnabled(Boolean enabled) {
        System.setProperty(MOTION_CONTROLS_PROPERTY, Boolean.toString(enabled != null && enabled));
    }

    public enum MotionTrackingMode {
        JAD_FRAME("jad-frame", "JAD Frame Only"),
        DESKTOP("desktop", "Whole Desktop");

        private final String id;
        private final String label;

        MotionTrackingMode(String id, String label) {
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

        public static MotionTrackingMode fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var mode : values()) {
                if (mode.id.equals(normalized) || mode.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return mode;
                }
            }
            return null;
        }

        public static MotionTrackingMode normalize(String candidate) {
            var mode = fromId(candidate);
            return mode == null ? JAD_FRAME : mode;
        }

        public static MotionTrackingMode resolveConfigured() {
            return normalize(System.getProperty(MOTION_TRACKING_MODE_PROPERTY, JAD_FRAME.id));
        }
    }

    public static void applyMotionTrackingMode(MotionTrackingMode mode) {
        var resolved = mode == null ? MotionTrackingMode.JAD_FRAME : mode;
        System.setProperty(MOTION_TRACKING_MODE_PROPERTY, resolved.id());
    }

    public static Integer parseMotionSensitivityPercent(String candidate) {
        if (candidate == null) {
            return null;
        }
        var normalized = candidate.trim();
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static int normalizeMotionSensitivityPercent(String candidate) {
        var parsed = parseMotionSensitivityPercent(candidate);
        return parsed == null ? DEFAULT_MOTION_SENSITIVITY_PERCENT : clampMotionSensitivityPercent(parsed);
    }

    public static int resolveConfiguredMotionSensitivityPercent() {
        return normalizeMotionSensitivityPercent(
                System.getProperty(
                        MOTION_SENSITIVITY_PROPERTY,
                        Integer.toString(DEFAULT_MOTION_SENSITIVITY_PERCENT)
                )
        );
    }

    public static void applyMotionSensitivityPercent(Integer sensitivityPercent) {
        var resolved = sensitivityPercent == null
                ? DEFAULT_MOTION_SENSITIVITY_PERCENT
                : clampMotionSensitivityPercent(sensitivityPercent);
        System.setProperty(MOTION_SENSITIVITY_PROPERTY, Integer.toString(resolved));
    }

    public static int clampMotionSensitivityPercent(int sensitivityPercent) {
        return Math.max(MIN_MOTION_SENSITIVITY_PERCENT, Math.min(MAX_MOTION_SENSITIVITY_PERCENT, sensitivityPercent));
    }

    public static boolean resolveConfiguredFlashBacklightEnabled() {
        return Boolean.parseBoolean(System.getProperty(FLASH_BACKLIGHT_PROPERTY, Boolean.TRUE.toString()));
    }

    public static void applyFlashBacklightEnabled(Boolean enabled) {
        System.setProperty(FLASH_BACKLIGHT_PROPERTY, Boolean.toString(enabled == null || enabled));
    }

    public static boolean resolveConfiguredFpsOverlayEnabled() {
        return Boolean.parseBoolean(System.getProperty(FPS_OVERLAY_PROPERTY, Boolean.FALSE.toString()));
    }

    public static void applyFpsOverlayEnabled(Boolean enabled) {
        System.setProperty(FPS_OVERLAY_PROPERTY, Boolean.toString(enabled != null && enabled));
    }

    public enum CameraInputMode {
        DISABLED("disabled", "Disabled"),
        FILE_PICKER("file-picker", "File Picker (MVP)");

        private final String id;
        private final String label;

        CameraInputMode(String id, String label) {
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

        public static CameraInputMode fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var mode : values()) {
                if (mode.id.equals(normalized) || mode.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return mode;
                }
            }
            return null;
        }

        public static CameraInputMode normalize(String candidate) {
            var mode = fromId(candidate);
            return mode == null ? FILE_PICKER : mode;
        }

        public static CameraInputMode resolveConfigured() {
            return normalize(System.getProperty(CAMERA_INPUT_MODE_PROPERTY, FILE_PICKER.id));
        }
    }

    public static void applyCameraInputMode(CameraInputMode mode) {
        var resolved = mode == null ? CameraInputMode.FILE_PICKER : mode;
        System.setProperty(CAMERA_INPUT_MODE_PROPERTY, resolved.id());
    }

    public static boolean resolveConfiguredLiveTranslationEnabled() {
        return Boolean.parseBoolean(System.getProperty(LIVE_TRANSLATION_PROPERTY, Boolean.FALSE.toString()));
    }

    public static void applyLiveTranslationEnabled(Boolean enabled) {
        System.setProperty(LIVE_TRANSLATION_PROPERTY, Boolean.toString(enabled != null && enabled));
    }

    public enum DeepLApiPlan {
        FREE("free", "Free API", "https://api-free.deepl.com/v2/translate"),
        PRO("pro", "Pro API", "https://api.deepl.com/v2/translate");

        private final String id;
        private final String label;
        private final String endpoint;

        DeepLApiPlan(String id, String label, String endpoint) {
            this.id = id;
            this.label = label;
            this.endpoint = endpoint;
        }

        public String id() {
            return id;
        }

        public String endpoint() {
            return endpoint;
        }

        @Override
        public String toString() {
            return label;
        }

        public static DeepLApiPlan fromId(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toLowerCase(Locale.ROOT);
            for (var plan : values()) {
                if (plan.id.equals(normalized) || plan.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return plan;
                }
            }
            return null;
        }

        public static DeepLApiPlan normalize(String candidate) {
            var plan = fromId(candidate);
            return plan == null ? FREE : plan;
        }

        public static DeepLApiPlan resolveConfigured() {
            return normalize(System.getProperty(DEEPL_API_PLAN_PROPERTY, FREE.id));
        }
    }

    public static void applyDeepLApiPlan(DeepLApiPlan plan) {
        var resolved = plan == null ? DeepLApiPlan.FREE : plan;
        System.setProperty(DEEPL_API_PLAN_PROPERTY, resolved.id());
    }

    public static String normalizeDeepLApiKey(String candidate) {
        return candidate == null ? "" : candidate.trim();
    }

    public static String resolveConfiguredDeepLApiKey() {
        return normalizeDeepLApiKey(System.getProperty(DEEPL_API_KEY_PROPERTY, ""));
    }

    public static void applyDeepLApiKey(String apiKey) {
        System.setProperty(DEEPL_API_KEY_PROPERTY, normalizeDeepLApiKey(apiKey));
    }

    public enum TranslationTargetLanguage {
        ENGLISH_US("EN-US", "English (US)"),
        ENGLISH_GB("EN-GB", "English (UK)"),
        GERMAN("DE", "German"),
        FRENCH("FR", "French"),
        ITALIAN("IT", "Italian"),
        SPANISH("ES", "Spanish"),
        PORTUGUESE_BR("PT-BR", "Portuguese (Brazil)"),
        PORTUGUESE_PT("PT-PT", "Portuguese (Portugal)"),
        DUTCH("NL", "Dutch"),
        POLISH("PL", "Polish"),
        RUSSIAN("RU", "Russian"),
        KOREAN("KO", "Korean"),
        CHINESE_SIMPLIFIED("ZH-HANS", "Chinese (Simplified)"),
        CHINESE_TRADITIONAL("ZH-HANT", "Chinese (Traditional)");

        private final String code;
        private final String label;

        TranslationTargetLanguage(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String code() {
            return code;
        }

        @Override
        public String toString() {
            return label;
        }

        public static TranslationTargetLanguage fromCode(String candidate) {
            if (candidate == null) {
                return null;
            }
            var normalized = candidate.trim().toUpperCase(Locale.ROOT);
            for (var language : values()) {
                if (language.code.equals(normalized) || language.label.toUpperCase(Locale.ROOT).equals(normalized)) {
                    return language;
                }
            }
            return null;
        }

        public static TranslationTargetLanguage normalize(String candidate) {
            var language = fromCode(candidate);
            return language == null ? ENGLISH_US : language;
        }

        public static TranslationTargetLanguage resolveConfigured() {
            return normalize(System.getProperty(DEEPL_TARGET_LANGUAGE_PROPERTY, ENGLISH_US.code));
        }
    }

    public static void applyTranslationTargetLanguage(TranslationTargetLanguage language) {
        var resolved = language == null ? TranslationTargetLanguage.ENGLISH_US : language;
        System.setProperty(DEEPL_TARGET_LANGUAGE_PROPERTY, resolved.code());
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
