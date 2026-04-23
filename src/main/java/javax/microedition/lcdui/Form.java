package javax.microedition.lcdui;

import java.util.ArrayList;
import java.util.List;

public class Form extends Screen {
    private final List<Item> items = new ArrayList<>();

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
        return items.size() - 1;
    }

    public int append(String value) {
        return append(new StringItem(null, value));
    }

    public int size() {
        return items.size();
    }
}
