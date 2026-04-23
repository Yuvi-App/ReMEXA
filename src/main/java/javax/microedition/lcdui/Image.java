package javax.microedition.lcdui;

public class Image {
    private final int width;
    private final int height;

    private Image(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static Image createImage(int width, int height) {
        return new Image(width, height);
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return new Image(0, 0);
    }

    public static Image createImage(String name) {
        return new Image(0, 0);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
