package javax.microedition.lcdui;

public class Alert extends Screen {
    private String text;
    private AlertType type;

    public Alert(String title) {
        setTitle(title);
    }

    public Alert(String title, String text, Image image, AlertType type) {
        this(title);
        this.text = text;
        this.type = type;
    }

    public void setString(String text) {
        this.text = text;
    }

    public String getString() {
        return text;
    }

    public AlertType getType() {
        return type;
    }
}
