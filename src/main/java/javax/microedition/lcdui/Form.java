package javax.microedition.lcdui;

import java.util.ArrayList;

public class Form extends Screen {
    private final java.util.List<Item> items = new ArrayList<>();

    public Form(String title) {
        setTitle(title);
    }

    public Form(String title, Item[] items) {
        this(title);
        if (items != null) {
            for (var item : items) {
                append(item);
            }
        }
    }

    public int append(Item item) {
        items.add(item);
        if (isShown()) {
            repaintHost();
        }
        return items.size() - 1;
    }

    public int append(String value) {
        return append(new StringItem(null, value));
    }

    public int size() {
        return items.size();
    }

    public Item get(int index) {
        return items.get(index);
    }

    @Override
    protected void paintScreen(Graphics graphics) {
        var bodyTop = paintChrome(graphics);
        var font = Font.getDefaultFont();
        var lines = new ArrayList<String>();
        var textWidth = bodyTextWidth();
        for (var item : items) {
            appendItemLines(item, font, textWidth, lines);
        }
        paintLines(graphics, lines, bodyTop);
    }

    private void appendItemLines(Item item, Font font, int maxWidth, java.util.List<String> lines) {
        if (item == null) {
            return;
        }
        var label = item.getLabel();
        if (label != null && !label.isEmpty()) {
            appendWrappedLines(label, font, maxWidth, lines);
        }
        if (item instanceof StringItem stringItem) {
            appendWrappedLines(stringItem.getText(), font, maxWidth, lines);
            return;
        }
        if (item instanceof ChoiceGroup choiceGroup) {
            appendChoiceLines(choiceGroup, font, maxWidth, lines);
            return;
        }
        if (label == null || label.isEmpty()) {
            appendWrappedLines(item.toString(), font, maxWidth, lines);
        }
    }

    private void appendChoiceLines(ChoiceGroup choiceGroup, Font font, int maxWidth, java.util.List<String> lines) {
        for (var index = 0; index < choiceGroup.size(); index++) {
            var prefix = choiceGroup.isSelected(index) ? "(*) " : "( ) ";
            appendWrappedLines(prefix + choiceGroup.getString(index), font, maxWidth, lines);
        }
    }
}
