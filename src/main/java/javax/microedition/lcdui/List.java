package javax.microedition.lcdui;

import java.util.ArrayList;

public class List extends Screen implements Choice {
    private final java.util.List<String> values = new ArrayList<>();
    private final int listType;
    private int selectedIndex;

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
