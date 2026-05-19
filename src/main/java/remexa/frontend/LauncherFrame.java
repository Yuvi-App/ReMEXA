package remexa.frontend;

import com.j_phone.io.StoragePathSupport;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import remexa.host.JadLauncher;
import remexa.host.HostUiSettings;
import remexa.host.LaunchConfig;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogSettings;
import remexa.ui.AppIcons;
import remexa.ui.RemexaTheme;

public final class LauncherFrame extends JFrame {
    private static final String DEFAULT_VERSION = "0.1.1";
    private static final Color APP_BACKGROUND = RemexaTheme.APP_BACKGROUND;
    private static final Color MENU_BACKGROUND = RemexaTheme.MENU_BACKGROUND;
    private static final Color MENU_BORDER = RemexaTheme.MENU_BORDER;
    private static final Color CARD_BACKGROUND = RemexaTheme.CARD_BACKGROUND;
    private static final Color CARD_BORDER = RemexaTheme.CARD_BORDER;
    private static final Color CARD_BORDER_ACTIVE = RemexaTheme.CARD_BORDER_ACTIVE;
    private static final Color MENU_HOVER_BACKGROUND = RemexaTheme.MENU_HOVER_BACKGROUND;
    private static final Color MENU_HOVER_FOREGROUND = RemexaTheme.MENU_HOVER_FOREGROUND;
    private static final Color POPUP_BACKGROUND = RemexaTheme.POPUP_BACKGROUND;
    private static final Color POPUP_BORDER = RemexaTheme.POPUP_BORDER;
    private static final Color TEXT_PRIMARY = RemexaTheme.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = RemexaTheme.TEXT_SECONDARY;

    private final JadLauncher launcher;
    private final JMenu recentJadsMenu = new JMenu("Recent JAD");
    private final DropPanel dropPanel = new DropPanel();

    public LauncherFrame(JadLauncher launcher) {
        super("ReMEXA Launcher");
        this.launcher = launcher;

        AppIcons.applyTo(this);
        installMenuTheme();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());

        var content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        content.setBackground(APP_BACKGROUND);
        dropPanel.setPreferredSize(new Dimension(560, 320));
        content.add(dropPanel, BorderLayout.CENTER);

        setTransferHandler(new JadTransferHandler(dropPanel));
        dropPanel.setTransferHandler(getTransferHandler());
        add(content, BorderLayout.CENTER);
        refreshRecents();
        pack();
        setLocationRelativeTo(null);
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
        if (!isJadPath(jadPath)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only .jad files can be opened in ReMEXA.",
                    "Unsupported File Type",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        DebugLog.log(LogCategory.FRONTEND, LauncherFrame.class.getName(), "Launching from frontend: " + jadPath);
        launcher.launch(jadPath);
        refreshRecents();
    }

