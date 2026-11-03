package javax.microedition.lcdui;

import java.util.ArrayList;
import javax.swing.SwingUtilities;
import remexa.host.input.HostTextInputRequest;
import remexa.host.runtime.MidletRuntime;

public class Form extends Screen {
    private final java.util.List<Item> items = new ArrayList<>();
    private ItemStateListener itemStateListener;
    private int focusedItem = -1;
    private boolean focusNeedsScroll = true;
    private boolean textInputInFlight;

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
        insert(items.size(), item);
        return items.size() - 1;
    }

    public int append(String value) {
        return append(new StringItem(null, value));
    }

    public void delete(int itemNum) {
        items.remove(itemNum).setOwner(null);
        if (focusedItem > itemNum) {
            focusedItem--;
        }
        contentsChanged();
    }

    public void deleteAll() {
        for (var item : items) {
            item.setOwner(null);
        }
        items.clear();
        focusedItem = -1;
        contentsChanged();
    }

    public void insert(int itemNum, Item item) {
        if (itemNum < 0 || itemNum > items.size()) {
            throw new IndexOutOfBoundsException("Form index out of range: " + itemNum);
        }
        checkItemOwner(item);
        items.add(itemNum, item);
        item.setOwner(this);
        if (focusedItem >= itemNum) {
            focusedItem++;
        }
        contentsChanged();
    }

    public void set(int itemNum, Item item) {
        var previous = items.get(itemNum);
        if (previous == item) {
            return;
        }
        checkItemOwner(item);
        previous.setOwner(null);
        items.set(itemNum, item);
        item.setOwner(this);
        contentsChanged();
    }

    public void setItemStateListener(ItemStateListener listener) {
        itemStateListener = listener;
    }

    void itemStateChanged(Item item) {
        if (item.owner() == this && itemStateListener != null) {
            itemStateListener.itemStateChanged(item);
        }
    }

    private void checkItemOwner(Item item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        if (item.owner() != null) {
            throw new IllegalStateException("Item is already contained in a Form.");
        }
    }

    private void contentsChanged() {
        ensureFocusedItem();
        if (isShown()) {
            repaintHost();
        }
    }

    public int size() {
        return items.size();
    }

    public Item get(int index) {
        return items.get(index);
    }

    @Override
    void fireScreenKeyPressed(int keyCode) {
        if (textInputInFlight) {
            return;
        }
        ensureFocusedItem();
        if (focusedItem < 0) {
            super.fireScreenKeyPressed(keyCode);
            return;
        }
        var item = items.get(focusedItem);
        if (item instanceof ChoiceGroup choiceGroup && choiceGroup.handleKeyPressed(keyCode)) {
            focusNeedsScroll = true;
            contentsChanged();
            return;
        }
        if (item instanceof TextField textField
                && (keyCode == Canvas.KEYCODE_FIRE || keyCode == Canvas.FIRE || keyCode == '5')) {
            editTextField(textField);
            return;
        }
        var direction = switch (keyCode) {
            case Canvas.KEYCODE_UP, Canvas.UP, '2' -> -1;
            case Canvas.KEYCODE_DOWN, Canvas.DOWN, '8' -> 1;
            default -> 0;
        };
        if (direction == 0) {
            super.fireScreenKeyPressed(keyCode);
            return;
        }
        for (var index = focusedItem + direction; index >= 0 && index < items.size(); index += direction) {
            if (isFocusable(items.get(index))) {
                focusedItem = index;
                focusNeedsScroll = true;
                contentsChanged();
                return;
            }
        }
        super.fireScreenKeyPressed(keyCode);
    }

    private void ensureFocusedItem() {
        if (focusedItem >= 0 && focusedItem < items.size() && isFocusable(items.get(focusedItem))) {
            return;
        }
        focusedItem = -1;
        focusNeedsScroll = true;
        for (var index = 0; index < items.size(); index++) {
            if (isFocusable(items.get(index))) {
                focusedItem = index;
                return;
            }
        }
    }

    private boolean isFocusable(Item item) {
        return item instanceof ChoiceGroup choiceGroup && choiceGroup.size() > 0
                || item instanceof TextField textField && (textField.getConstraints() & TextField.UNEDITABLE) == 0;
    }

    @Override
    protected void sizeChanged(int width, int height) {
        focusNeedsScroll = true;
    }

    private void editTextField(TextField field) {
        var request = new HostTextInputRequest(field.getLabel(), field.getString(),
                field.getConstraints(), field.getMaxSize(), false);
        var classLoader = MidletRuntime.currentAppClassLoader();
        textInputInFlight = true;
        var worker = new Thread(() -> {
            try {
                var result = MidletRuntime.requestTextInputResult(request);
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (result.accepted() && field.owner() == this && isShown()
                                && MidletRuntime.isAppActive(classLoader)) {
                            var text = result.text();
                            if (field.getMaxSize() > 0 && text.length() > field.getMaxSize()) {
                                text = text.substring(0, field.getMaxSize());
                            }
                            if (!text.equals(field.getString())) {
                                field.setString(text);
                                itemStateChanged(field);
                            }
                        }
                    } finally {
                        textInputInFlight = false;
                        contentsChanged();
                    }
                });
            } catch (RuntimeException | Error exception) {
                SwingUtilities.invokeLater(() -> textInputInFlight = false);
                throw exception;
            }
        }, "remexa-form-input");
        worker.setContextClassLoader(classLoader);
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    protected void paintScreen(Graphics graphics) {
        var bodyTop = paintChrome(graphics);
        var font = Font.getDefaultFont();
        var lines = new ArrayList<String>();
        var textWidth = bodyTextWidth();
        ensureFocusedItem();
        var focusedLine = -1;
        for (var index = 0; index < items.size(); index++) {
            var itemLine = appendItemLines(items.get(index), font, textWidth, lines, index == focusedItem);
            if (index == focusedItem) {
                focusedLine = itemLine;
            }
        }
        paintLines(graphics, lines, bodyTop, focusNeedsScroll ? focusedLine : -1);
        focusNeedsScroll = false;
    }

    private int appendItemLines(Item item, Font font, int maxWidth, java.util.List<String> lines, boolean focused) {
        var itemLine = lines.size();
        var label = item.getLabel();
        if (label != null && !label.isEmpty()) {
            appendWrappedLines(label, font, maxWidth, lines);
        }
        if (item instanceof StringItem stringItem) {
            appendWrappedLines(stringItem.getText(), font, maxWidth, lines);
            return itemLine;
        }
        if (item instanceof ChoiceGroup choiceGroup) {
            return appendChoiceLines(choiceGroup, font, maxWidth, lines, focused);
        }
        if (item instanceof TextField textField) {
            itemLine = lines.size();
            var text = textField.getString();
            if ((textField.getConstraints() & TextField.PASSWORD) != 0) {
                text = "*".repeat(text.length());
            }
            appendWrappedLines((focused ? "> " : "  ") + "[" + text + "]", font, maxWidth, lines);
            return itemLine;
        }
        if (label == null || label.isEmpty()) {
            appendWrappedLines(item.toString(), font, maxWidth, lines);
        }
        return itemLine;
    }

    private int appendChoiceLines(ChoiceGroup choiceGroup, Font font, int maxWidth,
                                 java.util.List<String> lines, boolean focused) {
        var focusedLine = lines.size();
        if (choiceGroup.choiceType() == Choice.POPUP) {
            var selected = choiceGroup.getSelectedIndex();
            var text = selected < 0 ? "" : choiceGroup.getString(selected);
            appendWrappedLines((focused ? "> " : "  ") + "< " + text + " >", font, maxWidth, lines);
            return focusedLine;
        }
        for (var index = 0; index < choiceGroup.size(); index++) {
            var highlighted = focused && index == choiceGroup.highlightedIndex();
            if (highlighted) {
                focusedLine = lines.size();
            }
            var prefix = highlighted ? "> " : "  ";
            prefix += choiceGroup.choiceType() == Choice.MULTIPLE
                    ? (choiceGroup.isSelected(index) ? "[x] " : "[ ] ")
                    : (choiceGroup.isSelected(index) ? "(*) " : "( ) ");
            appendWrappedLines(prefix + choiceGroup.getString(index), font, maxWidth, lines);
        }
        return focusedLine;
    }
}
