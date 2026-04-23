package javax.microedition.lcdui;

public class Item {
    private String label;

    protected Item(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
