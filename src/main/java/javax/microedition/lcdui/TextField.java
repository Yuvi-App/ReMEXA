package javax.microedition.lcdui;

public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;
    public static final int UNEDITABLE = 0x20000;
    public static final int SENSITIVE = 0x40000;
    public static final int NON_PREDICTIVE = 0x80000;
    public static final int INITIAL_CAPS_WORD = 0x100000;
    public static final int INITIAL_CAPS_SENTENCE = 0x200000;
    public static final int CONSTRAINT_MASK = 0xffff;

    private String value;
    private final int maxSize;
    private final int constraints;
    private String initialInputMode;

    protected TextField() {
        this(null, "", 0, ANY);
    }

    public TextField(String label, String value, int maxSize, int constraints) {
        super(label);
        this.value = value == null ? "" : value;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() {
        return value;
    }

    public void setString(String value) {
        this.value = value == null ? "" : value;
        repaintOwner();
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
