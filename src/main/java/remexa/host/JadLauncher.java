package remexa.host;

import java.nio.file.Path;
import javax.swing.JOptionPane;
import remexa.host.jad.JadDescriptor;
import remexa.host.jad.JadParser;
import remexa.host.jad.RecentJadsRepository;
import remexa.host.runtime.AppRuntime;
import remexa.host.runtime.LaunchException;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class JadLauncher {
    private final RecentJadsRepository recentJads = new RecentJadsRepository();
    private final AppRuntime runtime = new AppRuntime();
    private final boolean consoleLaunch;

    public JadLauncher() {
        this(false);
    }

    public JadLauncher(boolean consoleLaunch) {
        this.consoleLaunch = consoleLaunch;
    }

    public void launch(Path jadPath) {
        try {
            var descriptor = JadParser.parse(jadPath);
            recentJads.remember(descriptor);
            openFrame(descriptor);
        } catch (Exception exception) {
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Launch failed: " + exception.getMessage());
            if (consoleLaunch) {
                System.err.println("ReMEXA launch failed: " + exception.getMessage());
                exception.printStackTrace(System.err);
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        exception.getMessage(),
                        "ReMEXA Launch Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public RecentJadsRepository recentJads() {
        return recentJads;
    }

    private void openFrame(JadDescriptor descriptor) throws LaunchException {
        var frame = new JadFrame(descriptor);
        try {
            frame.showFrame();
            frame.updateStatus("Loading " + descriptor.title());
            var result = runtime.launch(descriptor);
            frame.updateStatus("Loaded " + result.entryClass());
            DebugLog.log(LogCategory.HOST, JadLauncher.class.getName(), "Loaded entry class: " + result.entryClass());
        } catch (LaunchException exception) {
            if (consoleLaunch) {
                frame.dispose();
            } else {
                frame.updateStatus("Launch failed");
            }
            throw exception;
        }
    }
}
