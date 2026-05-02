package javax.microedition.lcdui;

public class TextField extends Item {
    public static final int ANY = 0;

    private String value;
    private final int maxSize;
    private final int constraints;
    private String initialInputMode;

    protected TextField() {
        this(null, "", 0, ANY);
    }

    public TextField(String label, String value, int maxSize, int constraints) {
        super(label);
        this.value = value;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() {
        return value;
    }

    public void setString(String value) {
        this.value = value;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getConstraints() {
        return constraints;
    }

    public void setInitialInputMode(String characterSubset) {
        initialInputMode = characterSubset;
    }

    public String getInitialInputMode() {
        return initialInputMode;
    }
}
