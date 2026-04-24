package com.jblend.graphics.j3d;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.microedition.lcdui.Image;
import remexa.host.runtime.MidletRuntime;

public class Texture {
    private final Image image;
    private final int width;
    private final int height;
    private final int[] pixels;
    private final int[] indexedPixels;
    private final IndexColorModel indexedColorModel;
    private final boolean forModel;

    protected Texture() {
        this.image = null;
        this.width = 0;
        this.height = 0;
        this.pixels = new int[0];
        this.indexedPixels = null;
        this.indexedColorModel = null;
        this.forModel = true;
    }

    public Texture (byte[] data, boolean isForModel) {
        this(decodeTexture(data), isForModel);
    }

    public Texture (java.lang.String name, boolean isForModel) throws java.io.IOException {
        this(decodeTexture(loadResource(name)), isForModel);
    }

    public Texture (javax.microedition.lcdui.Image image, int x, int y, int width, int height, boolean isForModel) {
        if (image == null) {
            throw new NullPointerException();
        }
        this.image = image;
        this.width = width;
        this.height = height;
        this.pixels = new int[Math.max(1, width * height)];
        image.getRGB(this.pixels, 0, width, x, y, width, height);
        this.indexedPixels = null;
        this.indexedColorModel = null;
        this.forModel = isForModel;
    }

    private Texture(DecodedTexture decoded, boolean isForModel) {
        if (decoded == null || decoded.image() == null) {
            throw new NullPointerException("decoded");
        }
        this.image = decoded.image();
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = new int[Math.max(1, width * height)];
        image.getRGB(this.pixels, 0, width, 0, 0, width, height);
        this.indexedPixels = decoded.indexedPixels();
        this.indexedColorModel = decoded.indexedColorModel();
        this.forModel = isForModel;
    }

    public Image getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isForModel() {
        return forModel;
    }

    public int sampleColor(float u, float v, boolean transparent) {
        if (pixels.length == 0 || width <= 0 || height <= 0) {
            return 0;
        }
        int x = forModel ? wrap((int) Math.floor(u), width) : clamp((int) Math.floor(u), 0, width - 1);
        int y = forModel ? wrap((int) Math.floor(v), height) : clamp((int) Math.floor(v), 0, height - 1);
        if (indexedPixels != null && indexedColorModel != null) {
            int index = indexedPixels[y * width + x];
            if (transparent && index == 0) {
                return 0;
            }
            return indexedColorModel.getRGB(index);
        }
        int argb = pixels[y * width + x];
        if (transparent && (argb == 0xFFFF00FF || (argb >>> 24) == 0)) {
            return 0;
        }
        return argb;
    }

    private static byte[] loadResource(String name) throws IOException {
        try (InputStream stream = MidletRuntime.openResource(name)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + name);
            }
            return stream.readAllBytes();
        }
    }

    private static DecodedTexture decodeTexture(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new DecodedTexture(Image.createImage(1, 1), null, null);
        }
        try {
            BufferedImage raw = ImageIO.read(new ByteArrayInputStream(bytes));
            if (raw == null) {
                return new DecodedTexture(Image.createImage(bytes, 0, bytes.length), null, null);
            }
            BufferedImage converted = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            if (raw.getColorModel() instanceof IndexColorModel colorModel) {
                int[] indices = new int[raw.getWidth() * raw.getHeight()];
                var raster = raw.getRaster();
                for (int y = 0; y < raw.getHeight(); y++) {
                    for (int x = 0; x < raw.getWidth(); x++) {
                        int index = raster.getSample(x, y, 0);
                        indices[y * raw.getWidth() + x] = index;
                        converted.setRGB(x, y, colorModel.getRGB(index));
                    }
                }
                return new DecodedTexture(Image.fromBufferedImage(converted), indices, colorModel);
            }
            var g2 = converted.createGraphics();
            try {
                g2.drawImage(raw, 0, 0, null);
            } finally {
                g2.dispose();
            }
            return new DecodedTexture(Image.fromBufferedImage(converted), null, null);
        } catch (IOException exception) {
            return new DecodedTexture(Image.createImage(bytes, 0, bytes.length), null, null);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int wrap(int value, int size) {
        if (size <= 0) {
            return 0;
        }
        int wrapped = value % size;
        return wrapped < 0 ? wrapped + size : wrapped;
    }

    private record DecodedTexture(Image image, int[] indexedPixels, IndexColorModel indexedColorModel) {
    }
}
