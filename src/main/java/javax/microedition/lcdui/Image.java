package javax.microedition.lcdui;

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
                return new Image(decoded);
            }
        } catch (IOException exception) {
            DebugLog.log(LogCategory.UI, Image.class.getName(), "Failed to decode image bytes: " + exception.getMessage());
        }
        return createImage(1, 1);
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
            return new Image(decoded);
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
        return new CanvasGraphics3D(awtImage.createGraphics(), getWidth(), getHeight(), true, awtImage);
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        awtImage.getRGB(x, y, width, height, rgbData, offset, scanlength);
    }

    BufferedImage awtImage() {
        return awtImage;
    }
}
