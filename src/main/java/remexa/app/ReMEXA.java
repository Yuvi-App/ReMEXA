package remexa.app;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import remexa.frontend.LauncherFrame;
import remexa.host.HostUiSettings;
import remexa.host.JadLauncher;
import remexa.host.LaunchConfig;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.ui.AppIcons;

public final class ReMEXA {
    private static final String DEFAULT_LEGACY_ENCODING = "windows-31j";
    private static final String ENCODING_BOOTSTRAPPED_PROPERTY = "remexa.encoding.applied";
    private static final String NATIVE_ACCESS_BOOTSTRAPPED_PROPERTY = "remexa.nativeAccess.applied";
    private static final String DPI_SCALING_BOOTSTRAPPED_PROPERTY = "remexa.dpiScaling.applied";
    private static final String JAVA2D_UI_SCALE_PROPERTY = "sun.java2d.uiScale";
    private static final String JAVA2D_UNSCALED_VALUE = "1.0";
    private static final String NATIVE_ACCESS_FLAG = "--enable-native-access=ALL-UNNAMED";

    private ReMEXA() {
    }

    public static void main(String[] args) {
        bootstrapJvmConfigurationIfNeeded(args);
        AppIcons.applyToTaskbar();
        var arguments = List.of(args);
        var disableDpiScalingOverride = parseDisableDpiScalingOverride(arguments);
        var midiSynthOverride = parseMidiSynthOverride(arguments);
        var launchRequest = parseLaunchRequest(arguments);
        LaunchConfig.applyFontType(launchRequest.fontType() == null ? HostUiSettings.fontType() : launchRequest.fontType());
        LaunchConfig.applyJskyPhoneType(launchRequest.jskyPhoneType() == null ? HostUiSettings.jskyPhoneType() : launchRequest.jskyPhoneType());
        LaunchConfig.applyVodafonePhoneType(launchRequest.vodafonePhoneType() == null ? HostUiSettings.vodafonePhoneType() : launchRequest.vodafonePhoneType());
        LaunchConfig.applyMexaPhoneType(launchRequest.mexaPhoneType() == null ? HostUiSettings.mexaPhoneType() : launchRequest.mexaPhoneType());
        LaunchConfig.applySmafSynthType(HostUiSettings.smafSynthType());
        LaunchConfig.applyMidiSynthType(midiSynthOverride == null ? HostUiSettings.midiSynthType() : midiSynthOverride);
        LaunchConfig.applyHostScale(launchRequest.hostScale() == null ? HostUiSettings.hostScale() : launchRequest.hostScale());
        LaunchConfig.applyDisableDpiScaling(disableDpiScalingOverride == null ? HostUiSettings.disableDpiScaling() : disableDpiScalingOverride);
        LaunchConfig.applyFrameRateOption(launchRequest.frameRateOption() == null ? HostUiSettings.frameRateOption() : launchRequest.frameRateOption());
        LaunchConfig.applyTouchControlsEnabled(HostUiSettings.touchControlsEnabled());
        LaunchConfig.applyMotionControlsEnabled(HostUiSettings.motionControlsEnabled());
        LaunchConfig.applyMotionSensitivityPercent(HostUiSettings.motionSensitivityPercent());
        LaunchConfig.applyMotionTrackingMode(HostUiSettings.motionTrackingMode());
        LaunchConfig.applyFlashBacklightEnabled(HostUiSettings.flashBacklightEnabled());
        LaunchConfig.applyFpsOverlayEnabled(HostUiSettings.fpsOverlayEnabled());
        LaunchConfig.applyCameraInputMode(HostUiSettings.cameraInputMode());
        LaunchConfig.applyLiveTranslationEnabled(HostUiSettings.liveTranslationEnabled());
        LaunchConfig.applyDeepLApiPlan(HostUiSettings.deepLApiPlan());
        LaunchConfig.applyDeepLApiKey(HostUiSettings.deepLApiKey());
        LaunchConfig.applyTranslationTargetLanguage(HostUiSettings.translationTargetLanguage());
        LaunchConfig.applyBluetoothBackend(launchRequest.bluetoothBackend() == null ? HostUiSettings.bluetoothBackend() : launchRequest.bluetoothBackend());
        LaunchConfig.applyBluetoothRole(launchRequest.bluetoothRole() == null ? HostUiSettings.bluetoothRole() : launchRequest.bluetoothRole());
        LaunchConfig.applyBluetoothLocalName(launchRequest.bluetoothLocalName() == null ? HostUiSettings.bluetoothLocalName() : launchRequest.bluetoothLocalName());
        LaunchConfig.applyBluetoothRemoteHost(launchRequest.bluetoothRemoteHost() == null ? HostUiSettings.bluetoothRemoteHost() : launchRequest.bluetoothRemoteHost());
        LaunchConfig.applyBluetoothPort(launchRequest.bluetoothPort() == null ? HostUiSettings.bluetoothPort() : launchRequest.bluetoothPort());
        if (launchRequest.directLaunchRequested()) {
            if (launchRequest.jadPath() == null) {
                return;
            }
            launchDirect(
                    launchRequest.jadPath(),
                    launchRequest.showHostDetails(),
                    launchRequest.captureFramePath(),
                    launchRequest.captureDelayMs(),
                    launchRequest.exitAfterCapture()
            );
            return;
        }

        DebugLog.log(LogCategory.HOST, ReMEXA.class.getName(), "Launching desktop frontend");
        SwingUtilities.invokeLater(() -> new LauncherFrame(new JadLauncher()).setVisible(true));
    }

