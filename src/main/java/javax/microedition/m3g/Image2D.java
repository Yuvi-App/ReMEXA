package javax.microedition.m3g;

import java.util.Arrays;
import javax.microedition.lcdui.Image;

public class Image2D extends Object3D {
    public static final int ALPHA = 96;
    public static final int LUMINANCE = 97;
    public static final int LUMINANCE_ALPHA = 98;
    public static final int RGB = 99;
    public static final int RGBA = 100;

    private static byte[] opaqueScanline;

    private final int type;
    private final int width;
    private final int height;
    private final boolean mutable;
    private byte[] imageData;
    private boolean dirty = true;
    private boolean loaded;
    private int id;

    public Image2D(int type, Object image) {
        if (image == null) {
            throw new NullPointerException();
        }
        if (!isValidType(type)) {
            throw new IllegalArgumentException();
        }
        if (!(image instanceof Image source)) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.width = source.getWidth();
        this.height = source.getHeight();
        this.mutable = false;
        int[] pixels = new int[width * height];
        source.getRGB(pixels, 0, width, 0, 0, width, height);
        this.imageData = convert(type, pixels);
    }

    public Image2D(int type, int width, int height, byte[] pixels) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        validateDimensions(type, width, height);
        this.type = type;
        this.width = width;
        this.height = height;
        this.mutable = false;
        int required = requiredByteCount(type, width, height);
        if (pixels.length < required) {
            throw new IllegalArgumentException();
        }
        this.imageData = new byte[required];
        System.arraycopy(pixels, 0, this.imageData, 0, required);
    }

    public Image2D(int type, int width, int height, byte[] indices, byte[] palette) {
        if (indices == null || palette == null) {
            throw new NullPointerException();
        }
        validateDimensions(type, width, height);
        this.type = type;
        this.width = width;
        this.height = height;
        this.mutable = false;

        int componentCount = bytesPerPixel(type);
        int pixelCount = width * height;
        if (indices.length < pixelCount || palette.length == 0 || palette.length % componentCount != 0) {
            throw new IllegalArgumentException();
        }
        int paletteEntries = palette.length / componentCount;
        if (paletteEntries > 256) {
            throw new IllegalArgumentException();
        }

        this.imageData = new byte[pixelCount * componentCount];
        for (int index = 0; index < pixelCount; index++) {
            int paletteIndex = indices[index] & 0xFF;
            if (paletteIndex >= paletteEntries) {
                throw new IllegalArgumentException();
            }
            System.arraycopy(
                    palette,
                    paletteIndex * componentCount,
                    imageData,
                    index * componentCount,
                    componentCount
            );
        }
    }

    public Image2D(int type, int width, int height) {
        validateDimensions(type, width, height);
        this.type = type;
        this.width = width;
        this.height = height;
        this.mutable = true;
        this.imageData = new byte[requiredByteCount(type, width, height)];
        fillOpaque(imageData, width * bytesPerPixel(type));
    }

    public void set(int x, int y, int width, int height, byte[] pixels) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        if (!mutable
                || x < 0
                || y < 0
                || width <= 0
                || height <= 0
                || x + width > this.width
                || y + height > this.height) {
            throw new IllegalArgumentException();
        }
        int componentCount = bytesPerPixel(type);
        int sourceStride = width * componentCount;
        if (pixels.length < sourceStride * height) {
            throw new IllegalArgumentException();
        }
        int targetStride = this.width * componentCount;
        for (int row = 0; row < height; row++) {
            System.arraycopy(
                    pixels,
                    row * sourceStride,
                    imageData,
                    ((y + row) * this.width + x) * componentCount,
                    sourceStride
            );
        }
        dirty = true;
        loaded = false;
    }

    public boolean isMutable() {
        return mutable;
    }

    public int getFormat() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerColor() {
        return bytesPerPixel(type);
    }

    public final byte[] getImageData() {
        return imageData;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
        if (!loaded) {
            dirty = true;
        }
    }

    public void setId(int id) {
        this.id = id;
        if (id == 0) {
            loaded = false;
            dirty = true;
        }
    }

    public int getId() {
        return id;
    }

    public int size() {
        return imageData.length;
    }

    public void getPixels(byte[] dst) {
        System.arraycopy(imageData, 0, dst, 0, imageData.length);
    }

    public boolean isPalettized() {
        return false;
    }

    public void getPalette(byte[] array) {
    }

    boolean isDirty() {
        return dirty || !loaded;
    }

    void markClean(int textureId) {
        id = textureId;
        loaded = true;
        dirty = false;
    }

    byte[] rgbaData() {
        byte[] rgba = new byte[width * height * 4];
        switch (type) {
            case ALPHA -> {
                for (int index = 0; index < width * height; index++) {
                    int dst = index * 4;
                    rgba[dst] = (byte) 0xFF;
                    rgba[dst + 1] = (byte) 0xFF;
                    rgba[dst + 2] = (byte) 0xFF;
                    rgba[dst + 3] = imageData[index];
                }
            }
            case LUMINANCE -> {
                for (int index = 0; index < width * height; index++) {
                    int dst = index * 4;
                    byte value = imageData[index];
                    rgba[dst] = value;
                    rgba[dst + 1] = value;
                    rgba[dst + 2] = value;
                    rgba[dst + 3] = (byte) 0xFF;
                }
            }
            case LUMINANCE_ALPHA -> {
                for (int index = 0; index < width * height; index++) {
                    int src = index * 2;
                    int dst = index * 4;
                    byte value = imageData[src];
                    rgba[dst] = value;
                    rgba[dst + 1] = value;
                    rgba[dst + 2] = value;
                    rgba[dst + 3] = imageData[src + 1];
                }
            }
            case RGB -> {
                for (int index = 0; index < width * height; index++) {
                    int src = index * 3;
                    int dst = index * 4;
                    rgba[dst] = imageData[src];
                    rgba[dst + 1] = imageData[src + 1];
                    rgba[dst + 2] = imageData[src + 2];
                    rgba[dst + 3] = (byte) 0xFF;
                }
            }
            case RGBA -> {
                for (int index = 0; index < width * height; index++) {
                    int src = index * 4;
                    int dst = index * 4;
                    rgba[dst] = imageData[src];
                    rgba[dst + 1] = imageData[src + 1];
                    rgba[dst + 2] = imageData[src + 2];
                    rgba[dst + 3] = imageData[src + 3];
                }
            }
            default -> throw new IllegalStateException("Unsupported image format: " + type);
        }
        return rgba;
    }

    protected Object3D duplicateObject() {
        Image2D clone = (Image2D) super.duplicateObject();
        clone.imageData = imageData.clone();
        clone.dirty = true;
        clone.loaded = false;
        clone.id = 0;
        return clone;
    }

    private static void validateDimensions(int type, int width, int height) {
        if (!isValidType(type) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        requiredByteCount(type, width, height);
    }

    private static int requiredByteCount(int type, int width, int height) {
        long total = (long) width * (long) height * (long) bytesPerPixel(type);
        if (total <= 0L || total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        return (int) total;
    }

    private static int bytesPerPixel(int type) {
        return switch (type) {
            case ALPHA, LUMINANCE -> 1;
            case LUMINANCE_ALPHA -> 2;
            case RGB -> 3;
            case RGBA -> 4;
            default -> throw new IllegalArgumentException();
        };
    }

    private static boolean isValidType(int type) {
        return type >= ALPHA && type <= RGBA;
    }

    private static void fillOpaque(byte[] target, int rowSize) {
        if (opaqueScanline == null || opaqueScanline.length < rowSize) {
            opaqueScanline = new byte[rowSize];
            Arrays.fill(opaqueScanline, (byte) 0xFF);
        }
        for (int offset = 0; offset < target.length; offset += rowSize) {
            System.arraycopy(opaqueScanline, 0, target, offset, rowSize);
        }
    }

    private static byte[] convert(int type, int[] pixels) {
        byte[] converted = new byte[pixels.length * bytesPerPixel(type)];
        switch (type) {
            case ALPHA -> {
                for (int index = 0; index < pixels.length; index++) {
                    converted[index] = (byte) (pixels[index] >>> 24);
                }
            }
            case LUMINANCE -> {
                for (int index = 0; index < pixels.length; index++) {
                    converted[index] = luminance(pixels[index]);
                }
            }
            case LUMINANCE_ALPHA -> {
                for (int index = 0; index < pixels.length; index++) {
                    int dst = index * 2;
                    converted[dst] = luminance(pixels[index]);
                    converted[dst + 1] = (byte) (pixels[index] >>> 24);
                }
            }
            case RGB -> {
                for (int index = 0; index < pixels.length; index++) {
                    int pixel = pixels[index];
                    int dst = index * 3;
                    converted[dst] = (byte) (pixel >>> 16);
                    converted[dst + 1] = (byte) (pixel >>> 8);
                    converted[dst + 2] = (byte) pixel;
                }
            }
            case RGBA -> {
                for (int index = 0; index < pixels.length; index++) {
                    int pixel = pixels[index];
                    int dst = index * 4;
                    converted[dst] = (byte) (pixel >>> 16);
                    converted[dst + 1] = (byte) (pixel >>> 8);
                    converted[dst + 2] = (byte) pixel;
                    converted[dst + 3] = (byte) (pixel >>> 24);
                }
            }
            default -> throw new IllegalArgumentException();
        }
        return converted;
    }

    private static byte luminance(int pixel) {
        int red = (pixel >>> 16) & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return (byte) ((red + green + blue) / 3);
    }
}
