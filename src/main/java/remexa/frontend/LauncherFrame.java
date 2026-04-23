package remexa.frontend;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import remexa.host.JadLauncher;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogSettings;

public final class LauncherFrame extends JFrame {
    private static final Color APP_BACKGROUND = new Color(242, 239, 233);
    private static final Color MENU_BACKGROUND = new Color(250, 248, 244);
    private static final Color MENU_BORDER = new Color(221, 216, 207);
    private static final Color CARD_BACKGROUND = new Color(252, 251, 248);
    private static final Color CARD_BORDER = new Color(197, 190, 178);
    private static final Color CARD_BORDER_ACTIVE = new Color(52, 119, 89);
    private static final Color MENU_HOVER_BACKGROUND = new Color(228, 234, 226);
    private static final Color MENU_HOVER_FOREGROUND = new Color(35, 77, 58);
    private static final Color POPUP_BACKGROUND = new Color(250, 248, 244);
    private static final Color POPUP_BORDER = new Color(208, 201, 191);
    private static final Color TEXT_PRIMARY = new Color(44, 42, 37);
    private static final Color TEXT_SECONDARY = new Color(108, 102, 92);

    private final JadLauncher launcher;
    private final JMenu recentJadsMenu = new JMenu("Recent JAD");
    private final DropPanel dropPanel = new DropPanel();

    public LauncherFrame(JadLauncher launcher) {
        super("ReMEXA Launcher");
        this.launcher = launcher;

        installMenuTheme();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());

        var content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        content.setBackground(APP_BACKGROUND);
        dropPanel.setPreferredSize(new Dimension(520, 300));
        content.add(dropPanel, BorderLayout.CENTER);

