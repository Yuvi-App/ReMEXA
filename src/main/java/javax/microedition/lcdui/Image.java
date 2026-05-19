package javax.microedition.lcdui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import remexa.host.jblend.CanvasGraphics3D;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public class Image {
    private final BufferedImage awtImage;

    private Image(BufferedImage awtImage) {
        this.awtImage = awtImage;
    }

    public static Image createImage(int width, int height) {
        return new Image(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    public static Image createImage(Image source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        return new Image(copyRegion(source.awtImage(), 0, 0, source.getWidth(), source.getHeight()));
    }

    public static Image createImage(Image source, int x, int y, int width, int height, int transform) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        validateRegion(source, x, y, width, height);
        validateTransform(transform);
        BufferedImage region = copyRegion(source.awtImage(), x, y, width, height);
        return new Image(transformRegion(region, transform));
    }

    public static Image fromBufferedImage(BufferedImage awtImage) {
        if (awtImage == null) {
            throw new NullPointerException("awtImage");
        }
        return new Image(awtImage);
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        try (var stream = new java.io.ByteArrayInputStream(imageData, imageOffset, imageLength)) {
            var decoded = ImageIO.read(stream);
            if (decoded != null) {
                return new Image(normalizeDecodedImage(decoded));
            }
        } catch (IOException exception) {
            DebugLog.log(LogCategory.UI, Image.class.getName(), "Failed to decode image bytes: " + exception.getMessage());
        }
        return createImage(1, 1);
    }

    public static Image createImage(InputStream stream) throws IOException {
        if (stream == null) {
            throw new NullPointerException("stream");
        }
        var decoded = ImageIO.read(stream);
        if (decoded == null) {
            throw new IOException("Unsupported image format");
        }
        return new Image(normalizeDecodedImage(decoded));
    }

    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        if (rgb == null) {
            throw new NullPointerException("rgb");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("non-positive image dimensions: " + width + "x" + height);
        }
        if (rgb.length < width * height) {
            throw new ArrayIndexOutOfBoundsException(
                    "rgb array too small: have " + rgb.length + ", need " + (width * height));
        }
        var imageType = processAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        var image = new BufferedImage(width, height, imageType);
        if (processAlpha) {
            image.setRGB(0, 0, width, height, rgb, 0, width);
        } else {
            // Force opaque alpha for non-alpha images so callers don't see ghosting
            // when the source array happens to carry stray alpha bits.
            int[] opaque = new int[width * height];
            for (int i = 0; i < opaque.length; i++) {
                opaque[i] = 0xFF000000 | rgb[i];
            }
            image.setRGB(0, 0, width, height, opaque, 0, width);
        }
        return new Image(image);
    }

    public static Image createImage(String name) throws IOException {
        try (InputStream stream = MidletRuntime.openResource(name)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + name);
            }
            var decoded = ImageIO.read(stream);
            if (decoded == null) {
                throw new IOException("Unsupported image format: " + name);
            }
            return new Image(normalizeDecodedImage(decoded));
        } catch (IOException exception) {
            DebugLog.log(LogCategory.UI, Image.class.getName(), "Failed to load image " + name + ": " + exception.getMessage());
            throw exception;
        }
    }

    public int getWidth() {
        return awtImage.getWidth();
    }

    public int getHeight() {
        return awtImage.getHeight();
    }

    public Graphics getGraphics() {
        return getGraphics(false);
    }

    public Graphics getGraphics(boolean retainDepthAcrossFlushes) {
        return new CanvasGraphics3D(awtImage.createGraphics(), getWidth(), getHeight(), true, awtImage, retainDepthAcrossFlushes);
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        awtImage.getRGB(x, y, width, height, rgbData, offset, scanlength);
    }

    BufferedImage awtImage() {
        return awtImage;
    }

    private static void validateRegion(Image source, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("non-positive image region: " + width + "x" + height);
        }
        if (x < 0 || y < 0 || x + width > source.getWidth() || y + height > source.getHeight()) {
            throw new IllegalArgumentException("image region outside source bounds");
        }
    }

    private static void validateTransform(int transform) {
        if (transform < 0 || transform > 7) {
            throw new IllegalArgumentException("Unknown transform: " + transform);
        }
    }

    private static BufferedImage copyRegion(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static BufferedImage normalizeDecodedImage(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] & 0xFF000000) == 0) {
                // Some legacy assets carry chroma-key RGB under transparent PNG pixels.
                // Games that read those pixels back and redraw with drawRGB(..., false)
                // expect the hidden color not to become visible.
                pixels[i] = 0x00000000;
            }
        }
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        normalized.setRGB(0, 0, width, height, pixels, 0, width);
        return normalized;
    }

    private static BufferedImage transformRegion(BufferedImage image, int transform) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int targetWidth = swapsAxes(transform) ? sourceHeight : sourceWidth;
        int targetHeight = swapsAxes(transform) ? sourceWidth : sourceHeight;
        BufferedImage transformed = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = transformed.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.transform(transformMatrix(transform, sourceWidth, sourceHeight));
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return transformed;
    }

    private static boolean swapsAxes(int transform) {
        return switch (transform) {
            case 4, 5, 6, 7 -> true;
            default -> false;
        };
    }

    private static AffineTransform transformMatrix(int transform, int width, int height) {
        return switch (transform) {
            case 1 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 5 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 6 -> new AffineTransform(0, -1, 1, 0, 0, width);
            case 7 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            default -> new AffineTransform();
        };
    }
}
