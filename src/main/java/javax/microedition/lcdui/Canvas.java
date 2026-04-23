package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = -1;
    public static final int DOWN = -2;
    public static final int LEFT = -3;
    public static final int RIGHT = -4;
    public static final int FIRE = -5;

    protected abstract void paint(Graphics graphics);

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    public int getWidth() {
        return 240;
    }

    public int getHeight() {
        return 320;
    }

    public void repaint() {
    }

    public void serviceRepaints() {
    }
}