    private static void bootstrapJvmConfigurationIfNeeded(String[] args) {
        var arguments = List.of(args);
        var configured = configuredLegacyEncoding();
        var disableDpiScalingOverride = parseDisableDpiScalingOverride(arguments);
        boolean disableDpiScaling = disableDpiScalingOverride == null
                ? HostUiSettings.disableDpiScaling()
                : disableDpiScalingOverride;
        LaunchConfig.applyDisableDpiScaling(disableDpiScaling);
        boolean encodingReady = Boolean.getBoolean(ENCODING_BOOTSTRAPPED_PROPERTY)
                || Charset.defaultCharset().name().equalsIgnoreCase(configured);
        boolean nativeAccessReady = Boolean.getBoolean(NATIVE_ACCESS_BOOTSTRAPPED_PROPERTY)
                || hasNativeAccessFlag();
        boolean dpiScalingReady = !disableDpiScaling
                || JAVA2D_UNSCALED_VALUE.equals(System.getProperty(JAVA2D_UI_SCALE_PROPERTY));
        if (encodingReady && nativeAccessReady && dpiScalingReady) {
            return;
        }

        try {
            var command = new ArrayList<String>();
            command.add(javaBinary().toString());
            if (!encodingReady) {
                command.add("-Dfile.encoding=" + configured);
                command.add("-Dnative.encoding=" + configured);
                command.add("-D" + ENCODING_BOOTSTRAPPED_PROPERTY + "=true");
            }
            if (!nativeAccessReady) {
                command.add(NATIVE_ACCESS_FLAG);
                command.add("-D" + NATIVE_ACCESS_BOOTSTRAPPED_PROPERTY + "=true");
            }
            if (disableDpiScaling) {
                command.add("-D" + JAVA2D_UI_SCALE_PROPERTY + "=" + JAVA2D_UNSCALED_VALUE);
                command.add("-D" + DPI_SCALING_BOOTSTRAPPED_PROPERTY + "=true");
            } else if (!Boolean.FALSE.equals(disableDpiScalingOverride)) {
                appendSystemPropertyIfPresent(command, JAVA2D_UI_SCALE_PROPERTY);
            }
            appendRemexaSystemProperties(command);
            appendCurrentLaunchTarget(command);
            command.addAll(arguments);

            var exitCode = new ProcessBuilder(command)
                    .inheritIO()
                    .start()
                    .waitFor();
            System.exit(exitCode);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to bootstrap JVM configuration.", exception);
        }
    }

    private static void appendCurrentLaunchTarget(List<String> command) {
        var applicationPath = applicationPath();
        if (applicationPath != null && Files.isRegularFile(applicationPath)) {
            command.add("-jar");
            command.add(applicationPath.toString());
            return;
        }

        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(ReMEXA.class.getName());
    }

    private static void appendRemexaSystemProperties(List<String> command) {
        for (var propertyName : System.getProperties().stringPropertyNames()) {
            if (!propertyName.startsWith("remexa.")) {
                continue;
            }
            if (ENCODING_BOOTSTRAPPED_PROPERTY.equals(propertyName)) {
                continue;
            }
            if (NATIVE_ACCESS_BOOTSTRAPPED_PROPERTY.equals(propertyName)) {
                continue;
            }
            if (DPI_SCALING_BOOTSTRAPPED_PROPERTY.equals(propertyName)) {
                continue;
            }
            var value = System.getProperty(propertyName);
            if (value == null) {
                continue;
            }
            command.add("-D" + propertyName + "=" + value);
        }
    }

    private static void appendSystemPropertyIfPresent(List<String> command, String propertyName) {
        var value = System.getProperty(propertyName);
        if (value != null) {
            command.add("-D" + propertyName + "=" + value);
        }
    }

