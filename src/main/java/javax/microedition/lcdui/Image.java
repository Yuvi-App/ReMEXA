package javax.microedition.lcdui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
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

    public static Image createImage(String name) {
        try (InputStream stream = MidletRuntime.openResource(name)) {
            if (stream == null) {
                DebugLog.log(LogCategory.UI, Image.class.getName(), "Resource not found: " + name);
                return createImage(1, 1);
            }
            var decoded = ImageIO.read(stream);
            if (decoded == null) {
                DebugLog.log(LogCategory.UI, Image.class.getName(), "Unsupported image format: " + name);
                return createImage(1, 1);
            }
            return new Image(decoded);
        } catch (IOException exception) {
            DebugLog.log(LogCategory.UI, Image.class.getName(), "Failed to load image " + name + ": " + exception.getMessage());
            return createImage(1, 1);
        }
    }

    public int getWidth() {
        return awtImage.getWidth();
    }

    public int getHeight() {
        return awtImage.getHeight();
    }

    BufferedImage awtImage() {
        return awtImage;
    }
}
