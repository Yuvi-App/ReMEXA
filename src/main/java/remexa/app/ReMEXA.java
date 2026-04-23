package remexa.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;
import remexa.frontend.LauncherFrame;
import remexa.host.JadLauncher;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class ReMEXA {
    private ReMEXA() {
    }

    public static void main(String[] args) {
        var arguments = List.of(args);
        if (arguments.size() >= 2 && "--run-jad".equals(arguments.getFirst())) {
            launchDirect(Path.of(arguments.get(1)));
            return;
        }
        if (arguments.size() == 1 && arguments.getFirst().toLowerCase().endsWith(".jad")) {
            launchDirect(Path.of(arguments.getFirst()));
            return;
        }

        DebugLog.log(LogCategory.HOST, ReMEXA.class.getName(), "Launching desktop frontend");
        SwingUtilities.invokeLater(() -> new LauncherFrame(new JadLauncher()).setVisible(true));
    }

    private static void launchDirect(Path jadPath) {
        if (!Files.exists(jadPath)) {
            DebugLog.log(
                    LogCategory.HOST,
                    ReMEXA.class.getName(),
                    "JAD file does not exist: " + jadPath.toAbsolutePath()
            );
            return;
        }
        SwingUtilities.invokeLater(() -> new JadLauncher().launch(jadPath));
    }
}
