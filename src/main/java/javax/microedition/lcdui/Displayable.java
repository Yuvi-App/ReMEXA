package javax.microedition.lcdui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import remexa.host.runtime.MidletRuntime;

public class Displayable {
    private final List<Command> commands = new ArrayList<>();
    private String title;
    private Ticker ticker;
    private CommandListener commandListener;
    private boolean shown;
    private boolean sizeInitialized;
    private int width;
    private int height;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public void addCommand(Command command) {
        addCommandInternal(command);
    }

    public void removeCommand(Command command) {
        removeCommandInternal(command);
    }

    public void setCommandListener(CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public int getWidth() {
        return MidletRuntime.getDisplayMetrics(this).width();
    }

    public int getHeight() {
        return MidletRuntime.getDisplayMetrics(this).height();
    }

    public CommandListener getCommandListener() {
        return commandListener;
    }

    public boolean isShown() {
        return shown;
    }

    protected void sizeChanged(int width, int height) {
    }

    public void fireCommand(int index) {
        var command = resolveCommand(index);
        if (commandListener == null || command == null) {
            return;
        }
        commandListener.commandAction(command, this);
    }

    public Command[] softKeyCommands() {
        var ordered = orderedCommands();

        Command left = null;
        Command right = null;
        for (var command : ordered) {
            if (isRightSoftKeyType(command.getCommandType())) {
                if (right == null) {
                    right = command;
                } else if (left == null) {
                    left = command;
                }
            } else if (left == null) {
                left = command;
            } else if (right == null) {
                right = command;
            }

            if (left != null && right != null) {
                break;
            }
        }
        return new Command[]{left, right};
    }

    protected List<Command> commandSnapshot() {
        return List.copyOf(commands);
    }

    protected final boolean addCommandInternal(Command command) {
        if (command == null || commands.contains(command)) {
            return false;
        }
        commands.add(command);
        return true;
    }

    protected final boolean removeCommandInternal(Command command) {
        return commands.remove(command);
    }

    protected final boolean containsCommandInternal(Command command) {
        return commands.contains(command);
    }

    protected final int commandCountInternal() {
        return commands.size();
    }

    protected final Command resolveCommand(int index) {
        var availableCommands = orderedCommands();
        if (availableCommands.isEmpty()) {
            return null;
        }

        var softKeys = softKeyCommands();
        if (index >= 0 && index < softKeys.length && softKeys[index] != null) {
            return softKeys[index];
        }

        var resolvedIndex = Math.max(0, Math.min(index, availableCommands.size() - 1));
        return availableCommands.get(resolvedIndex);
    }

    private ArrayList<Command> orderedCommands() {
        var snapshot = commandSnapshot();
        var ordered = new ArrayList<Command>(snapshot.size());
        for (var command : snapshot) {
            if (command != null) {
                ordered.add(command);
            }
        }
        ordered.sort(
                Comparator
                        .comparingInt((Command command) -> softKeyBucket(command.getCommandType()))
                        .thenComparingInt(Command::getPriority)
                        .thenComparing(command -> command.getLabel() == null ? "" : command.getLabel(), String.CASE_INSENSITIVE_ORDER)
        );
        return ordered;
    }

    private static int softKeyBucket(int commandType) {
        return isRightSoftKeyType(commandType) ? 1 : 0;
    }

    private static boolean isRightSoftKeyType(int commandType) {
        return commandType == Command.BACK
                || commandType == Command.CANCEL
                || commandType == Command.STOP
                || commandType == Command.EXIT;
    }

    final void fireShown() {
        shown = true;
    }

    final void fireHidden() {
        shown = false;
    }

    final void fireSizeChanged(int width, int height) {
        if (sizeInitialized && this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        sizeInitialized = true;
        sizeChanged(width, height);
    }
}