        setTransferHandler(new JadTransferHandler(dropPanel));
        dropPanel.setTransferHandler(getTransferHandler());
        add(content, BorderLayout.CENTER);
        refreshRecents();
        pack();
        setLocationByPlatform(true);
    }

    private void chooseAndLaunch() {
        var dialog = new FileDialog((Frame) null, "Open JAD", FileDialog.LOAD);
        dialog.setDirectory(Path.of(System.getProperty("user.home")).toString());
        dialog.setFile("*.jad");
        dialog.setFilenameFilter((directory, name) -> name != null && name.toLowerCase().endsWith(".jad"));
        dialog.setVisible(true);

        if (dialog.getFile() != null) {
            var selected = Path.of(dialog.getDirectory(), dialog.getFile());
            launch(selected);
        }
    }

    private void launch(Path jadPath) {
        DebugLog.log(LogCategory.FRONTEND, LauncherFrame.class.getName(), "Launching from frontend: " + jadPath);
        launcher.launch(jadPath);
        refreshRecents();
    }

    private void refreshRecents() {
        recentJadsMenu.removeAll();
        var entries = launcher.recentJads().load();
        if (entries.isEmpty()) {
            var emptyItem = new JMenuItem("No recent JADs");
            emptyItem.setEnabled(false);
            recentJadsMenu.add(emptyItem);
            return;
        }

        for (var entry : entries) {
            var item = new JMenuItem(entry.title());
            item.setToolTipText(entry.jadPath().toString());
            item.addActionListener(event -> launch(entry.jadPath()));
            recentJadsMenu.add(item);
        }
    }

    private JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();
        menuBar.setOpaque(true);
        menuBar.setBackground(MENU_BACKGROUND);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, MENU_BORDER),
                new EmptyBorder(7, 10, 7, 10)
        ));

        var fileMenu = new JMenu("File");
        styleMenu(fileMenu);
        var openJadItem = new JMenuItem("Open JAD...");
        styleMenuItem(openJadItem);
        openJadItem.addActionListener(event -> chooseAndLaunch());
        fileMenu.add(openJadItem);
        styleMenu(recentJadsMenu);
        fileMenu.add(recentJadsMenu);
        menuBar.add(fileMenu);

        var settingsMenu = new JMenu("Settings");
        styleMenu(settingsMenu);
        var debugMenu = new JMenu("Debug Categories");
        styleMenu(debugMenu);
        for (var category : LogCategory.values()) {
            var categoryItem = new JCheckBoxMenuItem(category.name().replace('_', ' '), LogSettings.isEnabled(category));
            styleMenuItem(categoryItem);
            categoryItem.addActionListener(event -> {
                LogSettings.setEnabled(category, categoryItem.isSelected());
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Debug category " + category + " set to " + categoryItem.isSelected()
                );
            });
            debugMenu.add(categoryItem);
        }
        settingsMenu.add(debugMenu);
        menuBar.add(settingsMenu);
        menuBar.add(Box.createHorizontalGlue());

        var brand = new JLabel("ReMEXA");
        brand.setForeground(TEXT_SECONDARY);
        brand.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brand.setBorder(new EmptyBorder(0, 8, 0, 2));
        menuBar.add(brand);

        return menuBar;
    }

    private void styleMenu(JMenu menu) {
        menu.setOpaque(false);
        menu.setForeground(TEXT_PRIMARY);
        menu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        menu.getPopupMenu().setBackground(POPUP_BACKGROUND);
        menu.getPopupMenu().setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, POPUP_BORDER),
                new EmptyBorder(6, 6, 6, 6)
        ));
    }

    private void styleMenuItem(javax.swing.AbstractButton item) {
        item.setOpaque(true);
        item.setBackground(POPUP_BACKGROUND);
        item.setForeground(TEXT_PRIMARY);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(8, 12, 8, 12));
    }

    private void installMenuTheme() {
        UIManager.put("MenuBar.background", MENU_BACKGROUND);
        UIManager.put("MenuBar.borderColor", MENU_BORDER);

        UIManager.put("Menu.background", MENU_BACKGROUND);
        UIManager.put("Menu.foreground", TEXT_PRIMARY);
        UIManager.put("Menu.selectionBackground", MENU_HOVER_BACKGROUND);
        UIManager.put("Menu.selectionForeground", MENU_HOVER_FOREGROUND);

        UIManager.put("MenuItem.background", POPUP_BACKGROUND);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", MENU_HOVER_BACKGROUND);
        UIManager.put("MenuItem.selectionForeground", MENU_HOVER_FOREGROUND);

        UIManager.put("CheckBoxMenuItem.background", POPUP_BACKGROUND);
        UIManager.put("CheckBoxMenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("CheckBoxMenuItem.selectionBackground", MENU_HOVER_BACKGROUND);
        UIManager.put("CheckBoxMenuItem.selectionForeground", MENU_HOVER_FOREGROUND);

        UIManager.put("PopupMenu.background", POPUP_BACKGROUND);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(POPUP_BORDER, 1));
    }

    private final class JadTransferHandler extends TransferHandler {
        private final DropPanel panel;

        private JadTransferHandler(DropPanel panel) {
            this.panel = panel;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            var canImport = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            panel.setDragActive(canImport);
            return canImport;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                panel.setDragActive(false);
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (files.isEmpty()) {
                    panel.setDragActive(false);
                    return false;
                }
                launch(files.getFirst().toPath());
                panel.setDragActive(false);
                return true;
            } catch (Exception exception) {
                panel.setDragActive(false);
                DebugLog.log(LogCategory.FRONTEND, LauncherFrame.class.getName(), "Drag-and-drop failed: " + exception.getMessage());
                JOptionPane.showMessageDialog(
                        LauncherFrame.this,
                        exception.getMessage(),
                        "Drop Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
        }

        @Override
        protected void exportDone(javax.swing.JComponent source, java.awt.datatransfer.Transferable data, int action) {
            panel.setDragActive(false);
        }
    }

    private static final class DropPanel extends JPanel {
        private final JLabel eyebrow;
        private final JLabel subtitle;
        private final JLabel hint;
        private boolean dragActive;

        private DropPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());

            eyebrow = new JLabel("JAD Launcher", SwingConstants.CENTER);
            eyebrow.setForeground(TEXT_SECONDARY);
            eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));

            subtitle = new JLabel(
                    "<html><div style='text-align:center;'>Drag a <b>.jad</b> file into this space to launch it.</div></html>",
                    SwingConstants.CENTER
            );
            subtitle.setForeground(TEXT_PRIMARY);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 20));

            hint = new JLabel(
                    "<html><div style='text-align:center;'>Use <b>File</b> -> <b>Open JAD...</b> for manual selection.<br/>Your recent JADs stay available from the menu.</div></html>",
                    SwingConstants.CENTER
            );
            hint.setForeground(TEXT_SECONDARY);
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            var content = new JPanel(new BorderLayout(0, 16));
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(34, 34, 34, 34));
            content.add(eyebrow, BorderLayout.NORTH);
            content.add(subtitle, BorderLayout.CENTER);
            content.add(hint, BorderLayout.SOUTH);
            add(content, BorderLayout.CENTER);
        }

        private void setDragActive(boolean dragActive) {
            this.dragActive = dragActive;
            eyebrow.setText(dragActive ? "Release to Launch" : "JAD Launcher");
            subtitle.setText(dragActive
                    ? "<html><div style='text-align:center;'>Drop the file to open it in ReMEXA.</div></html>"
                    : "<html><div style='text-align:center;'>Drag a <b>.jad</b> file into this space to launch it.</div></html>");
            hint.setText(dragActive
                    ? "<html><div style='text-align:center;'>The selected JAD will be added to your recent list automatically.</div></html>"
                    : "<html><div style='text-align:center;'>Use <b>File</b> -> <b>Open JAD...</b> for manual selection.<br/>Your recent JADs stay available from the menu.</div></html>");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(228, 223, 214));
                g2.fillRoundRect(10, 14, getWidth() - 20, getHeight() - 20, 34, 34);

                g2.setColor(CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 8, 34, 34);

                g2.setStroke(new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{9f, 9f}, 0f));
                g2.setColor(dragActive ? CARD_BORDER_ACTIVE : CARD_BORDER);
                g2.drawRoundRect(16, 16, getWidth() - 33, getHeight() - 39, 24, 24);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(236, 232, 224));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 8, 34, 34);
                if (dragActive) {
                    g2.setColor(new Color(52, 119, 89, 24));
                    g2.fillRoundRect(16, 16, getWidth() - 33, getHeight() - 39, 24, 24);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