    private static boolean isJadPath(Path path) {
        if (path == null) {
            return false;
        }
        var fileName = path.getFileName();
        return fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(".jad");
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
        var displayMenu = new JMenu("Display");
        styleMenu(displayMenu);
        var fontMenu = new JMenu("Font");
        styleMenu(fontMenu);
        var fontGroup = new ButtonGroup();
        for (var fontType : LaunchConfig.FontType.values()) {
            var fontItem = new JRadioButtonMenuItem(fontType.toString(), HostUiSettings.fontType() == fontType);
            styleMenuItem(fontItem);
            fontGroup.add(fontItem);
            fontItem.addActionListener(event -> {
                HostUiSettings.setFontType(fontType);
                LaunchConfig.applyFontType(fontType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Font type set to " + fontType.id()
                );
            });
            fontMenu.add(fontItem);
        }
        displayMenu.add(fontMenu);
        var hostScaleMenu = new JMenu("Host Scale");
        styleMenu(hostScaleMenu);
        var hostScaleGroup = new ButtonGroup();
        for (int scale = LaunchConfig.MIN_HOST_SCALE; scale <= LaunchConfig.MAX_HOST_SCALE; scale++) {
            var hostScale = scale;
            var scaleItem = new JRadioButtonMenuItem(hostScale + "x", HostUiSettings.hostScale() == hostScale);
            styleMenuItem(scaleItem);
            hostScaleGroup.add(scaleItem);
            scaleItem.addActionListener(event -> {
                HostUiSettings.setHostScale(hostScale);
                LaunchConfig.applyHostScale(hostScale);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Host scale set to " + hostScale + "x"
                );
            });
            hostScaleMenu.add(scaleItem);
        }
        displayMenu.add(hostScaleMenu);
        var frameRateMenu = new JMenu("Frame Rate");
        styleMenu(frameRateMenu);
        var frameRateGroup = new ButtonGroup();
        for (var frameRateOption : LaunchConfig.FrameRateOption.values()) {
            var frameRateItem = new JRadioButtonMenuItem(
                    frameRateOption.toString(),
                    HostUiSettings.frameRateOption() == frameRateOption
            );
            styleMenuItem(frameRateItem);
            frameRateGroup.add(frameRateItem);
            frameRateItem.addActionListener(event -> {
                HostUiSettings.setFrameRateOption(frameRateOption);
                LaunchConfig.applyFrameRateOption(frameRateOption);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Frame rate set to " + frameRateOption.id()
                );
            });
            frameRateMenu.add(frameRateItem);
        }
        displayMenu.add(frameRateMenu);
        var disableDpiScalingItem = new JCheckBoxMenuItem(
                "Disable DPI Scaling",
                HostUiSettings.disableDpiScaling()
        );
        styleMenuItem(disableDpiScalingItem);
        disableDpiScalingItem.addActionListener(event -> {
            HostUiSettings.setDisableDpiScaling(disableDpiScalingItem.isSelected());
            LaunchConfig.applyDisableDpiScaling(disableDpiScalingItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Disable DPI scaling set to " + disableDpiScalingItem.isSelected()
            );
        });
        displayMenu.add(disableDpiScalingItem);
        settingsMenu.add(displayMenu);

        var deviceMenu = new JMenu("Device");
        styleMenu(deviceMenu);
        var jskyPhoneMenu = new JMenu("JSKY Phone Type");
        styleMenu(jskyPhoneMenu);
        var jskyPhoneGroup = new ButtonGroup();
        for (var phoneType : LaunchConfig.JskyPhoneType.values()) {
            var phoneItem = new JRadioButtonMenuItem(phoneType.toString(), HostUiSettings.jskyPhoneType() == phoneType);
            styleMenuItem(phoneItem);
            jskyPhoneGroup.add(phoneItem);
            phoneItem.addActionListener(event -> {
                HostUiSettings.setJskyPhoneType(phoneType);
                LaunchConfig.applyJskyPhoneType(phoneType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "JSKY phone type set to " + phoneType.platformName()
                );
            });
            jskyPhoneMenu.add(phoneItem);
        }
        deviceMenu.add(jskyPhoneMenu);
        var vodafonePhoneMenu = new JMenu("Vodafone Phone Type");
        styleMenu(vodafonePhoneMenu);
        var vodafonePhoneGroup = new ButtonGroup();
        for (var phoneType : LaunchConfig.VodafonePhoneType.values()) {
            var phoneItem = new JRadioButtonMenuItem(phoneType.toString(), HostUiSettings.vodafonePhoneType() == phoneType);
            styleMenuItem(phoneItem);
            vodafonePhoneGroup.add(phoneItem);
            phoneItem.addActionListener(event -> {
                HostUiSettings.setVodafonePhoneType(phoneType);
                LaunchConfig.applyVodafonePhoneType(phoneType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Vodafone phone type set to " + phoneType.platformName()
                );
            });
            vodafonePhoneMenu.add(phoneItem);
        }
        deviceMenu.add(vodafonePhoneMenu);
        var mexaPhoneMenu = new JMenu("MEXA Phone Type");
        styleMenu(mexaPhoneMenu);
        var mexaPhoneGroup = new ButtonGroup();
        for (var phoneType : LaunchConfig.MexaPhoneType.values()) {
            var phoneItem = new JRadioButtonMenuItem(phoneType.toString(), HostUiSettings.mexaPhoneType() == phoneType);
            styleMenuItem(phoneItem);
            mexaPhoneGroup.add(phoneItem);
            phoneItem.addActionListener(event -> {
                HostUiSettings.setMexaPhoneType(phoneType);
                LaunchConfig.applyMexaPhoneType(phoneType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "MEXA phone type set to " + phoneType.platformName()
                );
            });
            mexaPhoneMenu.add(phoneItem);
        }
        deviceMenu.add(mexaPhoneMenu);
        settingsMenu.add(deviceMenu);

        var controlsMenu = new JMenu("Controls");
        styleMenu(controlsMenu);
        var touchControlsItem = new JCheckBoxMenuItem("Enable Touch Controls", HostUiSettings.touchControlsEnabled());
        styleMenuItem(touchControlsItem);
        touchControlsItem.addActionListener(event -> {
            HostUiSettings.setTouchControlsEnabled(touchControlsItem.isSelected());
            LaunchConfig.applyTouchControlsEnabled(touchControlsItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Touch controls set to " + touchControlsItem.isSelected()
            );
        });
        controlsMenu.add(touchControlsItem);
        var rotateWidescreenKeysItem = new JCheckBoxMenuItem(
                "Rotate Widescreen Keys",
                HostUiSettings.rotateWidescreenKeysEnabled()
        );
        styleMenuItem(rotateWidescreenKeysItem);
        rotateWidescreenKeysItem.addActionListener(event -> {
            HostUiSettings.setRotateWidescreenKeysEnabled(rotateWidescreenKeysItem.isSelected());
            LaunchConfig.applyRotateWidescreenKeysEnabled(rotateWidescreenKeysItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Rotate widescreen keys set to " + rotateWidescreenKeysItem.isSelected()
            );
        });
        controlsMenu.add(rotateWidescreenKeysItem);
        var motionMenu = new JMenu("Motion");
        styleMenu(motionMenu);
        var motionControlsItem = new JCheckBoxMenuItem("Enable Mouse Motion", HostUiSettings.motionControlsEnabled());
        styleMenuItem(motionControlsItem);
        motionControlsItem.addActionListener(event -> {
            HostUiSettings.setMotionControlsEnabled(motionControlsItem.isSelected());
            LaunchConfig.applyMotionControlsEnabled(motionControlsItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Mouse motion controls set to " + motionControlsItem.isSelected()
            );
        });
        motionMenu.add(motionControlsItem);
        var motionTrackingMenu = new JMenu("Tracking Area");
        styleMenu(motionTrackingMenu);
        var motionTrackingGroup = new ButtonGroup();
        for (var trackingMode : LaunchConfig.MotionTrackingMode.values()) {
            var trackingItem = new JRadioButtonMenuItem(
                    trackingMode.toString(),
                    HostUiSettings.motionTrackingMode() == trackingMode
            );
            styleMenuItem(trackingItem);
            motionTrackingGroup.add(trackingItem);
            trackingItem.addActionListener(event -> {
                HostUiSettings.setMotionTrackingMode(trackingMode);
                LaunchConfig.applyMotionTrackingMode(trackingMode);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Motion tracking area set to " + trackingMode.id()
                );
            });
            motionTrackingMenu.add(trackingItem);
        }
        motionMenu.add(motionTrackingMenu);
        var motionSensitivityMenu = new JMenu("Sensitivity");
        styleMenu(motionSensitivityMenu);
        var motionSensitivityGroup = new ButtonGroup();
        for (int sensitivity : new int[]{50, 75, 100, 150, 200, 300}) {
            var sensitivityPercent = sensitivity;
            var sensitivityItem = new JRadioButtonMenuItem(
                    sensitivityPercent + "%",
                    HostUiSettings.motionSensitivityPercent() == sensitivityPercent
            );
            styleMenuItem(sensitivityItem);
            motionSensitivityGroup.add(sensitivityItem);
            sensitivityItem.addActionListener(event -> {
                HostUiSettings.setMotionSensitivityPercent(sensitivityPercent);
                LaunchConfig.applyMotionSensitivityPercent(sensitivityPercent);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Motion sensitivity set to " + sensitivityPercent + "%"
                );
            });
            motionSensitivityMenu.add(sensitivityItem);
        }
        motionMenu.add(motionSensitivityMenu);
        controlsMenu.add(motionMenu);
        settingsMenu.add(controlsMenu);

        var phoneFeaturesMenu = new JMenu("Phone Features");
        styleMenu(phoneFeaturesMenu);
        var flashBacklightItem = new JCheckBoxMenuItem("Enable Flash Backlight", HostUiSettings.flashBacklightEnabled());
        styleMenuItem(flashBacklightItem);
        flashBacklightItem.addActionListener(event -> {
            HostUiSettings.setFlashBacklightEnabled(flashBacklightItem.isSelected());
            LaunchConfig.applyFlashBacklightEnabled(flashBacklightItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Flash backlight set to " + flashBacklightItem.isSelected()
            );
        });
        phoneFeaturesMenu.add(flashBacklightItem);
        var cameraMenu = new JMenu("Camera");
        styleMenu(cameraMenu);
        var cameraGroup = new ButtonGroup();
        for (var cameraMode : LaunchConfig.CameraInputMode.values()) {
            var cameraItem = new JRadioButtonMenuItem(cameraMode.toString(), HostUiSettings.cameraInputMode() == cameraMode);
            styleMenuItem(cameraItem);
            cameraGroup.add(cameraItem);
            cameraItem.addActionListener(event -> {
                HostUiSettings.setCameraInputMode(cameraMode);
                LaunchConfig.applyCameraInputMode(cameraMode);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Camera input mode set to " + cameraMode.id()
                );
            });
            cameraMenu.add(cameraItem);
        }
        phoneFeaturesMenu.add(cameraMenu);
        settingsMenu.add(phoneFeaturesMenu);

        var audioMenu = new JMenu("Audio");
        styleMenu(audioMenu);
        var audioTypeMenu = new JMenu("Audio Type");
        styleMenu(audioTypeMenu);
        var audioGroup = new ButtonGroup();
        for (var synthType : LaunchConfig.SmafSynthType.values()) {
            var audioItem = new JRadioButtonMenuItem(synthType.toString(), HostUiSettings.smafSynthType() == synthType);
            styleMenuItem(audioItem);
            audioGroup.add(audioItem);
            audioItem.addActionListener(event -> {
                HostUiSettings.setSmafSynthType(synthType);
                LaunchConfig.applySmafSynthType(synthType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "SMAF audio type set to " + synthType.id()
                );
            });
            audioTypeMenu.add(audioItem);
        }
        audioMenu.add(audioTypeMenu);
        var midiTypeMenu = new JMenu("MIDI Type");
        styleMenu(midiTypeMenu);
        var midiGroup = new ButtonGroup();
        for (var midiSynthType : LaunchConfig.MidiSynthType.values()) {
            var midiItem = new JRadioButtonMenuItem(
                    midiSynthType.toString(),
                    HostUiSettings.midiSynthType() == midiSynthType
            );
            styleMenuItem(midiItem);
            midiGroup.add(midiItem);
            midiItem.addActionListener(event -> {
                HostUiSettings.setMidiSynthType(midiSynthType);
                LaunchConfig.applyMidiSynthType(midiSynthType);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "MIDI synth type set to " + midiSynthType.id()
                );
            });
            midiTypeMenu.add(midiItem);
        }
        audioMenu.add(midiTypeMenu);
        settingsMenu.add(audioMenu);

        var connectivityMenu = new JMenu("Connectivity");
        styleMenu(connectivityMenu);
        var bluetoothSettingsItem = new JMenuItem("Bluetooth...");
        styleMenuItem(bluetoothSettingsItem);
        bluetoothSettingsItem.addActionListener(event -> showBluetoothSettingsDialog());
        connectivityMenu.add(bluetoothSettingsItem);
        settingsMenu.add(connectivityMenu);

        var extrasMenu = new JMenu("Extra's");
        styleMenu(extrasMenu);
        var deepLConfigurationItem = new JMenuItem("DeepL Configuration...");
        styleMenuItem(deepLConfigurationItem);
        deepLConfigurationItem.addActionListener(event -> showDeepLConfigurationDialog());
        extrasMenu.add(deepLConfigurationItem);
        settingsMenu.add(extrasMenu);

        var storageMenu = new JMenu("Storage");
        styleMenu(storageMenu);
        var openStorageFolderItem = new JMenuItem("Open Storage Folder");
        styleMenuItem(openStorageFolderItem);
        openStorageFolderItem.addActionListener(event -> openStorageFolder());
        storageMenu.add(openStorageFolderItem);
        settingsMenu.add(storageMenu);

        var debuggingMenu = new JMenu("Debugging");
        styleMenu(debuggingMenu);
        var hostDetailsItem = new JCheckBoxMenuItem("Show Host Details", HostUiSettings.showHostDetails());
        styleMenuItem(hostDetailsItem);
        hostDetailsItem.addActionListener(event -> {
            HostUiSettings.setShowHostDetails(hostDetailsItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Show host details set to " + hostDetailsItem.isSelected()
            );
        });
        debuggingMenu.add(hostDetailsItem);
        debuggingMenu.addSeparator();
        var fpsOverlayItem = new JCheckBoxMenuItem("Show FPS Overlay", HostUiSettings.fpsOverlayEnabled());
        styleMenuItem(fpsOverlayItem);
        fpsOverlayItem.addActionListener(event -> {
            HostUiSettings.setFpsOverlayEnabled(fpsOverlayItem.isSelected());
            LaunchConfig.applyFpsOverlayEnabled(fpsOverlayItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "FPS overlay set to " + fpsOverlayItem.isSelected()
            );
        });
        debuggingMenu.add(fpsOverlayItem);
        debuggingMenu.addSeparator();
        var dumpRmsItem = new JCheckBoxMenuItem("Dump RMS", HostUiSettings.dumpRms());
        styleMenuItem(dumpRmsItem);
        dumpRmsItem.addActionListener(event -> {
            HostUiSettings.setDumpRms(dumpRmsItem.isSelected());
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Debug RMS dump set to " + dumpRmsItem.isSelected()
            );
        });
        debuggingMenu.add(dumpRmsItem);
        debuggingMenu.addSeparator();
        var debugMenu = new JMenu("Log Categories");
        styleMenu(debugMenu);
        var categoryItems = new ArrayList<JCheckBoxMenuItem>();
        var toggleAllLogsItem = new JMenuItem();
        styleMenuItem(toggleAllLogsItem);
        toggleAllLogsItem.addActionListener(event -> {
            boolean enable = !LogSettings.areAllEnabled();
            LogSettings.setAllEnabled(enable);
            updateToggleAllLogsItem(toggleAllLogsItem);
            for (var categoryItem : categoryItems) {
                categoryItem.setSelected(enable);
            }
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "All debug logs set to " + enable
            );
        });
        updateToggleAllLogsItem(toggleAllLogsItem);
        debugMenu.add(toggleAllLogsItem);
        debugMenu.addSeparator();
        for (var category : LogCategory.values()) {
            var categoryItem = new JCheckBoxMenuItem(category.name().replace('_', ' '), LogSettings.isEnabled(category));
            styleMenuItem(categoryItem);
            categoryItem.addActionListener(event -> {
                LogSettings.setEnabled(category, categoryItem.isSelected());
                updateToggleAllLogsItem(toggleAllLogsItem);
                DebugLog.log(
                        LogCategory.FRONTEND,
                        LauncherFrame.class.getName(),
                        "Debug category " + category + " set to " + categoryItem.isSelected()
                );
            });
            categoryItems.add(categoryItem);
            debugMenu.add(categoryItem);
        }
        debuggingMenu.add(debugMenu);
        settingsMenu.add(debuggingMenu);
        menuBar.add(settingsMenu);
        menuBar.add(Box.createHorizontalGlue());

        var brand = new JLabel("ReMEXA-v" + resolveVersionLabel());
        brand.setForeground(TEXT_SECONDARY);
        brand.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brand.setHorizontalAlignment(SwingConstants.RIGHT);
        brand.setBorder(new EmptyBorder(0, 8, 0, 2));
        menuBar.add(brand);

        return menuBar;
    }

    private static String resolveVersionLabel() {
        var implementationVersion = LauncherFrame.class.getPackage().getImplementationVersion();
        if (implementationVersion == null || implementationVersion.isBlank()) {
            return DEFAULT_VERSION;
        }
        return implementationVersion.trim();
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
        item.setHorizontalAlignment(SwingConstants.LEADING);
        item.setHorizontalTextPosition(SwingConstants.RIGHT);
    }

    private void styleLauncherButton(JButton button, boolean primary) {
        button.setFocusable(false);
        button.setOpaque(true);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? CARD_BORDER_ACTIVE : CARD_BORDER, 1),
                new EmptyBorder(9, 14, 9, 14)
        ));
        button.setBackground(primary ? MENU_HOVER_BACKGROUND : POPUP_BACKGROUND);
        button.setForeground(primary ? MENU_HOVER_FOREGROUND : TEXT_PRIMARY);
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

    private void updateToggleAllLogsItem(JMenuItem item) {
        item.setText(LogSettings.areAllEnabled() ? "Disable All Debug Logs" : "Enable All Debug Logs");
    }

    private void showBluetoothSettingsDialog() {
        var backendBox = new JComboBox<>(LaunchConfig.BluetoothBackend.values());
        backendBox.setSelectedItem(HostUiSettings.bluetoothBackend());
        var roleBox = new JComboBox<>(LaunchConfig.BluetoothRole.values());
        roleBox.setSelectedItem(HostUiSettings.bluetoothRole());
        var localNameField = new JTextField(HostUiSettings.bluetoothLocalName(), 18);
        var remoteHostField = new JTextField(HostUiSettings.bluetoothRemoteHost(), 18);
        var portField = new JTextField(Integer.toString(HostUiSettings.bluetoothPort()), 8);

        var panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        var constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        panel.add(new JLabel("Backend"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(backendBox, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("Role"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(roleBox, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("Local Name"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(localNameField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("Remote Host"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(remoteHostField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("Port"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(portField, constraints);

        var note = new JLabel("<html><div style='width:280px;'>Use <b>Host</b> on the machine waiting for peers. Use <b>Client</b> on the joining machine and point <b>Remote Host</b> at the host IP or DNS name.</div></html>");
        note.setForeground(TEXT_SECONDARY);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        panel.add(note, constraints);

        Runnable updateEnabledState = () -> {
            var backend = (LaunchConfig.BluetoothBackend) backendBox.getSelectedItem();
            var role = (LaunchConfig.BluetoothRole) roleBox.getSelectedItem();
            boolean enabled = backend == LaunchConfig.BluetoothBackend.VIRTUAL_IP;
            boolean needsRemoteHost = enabled && role == LaunchConfig.BluetoothRole.CLIENT;
            roleBox.setEnabled(enabled);
            localNameField.setEnabled(enabled);
            remoteHostField.setEnabled(needsRemoteHost);
            portField.setEnabled(enabled);
        };
        backendBox.addActionListener(event -> updateEnabledState.run());
        roleBox.addActionListener(event -> updateEnabledState.run());
        updateEnabledState.run();

        while (true) {
            var result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Bluetooth Over IP",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            var backend = (LaunchConfig.BluetoothBackend) backendBox.getSelectedItem();
            var role = (LaunchConfig.BluetoothRole) roleBox.getSelectedItem();
            var normalizedLocalName = LaunchConfig.normalizeBluetoothLocalName(localNameField.getText());
            var normalizedRemoteHost = LaunchConfig.normalizeBluetoothRemoteHost(remoteHostField.getText());
            var parsedPort = LaunchConfig.parseBluetoothPort(portField.getText().trim());
            if (parsedPort == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Bluetooth port must be a number from 1 to 65535.",
                        "Invalid Bluetooth Port",
                        JOptionPane.ERROR_MESSAGE
                );
                continue;
            }

            HostUiSettings.setBluetoothBackend(backend);
            HostUiSettings.setBluetoothRole(role);
            HostUiSettings.setBluetoothLocalName(normalizedLocalName);
            HostUiSettings.setBluetoothRemoteHost(normalizedRemoteHost);
            HostUiSettings.setBluetoothPort(parsedPort);

            LaunchConfig.applyBluetoothBackend(backend);
            LaunchConfig.applyBluetoothRole(role);
            LaunchConfig.applyBluetoothLocalName(normalizedLocalName);
            LaunchConfig.applyBluetoothRemoteHost(normalizedRemoteHost);
            LaunchConfig.applyBluetoothPort(parsedPort);

            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Bluetooth settings updated: backend=" + backend.id()
                            + ", role=" + role.id()
                            + ", localName=" + normalizedLocalName
                            + ", remoteHost=" + normalizedRemoteHost
                            + ", port=" + parsedPort
            );
            return;
        }
    }

    private void showDeepLConfigurationDialog() {
        var liveTranslationItem = new JCheckBox("Enable Live Translation", HostUiSettings.liveTranslationEnabled());
        liveTranslationItem.setOpaque(false);
        var apiPlanBox = new JComboBox<>(LaunchConfig.DeepLApiPlan.values());
        apiPlanBox.setSelectedItem(HostUiSettings.deepLApiPlan());
        var apiKeyField = new JPasswordField(HostUiSettings.deepLApiKey(), 22);
        var languageBox = new JComboBox<>(LaunchConfig.TranslationTargetLanguage.values());
        languageBox.setSelectedItem(HostUiSettings.translationTargetLanguage());

        var panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        var constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        panel.add(liveTranslationItem, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        panel.add(new JLabel("API Plan"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(apiPlanBox, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("API Key"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(apiKeyField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        panel.add(new JLabel("Target Language"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(languageBox, constraints);

        var note = new JLabel("<html><div style='width:320px;'>Live translation uses DeepL to replace rendered Japanese text on the fly. Choose the correct API plan for your key and keep in mind that some games may still draw text as images or per-character glyphs.</div></html>");
        note.setForeground(TEXT_SECONDARY);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        panel.add(note, constraints);

        while (true) {
            var result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "DeepL Configuration",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            var liveTranslationEnabled = liveTranslationItem.isSelected();
            var apiPlan = (LaunchConfig.DeepLApiPlan) apiPlanBox.getSelectedItem();
            var apiKey = LaunchConfig.normalizeDeepLApiKey(new String(apiKeyField.getPassword()));
            var targetLanguage = (LaunchConfig.TranslationTargetLanguage) languageBox.getSelectedItem();
            if (liveTranslationEnabled && apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "An API key is required when live translation is enabled.",
                        "Missing DeepL API Key",
                        JOptionPane.ERROR_MESSAGE
                );
                continue;
            }

            HostUiSettings.setLiveTranslationEnabled(liveTranslationEnabled);
            HostUiSettings.setDeepLApiPlan(apiPlan);
            HostUiSettings.setDeepLApiKey(apiKey);
            HostUiSettings.setTranslationTargetLanguage(targetLanguage);

            LaunchConfig.applyLiveTranslationEnabled(liveTranslationEnabled);
            LaunchConfig.applyDeepLApiPlan(apiPlan);
            LaunchConfig.applyDeepLApiKey(apiKey);
            LaunchConfig.applyTranslationTargetLanguage(targetLanguage);

            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "DeepL settings updated: enabled=" + liveTranslationEnabled
                            + ", apiPlan=" + (apiPlan == null ? "null" : apiPlan.id())
                            + ", targetLanguage=" + (targetLanguage == null ? "null" : targetLanguage.code())
                            + ", apiKeyPresent=" + !apiKey.isEmpty()
            );
            return;
        }
    }

    private void openStorageFolder() {
        try {
            Path storageFolder = StoragePathSupport.storageRoot().toAbsolutePath().normalize();
            Files.createDirectories(storageFolder);
            openFolderInExplorer(storageFolder);
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Opened storage folder: " + storageFolder
            );
        } catch (IOException exception) {
            DebugLog.log(
                    LogCategory.FRONTEND,
                    LauncherFrame.class.getName(),
                    "Failed to open storage folder: " + exception.getMessage()
            );
            JOptionPane.showMessageDialog(
                    LauncherFrame.this,
                    exception.getMessage(),
                    "Open Storage Folder Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static void openFolderInExplorer(Path folder) throws IOException {
        if (Desktop.isDesktopSupported()) {
            var desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(folder.toFile());
                return;
            }
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            new ProcessBuilder("explorer.exe", folder.toString()).start();
            return;
        }
        if (osName.contains("mac")) {
            new ProcessBuilder("open", folder.toString()).start();
            return;
        }
        new ProcessBuilder("xdg-open", folder.toString()).start();
    }

    private final class JadTransferHandler extends TransferHandler {
        private final DropPanel panel;

        private JadTransferHandler(DropPanel panel) {
            this.panel = panel;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            boolean canImport = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            if (canImport) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    canImport = files != null && !files.isEmpty() && isJadPath(files.getFirst().toPath());
                } catch (Exception ignored) {
                    // Some drag sources expose the file list lazily during hover.
                    // Keep the drop eligible here and enforce the .jad check in importData().
                    canImport = true;
                }
            }
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
                var firstPath = files.getFirst().toPath();
                if (!isJadPath(firstPath)) {
                    panel.setDragActive(false);
                    JOptionPane.showMessageDialog(
                            LauncherFrame.this,
                            "Only .jad files can be dropped into ReMEXA.",
                            "Unsupported File Type",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
                launch(firstPath);
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

    private final class DropPanel extends JPanel {
        private final JLabel eyebrow;
        private final JLabel subtitle;
        private final JLabel hint;
        private final JButton openButton;
        private boolean dragActive;

        private DropPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());

            eyebrow = new JLabel("", SwingConstants.CENTER);
            eyebrow.setForeground(TEXT_SECONDARY);
            eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));

            subtitle = new JLabel(
                    "<html><div style='text-align:center;'>Drop a <b>.jad</b> file here to launch it.</div></html>",
                    SwingConstants.CENTER
            );
            subtitle.setForeground(TEXT_PRIMARY);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 22));

            hint = new JLabel(
                    "<html><div style='text-align:center;'>Open a file manually or drag one in. Recent launches stay available from the <b>File</b> menu.</div></html>",
                    SwingConstants.CENTER
            );
            hint.setForeground(TEXT_SECONDARY);
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            openButton = new JButton("Open JAD...");
            styleLauncherButton(openButton, true);
            openButton.addActionListener(event -> chooseAndLaunch());

            var buttonRow = new JPanel(new GridLayout(1, 1, 0, 0));
            buttonRow.setOpaque(false);
            buttonRow.add(openButton);

            var content = new JPanel(new BorderLayout(0, 18));
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(38, 38, 38, 38));
            content.add(eyebrow, BorderLayout.NORTH);
            var body = new JPanel(new BorderLayout(0, 14));
            body.setOpaque(false);
            body.add(subtitle, BorderLayout.NORTH);
            body.add(hint, BorderLayout.CENTER);
            body.add(buttonRow, BorderLayout.SOUTH);
            content.add(body, BorderLayout.CENTER);
            add(content, BorderLayout.CENTER);
        }

        private void setDragActive(boolean dragActive) {
            this.dragActive = dragActive;
            eyebrow.setText(dragActive ? "Release to Launch" : "Launcher");
            subtitle.setText(dragActive
                    ? "<html><div style='text-align:center;'>Drop the file to open it in ReMEXA.</div></html>"
                    : "<html><div style='text-align:center;'>Drop a <b>.jad</b> file here to launch it.</div></html>");
            hint.setText(dragActive
                    ? "<html><div style='text-align:center;'>The selected JAD will be added to your recent list automatically.</div></html>"
                    : "<html><div style='text-align:center;'>Open a file manually or drag one in. Recent launches stay available from the <b>File</b> menu.</div></html>");
            openButton.setEnabled(!dragActive);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(228, 223, 214));
                g2.fillRoundRect(12, 16, getWidth() - 24, getHeight() - 24, 34, 34);

                g2.setColor(CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 10, 34, 34);

                g2.setStroke(new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{9f, 9f}, 0f));
                g2.setColor(dragActive ? CARD_BORDER_ACTIVE : CARD_BORDER);
                g2.drawRoundRect(18, 18, getWidth() - 37, getHeight() - 45, 24, 24);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(236, 232, 224));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 10, 34, 34);
                if (dragActive) {
                    g2.setColor(new Color(52, 119, 89, 24));
                    g2.fillRoundRect(18, 18, getWidth() - 37, getHeight() - 45, 24, 24);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
