package remexa.host;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import remexa.host.jad.JadDescriptor;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.LogEvent;

public final class JadFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Idle");
    private final Consumer<LogEvent> listener = this::appendLog;

    public JadFrame(JadDescriptor descriptor) {
        super(descriptor.title() + " - ReMEXA");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setLayout(new BorderLayout());

        detailsArea.setEditable(false);
        detailsArea.setFont(Font.decode(Font.MONOSPACED));
        detailsArea.setText(String.join(System.lineSeparator(), descriptor.summaryLines()));

        logArea.setEditable(false);
        logArea.setFont(Font.decode(Font.MONOSPACED));

        var renderPanel = new JPanel(new BorderLayout());
        renderPanel.add(new JLabel("Legacy display host groundwork is active. SDK calls and display transitions will appear in the log."), BorderLayout.NORTH);
        renderPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, renderPanel, new JScrollPane(logArea));
        splitPane.setResizeWeight(0.45);

        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        DebugLog.addListener(listener);
    }

    public void showFrame() {
        pack();
        setLocationByPlatform(true);
        setVisible(true);
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    @Override
    public void dispose() {
        DebugLog.removeListener(listener);
        super.dispose();
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
}
