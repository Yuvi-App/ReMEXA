package remexa.host.media;

import com.sun.jna.NativeLibrary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

public final class VlcVideoWindow {
    private static final String VLC_LIBRARY_NAME = "libvlc";
    private static final AtomicBoolean VLC_CONFIGURATION_APPLIED = new AtomicBoolean();

    private final JFrame owner;
    private final JDialog dialog;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean signalled = new AtomicBoolean();
    private volatile Runnable finishedCallback;
    private volatile Consumer<IOException> errorCallback;

    public VlcVideoWindow(JFrame owner, String title) throws IOException {
        this.owner = Objects.requireNonNull(owner, "owner");
        configureVlcIfAvailable();
        this.mediaPlayerComponent = new EmbeddedMediaPlayerComponent() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                signalFinished(null);
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                signalFinished(new IOException("VLC failed to play hosted video."));
            }
        };
        this.dialog = new JDialog(owner, title, JDialog.ModalityType.MODELESS);
        buildDialog();
    }

    public void play(Path mediaPath, Runnable onFinished, Consumer<IOException> onError) {
        Objects.requireNonNull(mediaPath, "mediaPath");
        Objects.requireNonNull(onFinished, "onFinished");
        Objects.requireNonNull(onError, "onError");
        if (!Files.isRegularFile(mediaPath)) {
            onError.accept(new IOException("Hosted video file does not exist: " + mediaPath));
            return;
        }

        finishedCallback = onFinished;
        errorCallback = onError;
        SwingUtilities.invokeLater(() -> {
            if (closed.get()) {
                return;
            }
            syncBoundsToOwner();
            dialog.setVisible(true);
            dialog.getRootPane().requestFocusInWindow();
            DebugLog.log(LogCategory.MEDIA, VlcVideoWindow.class.getName(), "Starting VLC playback for " + mediaPath.getFileName());
            mediaPlayerComponent.mediaPlayer().media().play(mediaPath.toAbsolutePath().toString());
        });
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                mediaPlayerComponent.mediaPlayer().controls().stop();
            } catch (RuntimeException ignored) {
                // Best-effort shutdown only.
            }
            dialog.setVisible(false);
            dialog.dispose();
            mediaPlayerComponent.release();
        });
    }

    private void signalFinished(IOException error) {
        if (!signalled.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (error == null) {
                DebugLog.log(LogCategory.MEDIA, VlcVideoWindow.class.getName(), "VLC playback reached end-of-media.");
                var callback = finishedCallback;
                if (callback != null) {
                    callback.run();
                }
            } else {
                DebugLog.log(LogCategory.MEDIA, VlcVideoWindow.class.getName(), "VLC playback failed: " + error.getMessage());
                var callback = errorCallback;
                if (callback != null) {
                    callback.accept(error);
                }
            }
        });
    }

    private void buildDialog() {
        dialog.setUndecorated(true);
        dialog.setBackground(Color.BLACK);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.BLACK);
        dialog.getContentPane().add(mediaPlayerComponent, BorderLayout.CENTER);

        var hintLabel = new JLabel("Press Esc, Enter, or Space to skip", SwingConstants.CENTER);
        hintLabel.setForeground(Color.WHITE);
        hintLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        hintLabel.setBorder(new EmptyBorder(6, 8, 8, 8));
        dialog.getContentPane().add(hintLabel, BorderLayout.SOUTH);

        installSkipBinding(dialog.getRootPane(), KeyStroke.getKeyStroke("ESCAPE"));
        installSkipBinding(dialog.getRootPane(), KeyStroke.getKeyStroke("ENTER"));
        installSkipBinding(dialog.getRootPane(), KeyStroke.getKeyStroke("SPACE"));

        owner.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                syncBoundsToOwner();
            }

            @Override
            public void componentResized(ComponentEvent event) {
                syncBoundsToOwner();
            }
        });
    }

    private void installSkipBinding(JComponent rootPane, KeyStroke keyStroke) {
        var actionId = "skip-" + keyStroke;
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionId);
        rootPane.getActionMap().put(actionId, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                var callback = finishedCallback;
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    private void syncBoundsToOwner() {
        if (!owner.isShowing()) {
            return;
        }
        try {
            var location = owner.getLocationOnScreen();
            dialog.setBounds(location.x, location.y, owner.getWidth(), owner.getHeight());
        } catch (IllegalComponentStateException ignored) {
            dialog.setBounds(owner.getBounds());
        }
    }

    private static void configureVlcIfAvailable() throws IOException {
        if (!VLC_CONFIGURATION_APPLIED.compareAndSet(false, true)) {
            return;
        }
        var vlcDirectory = locateVlcDirectory();
        if (vlcDirectory == null) {
            throw new IOException("VLC 3.x was not found. Install VLC to enable legacy 3GP playback.");
        }

        NativeLibrary.addSearchPath(VLC_LIBRARY_NAME, vlcDirectory.toString());
        System.setProperty("jna.library.path", vlcDirectory.toString());
        var pluginsDirectory = vlcDirectory.resolve("plugins");
        if (Files.isDirectory(pluginsDirectory)) {
            System.setProperty("VLC_PLUGIN_PATH", pluginsDirectory.toString());
        }
        DebugLog.log(LogCategory.MEDIA, VlcVideoWindow.class.getName(), "Configured VLC native directory: " + vlcDirectory);
    }

    private static Path locateVlcDirectory() {
        var configured = System.getenv("VLC_HOME");
        if (configured != null && !configured.isBlank()) {
            var path = Path.of(configured.trim());
            if (Files.isDirectory(path)) {
                return path;
            }
        }

        for (var root : new String[]{
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)")
        }) {
            if (root == null || root.isBlank()) {
                continue;
            }
            var candidate = Path.of(root, "VideoLAN", "VLC");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
