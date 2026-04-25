package javax.microedition.lcdui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Displayable {
    private final List<Command> commands = new ArrayList<>();
    private String title;
    private Ticker ticker;
    private CommandListener commandListener;
    private boolean shown;

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
        if (command != null && !commands.contains(command)) {
            commands.add(command);
        }
    }

    public void removeCommand(Command command) {
        commands.remove(command);
    }

    public void setCommandListener(CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public CommandListener getCommandListener() {
        return commandListener;
    }

    public boolean isShown() {
        return shown;
    }

    public void fireCommand(int index) {
        var availableCommands = orderedCommands();
        if (commandListener == null || availableCommands.isEmpty()) {
            return;
        }

        var softKeys = softKeyCommands();
        if (index >= 0 && index < softKeys.length && softKeys[index] != null) {
            commandListener.commandAction(softKeys[index], this);
            return;
        }

        var resolvedIndex = Math.max(0, Math.min(index, availableCommands.size() - 1));
        commandListener.commandAction(availableCommands.get(resolvedIndex), this);
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

    private ArrayList<Command> orderedCommands() {
        var ordered = new ArrayList<Command>(commands.size());
        for (var command : commands) {
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
}
