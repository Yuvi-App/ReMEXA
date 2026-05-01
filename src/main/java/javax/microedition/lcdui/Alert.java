package javax.microedition.lcdui;

public class Alert extends Screen {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("", Command.OK, 0);
    private static final int DEFAULT_TIMEOUT_MILLIS = 2_000;

    private String text;
    private Image image;
    private AlertType type;
    private Gauge indicator;
    private int timeout = DEFAULT_TIMEOUT_MILLIS;
    private boolean defaultListenerActive = true;
    private Display ownerDisplay;
    private Displayable nextDisplayable;

    public Alert(String title) {
        setTitle(title);
    }

    public Alert(String title, String text, Image image, AlertType type) {
        this(title);
        this.text = text;
        this.image = image;
        this.type = type;
    }

    @Override
    protected java.util.List<Command> commandSnapshot() {
        var commands = super.commandSnapshot();
        return commands.isEmpty() ? java.util.List.of(DISMISS_COMMAND) : commands;
    }

    @Override
    public void addCommand(Command command) {
        if (command == null || command == DISMISS_COMMAND) {
            return;
        }
        addCommandInternal(command);
    }

    @Override
    public void removeCommand(Command command) {
        if (command == null || command == DISMISS_COMMAND) {
            return;
        }
        removeCommandInternal(command);
    }

    @Override
    public void setCommandListener(CommandListener commandListener) {
        defaultListenerActive = commandListener == null;
        super.setCommandListener(commandListener);
    }

    @Override
    public void fireCommand(int index) {
        var command = resolveCommand(index);
        if (command == null) {
            return;
        }
        if (defaultListenerActive || getCommandListener() == null) {
            dismiss(command);
            return;
        }
        getCommandListener().commandAction(command, this);
    }

    public int getDefaultTimeout() {
        return DEFAULT_TIMEOUT_MILLIS;
    }

    public Image getImage() {
        return image;
    }

    public void setString(String text) {
        this.text = text;
    }

    public String getString() {
        return text;
    }

    public int getTimeout() {
        return effectiveCommandCount() >= 2 ? FOREVER : timeout;
    }

    public AlertType getType() {
        return type;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public void setTimeout(int timeout) {
        if (timeout <= 0 && timeout != FOREVER) {
            throw new IllegalArgumentException("Alert timeout must be positive or FOREVER.");
        }
        this.timeout = timeout;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public void setIndicator(Gauge indicator) {
        if (indicator != null && !indicator.canBeAlertIndicator()) {
            throw new IllegalArgumentException("Gauge does not satisfy Alert indicator restrictions.");
        }
        if (this.indicator != null) {
            this.indicator.detachFromAlert();
        }
        this.indicator = indicator;
        if (indicator != null) {
            indicator.attachToAlert();
        }
    }

    public Gauge getIndicator() {
        return indicator;
    }

    void attachToDisplay(Display ownerDisplay, Displayable nextDisplayable) {
        this.ownerDisplay = ownerDisplay;
        this.nextDisplayable = nextDisplayable;
    }

    void detachFromDisplay(Display ownerDisplay) {
        if (this.ownerDisplay != ownerDisplay) {
            return;
        }
        this.ownerDisplay = null;
        this.nextDisplayable = null;
    }

    Displayable nextDisplayable() {
        return nextDisplayable;
    }

    void fireTimeout() {
        var command = resolveCommand(0);
        if (command != null) {
            fireCommand(0);
        }
    }

    @Override
    protected void paintScreen(Graphics graphics) {
        var bodyTop = paintChrome(graphics);
        var lines = new java.util.ArrayList<String>();
        if (image != null) {
            lines.add("");
        }
        appendWrappedLines(text, Font.getDefaultFont(), bodyTextWidth(), lines);
        paintLines(graphics, lines, bodyTop);
        if (image != null) {
            graphics.drawImage(image, getWidth() / 2, bodyTop + 4, Graphics.HCENTER | Graphics.TOP);
        }
    }

    private int effectiveCommandCount() {
        return Math.max(1, commandCountInternal());
    }

    private void dismiss(Command command) {
        var display = ownerDisplay;
        if (display == null) {
            return;
        }
        display.dismissAlert(this, command);
    }
}
