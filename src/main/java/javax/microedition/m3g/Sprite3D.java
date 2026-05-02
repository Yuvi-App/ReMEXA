package javax.microedition.m3g;

public class Sprite3D extends Node {
    private final boolean scaled;
    private final Image2D image;
    private final Appearance appearance;
    private int cropX;
    private int cropY;
    private int cropWidth;
    private int cropHeight;

    public Sprite3D(boolean scaled, Image2D image, Appearance appearance) {
        if (image == null) {
            throw new NullPointerException();
        }
        this.scaled = scaled;
        this.image = image;
        this.appearance = appearance;
        this.cropWidth = image.getWidth();
        this.cropHeight = image.getHeight();
        addReference(image);
        addReference(appearance);
    }

    public void setCrop(int cropX, int cropY, int cropWidth, int cropHeight) {
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
    }

    public int getCropX() {
        return cropX;
    }

    public int getCropY() {
        return cropY;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public boolean isScaled() {
        return scaled;
    }

    public Image2D getImage() {
        return image;
    }

    public Appearance getAppearance() {
        return appearance;
    }

    protected boolean rayIntersect(int scope, float[] ray, RayIntersection ri, Transform transform) {
        return false;
    }
}
