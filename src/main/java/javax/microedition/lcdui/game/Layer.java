package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;

public abstract class Layer {
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = true;

    protected Layer(int width, int height) {
        setSize(width, height);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final boolean isVisible() {
        return visible;
    }

    public abstract void paint(Graphics graphics);

    protected final void setSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Layer size must be non-negative");
        }
        this.width = width;
        this.height = height;
    }
}
