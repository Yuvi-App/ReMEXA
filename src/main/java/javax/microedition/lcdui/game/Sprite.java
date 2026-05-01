package javax.microedition.lcdui.game;

import java.awt.Rectangle;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {
    public static final int TRANS_NONE = 0;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_MIRROR_ROT270 = 4;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR_ROT90 = 7;

    private Image image;
    private int frameWidth;
    private int frameHeight;
    private int columns;
    private int rawFrameCount;
    private int frame;
    private int[] frameSequence;
    private int transform = TRANS_NONE;
    private int referencePixelX;
    private int referencePixelY;
    private Rectangle collisionRectangle;

    public Sprite(Image image) {
        this(image, imageWidth(image), imageHeight(image));
    }

    public Sprite(Image image, int frameWidth, int frameHeight) {
        super(frameWidth, frameHeight);
        setImage(image, frameWidth, frameHeight);
    }

    public Sprite(Sprite sprite) {
        this(requireSprite(sprite).image, sprite.frameWidth, sprite.frameHeight);
        setPosition(sprite.getX(), sprite.getY());
        setVisible(sprite.isVisible());
        transform = sprite.transform;
        frame = sprite.frame;
        frameSequence = sprite.frameSequence == null ? null : sprite.frameSequence.clone();
        referencePixelX = sprite.referencePixelX;
        referencePixelY = sprite.referencePixelY;
        collisionRectangle = new Rectangle(sprite.collisionRectangle);
        updateLayerSize();
    }

    public void setImage(Image image, int frameWidth, int frameHeight) {
        requireImage(image);
        validateFrameSize(image, frameWidth, frameHeight);
        boolean initialized = this.image != null;
        boolean sizeChanged = initialized && (this.frameWidth != frameWidth || this.frameHeight != frameHeight);
        int refScreenX = initialized ? getRefPixelX() : 0;
        int refScreenY = initialized ? getRefPixelY() : 0;
        this.image = image;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        columns = Math.max(1, image.getWidth() / frameWidth);
        rawFrameCount = columns * Math.max(1, image.getHeight() / frameHeight);
        if (frameSequence != null && !isValidFrameSequence(frameSequence)) {
            frameSequence = null;
            frame = 0;
        }
        frame = Math.min(frame, getFrameSequenceLength() - 1);
        if (collisionRectangle == null || sizeChanged) {
            collisionRectangle = new Rectangle(0, 0, frameWidth, frameHeight);
        }
        updateLayerSize();
        if (initialized && sizeChanged) {
            setRefPixelPosition(refScreenX, refScreenY);
        }
    }

    public void setFrame(int sequenceIndex) {
        if (sequenceIndex < 0 || sequenceIndex >= getFrameSequenceLength()) {
            throw new IndexOutOfBoundsException("Frame index out of range: " + sequenceIndex);
        }
        frame = sequenceIndex;
    }

    public final int getFrame() {
        return frame;
    }

    public void nextFrame() {
        frame = (frame + 1) % getFrameSequenceLength();
    }

    public void prevFrame() {
        frame = (frame + getFrameSequenceLength() - 1) % getFrameSequenceLength();
    }

    public void setFrameSequence(int[] sequence) {
        if (sequence == null) {
            frameSequence = null;
            frame = 0;
            return;
        }
        if (sequence.length == 0 || !isValidFrameSequence(sequence)) {
            throw new IllegalArgumentException("Invalid frame sequence");
        }
        frameSequence = sequence.clone();
        frame = 0;
    }

    public int getFrameSequenceLength() {
        return frameSequence == null ? rawFrameCount : frameSequence.length;
    }

    public int getRawFrameCount() {
        return rawFrameCount;
    }

    public void setTransform(int transform) {
        validateTransform(transform);
        if (this.transform == transform) {
            return;
        }
        int refScreenX = getRefPixelX();
        int refScreenY = getRefPixelY();
        this.transform = transform;
        updateLayerSize();
        setRefPixelPosition(refScreenX, refScreenY);
    }

    public int getTransform() {
        return transform;
    }

    public void defineReferencePixel(int x, int y) {
        referencePixelX = x;
        referencePixelY = y;
    }

    public void setRefPixelPosition(int x, int y) {
        int[] ref = transformedPoint(referencePixelX, referencePixelY);
        setPosition(x - ref[0], y - ref[1]);
    }

    public int getRefPixelX() {
        return getX() + transformedPoint(referencePixelX, referencePixelY)[0];
    }

    public int getRefPixelY() {
        return getY() + transformedPoint(referencePixelX, referencePixelY)[1];
    }

    public void defineCollisionRectangle(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Collision rectangle size must be non-negative");
        }
        collisionRectangle = new Rectangle(x, y, width, height);
    }

    public final boolean collidesWith(Sprite sprite, boolean pixelLevel) {
        if (sprite == null || !isVisible() || !sprite.isVisible()) {
            return false;
        }
        return transformedCollisionBounds().intersects(sprite.transformedCollisionBounds());
    }

    public final boolean collidesWith(Image image, int x, int y, boolean pixelLevel) {
        if (image == null || !isVisible()) {
            return false;
        }
        return transformedCollisionBounds().intersects(new Rectangle(x, y, image.getWidth(), image.getHeight()));
    }

    @Override
    public final void paint(Graphics graphics) {
        if (graphics == null) {
            throw new NullPointerException("graphics");
        }
        if (!isVisible()) {
            return;
        }
        int rawFrame = rawFrameFor(frame);
        int sourceX = rawFrame % columns * frameWidth;
        int sourceY = rawFrame / columns * frameHeight;
        graphics.drawRegion(image, sourceX, sourceY, frameWidth, frameHeight, transform, getX(), getY(), Graphics.LEFT | Graphics.TOP);
    }

    private int rawFrameFor(int sequenceIndex) {
        return frameSequence == null ? sequenceIndex : frameSequence[sequenceIndex];
    }

    private boolean isValidFrameSequence(int[] sequence) {
        for (int rawFrame : sequence) {
            if (rawFrame < 0 || rawFrame >= rawFrameCount) {
                return false;
            }
        }
        return true;
    }

    private Rectangle transformedCollisionBounds() {
        int collisionX = collisionRectangle.x;
        int collisionY = collisionRectangle.y;
        int collisionWidth = collisionRectangle.width;
        int collisionHeight = collisionRectangle.height;
        return switch (transform) {
            case TRANS_MIRROR_ROT180 -> new Rectangle(getX() + collisionX, getY() + frameHeight - (collisionY + collisionHeight), collisionWidth, collisionHeight);
            case TRANS_MIRROR -> new Rectangle(getX() + frameWidth - (collisionX + collisionWidth), getY() + collisionY, collisionWidth, collisionHeight);
            case TRANS_ROT180 -> new Rectangle(getX() + frameWidth - (collisionX + collisionWidth), getY() + frameHeight - (collisionY + collisionHeight), collisionWidth, collisionHeight);
            case TRANS_MIRROR_ROT270 -> new Rectangle(getX() + collisionY, getY() + collisionX, collisionHeight, collisionWidth);
            case TRANS_ROT90 -> new Rectangle(getX() + frameHeight - (collisionY + collisionHeight), getY() + collisionX, collisionHeight, collisionWidth);
            case TRANS_ROT270 -> new Rectangle(getX() + collisionY, getY() + frameWidth - (collisionX + collisionWidth), collisionHeight, collisionWidth);
            case TRANS_MIRROR_ROT90 -> new Rectangle(getX() + frameHeight - (collisionY + collisionHeight), getY() + frameWidth - (collisionX + collisionWidth), collisionHeight, collisionWidth);
            default -> new Rectangle(getX() + collisionX, getY() + collisionY, collisionWidth, collisionHeight);
        };
    }

    private int[] transformedPoint(int sourceX, int sourceY) {
        return switch (transform) {
            case TRANS_MIRROR_ROT180 -> new int[] {sourceX, frameHeight - sourceY - 1};
            case TRANS_MIRROR -> new int[] {frameWidth - sourceX - 1, sourceY};
            case TRANS_ROT180 -> new int[] {frameWidth - sourceX - 1, frameHeight - sourceY - 1};
            case TRANS_MIRROR_ROT270 -> new int[] {sourceY, sourceX};
            case TRANS_ROT90 -> new int[] {frameHeight - sourceY - 1, sourceX};
            case TRANS_ROT270 -> new int[] {sourceY, frameWidth - sourceX - 1};
            case TRANS_MIRROR_ROT90 -> new int[] {frameHeight - sourceY - 1, frameWidth - sourceX - 1};
            default -> new int[] {sourceX, sourceY};
        };
    }

    private void updateLayerSize() {
        setSize(swapsAxes(transform) ? frameHeight : frameWidth, swapsAxes(transform) ? frameWidth : frameHeight);
    }

    private static boolean swapsAxes(int transform) {
        return transform == TRANS_MIRROR_ROT270
                || transform == TRANS_ROT90
                || transform == TRANS_ROT270
                || transform == TRANS_MIRROR_ROT90;
    }

    private static void validateTransform(int transform) {
        if (transform < TRANS_NONE || transform > TRANS_MIRROR_ROT90) {
            throw new IllegalArgumentException("Unknown transform: " + transform);
        }
    }

    private static void validateFrameSize(Image image, int frameWidth, int frameHeight) {
        if (frameWidth <= 0 || frameHeight <= 0) {
            throw new IllegalArgumentException("Frame size must be positive");
        }
        if (image.getWidth() < frameWidth || image.getHeight() < frameHeight) {
            throw new IllegalArgumentException("Frame size exceeds image size");
        }
        if (image.getWidth() % frameWidth != 0 || image.getHeight() % frameHeight != 0) {
            throw new IllegalArgumentException("Image dimensions must be divisible by frame size");
        }
    }

    private static Image requireImage(Image image) {
        if (image == null) {
            throw new NullPointerException("image");
        }
        return image;
    }

    private static int imageWidth(Image image) {
        return requireImage(image).getWidth();
    }

    private static int imageHeight(Image image) {
        return requireImage(image).getHeight();
    }

    private static Sprite requireSprite(Sprite sprite) {
        if (sprite == null) {
            throw new NullPointerException("sprite");
        }
        return sprite;
    }
}