    private static boolean hasNativeAccessFlag() {
        for (var argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument != null && argument.startsWith("--enable-native-access=")) {
                return true;
            }
        }
        return false;
    }

    private static Path applicationPath() {
        try {
            return Path.of(ReMEXA.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path javaBinary() {
        var executable = System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable)
                .toAbsolutePath()
                .normalize();
    }

    private static String configuredLegacyEncoding() {
        var configured = System.getProperty("remexa.encoding", DEFAULT_LEGACY_ENCODING).trim();
        return configured.isEmpty() ? DEFAULT_LEGACY_ENCODING : configured;
    }

    private static void launchDirect(Path jadPath, boolean showHostDetails) {
        launchDirect(jadPath, showHostDetails, null, 0, false);
    }

    private static void launchDirect(
            Path jadPath,
            boolean showHostDetails,
            Path captureFramePath,
            int captureDelayMs,
            boolean exitAfterCapture
    ) {
        if (!Files.exists(jadPath)) {
            DebugLog.log(
                    LogCategory.HOST,
                    ReMEXA.class.getName(),
                    "JAD file does not exist: " + jadPath.toAbsolutePath()
            );
            System.err.println("ReMEXA launch failed: JAD file does not exist: " + jadPath.toAbsolutePath());
            return;
        }
        SwingUtilities.invokeLater(() -> new JadLauncher(
                true,
                showHostDetails,
                captureFramePath,
                captureDelayMs,
                exitAfterCapture
        ).launch(jadPath));
    }

    private static LaunchRequest parseLaunchRequest(List<String> arguments) {
        Path directJad = null;
        boolean showHostDetails = false;
        boolean directLaunchRequested = false;
        Path captureFramePath = null;
        int captureDelayMs = 0;
        boolean exitAfterCapture = false;
        LaunchConfig.FontType fontType = null;
        LaunchConfig.JskyPhoneType jskyPhoneType = null;
        LaunchConfig.VodafonePhoneType vodafonePhoneType = null;
        LaunchConfig.MexaPhoneType mexaPhoneType = null;
        Integer hostScale = null;
        LaunchConfig.FrameRateOption frameRateOption = null;
        LaunchConfig.BluetoothBackend bluetoothBackend = null;
        LaunchConfig.BluetoothRole bluetoothRole = null;
        String bluetoothLocalName = null;
        String bluetoothRemoteHost = null;
        Integer bluetoothPort = null;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);
            if ("--show-host-details".equals(argument)) {
                showHostDetails = true;
                continue;
            }
            if ("--midi-synth".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --midi-synth requires 'host', 'ma3', or 'ma5'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, null, null, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                index++;
                continue;
            }
            if ("--disable-dpi-scaling".equals(argument) || "--enable-dpi-scaling".equals(argument)) {
                continue;
            }
            if ("--font".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --font requires 'bitmap' or 'system'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, null, null, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                var candidate = LaunchConfig.FontType.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported font type. Use 'bitmap' or 'system'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, null, null, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                fontType = candidate;
                continue;
            }
            if ("--jsky-phone".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --jsky-phone requires 'JSKY-Generic' or 'J-SH53'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, null, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                var candidate = LaunchConfig.JskyPhoneType.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported JSKY phone type. Use 'JSKY-Generic' or 'J-SH53'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, null, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                jskyPhoneType = candidate;
                continue;
            }
            if ("--vodafone-phone".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --vodafone-phone requires 'Vodafone-Generic' or 'V604SH'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                var candidate = LaunchConfig.VodafonePhoneType.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported Vodafone phone type. Use 'Vodafone-Generic' or 'V604SH'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, null, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                vodafonePhoneType = candidate;
                continue;
            }
            if ("--mexa-phone".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --mexa-phone requires 'MEXA-Generic' or '930SH'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                var candidate = LaunchConfig.MexaPhoneType.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported MEXA phone type. Use 'MEXA-Generic' or '930SH'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, null, hostScale, frameRateOption, null, null, null, null, null);
                }
                mexaPhoneType = candidate;
                continue;
            }
            if ("--host-scale".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --host-scale requires a value from 1 to 5.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, null, null, null, null, null, null, null);
                }
                var candidate = LaunchConfig.parseHostScale(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported host scale. Use 1 to 5.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, null, null, null, null, null, null, null);
                }
                hostScale = candidate;
                continue;
            }
            if ("--frame-rate".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --frame-rate requires one of uncapped, 5, 10, 15, 20, 30, or 60.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, null, null, null, null, null, null);
                }
                var candidate = LaunchConfig.parseFrameRateOption(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported frame rate. Use uncapped, 5, 10, 15, 20, 30, or 60.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, null, null, null, null, null, null);
                }
                frameRateOption = candidate;
                continue;
            }
            if ("--bluetooth-backend".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --bluetooth-backend requires 'off' or 'virtual-ip'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, null, null, null, null, null);
                }
                var candidate = LaunchConfig.BluetoothBackend.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported Bluetooth backend. Use 'off' or 'virtual-ip'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, null, null, null, null, null);
                }
                bluetoothBackend = candidate;
                continue;
            }
            if ("--bluetooth-role".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --bluetooth-role requires 'host' or 'client'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, null, null, null, null);
                }
                var candidate = LaunchConfig.BluetoothRole.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported Bluetooth role. Use 'host' or 'client'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, null, null, null, null);
                }
                bluetoothRole = candidate;
                continue;
            }
            if ("--bluetooth-local-name".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --bluetooth-local-name requires a value.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, null, null, null);
                }
                bluetoothLocalName = LaunchConfig.normalizeBluetoothLocalName(arguments.get(++index));
                continue;
            }
            if ("--bluetooth-host".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --bluetooth-host requires a value.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, null, null);
                }
                bluetoothRemoteHost = LaunchConfig.normalizeBluetoothRemoteHost(arguments.get(++index));
                continue;
            }
            if ("--bluetooth-port".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --bluetooth-port requires a numeric value.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, bluetoothRemoteHost, null);
                }
                var candidate = LaunchConfig.parseBluetoothPort(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported Bluetooth port. Use 1 to 65535.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, bluetoothRemoteHost, null);
                }
                bluetoothPort = candidate;
                continue;
            }
            if ("--capture-frame".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --capture-frame requires an output path.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, bluetoothRemoteHost, bluetoothPort);
                }
                captureFramePath = Path.of(arguments.get(++index));
                continue;
            }
            if ("--capture-after-ms".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --capture-after-ms requires a numeric value.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, bluetoothRemoteHost, bluetoothPort);
                }
                captureDelayMs = Integer.parseInt(arguments.get(++index));
                continue;
            }
            if ("--exit-after-capture".equals(argument)) {
                exitAfterCapture = true;
                continue;
            }
            if ("--run-jad".equals(argument)) {
                directLaunchRequested = true;
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --run-jad requires a JAD path.");
                    return new LaunchRequest(null, showHostDetails, true, captureFramePath, captureDelayMs, exitAfterCapture, fontType, jskyPhoneType, vodafonePhoneType, mexaPhoneType, hostScale, frameRateOption, bluetoothBackend, bluetoothRole, bluetoothLocalName, bluetoothRemoteHost, bluetoothPort);
                }
                directJad = Path.of(arguments.get(++index));
                continue;
            }
            if (argument.toLowerCase().endsWith(".jad")) {
                directLaunchRequested = true;
                directJad = Path.of(argument);
            }
        }

        return new LaunchRequest(
                directJad,
                showHostDetails,
                directLaunchRequested,
                captureFramePath,
                captureDelayMs,
                exitAfterCapture,
                fontType,
                jskyPhoneType,
                vodafonePhoneType,
                mexaPhoneType,
                hostScale,
                frameRateOption,
                bluetoothBackend,
                bluetoothRole,
                bluetoothLocalName,
                bluetoothRemoteHost,
                bluetoothPort
        );
    }

    private static Boolean parseDisableDpiScalingOverride(List<String> arguments) {
        Boolean override = null;
        for (var argument : arguments) {
            if ("--disable-dpi-scaling".equals(argument)) {
                override = true;
            } else if ("--enable-dpi-scaling".equals(argument)) {
                override = false;
            }
        }
        return override;
    }

    private static LaunchConfig.MidiSynthType parseMidiSynthOverride(List<String> arguments) {
        LaunchConfig.MidiSynthType override = null;
        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);
            if (!"--midi-synth".equals(argument)) {
                continue;
            }
            if (index + 1 >= arguments.size()) {
                System.err.println("ReMEXA launch failed: --midi-synth requires 'host', 'ma3', or 'ma5'.");
                return override;
            }
            var candidate = LaunchConfig.MidiSynthType.fromId(arguments.get(++index));
            if (candidate == null) {
                System.err.println("ReMEXA launch failed: unsupported MIDI synth. Use 'host', 'ma3', or 'ma5'.");
                return override;
            }
            override = candidate;
        }
        return override;
    }

    private record LaunchRequest(
            Path jadPath,
            boolean showHostDetails,
            boolean directLaunchRequested,
            Path captureFramePath,
            int captureDelayMs,
            boolean exitAfterCapture,
            LaunchConfig.FontType fontType,
            LaunchConfig.JskyPhoneType jskyPhoneType,
            LaunchConfig.VodafonePhoneType vodafonePhoneType,
            LaunchConfig.MexaPhoneType mexaPhoneType,
            Integer hostScale,
            LaunchConfig.FrameRateOption frameRateOption,
            LaunchConfig.BluetoothBackend bluetoothBackend,
            LaunchConfig.BluetoothRole bluetoothRole,
            String bluetoothLocalName,
            String bluetoothRemoteHost,
            Integer bluetoothPort
    ) {
    }
}
