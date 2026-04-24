package remexa.host;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.microedition.lcdui.Command;
import remexa.host.input.HostKeyMapper;
import remexa.host.jad.JadDescriptor;
import remexa.host.profile.DisplayMetrics;
import remexa.host.profile.LaunchProfile;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogEvent;
import remexa.probes.LogSettings;

public final class JadFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int SOFT_KEY_BAR_HEIGHT = 28;

    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Idle");
    private final JLabel renderInfoLabel = new JLabel();
    private final JButton logToggleButton = new JButton();
    private final JLabel leftSoftKeyLabel = new JLabel();
    private final JLabel rightSoftKeyLabel = new JLabel();
    private final JPanel softKeyBar = new JPanel(new GridLayout(1, 2, 0, 0));
    private final JPanel renderSurface = new RenderSurfacePanel();
    private final Consumer<LogEvent> listener = this::appendLog;
    private final LaunchProfile launchProfile;
    private final boolean showHostDetails;
    private final Timer refreshTimer;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final int hostScale;
    private Runnable closeHandler;

    public JadFrame(JadDescriptor descriptor, LaunchProfile launchProfile, boolean showHostDetails) {
        super(descriptor.title() + " - ReMEXA");
        this.launchProfile = launchProfile;
        this.showHostDetails = showHostDetails;
        this.hostScale = LaunchConfig.resolveConfiguredHostScale();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        renderSurface.setBackground(new Color(22, 24, 29));
        renderSurface.setBorder(BorderFactory.createLineBorder(new Color(68, 74, 83)));

        if (showHostDetails) {
            buildDetailedLayout(descriptor, launchProfile);
        } else {
            buildGameOnlyLayout();
        }
        installInputBindings();
        updateDisplayMetrics(launchProfile.initialDisplay());
        setMinimumSize(minimumWindowSize(launchProfile.initialDisplay(), showHostDetails));

        if (showHostDetails) {
            DebugLog.addListener(listener);
        }
        refreshSoftKeyLabels();
        refreshTimer = new Timer(33, event -> {
            refreshSoftKeyLabels();
            if (showHostDetails) {
                refreshLogToggleButton();
            }
            renderSurface.repaint();
        });
        refreshTimer.start();
    }

    public void showFrame() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(() -> getRootPane().requestFocusInWindow());
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    public void setAppIcon(Image image) {
        if (image != null) {
            setIconImage(image);
        }
    }

    @Override
    public void dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        var shutdownTask = closeHandler;
        closeHandler = null;
        refreshTimer.stop();
        if (showHostDetails) {
            DebugLog.removeListener(listener);
        }
        super.dispose();
        if (shutdownTask != null) {
            var shutdownThread = new Thread(shutdownTask, "remexa-app-shutdown");
            shutdownThread.setDaemon(true);
            shutdownThread.start();
        }
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    private void appendLog(LogEvent event) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(
                    TIME_FORMAT.format(event.timestamp()) +
                            " [" + event.category() + "] " +
                            event.source() +
                            " :: " +
                            event.message() +
                            System.lineSeparator()
            );
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateDisplayMetrics(DisplayMetrics displayMetrics) {
        SwingUtilities.invokeLater(() -> {
            if (showHostDetails) {
                renderInfoLabel.setText(
                        "Active display: " + displayMetrics.dimensions() +
                                " | Source: " + displayMetrics.source() +
                                " | Profile: " + launchProfile.profile().displayName()
                );
            }
            renderSurface.setPreferredSize(new Dimension(scaledWidth(displayMetrics), scaledHeight(displayMetrics)));
            renderSurface.revalidate();
            pack();
        });
    }

    private void buildDetailedLayout(JadDescriptor descriptor, LaunchProfile launchProfile) {
        detailsArea.setEditable(false);
        detailsArea.setFont(Font.decode(Font.MONOSPACED));
        detailsArea.setText(String.join(System.lineSeparator(), summaryLines(descriptor, launchProfile)));

        logArea.setEditable(false);
        logArea.setFont(Font.decode(Font.MONOSPACED));

        var renderPanel = new JPanel(new BorderLayout());
        renderPanel.add(new JLabel("Legacy display host groundwork is active. SDK calls and display transitions will appear in the log."), BorderLayout.NORTH);

        var displayHostPanel = new JPanel(new BorderLayout(0, 8));
        var displayHeader = new JPanel(new BorderLayout(8, 0));
        displayHeader.setOpaque(false);
        configureLogToggleButton();
        displayHeader.add(renderInfoLabel, BorderLayout.CENTER);
        displayHeader.add(logToggleButton, BorderLayout.EAST);
        displayHostPanel.add(displayHeader, BorderLayout.NORTH);
        displayHostPanel.add(renderSurface, BorderLayout.CENTER);

        var detailsPanel = new JPanel(new BorderLayout(0, 8));
        detailsPanel.add(displayHostPanel, BorderLayout.NORTH);
        detailsPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        renderPanel.add(detailsPanel, BorderLayout.CENTER);

        var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, renderPanel, new JScrollPane(logArea));
        splitPane.setResizeWeight(0.75);

        add(splitPane, BorderLayout.CENTER);
        var footerPanel = new JPanel(new BorderLayout());
        footerPanel.add(createSoftKeyBar(), BorderLayout.CENTER);
        footerPanel.add(statusLabel, BorderLayout.SOUTH);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void configureLogToggleButton() {
        logToggleButton.setFocusable(false);
        logToggleButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logToggleButton.addActionListener(event -> toggleAllLogs());
        refreshLogToggleButton();
    }

    private void toggleAllLogs() {
        boolean enable = !LogSettings.areAllEnabled();
        LogSettings.setAllEnabled(enable);
        refreshLogToggleButton();
        DebugLog.log(
                LogCategory.HOST,
                JadFrame.class.getName(),
                "All debug logs set to " + enable
        );
    }

    private void refreshLogToggleButton() {
        boolean allEnabled = LogSettings.areAllEnabled();
        logToggleButton.setText(allEnabled ? "Disable All Logs" : "Enable All Logs");
        logToggleButton.setToolTipText(allEnabled
                ? "Turn every debug category off"
                : "Turn every debug category on");
    }

    private void buildGameOnlyLayout() {
        var gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gamePanel.add(renderSurface, BorderLayout.CENTER);
        add(gamePanel, BorderLayout.CENTER);
        add(createSoftKeyBar(), BorderLayout.SOUTH);
    }

    private JPanel createSoftKeyBar() {
        softKeyBar.setOpaque(true);
        softKeyBar.setBackground(new Color(0xD7D8DB));
        softKeyBar.setPreferredSize(new Dimension(10, SOFT_KEY_BAR_HEIGHT));
        softKeyBar.setMinimumSize(new Dimension(10, SOFT_KEY_BAR_HEIGHT));
        softKeyBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(0xA9ABB0)),
                new EmptyBorder(2, 4, 2, 4)
        ));

        leftSoftKeyLabel.setForeground(new Color(0x1F2229));
        leftSoftKeyLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        leftSoftKeyLabel.setVerticalAlignment(SwingConstants.CENTER);
        leftSoftKeyLabel.setBorder(new EmptyBorder(0, 4, 0, 4));

        rightSoftKeyLabel.setForeground(new Color(0x1F2229));
        rightSoftKeyLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        rightSoftKeyLabel.setVerticalAlignment(SwingConstants.CENTER);
        rightSoftKeyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rightSoftKeyLabel.setBorder(new EmptyBorder(0, 4, 0, 4));

        softKeyBar.removeAll();
        softKeyBar.add(leftSoftKeyLabel);
        softKeyBar.add(rightSoftKeyLabel);
        return softKeyBar;
    }

    private void refreshSoftKeyLabels() {
        var displayable = MidletRuntime.currentDisplayable();
        var softKeys = displayable == null ? null : displayable.softKeyCommands();
        updateSoftKeyLabel(leftSoftKeyLabel, softKeys == null ? null : softKeys[0], false);
        updateSoftKeyLabel(rightSoftKeyLabel, softKeys == null ? null : softKeys[1], true);
    }

    private void updateSoftKeyLabel(JLabel label, Command command, boolean alignRight) {
        var commandLabel = command == null || command.getLabel() == null || command.getLabel().isBlank()
                ? ""
                : command.getLabel().trim();
        var text = clipLabel(label, commandLabel, alignRight);
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }

    private String clipLabel(JLabel label, String text, boolean alignRight) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        FontMetrics metrics = label.getFontMetrics(label.getFont());
        int availableWidth = softKeyBar.getWidth() > 0
                ? softKeyBar.getWidth()
                : Math.max(renderSurface.getPreferredSize().width, renderSurface.getWidth());
        int maxWidth = Math.max(0, availableWidth / 2 - 16);
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? "" : text.substring(0, end) + ellipsis;
    }

    private void installInputBindings() {
        var inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = getRootPane().getActionMap();
        var keyCodes = new int[]{
                KeyEvent.VK_UP,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_0,
                KeyEvent.VK_1,
                KeyEvent.VK_2,
                KeyEvent.VK_3,
                KeyEvent.VK_4,
                KeyEvent.VK_5,
                KeyEvent.VK_6,
                KeyEvent.VK_7,
                KeyEvent.VK_8,
                KeyEvent.VK_9,
                KeyEvent.VK_KP_UP,
                KeyEvent.VK_KP_DOWN,
                KeyEvent.VK_KP_LEFT,
                KeyEvent.VK_KP_RIGHT,
                KeyEvent.VK_ENTER,
                KeyEvent.VK_NUMPAD0,
                KeyEvent.VK_NUMPAD1,
                KeyEvent.VK_NUMPAD2,
                KeyEvent.VK_NUMPAD3,
                KeyEvent.VK_NUMPAD4,
                KeyEvent.VK_NUMPAD5,
                KeyEvent.VK_NUMPAD6,
                KeyEvent.VK_NUMPAD7,
                KeyEvent.VK_NUMPAD8,
                KeyEvent.VK_NUMPAD9,
                KeyEvent.VK_MULTIPLY,
                KeyEvent.VK_DIVIDE,
                KeyEvent.VK_DECIMAL,
                KeyEvent.VK_NUMBER_SIGN,
                KeyEvent.VK_A,
                KeyEvent.VK_S,
                KeyEvent.VK_F1,
                KeyEvent.VK_F2
        };

        for (var keyCode : keyCodes) {
            bindKey(inputMap, actionMap, keyCode, false);
            bindKey(inputMap, actionMap, keyCode, true);
        }
    }

    private void bindKey(
            javax.swing.InputMap inputMap,
            javax.swing.ActionMap actionMap,
            int keyCode,
            boolean release
    ) {
        var actionId = (release ? "release-" : "press-") + keyCode;
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, release), actionId);
        actionMap.put(actionId, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispatchHostKey(keyCode, release);
            }
        });
    }

    private void dispatchHostKey(int awtKeyCode, boolean release) {
        var displayable = MidletRuntime.currentDisplayable();
        var jPhoneDirectionalLayout =
                launchProfile.profile().id().startsWith("jsky-")
                        || displayable instanceof com.j_phone.amuse.ACanvas
                        || displayable instanceof com.j_phone.amuse.j3d.Canvas3D;
        var softKeyIndex = HostKeyMapper.toSoftKeyIndex(awtKeyCode);
        if (softKeyIndex >= 0) {
            if (!release) {
                MidletRuntime.dispatchSoftKey(softKeyIndex);
            }
        }

        var phoneKeyCode = HostKeyMapper.toPhoneKeyCode(awtKeyCode, jPhoneDirectionalLayout);
        if (phoneKeyCode == Integer.MIN_VALUE) {
            return;
        }

        dispatchPhoneKey(phoneKeyCode, release);
    }

    private void dispatchPhoneKey(int phoneKeyCode, boolean release) {
        if (release) {
            MidletRuntime.dispatchKeyReleased(phoneKeyCode);
        } else {
            MidletRuntime.dispatchKeyPressed(phoneKeyCode);
        }
    }

    private static java.util.List<String> summaryLines(JadDescriptor descriptor, LaunchProfile launchProfile) {
        var lines = new ArrayList<>(descriptor.summaryLines());
        lines.add("Profile: " + launchProfile.profile().displayName());
        lines.add("Display: " + launchProfile.initialDisplay().dimensions() + " (" + launchProfile.initialDisplay().source() + ")");
        return lines;
    }

    private int scaledWidth(DisplayMetrics displayMetrics) {
        return displayMetrics.width() * hostScale;
    }

    private int scaledHeight(DisplayMetrics displayMetrics) {
        return displayMetrics.height() * hostScale;
    }

    private Dimension minimumWindowSize(DisplayMetrics displayMetrics, boolean showHostDetails) {
        if (showHostDetails) {
            return new Dimension(Math.max(620, scaledWidth(displayMetrics) + 280), 520 + SOFT_KEY_BAR_HEIGHT);
        }
        return new Dimension(scaledWidth(displayMetrics), scaledHeight(displayMetrics) + SOFT_KEY_BAR_HEIGHT);
    }

    private static final class RenderSurfacePanel extends JPanel {
        private RenderSurfacePanel() {
            setOpaque(true);
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var frame = MidletRuntime.currentFrameSnapshot();
            if (frame == null) {
                return;
            }

            var g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                var frameWidth = frame.getWidth();
                var frameHeight = frame.getHeight();
                if (frameWidth <= 0 || frameHeight <= 0) {
                    return;
                }

                var scale = Math.min(
                        (double) getWidth() / frameWidth,
                        (double) getHeight() / frameHeight
                );
                var drawWidth = Math.max(1, (int) Math.round(frameWidth * scale));
                var drawHeight = Math.max(1, (int) Math.round(frameHeight * scale));
                var drawX = (getWidth() - drawWidth) / 2;
                var drawY = (getHeight() - drawHeight) / 2;

                g2.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
            } finally {
                g2.dispose();
            }
        }
    }
}
