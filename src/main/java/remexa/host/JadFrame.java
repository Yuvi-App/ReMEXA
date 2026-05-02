package remexa.host;

import java.awt.BorderLayout;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.JTextComponent;
import javax.microedition.lcdui.Command;
import remexa.host.input.HostKeyMapper;
import remexa.host.input.HostTextInputRequest;
import remexa.host.input.InputProfile;
import remexa.host.jad.JadDescriptor;
import remexa.host.media.VlcVideoWindow;
import remexa.host.profile.DisplayMetrics;
import remexa.host.profile.LaunchProfile;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogEvent;
import remexa.probes.LogSettings;
import remexa.ui.RemexaTheme;

public final class JadFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int SOFT_KEY_BAR_HEIGHT = 28;
    private static final int[] HOST_KEY_CODES = new int[]{
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
    private final AtomicBoolean fatalFailure = new AtomicBoolean();
    private final Object closeHandlerLock = new Object();
    private final int hostScale;
    private volatile long backlightFlashUntilMs;
    private volatile ActiveTextInput activeTextInput;
    private volatile ActiveHostedVideo activeHostedVideo;
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
            buildDetailedLayout();
        } else {
            buildGameOnlyLayout();
        }
        installInputBindings();
        installPointerBindings();
        updateDisplayMetrics(launchProfile.initialDisplay());
        setMinimumSize(minimumWindowSize(launchProfile.initialDisplay(), showHostDetails));

        if (showHostDetails) {
            DebugLog.addListener(listener);
        }
        refreshSoftKeyLabels();
        refreshTimer = new Timer(33, event -> runHostAction("refresh timer", () -> {
            refreshSoftKeyLabels();
            if (showHostDetails) {
                refreshLogToggleButton();
            }
            renderSurface.repaint();
        }));
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

    public String requestTextInput(HostTextInputRequest request) {
        var result = requestTextInputResult(request);
        var resolvedRequest = request == null
                ? new HostTextInputRequest("Input", "", 0, 0, false)
                : request;
        return result.accepted() ? result.text() : resolvedRequest.initialText();
    }

    public HostTextInputRequest.Result requestTextInputResult(HostTextInputRequest request) {
        var resolvedRequest = request == null
                ? new HostTextInputRequest("Input", "", 0, 0, false)
                : request;
        if (disposed.get()) {
            return new HostTextInputRequest.Result(resolvedRequest.initialText(), false);
        }

        var future = new CompletableFuture<HostTextInputRequest.Result>();
        Runnable showTask = () -> presentTextInputOverlay(resolvedRequest, future);
        if (SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            SwingUtilities.invokeLater(showTask);
        }
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new HostTextInputRequest.Result(resolvedRequest.initialText(), false);
        } catch (java.util.concurrent.ExecutionException exception) {
            return new HostTextInputRequest.Result(resolvedRequest.initialText(), false);
        }
    }

    public void playHostedVideoBlocking(java.nio.file.Path mediaPath) throws IOException {
        if (mediaPath == null) {
            throw new IOException("Hosted video path is missing.");
        }
        if (disposed.get()) {
            throw new IOException("Host frame is already disposed.");
        }

        var completion = new CompletableFuture<Void>();
        Runnable showTask = () -> presentHostedVideo(mediaPath.toAbsolutePath(), completion);
        if (SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(showTask);
            } catch (Exception exception) {
                throw new IOException("Failed to initialize hosted video playback.", exception);
            }
        }

        try {
            completion.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            SwingUtilities.invokeLater(this::dismissHostedVideoAsCancelled);
            throw new IOException("Hosted video playback was interrupted.", exception);
        } catch (ExecutionException exception) {
            var cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Hosted video playback failed.", cause);
        }
    }

    public void flashBacklight(int durationMs) {
        if (disposed.get()) {
            return;
        }
        var now = System.currentTimeMillis();
        backlightFlashUntilMs = durationMs <= 0
                ? 0L
                : now + Math.min(Math.max(durationMs, 100), 2_000);
        SwingUtilities.invokeLater(renderSurface::repaint);
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
        Runnable shutdownTask;
        synchronized (closeHandlerLock) {
            shutdownTask = closeHandler;
            closeHandler = null;
        }
        refreshTimer.stop();
        var pendingInput = activeTextInput;
        if (pendingInput != null) {
            pendingInput.result().complete(new HostTextInputRequest.Result(
                    pendingInput.request().initialText(),
                    false
            ));
            activeTextInput = null;
        }
        dismissHostedVideoOnDispose();
        if (showHostDetails) {
            DebugLog.removeListener(listener);
        }
        super.dispose();
        runCloseHandlerAsync(shutdownTask);
    }

    public void setCloseHandler(Runnable closeHandler) {
        if (closeHandler == null) {
            return;
        }
        synchronized (closeHandlerLock) {
            if (!disposed.get()) {
                this.closeHandler = closeHandler;
                return;
            }
        }
        runCloseHandlerAsync(closeHandler);
    }

    public void exitOnFatalException(String activity, Throwable throwable) {
        handleAppFailure(activity, throwable);
    }

    private void appendLog(LogEvent event) {
        SwingUtilities.invokeLater(() -> runHostAction("debug log update", () -> {
            logArea.append(
                    TIME_FORMAT.format(event.timestamp()) +
                            " [" + event.category() + "] " +
                            event.source() +
                            " :: " +
                            event.message() +
                            System.lineSeparator()
            );
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));
    }

    public void updateDisplayMetrics(DisplayMetrics displayMetrics) {
        SwingUtilities.invokeLater(() -> runHostAction("display resize", () -> {
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
        }));
    }

    private void buildDetailedLayout() {
        logArea.setEditable(false);
        logArea.setFont(Font.decode(Font.MONOSPACED));
        logArea.setForeground(RemexaTheme.TEXT_PRIMARY);
        logArea.setBackground(RemexaTheme.EDITOR_BACKGROUND);
        logArea.setCaretColor(RemexaTheme.MENU_HOVER_FOREGROUND);
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        var root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(RemexaTheme.APP_BACKGROUND);
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        var renderPanel = new JPanel(new BorderLayout(0, 8));
        renderPanel.setBackground(RemexaTheme.CARD_BACKGROUND);
        renderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        var displayHeader = new JPanel(new BorderLayout(8, 0));
        displayHeader.setOpaque(false);
        var displayTitle = new JLabel("Display Host");
        displayTitle.setForeground(RemexaTheme.TEXT_PRIMARY);
        displayTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        configureLogToggleButton();
        var headerText = new JPanel(new BorderLayout(0, 4));
        headerText.setOpaque(false);
        renderInfoLabel.setForeground(RemexaTheme.TEXT_SECONDARY);
        renderInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        headerText.add(displayTitle, BorderLayout.NORTH);
        headerText.add(renderInfoLabel, BorderLayout.CENTER);
        displayHeader.add(headerText, BorderLayout.CENTER);
        displayHeader.add(logToggleButton, BorderLayout.EAST);
        renderPanel.add(displayHeader, BorderLayout.NORTH);
        renderPanel.add(renderSurface, BorderLayout.CENTER);

        var logPanel = new JPanel(new BorderLayout(0, 8));
        logPanel.setBackground(RemexaTheme.CARD_BACKGROUND);
        logPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        var logTitle = new JLabel("Debug Log");
        logTitle.setForeground(RemexaTheme.TEXT_PRIMARY);
        logTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        logPanel.add(logTitle, BorderLayout.NORTH);
        var logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1));
        logScrollPane.getViewport().setBackground(RemexaTheme.EDITOR_BACKGROUND);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, renderPanel, logPanel);
        splitPane.setResizeWeight(0.75);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setBackground(RemexaTheme.APP_BACKGROUND);

        root.add(splitPane, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
        var footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(RemexaTheme.APP_BACKGROUND);
        footerPanel.add(createSoftKeyBar(), BorderLayout.CENTER);
        footerPanel.add(statusLabel, BorderLayout.SOUTH);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void configureLogToggleButton() {
        logToggleButton.setFocusable(false);
        logToggleButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logToggleButton.addActionListener(event -> runHostAction("log toggle", this::toggleAllLogs));
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
        if (activeHostedVideo != null) {
            leftSoftKeyLabel.setText("Skip");
            rightSoftKeyLabel.setText("Skip");
            return;
        }
        if (activeTextInput != null) {
            leftSoftKeyLabel.setText("Cancel");
            rightSoftKeyLabel.setText("OK");
            return;
        }
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
        for (var keyCode : HOST_KEY_CODES) {
            bindKey(inputMap, actionMap, keyCode, false);
            bindKey(inputMap, actionMap, keyCode, true);
        }
    }

    private void installPointerBindings() {
        var mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                runHostAction("pointer press", () -> dispatchHostPointer(event, PointerAction.PRESS));
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                runHostAction("pointer release", () -> dispatchHostPointer(event, PointerAction.RELEASE));
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                runHostAction("pointer drag", () -> dispatchHostPointer(event, PointerAction.DRAG));
            }
        };
        renderSurface.addMouseListener(mouseHandler);
        renderSurface.addMouseMotionListener(mouseHandler);
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
                runHostAction("input dispatch", () -> dispatchHostKey(keyCode, release));
            }
        });
    }

    private void runHostAction(String activity, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            handleFatalFailure(activity, throwable);
        }
    }

    private void handleFatalFailure(String activity, Throwable throwable) {
        if (throwable == null || MidletRuntime.isExpectedShutdownThrowable(throwable)) {
            return;
        }
        if (!fatalFailure.compareAndSet(false, true)) {
            return;
        }
        System.err.println("ReMEXA fatal JadFrame exception during " + activity + ": " + throwable);
        throwable.printStackTrace(System.err);
        if (SwingUtilities.isEventDispatchThread()) {
            updateStatus("Host failure");
            dispose();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            updateStatus("Host failure");
            dispose();
        });
    }

    private void handleAppFailure(String activity, Throwable throwable) {
        if (throwable == null || MidletRuntime.isExpectedShutdownThrowable(throwable)) {
            return;
        }
        if (!fatalFailure.compareAndSet(false, true)) {
            return;
        }
        System.err.println("ReMEXA app exception during " + activity + ": " + throwable);
        throwable.printStackTrace(System.err);
        SwingUtilities.invokeLater(() -> {
            updateStatus("App exception - check log");
            toFront();
            repaint();
        });
    }

    private void runCloseHandlerAsync(Runnable shutdownTask) {
        if (shutdownTask == null) {
            return;
        }
        var shutdownThread = new Thread(shutdownTask, "remexa-app-shutdown");
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    private void dispatchHostKey(int awtKeyCode, boolean release) {
        var displayable = MidletRuntime.currentDisplayable();
        InputProfile inputProfile = launchProfile.profile().inputProfile();
        var softKeyIndex = HostKeyMapper.toSoftKeyIndex(awtKeyCode, inputProfile);
        var dispatchCanvasSoftKeyCommand =
                softKeyIndex >= 0 && !release && shouldDispatchCanvasSoftKeyCommand(displayable, softKeyIndex);
        if (softKeyIndex >= 0) {
            if (!(displayable instanceof javax.microedition.lcdui.Canvas)
                    && shouldDispatchSoftKeyAsCommand(displayable, softKeyIndex)) {
                if (!release) {
                    MidletRuntime.dispatchSoftKey(softKeyIndex);
                }
                return;
            }
        }

        var phoneKeyCode = HostKeyMapper.toPhoneKeyCode(awtKeyCode, inputProfile);
        if (phoneKeyCode == HostKeyMapper.NO_MAPPING) {
            if (dispatchCanvasSoftKeyCommand) {
                MidletRuntime.dispatchSoftKey(softKeyIndex);
            }
            return;
        }

        dispatchPhoneKey(phoneKeyCode, release);
        if (dispatchCanvasSoftKeyCommand) {
            MidletRuntime.dispatchSoftKey(softKeyIndex);
        }
    }

    private static boolean shouldDispatchSoftKeyAsCommand(javax.microedition.lcdui.Displayable displayable, int softKeyIndex) {
        // Canvas-based titles often poll the physical soft-key state directly via key events
        // or DeviceControl.getDeviceState(...). Keep soft keys as key presses there instead
        // of converting them into command callbacks.
        if (displayable instanceof javax.microedition.lcdui.Canvas) {
            return false;
        }
        return hasBoundSoftKey(displayable, softKeyIndex);
    }

    private static boolean shouldDispatchCanvasSoftKeyCommand(javax.microedition.lcdui.Displayable displayable, int softKeyIndex) {
        // Some Canvas titles still bind Command callbacks for the labeled soft keys even though
        // gameplay polls the physical key state directly. Fire the command on press while
        // preserving the underlying key press/release so both patterns continue to work.
        return displayable instanceof javax.microedition.lcdui.Canvas
                && hasBoundSoftKey(displayable, softKeyIndex);
    }

    private static boolean hasBoundSoftKey(javax.microedition.lcdui.Displayable displayable, int softKeyIndex) {
        if (displayable == null) {
            return false;
        }
        Command[] softKeys = displayable.softKeyCommands();
        return softKeys != null
                && softKeyIndex >= 0
                && softKeyIndex < softKeys.length
                && softKeys[softKeyIndex] != null;
    }

    private void dispatchPhoneKey(int phoneKeyCode, boolean release) {
        if (release) {
            MidletRuntime.dispatchKeyReleased(phoneKeyCode);
        } else {
            MidletRuntime.dispatchKeyPressed(phoneKeyCode);
        }
    }

    public void showTouchControlsDisabledWarning() {
        Runnable showTask = () -> {
            if (disposed.get()) {
                return;
            }
            showThemedInfoDialog(
                    "Touch Controls Required",
                    "This application uses touch controls. Enable Touch Controls in Settings and restart the app."
            );
        };
        if (SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            SwingUtilities.invokeLater(showTask);
        }
    }

    private void dispatchHostPointer(MouseEvent event, PointerAction action) {
        if (disposed.get() || activeTextInput != null || activeHostedVideo != null) {
            return;
        }
        if (action != PointerAction.DRAG && !SwingUtilities.isLeftMouseButton(event)) {
            return;
        }
        if (action == PointerAction.DRAG && (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
            return;
        }

        var viewport = currentRenderViewport();
        if (viewport == null) {
            return;
        }
        var mappedPoint = viewport.map(event.getX(), event.getY());
        if (mappedPoint == null) {
            return;
        }

        switch (action) {
            case PRESS -> MidletRuntime.dispatchPointerPressed(mappedPoint.x(), mappedPoint.y());
            case RELEASE -> MidletRuntime.dispatchPointerReleased(mappedPoint.x(), mappedPoint.y());
            case DRAG -> MidletRuntime.dispatchPointerDragged(mappedPoint.x(), mappedPoint.y());
        }
    }

    private RenderViewport currentRenderViewport() {
        int surfaceWidth = Math.max(0, renderSurface.getWidth());
        int surfaceHeight = Math.max(0, renderSurface.getHeight());
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return null;
        }

        var frame = MidletRuntime.currentFrameSnapshot();
        int sourceWidth = frame == null ? 0 : frame.getWidth();
        int sourceHeight = frame == null ? 0 : frame.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            var displayable = MidletRuntime.currentDisplayable();
            if (displayable == null) {
                return null;
            }
            sourceWidth = displayable.getWidth();
            sourceHeight = displayable.getHeight();
        }
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null;
        }

        double scale = Math.min(
                (double) surfaceWidth / sourceWidth,
                (double) surfaceHeight / sourceHeight
        );
        int drawWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int drawHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        int drawX = (surfaceWidth - drawWidth) / 2;
        int drawY = (surfaceHeight - drawHeight) / 2;
        return new RenderViewport(drawX, drawY, drawWidth, drawHeight, sourceWidth, sourceHeight);
    }

    private void presentTextInputOverlay(HostTextInputRequest request, CompletableFuture<HostTextInputRequest.Result> result) {
        if (disposed.get() || result.isDone()) {
            result.complete(new HostTextInputRequest.Result(request.initialText(), false));
            return;
        }
        if (activeTextInput != null) {
            result.complete(new HostTextInputRequest.Result(request.initialText(), false));
            return;
        }

        var overlay = new JPanel(new GridBagLayout());
        overlay.setOpaque(true);
        overlay.setBackground(new Color(
                RemexaTheme.TEXT_PRIMARY.getRed(),
                RemexaTheme.TEXT_PRIMARY.getGreen(),
                RemexaTheme.TEXT_PRIMARY.getBlue(),
                120
        ));

        var editor = request.wrapAllowed() ? createMultilineEditor(request) : createSingleLineEditor(request);
        var editorHost = createEditorHost(request, editor);

        var panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(RemexaTheme.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(14, 14, 14, 14)
        ));

        var titleLabel = new JLabel(request.title().isBlank() ? "Input" : request.title());
        titleLabel.setForeground(RemexaTheme.TEXT_PRIMARY);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(editorHost, BorderLayout.CENTER);

        var footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        var hintLabel = new JLabel(request.wrapAllowed()
                ? "Esc cancel, Ctrl+Enter submit"
                : "Esc cancel, Enter submit");
        hintLabel.setForeground(RemexaTheme.TEXT_SECONDARY);
        hintLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        footer.add(hintLabel, BorderLayout.WEST);

        var buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        var cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        var okButton = new JButton("OK");
        okButton.setFocusable(false);
        styleOverlayButton(cancelButton, false);
        styleOverlayButton(okButton, true);
        buttons.add(cancelButton);
        buttons.add(okButton);
        footer.add(buttons, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);

        overlay.add(panel, centeredConstraints());

        var session = new ActiveTextInput(request, result);
        activeTextInput = session;
        setHostInputBindingsEnabled(false);
        refreshSoftKeyLabels();
        setGlassPane(overlay);
        overlay.setVisible(true);

        Runnable cancel = () -> finishTextInput(session, editor.getText(), false);
        Runnable accept = () -> finishTextInput(session, editor.getText(), true);
        installTextInputBindings(overlay, editor, request.wrapAllowed(), cancel, accept);
        cancelButton.addActionListener(event -> cancel.run());
        okButton.addActionListener(event -> accept.run());
        SwingUtilities.invokeLater(editor::requestFocusInWindow);
    }

    private void finishTextInput(ActiveTextInput session, String text, boolean accept) {
        if (session == null || activeTextInput != session || session.result().isDone()) {
            return;
        }
        activeTextInput = null;
        setHostInputBindingsEnabled(true);
        refreshSoftKeyLabels();
        var glassPane = getGlassPane();
        if (glassPane != null) {
            glassPane.setVisible(false);
        }
        getRootPane().requestFocusInWindow();
        session.result().complete(new HostTextInputRequest.Result(
                accept ? normalizeInputText(session.request(), text) : session.request().initialText(),
                accept
        ));
    }

    private void presentHostedVideo(java.nio.file.Path mediaPath, CompletableFuture<Void> completion) {
        if (disposed.get()) {
            completion.completeExceptionally(new IOException("Host frame was closed before playback started."));
            return;
        }
        if (activeHostedVideo != null) {
            completion.completeExceptionally(new IOException("Another hosted video is already playing."));
            return;
        }
        if (activeTextInput != null) {
            completion.completeExceptionally(new IOException("Cannot start hosted video while a text input overlay is open."));
            return;
        }

        VlcVideoWindow videoWindow;
        try {
            videoWindow = new VlcVideoWindow(this, currentTitleText());
        } catch (IOException exception) {
            completion.completeExceptionally(exception);
            return;
        }

        var session = new ActiveHostedVideo(completion, videoWindow);
        activeHostedVideo = session;
        setHostInputBindingsEnabled(false);
        refreshSoftKeyLabels();
        videoWindow.play(
                mediaPath,
                () -> SwingUtilities.invokeLater(() -> completeHostedVideo(session, null)),
                exception -> SwingUtilities.invokeLater(() -> completeHostedVideo(session, exception))
        );
    }

    private void dismissHostedVideoAsCancelled() {
        var session = activeHostedVideo;
        if (session == null) {
            return;
        }
        completeHostedVideo(session, null);
    }

    private void dismissHostedVideoOnDispose() {
        var session = activeHostedVideo;
        if (session == null) {
            return;
        }
        completeHostedVideo(session, new IOException("Host frame was closed during video playback."));
    }

    private void completeHostedVideo(ActiveHostedVideo session, IOException error) {
        if (session == null || activeHostedVideo != session) {
            return;
        }
        activeHostedVideo = null;
        try {
            session.videoWindow().close();
        } finally {
            setHostInputBindingsEnabled(true);
            refreshSoftKeyLabels();
            getRootPane().requestFocusInWindow();
        }

        if (error == null) {
            session.completion().complete(null);
        } else {
            session.completion().completeExceptionally(error);
        }
    }

    private JComponent createEditorHost(HostTextInputRequest request, JTextComponent editor) {
        int width = Math.max(260, Math.min(420, scaledWidth(launchProfile.initialDisplay()) - 48));
        if (request.wrapAllowed()) {
            var scrollPane = new JScrollPane(editor);
            scrollPane.setBorder(BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1));
            scrollPane.getViewport().setBackground(RemexaTheme.EDITOR_BACKGROUND);
            scrollPane.setPreferredSize(new Dimension(width, 140));
            return scrollPane;
        }

        var host = new JPanel(new BorderLayout());
        host.setOpaque(true);
        host.setBackground(RemexaTheme.EDITOR_BACKGROUND);
        host.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        host.setPreferredSize(new Dimension(width, 54));
        host.add(editor, BorderLayout.CENTER);
        return host;
    }

    private void styleOverlayButton(JButton button, boolean primary) {
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? RemexaTheme.CARD_BORDER_ACTIVE : RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(7, 12, 7, 12)
        ));
        button.setBackground(primary ? RemexaTheme.MENU_HOVER_BACKGROUND : RemexaTheme.POPUP_BACKGROUND);
        button.setForeground(primary ? RemexaTheme.MENU_HOVER_FOREGROUND : RemexaTheme.TEXT_PRIMARY);
        button.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
    }

    private void showThemedInfoDialog(String title, String message) {
        var dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        var content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(RemexaTheme.CARD_BACKGROUND);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RemexaTheme.CARD_BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        var titleLabel = new JLabel(title);
        titleLabel.setForeground(RemexaTheme.TEXT_PRIMARY);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        content.add(titleLabel, BorderLayout.NORTH);

        var messageLabel = new JLabel("<html><div style='width:320px;'>" + message + "</div></html>");
        messageLabel.setForeground(RemexaTheme.TEXT_SECONDARY);
        messageLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        content.add(messageLabel, BorderLayout.CENTER);

        var footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        var okButton = new JButton("OK");
        okButton.setFocusable(false);
        styleOverlayButton(okButton, true);
        okButton.addActionListener(event -> dialog.dispose());
        footer.add(okButton, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(okButton::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private JTextComponent createSingleLineEditor(HostTextInputRequest request) {
        var field = new JTextField();
        field.setDocument(limitedDocument(request.maxSize()));
        field.setText(request.initialText());
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        field.setForeground(RemexaTheme.TEXT_PRIMARY);
        field.setBackground(RemexaTheme.EDITOR_BACKGROUND);
        field.setCaretColor(RemexaTheme.MENU_HOVER_FOREGROUND);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setMargin(new Insets(0, 0, 0, 0));
        return field;
    }

    private JTextComponent createMultilineEditor(HostTextInputRequest request) {
        var area = new JTextArea();
        area.setDocument(limitedDocument(request.maxSize()));
        area.setText(request.initialText());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        area.setForeground(RemexaTheme.TEXT_PRIMARY);
        area.setBackground(RemexaTheme.EDITOR_BACKGROUND);
        area.setCaretColor(RemexaTheme.MENU_HOVER_FOREGROUND);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));
        return area;
    }

    private void installTextInputBindings(
            JComponent overlay,
            JTextComponent editor,
            boolean multiline,
            Runnable cancel,
            Runnable accept
    ) {
        bindTextInputAction(overlay, editor, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-escape", cancel);
        bindTextInputAction(
                overlay,
                editor,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, multiline ? KeyEvent.CTRL_DOWN_MASK : 0),
                multiline ? "accept-ctrl-enter" : "accept-enter",
                accept
        );
    }

    private void bindTextInputAction(
            JComponent overlay,
            JTextComponent editor,
            KeyStroke keyStroke,
            String actionId,
            Runnable action
    ) {
        overlay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionId);
        overlay.getActionMap().put(actionId, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, actionId);
        editor.getActionMap().put(actionId, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void setHostInputBindingsEnabled(boolean enabled) {
        var inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = getRootPane().getActionMap();
        for (var keyCode : HOST_KEY_CODES) {
            var pressedActionId = "press-" + keyCode;
            var releasedActionId = "release-" + keyCode;
            if (enabled) {
                bindKey(inputMap, actionMap, keyCode, false);
                bindKey(inputMap, actionMap, keyCode, true);
                continue;
            }
            inputMap.remove(KeyStroke.getKeyStroke(keyCode, 0, false));
            inputMap.remove(KeyStroke.getKeyStroke(keyCode, 0, true));
            actionMap.remove(pressedActionId);
            actionMap.remove(releasedActionId);
        }
    }

    private static PlainDocument limitedDocument(int maxSize) {
        return new PlainDocument() {
            @Override
            public void insertString(int offset, String text, AttributeSet attributes) throws BadLocationException {
                if (text == null) {
                    return;
                }
                var cappedText = maxSize > 0
                        ? text.substring(0, Math.min(text.length(), Math.max(0, maxSize - getLength())))
                        : text;
                if (!cappedText.isEmpty()) {
                    super.insertString(offset, cappedText, attributes);
                }
            }

            @Override
            public void replace(int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                if (text == null) {
                    super.replace(offset, length, null, attributes);
                    return;
                }
                var remaining = maxSize > 0 ? Math.max(0, maxSize - (getLength() - length)) : Integer.MAX_VALUE;
                var cappedText = text.substring(0, Math.min(text.length(), remaining));
                super.replace(offset, length, cappedText, attributes);
            }
        };
    }

    private static GridBagConstraints centeredConstraints() {
        var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(24, 24, 24, 24);
        return constraints;
    }

    private static String normalizeInputText(HostTextInputRequest request, String text) {
        var normalized = text == null ? "" : java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC);
        if (request.maxSize() > 0 && normalized.length() > request.maxSize()) {
            return normalized.substring(0, request.maxSize());
        }
        return normalized;
    }

    private String currentTitleText() {
        var title = getTitle();
        return title == null || title.isBlank() ? "Video Playback" : title;
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

    private final class RenderSurfacePanel extends JPanel {
        private RenderSurfacePanel() {
            setOpaque(true);
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            try {
                super.paintComponent(graphics);
                var frame = MidletRuntime.currentFrameSnapshot();
                if (frame == null) {
                    return;
                }

                var g2 = (Graphics2D) graphics.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                    var viewport = currentRenderViewport();
                    if (viewport == null) {
                        return;
                    }
                    g2.drawImage(frame, viewport.drawX(), viewport.drawY(), viewport.drawWidth(), viewport.drawHeight(), null);
                    paintBacklightFlash(g2, viewport);
                } finally {
                    g2.dispose();
                }
            } catch (Throwable throwable) {
                handleFatalFailure("render surface repaint", throwable);
            }
        }
    }

    private void paintBacklightFlash(Graphics2D graphics, RenderViewport viewport) {
        var flashUntil = backlightFlashUntilMs;
        if (flashUntil <= 0L) {
            return;
        }
        var remaining = flashUntil - System.currentTimeMillis();
        if (remaining <= 0L) {
            backlightFlashUntilMs = 0L;
            return;
        }

        var originalStroke = graphics.getStroke();
        var originalComposite = graphics.getComposite();
        try {
            float alpha = Math.min(0.9f, Math.max(0.35f, remaining / 250.0f));
            graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(Math.max(2.0f, hostScale)));
            graphics.drawRect(
                    viewport.drawX() + 1,
                    viewport.drawY() + 1,
                    Math.max(1, viewport.drawWidth() - 3),
                    Math.max(1, viewport.drawHeight() - 3)
            );
        } finally {
            graphics.setComposite(originalComposite);
            graphics.setStroke(originalStroke);
        }
    }

    private record ActiveTextInput(
            HostTextInputRequest request,
            CompletableFuture<HostTextInputRequest.Result> result
    ) {
    }

    private record ActiveHostedVideo(
            CompletableFuture<Void> completion,
            VlcVideoWindow videoWindow
    ) {
    }

    private enum PointerAction {
        PRESS,
        RELEASE,
        DRAG
    }

    private record RenderViewport(
            int drawX,
            int drawY,
            int drawWidth,
            int drawHeight,
            int sourceWidth,
            int sourceHeight
    ) {
        private MappedPoint map(int hostX, int hostY) {
            if (hostX < drawX || hostY < drawY || hostX >= drawX + drawWidth || hostY >= drawY + drawHeight) {
                return null;
            }
            int sourceX = (int) ((long) (hostX - drawX) * sourceWidth / drawWidth);
            int sourceY = (int) ((long) (hostY - drawY) * sourceHeight / drawHeight);
            sourceX = Math.max(0, Math.min(sourceWidth - 1, sourceX));
            sourceY = Math.max(0, Math.min(sourceHeight - 1, sourceY));
            return new MappedPoint(sourceX, sourceY);
        }
    }

    private record MappedPoint(int x, int y) {
    }
}
