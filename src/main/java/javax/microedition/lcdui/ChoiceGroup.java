package javax.microedition.lcdui;

import java.util.ArrayList;

public class ChoiceGroup extends Item implements Choice {
    private final int choiceType;
    private final java.util.List<String> strings = new ArrayList<>();
    private final java.util.List<Boolean> selected = new ArrayList<>();

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
            return;
        }
        if (!selected) {
            this.selected.set(elementNum, false);
            return;
        }
        for (var index = 0; index < this.selected.size(); index++) {
            this.selected.set(index, index == elementNum);
        }
    }

    boolean isSelected(int elementNum) {
        checkIndex(elementNum);
        return Boolean.TRUE.equals(selected.get(elementNum));
    }

    private void checkIndex(int elementNum) {
        if (elementNum < 0 || elementNum >= strings.size()) {
            throw new IndexOutOfBoundsException("Choice index out of range: " + elementNum);
        }
    }

    private static void validateChoiceType(int choiceType) {
        if (choiceType != EXCLUSIVE && choiceType != MULTIPLE && choiceType != IMPLICIT) {
            throw new IllegalArgumentException("Unsupported choice type: " + choiceType);
        }
    }
}
