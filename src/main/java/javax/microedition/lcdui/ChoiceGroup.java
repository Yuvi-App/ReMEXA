package javax.microedition.lcdui;

import java.util.ArrayList;

public class ChoiceGroup extends Item implements Choice {
    private final int choiceType;
    private final java.util.List<String> strings = new ArrayList<>();
    private final java.util.List<Boolean> selected = new ArrayList<>();
    private int highlightedIndex;

    public ChoiceGroup(String label, int choiceType) {
        super(label);
        validateChoiceType(choiceType);
        this.choiceType = choiceType;
    }

    public ChoiceGroup(String label, int choiceType, String[] stringElements, Image[] imageElements) {
        this(label, choiceType);
        if (stringElements == null) {
            return;
        }
        for (var stringElement : stringElements) {
            append(stringElement, null);
        }
    }

    public int append(String stringPart, Image imagePart) {
        strings.add(stringPart == null ? "" : stringPart);
        selected.add(strings.size() == 1 && choiceType != MULTIPLE);
        repaintOwner();
        return strings.size() - 1;
    }

    public String getString(int elementNum) {
        return strings.get(elementNum);
    }

    public int size() {
        return strings.size();
    }

    public int getSelectedIndex() {
        for (var index = 0; index < selected.size(); index++) {
            if (Boolean.TRUE.equals(selected.get(index))) {
                return index;
            }
        }
        return -1;
    }

    public void setSelectedIndex(int elementNum, boolean selected) {
        checkIndex(elementNum);
        if (choiceType == MULTIPLE) {
            this.selected.set(elementNum, selected);
            repaintOwner();
            return;
        }
        if (!selected) {
            return;
        }
        for (var index = 0; index < this.selected.size(); index++) {
            this.selected.set(index, index == elementNum);
        }
        repaintOwner();
    }

    public boolean isSelected(int elementNum) {
        checkIndex(elementNum);
        return Boolean.TRUE.equals(selected.get(elementNum));
    }

    int choiceType() {
        return choiceType;
    }

    int highlightedIndex() {
        return highlightedIndex;
    }

    boolean handleKeyPressed(int keyCode) {
        if (strings.isEmpty()) {
            return false;
        }
        if (choiceType == POPUP) {
            var direction = switch (keyCode) {
                case Canvas.KEYCODE_LEFT, Canvas.LEFT, '4' -> -1;
                case Canvas.KEYCODE_RIGHT, Canvas.RIGHT, '6' -> 1;
                default -> 0;
            };
            if (direction == 0) {
                return false;
            }
            var previous = getSelectedIndex();
            var next = Math.max(0, Math.min(strings.size() - 1, previous + direction));
            if (next != previous) {
                setSelectedIndex(next, true);
                notifyStateChanged();
            }
            return true;
        }
        var direction = switch (keyCode) {
            case Canvas.KEYCODE_UP, Canvas.UP, '2' -> -1;
            case Canvas.KEYCODE_DOWN, Canvas.DOWN, '8' -> 1;
            default -> 0;
        };
        if (direction != 0) {
            var next = highlightedIndex + direction;
            if (next < 0 || next >= strings.size()) {
                return false;
            }
            highlightedIndex = next;
            return true;
        }
        if (keyCode == Canvas.KEYCODE_FIRE || keyCode == Canvas.FIRE || keyCode == '5') {
            var wasSelected = isSelected(highlightedIndex);
            setSelectedIndex(highlightedIndex, choiceType != MULTIPLE || !wasSelected);
            if (choiceType == MULTIPLE || !wasSelected) {
                notifyStateChanged();
            }
            return true;
        }
        return false;
    }

    private void checkIndex(int elementNum) {
        if (elementNum < 0 || elementNum >= strings.size()) {
            throw new IndexOutOfBoundsException("Choice index out of range: " + elementNum);
        }
    }

    private static void validateChoiceType(int choiceType) {
        if (choiceType != EXCLUSIVE && choiceType != MULTIPLE && choiceType != IMPLICIT && choiceType != POPUP) {
            throw new IllegalArgumentException("Unsupported choice type: " + choiceType);
        }
    }
}
