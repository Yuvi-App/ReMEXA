package javax.microedition.lcdui;

public class TextBox extends Screen {
    private String text;
    private final int maxSize;
    private final int constraints;

    protected TextBox() {
        this(null, "", 0, TextField.ANY);
    }

    public TextBox(String title, String text, int maxSize, int constraints) {
        setTitle(title);
        this.text = text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() {
        return text;
    }

    public void setString(String text) {
        this.text = text;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getConstraints() {
        return constraints;
    }
}
