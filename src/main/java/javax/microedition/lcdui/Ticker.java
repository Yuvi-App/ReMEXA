package javax.microedition.lcdui;

public class Ticker {
    private String string;

    public Ticker(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
