package remexa.frontend;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import remexa.host.JadLauncher;
import remexa.host.jad.RecentJadEntry;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class LauncherFrame extends JFrame {
    private final JadLauncher launcher;
    private final DefaultListModel<RecentJadEntry> recentsModel = new DefaultListModel<>();

    public LauncherFrame(JadLauncher launcher) {
        super("ReMEXA Launcher");
        this.launcher = launcher;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        setLayout(new BorderLayout(12, 12));

        var content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        var hero = new JLabel("<html><h1>ReMEXA</h1><p>Drop a JAD file here or open one manually.</p></html>");
        content.add(hero, BorderLayout.NORTH);

        var recentsList = new JList<>(recentsModel);
        recentsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    var selected = recentsList.getSelectedValue();
                    if (selected != null) {
                        launch(selected.jadPath());
                    }
                }
            }
        });

        var center = new JPanel(new BorderLayout(12, 12));
        center.setBorder(BorderFactory.createTitledBorder("Recent JADs"));
        center.add(new JScrollPane(recentsList), BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);

        var openButton = new JButton("Open JAD...");
        openButton.addActionListener(event -> chooseAndLaunch());

        var footer = new JPanel(new BorderLayout());
        footer.add(openButton, BorderLayout.WEST);
        footer.add(new JLabel("Supports drag and drop plus direct --run-jad launching."), BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        setTransferHandler(new JadTransferHandler());
        add(content, BorderLayout.CENTER);
        refreshRecents();
        pack();
        setLocationByPlatform(true);
    }

    private void chooseAndLaunch() {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            launch(chooser.getSelectedFile().toPath());
        }
    }

    private void launch(Path jadPath) {
        DebugLog.log(LogCategory.FRONTEND, LauncherFrame.class.getName(), "Launching from frontend: " + jadPath);
        launcher.launch(jadPath);
        refreshRecents();
    }

    private void refreshRecents() {
        recentsModel.clear();
        for (var entry : launcher.recentJads().load()) {
            recentsModel.addElement(entry);
        }
    }

    private final class JadTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (files.isEmpty()) {
                    return false;
                }
                launch(files.getFirst().toPath());
                return true;
            } catch (Exception exception) {
                DebugLog.log(LogCategory.FRONTEND, LauncherFrame.class.getName(), "Drag-and-drop failed: " + exception.getMessage());
                return false;
            }
        }
    }
}
