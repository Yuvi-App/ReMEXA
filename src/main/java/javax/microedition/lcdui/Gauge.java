package javax.microedition.lcdui;

public class Gauge extends Item {
    public static final int INDEFINITE = -1;
    public static final int CONTINUOUS_IDLE = 0;
    public static final int INCREMENTAL_IDLE = 1;
    public static final int CONTINUOUS_RUNNING = 2;
    public static final int INCREMENTAL_UPDATING = 3;

    private final boolean interactive;
    private int maxValue;
    private int value;
    private boolean attachedToAlert;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        super(label);
        this.interactive = interactive;
        validateMaxValue(interactive, maxValue);
        this.maxValue = maxValue;
        this.value = normalizeInitialValue(interactive, maxValue, initialValue);
    }

    @Override
    public void setLabel(String label) {
        ensureMutableWhileDetached();
        super.setLabel(label);
    }

    public void setValue(int value) {
        this.value = normalizeValue(maxValue, value);
    }

    public int getValue() {
        return value;
    }

    public void setMaxValue(int maxValue) {
        validateMaxValue(interactive, maxValue);
        if (this.maxValue == maxValue) {
            return;
        }

        var hadIndefiniteRange = isIndefiniteRange(this.maxValue);
        this.maxValue = maxValue;
        if (isIndefiniteRange(maxValue)) {
            value = hadIndefiniteRange ? value : CONTINUOUS_IDLE;
            return;
        }
        value = hadIndefiniteRange ? 0 : Math.min(value, maxValue);
    }

    public int getMaxValue() {
        return maxValue;
    }

    public boolean isInteractive() {
        return interactive;
    }

    boolean canBeAlertIndicator() {
        return !interactive && getLabel() == null && !attachedToAlert;
    }

    void attachToAlert() {
        attachedToAlert = true;
    }

    void detachFromAlert() {
        attachedToAlert = false;
    }

    private static void validateMaxValue(boolean interactive, int maxValue) {
        if (interactive) {
            if (maxValue <= 0) {
                throw new IllegalArgumentException("Interactive gauges require a positive maxValue.");
            }
            return;
        }
        if (maxValue <= 0 && maxValue != INDEFINITE) {
            throw new IllegalArgumentException("Non-interactive gauges require a positive maxValue or INDEFINITE.");
        }
    }

    private static int normalizeInitialValue(boolean interactive, int maxValue, int initialValue) {
        if (!interactive && isIndefiniteRange(maxValue)) {
            return normalizeIndefiniteValue(initialValue);
        }
        return Math.max(0, Math.min(initialValue, maxValue));
    }

    private static int normalizeValue(int maxValue, int value) {
        if (isIndefiniteRange(maxValue)) {
            return normalizeIndefiniteValue(value);
        }
        return Math.max(0, Math.min(value, maxValue));
    }

    private static int normalizeIndefiniteValue(int value) {
        return switch (value) {
            case CONTINUOUS_IDLE, INCREMENTAL_IDLE, CONTINUOUS_RUNNING, INCREMENTAL_UPDATING -> value;
            default -> throw new IllegalArgumentException("Indefinite gauges only accept the MIDP activity constants.");
        };
    }

    private static boolean isIndefiniteRange(int maxValue) {
        return maxValue == INDEFINITE;
    }

    private void ensureMutableWhileDetached() {
        if (attachedToAlert) {
            throw new IllegalStateException("Gauge cannot be modified while attached to an Alert.");
        }
    }
}
