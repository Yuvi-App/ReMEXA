package remexa.host;

import remexa.settings.RemexaPreferences;

public final class HostUiSettings {
    private HostUiSettings() {
    }

    public static boolean showHostDetails() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.SHOW_HOST_DETAILS_KEY, false);
    }

    public static void setShowHostDetails(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.SHOW_HOST_DETAILS_KEY, enabled);
    }

    public static LaunchConfig.FontType fontType() {
        return LaunchConfig.FontType.normalize(
                RemexaPreferences.ui().get(RemexaPreferences.FONT_TYPE_KEY, LaunchConfig.FontType.BITMAP.id())
        );
    }

    public static void setFontType(LaunchConfig.FontType fontType) {
        var resolved = fontType == null ? LaunchConfig.FontType.BITMAP : fontType;
        RemexaPreferences.ui().put(RemexaPreferences.FONT_TYPE_KEY, resolved.id());
    }

    public static LaunchConfig.JskyPhoneType jskyPhoneType() {
        return LaunchConfig.JskyPhoneType.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.JSKY_PHONE_TYPE_KEY,
                        LaunchConfig.JskyPhoneType.GENERIC.id()
                )
        );
    }

    public static void setJskyPhoneType(LaunchConfig.JskyPhoneType jskyPhoneType) {
        var resolved = jskyPhoneType == null ? LaunchConfig.JskyPhoneType.GENERIC : jskyPhoneType;
        RemexaPreferences.ui().put(RemexaPreferences.JSKY_PHONE_TYPE_KEY, resolved.id());
    }

    public static LaunchConfig.VodafonePhoneType vodafonePhoneType() {
        return LaunchConfig.VodafonePhoneType.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.VODAFONE_PHONE_TYPE_KEY,
                        LaunchConfig.VodafonePhoneType.GENERIC.id()
                )
        );
    }

    public static void setVodafonePhoneType(LaunchConfig.VodafonePhoneType vodafonePhoneType) {
        var resolved = vodafonePhoneType == null ? LaunchConfig.VodafonePhoneType.GENERIC : vodafonePhoneType;
        RemexaPreferences.ui().put(RemexaPreferences.VODAFONE_PHONE_TYPE_KEY, resolved.id());
    }

    public static LaunchConfig.MexaPhoneType mexaPhoneType() {
        return LaunchConfig.MexaPhoneType.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.MEXA_PHONE_TYPE_KEY,
                        LaunchConfig.MexaPhoneType.GENERIC.id()
                )
        );
    }

    public static void setMexaPhoneType(LaunchConfig.MexaPhoneType mexaPhoneType) {
        var resolved = mexaPhoneType == null ? LaunchConfig.MexaPhoneType.GENERIC : mexaPhoneType;
        RemexaPreferences.ui().put(RemexaPreferences.MEXA_PHONE_TYPE_KEY, resolved.id());
    }

    public static LaunchConfig.SmafSynthType smafSynthType() {
        return LaunchConfig.SmafSynthType.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.SMAF_SYNTH_TYPE_KEY,
                        LaunchConfig.SmafSynthType.AUTO.id()
                )
        );
    }

    public static void setSmafSynthType(LaunchConfig.SmafSynthType synthType) {
        var resolved = synthType == null ? LaunchConfig.SmafSynthType.AUTO : synthType;
        RemexaPreferences.ui().put(RemexaPreferences.SMAF_SYNTH_TYPE_KEY, resolved.id());
    }

    public static int hostScale() {
        return LaunchConfig.normalizeHostScale(
                RemexaPreferences.ui().get(RemexaPreferences.HOST_SCALE_KEY, "3")
        );
    }

    public static void setHostScale(int hostScale) {
        RemexaPreferences.ui().putInt(
                RemexaPreferences.HOST_SCALE_KEY,
                LaunchConfig.clampHostScale(hostScale)
        );
    }

    public static LaunchConfig.FrameRateOption frameRateOption() {
        return LaunchConfig.FrameRateOption.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.FRAME_RATE_KEY,
                        LaunchConfig.FrameRateOption.UNCAPPED.id()
                )
        );
    }

    public static void setFrameRateOption(LaunchConfig.FrameRateOption option) {
        var resolved = option == null ? LaunchConfig.FrameRateOption.UNCAPPED : option;
        RemexaPreferences.ui().put(RemexaPreferences.FRAME_RATE_KEY, resolved.id());
    }

    public static boolean touchControlsEnabled() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.TOUCH_CONTROLS_ENABLED_KEY, false);
    }

    public static void setTouchControlsEnabled(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.TOUCH_CONTROLS_ENABLED_KEY, enabled);
    }

    public static boolean motionControlsEnabled() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.MOTION_CONTROLS_ENABLED_KEY, false);
    }

    public static void setMotionControlsEnabled(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.MOTION_CONTROLS_ENABLED_KEY, enabled);
    }

    public static int motionSensitivityPercent() {
        return LaunchConfig.normalizeMotionSensitivityPercent(
                RemexaPreferences.ui().get(
                        RemexaPreferences.MOTION_SENSITIVITY_KEY,
                        Integer.toString(LaunchConfig.DEFAULT_MOTION_SENSITIVITY_PERCENT)
                )
        );
    }

    public static void setMotionSensitivityPercent(int sensitivityPercent) {
        RemexaPreferences.ui().putInt(
                RemexaPreferences.MOTION_SENSITIVITY_KEY,
                LaunchConfig.clampMotionSensitivityPercent(sensitivityPercent)
        );
    }

    public static LaunchConfig.MotionTrackingMode motionTrackingMode() {
        return LaunchConfig.MotionTrackingMode.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.MOTION_TRACKING_MODE_KEY,
                        LaunchConfig.MotionTrackingMode.JAD_FRAME.id()
                )
        );
    }

    public static void setMotionTrackingMode(LaunchConfig.MotionTrackingMode mode) {
        var resolved = mode == null ? LaunchConfig.MotionTrackingMode.JAD_FRAME : mode;
        RemexaPreferences.ui().put(RemexaPreferences.MOTION_TRACKING_MODE_KEY, resolved.id());
    }

    public static boolean flashBacklightEnabled() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.FLASH_BACKLIGHT_ENABLED_KEY, true);
    }

    public static void setFlashBacklightEnabled(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.FLASH_BACKLIGHT_ENABLED_KEY, enabled);
    }

    public static LaunchConfig.CameraInputMode cameraInputMode() {
        return LaunchConfig.CameraInputMode.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.CAMERA_INPUT_MODE_KEY,
                        LaunchConfig.CameraInputMode.FILE_PICKER.id()
                )
        );
    }

    public static void setCameraInputMode(LaunchConfig.CameraInputMode mode) {
        var resolved = mode == null ? LaunchConfig.CameraInputMode.FILE_PICKER : mode;
        RemexaPreferences.ui().put(RemexaPreferences.CAMERA_INPUT_MODE_KEY, resolved.id());
    }

    public static boolean liveTranslationEnabled() {
        return RemexaPreferences.ui().getBoolean(RemexaPreferences.LIVE_TRANSLATION_ENABLED_KEY, false);
    }

    public static void setLiveTranslationEnabled(boolean enabled) {
        RemexaPreferences.ui().putBoolean(RemexaPreferences.LIVE_TRANSLATION_ENABLED_KEY, enabled);
    }

    public static LaunchConfig.DeepLApiPlan deepLApiPlan() {
        return LaunchConfig.DeepLApiPlan.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.DEEPL_API_PLAN_KEY,
                        LaunchConfig.DeepLApiPlan.FREE.id()
                )
        );
    }

    public static void setDeepLApiPlan(LaunchConfig.DeepLApiPlan plan) {
        var resolved = plan == null ? LaunchConfig.DeepLApiPlan.FREE : plan;
        RemexaPreferences.ui().put(RemexaPreferences.DEEPL_API_PLAN_KEY, resolved.id());
    }

    public static String deepLApiKey() {
        return LaunchConfig.normalizeDeepLApiKey(
                RemexaPreferences.ui().get(RemexaPreferences.DEEPL_API_KEY_KEY, "")
        );
    }

    public static void setDeepLApiKey(String apiKey) {
        RemexaPreferences.ui().put(
                RemexaPreferences.DEEPL_API_KEY_KEY,
                LaunchConfig.normalizeDeepLApiKey(apiKey)
        );
    }

    public static LaunchConfig.TranslationTargetLanguage translationTargetLanguage() {
        return LaunchConfig.TranslationTargetLanguage.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.DEEPL_TARGET_LANGUAGE_KEY,
                        LaunchConfig.TranslationTargetLanguage.ENGLISH_US.code()
                )
        );
    }

    public static void setTranslationTargetLanguage(LaunchConfig.TranslationTargetLanguage language) {
        var resolved = language == null ? LaunchConfig.TranslationTargetLanguage.ENGLISH_US : language;
        RemexaPreferences.ui().put(RemexaPreferences.DEEPL_TARGET_LANGUAGE_KEY, resolved.code());
    }

    public static LaunchConfig.BluetoothBackend bluetoothBackend() {
        return LaunchConfig.BluetoothBackend.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.BLUETOOTH_BACKEND_KEY,
                        LaunchConfig.BluetoothBackend.OFF.id()
                )
        );
    }

    public static void setBluetoothBackend(LaunchConfig.BluetoothBackend backend) {
        var resolved = backend == null ? LaunchConfig.BluetoothBackend.OFF : backend;
        RemexaPreferences.ui().put(RemexaPreferences.BLUETOOTH_BACKEND_KEY, resolved.id());
    }

    public static LaunchConfig.BluetoothRole bluetoothRole() {
        return LaunchConfig.BluetoothRole.normalize(
                RemexaPreferences.ui().get(
                        RemexaPreferences.BLUETOOTH_ROLE_KEY,
                        LaunchConfig.BluetoothRole.HOST.id()
                )
        );
    }

    public static void setBluetoothRole(LaunchConfig.BluetoothRole role) {
        var resolved = role == null ? LaunchConfig.BluetoothRole.HOST : role;
        RemexaPreferences.ui().put(RemexaPreferences.BLUETOOTH_ROLE_KEY, resolved.id());
    }

    public static String bluetoothLocalName() {
        return LaunchConfig.normalizeBluetoothLocalName(
                RemexaPreferences.ui().get(
                        RemexaPreferences.BLUETOOTH_LOCAL_NAME_KEY,
                        LaunchConfig.resolveConfiguredBluetoothLocalName()
                )
        );
    }

    public static void setBluetoothLocalName(String localName) {
        RemexaPreferences.ui().put(
                RemexaPreferences.BLUETOOTH_LOCAL_NAME_KEY,
                LaunchConfig.normalizeBluetoothLocalName(localName)
        );
    }

    public static String bluetoothRemoteHost() {
        return LaunchConfig.normalizeBluetoothRemoteHost(
                RemexaPreferences.ui().get(
                        RemexaPreferences.BLUETOOTH_REMOTE_HOST_KEY,
                        LaunchConfig.resolveConfiguredBluetoothRemoteHost()
                )
        );
    }

    public static void setBluetoothRemoteHost(String remoteHost) {
        RemexaPreferences.ui().put(
                RemexaPreferences.BLUETOOTH_REMOTE_HOST_KEY,
                LaunchConfig.normalizeBluetoothRemoteHost(remoteHost)
        );
    }

    public static int bluetoothPort() {
        return LaunchConfig.normalizeBluetoothPort(
                RemexaPreferences.ui().get(
                        RemexaPreferences.BLUETOOTH_PORT_KEY,
                        Integer.toString(LaunchConfig.DEFAULT_BLUETOOTH_PORT)
                )
        );
    }

    public static void setBluetoothPort(int port) {
        RemexaPreferences.ui().putInt(
                RemexaPreferences.BLUETOOTH_PORT_KEY,
                LaunchConfig.clampBluetoothPort(port)
        );
    }

    public static boolean dumpRms() {
        return RemexaPreferences.debug().getBoolean(RemexaPreferences.DUMP_RMS_KEY, false);
    }

    public static void setDumpRms(boolean enabled) {
        RemexaPreferences.debug().putBoolean(RemexaPreferences.DUMP_RMS_KEY, enabled);
    }

    public static boolean fpsOverlayEnabled() {
        return RemexaPreferences.debug().getBoolean(RemexaPreferences.FPS_OVERLAY_ENABLED_KEY, false);
    }

    public static void setFpsOverlayEnabled(boolean enabled) {
        RemexaPreferences.debug().putBoolean(RemexaPreferences.FPS_OVERLAY_ENABLED_KEY, enabled);
    }
}
