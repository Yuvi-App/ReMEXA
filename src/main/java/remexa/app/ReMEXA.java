package remexa.app;

import java.io.IOException;
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

public final class ReMEXA {
    private static final String DEFAULT_LEGACY_ENCODING = "windows-31j";
    private static final String ENCODING_BOOTSTRAPPED_PROPERTY = "remexa.encoding.applied";

    private ReMEXA() {
    }

    public static void main(String[] args) {
        bootstrapLegacyEncodingIfNeeded(args);
        var arguments = List.of(args);
        var launchRequest = parseLaunchRequest(arguments);
        LaunchConfig.applyFontType(launchRequest.fontType() == null ? HostUiSettings.fontType() : launchRequest.fontType());
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

    private static void bootstrapLegacyEncodingIfNeeded(String[] args) {
        var configured = configuredLegacyEncoding();
        if (Boolean.getBoolean(ENCODING_BOOTSTRAPPED_PROPERTY)) {
            return;
        }
        if (Charset.defaultCharset().name().equalsIgnoreCase(configured)) {
            return;
        }

        try {
            var command = new ArrayList<String>();
            command.add(javaBinary().toString());
            command.add("-Dfile.encoding=" + configured);
            command.add("-Dnative.encoding=" + configured);
            command.add("-D" + ENCODING_BOOTSTRAPPED_PROPERTY + "=true");
            appendCurrentLaunchTarget(command);
            command.addAll(List.of(args));

            var exitCode = new ProcessBuilder(command)
                    .inheritIO()
                    .start()
                    .waitFor();
            System.exit(exitCode);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to bootstrap legacy encoding.", exception);
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

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);
            if ("--show-host-details".equals(argument)) {
                showHostDetails = true;
                continue;
            }
            if ("--font".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --font requires 'bitmap' or 'system'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, null);
                }
                var candidate = LaunchConfig.FontType.fromId(arguments.get(++index));
                if (candidate == null) {
                    System.err.println("ReMEXA launch failed: unsupported font type. Use 'bitmap' or 'system'.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, null);
                }
                fontType = candidate;
                continue;
            }
            if ("--capture-frame".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --capture-frame requires an output path.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType);
                }
                captureFramePath = Path.of(arguments.get(++index));
                continue;
            }
            if ("--capture-after-ms".equals(argument)) {
                if (index + 1 >= arguments.size()) {
                    System.err.println("ReMEXA launch failed: --capture-after-ms requires a numeric value.");
                    return new LaunchRequest(null, showHostDetails, true, null, 0, false, fontType);
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
                    return new LaunchRequest(null, showHostDetails, true, captureFramePath, captureDelayMs, exitAfterCapture, fontType);
                }
                directJad = Path.of(arguments.get(++index));
                continue;
            }
            if (argument.toLowerCase().endsWith(".jad")) {
                directLaunchRequested = true;
                directJad = Path.of(argument);
            }
        }

        return new LaunchRequest(directJad, showHostDetails, directLaunchRequested, captureFramePath, captureDelayMs, exitAfterCapture, fontType);
    }

    private record LaunchRequest(
            Path jadPath,
            boolean showHostDetails,
            boolean directLaunchRequested,
            Path captureFramePath,
            int captureDelayMs,
            boolean exitAfterCapture,
            LaunchConfig.FontType fontType
    ) {
    }
}
