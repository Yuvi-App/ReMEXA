package javax.microedition.lcdui;

import java.util.ArrayList;

public class List extends Screen implements Choice {
    public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);
    private final java.util.List<String> values = new ArrayList<>();
    private final int listType;
    private int selectedIndex;
    private Command selectCommand = SELECT_COMMAND;

    public List(String title, int listType) {
        setTitle(title);
        this.listType = listType;
    }

    public List(String title, int listType, String[] stringElements, Image[] imageElements) {
        this(title, listType);
        if (stringElements == null) {
            return;
        }
        for (var stringElement : stringElements) {
            append(stringElement, null);
        }
    }

    public int append(String stringPart, Image imagePart) {
        values.add(stringPart == null ? "" : stringPart);
        if (isShown()) {
            repaintHost();
        }
        return values.size() - 1;
    }

    public String getString(int elementNum) {
        return values.get(elementNum);
    }

    public int size() {
        return values.size();
    }

    public int getSelectedIndex() {
        return values.isEmpty() ? -1 : selectedIndex;
    }

    public void setSelectCommand(Command command) {
        if (listType != IMPLICIT) {
            return;
        }
        selectCommand = command;
        if (command != null && command != SELECT_COMMAND) {
            addCommand(command);
        }
    }

    @Override
    public void removeCommand(Command command) {
        if (selectCommand == command) {
            selectCommand = null;
        }
        super.removeCommand(command);
    }

    public void setSelectedIndex(int elementNum, boolean selected) {
        if (elementNum < 0 || elementNum >= values.size()) {
            throw new IndexOutOfBoundsException("List index out of range: " + elementNum);
        }
        if (selected || listType != MULTIPLE) {
            selectedIndex = elementNum;
            if (isShown()) {
                repaintHost();
            }
        }
    }

    @Override
    void fireScreenKeyPressed(int keyCode) {
        if (values.isEmpty()) {
            return;
        }
        if (listType == IMPLICIT && isSelectKey(keyCode)) {
            var listener = getCommandListener();
            if (selectCommand != null && listener != null) {
                listener.commandAction(selectCommand, this);
            }
            return;
        }
        var previous = selectedIndex;
        switch (keyCode) {
            case Canvas.KEYCODE_UP, Canvas.UP, '2' -> selectedIndex = Math.max(0, selectedIndex - 1);
            case Canvas.KEYCODE_DOWN, Canvas.DOWN, '8' -> selectedIndex = Math.min(values.size() - 1, selectedIndex + 1);
            default -> {
                super.fireScreenKeyPressed(keyCode);
                return;
            }
        }
        if (selectedIndex != previous) {
            repaintHost();
        }
    }

    @Override
    void fireScreenKeyRepeated(int keyCode) {
        if (!isSelectKey(keyCode)) {
            fireScreenKeyPressed(keyCode);
        }
    }

    private boolean isSelectKey(int keyCode) {
        return keyCode == Canvas.KEYCODE_FIRE || keyCode == Canvas.FIRE || keyCode == '5';
    }

    @Override
    protected void paintScreen(Graphics graphics) {
        var bodyTop = paintChrome(graphics);
        var font = Font.getDefaultFont();
        var lines = new ArrayList<String>();
        for (var index = 0; index < values.size(); index++) {
            var prefix = index == selectedIndex ? "> " : "  ";
            appendWrappedLines(prefix + values.get(index), font, bodyTextWidth(), lines);
        }
        paintLines(graphics, lines, bodyTop);
    }
}
