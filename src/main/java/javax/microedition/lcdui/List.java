package javax.microedition.lcdui;

import java.util.ArrayList;

public class List extends Screen implements Choice {
    private final java.util.List<String> values = new ArrayList<>();

    public List(String title, int listType) {
        setTitle(title);
    }

    public int append(String stringPart, Image imagePart) {
        values.add(stringPart);
        return values.size() - 1;
    }

    public String getString(int elementNum) {
        return values.get(elementNum);
    }

    public int size() {
        return values.size();
    }
}
