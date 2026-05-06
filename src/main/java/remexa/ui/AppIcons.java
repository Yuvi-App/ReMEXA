package remexa.ui;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public final class AppIcons {
    private static final String APP_ICON_RESOURCE = "/remexa/images/app_icon.png";
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};
    private static volatile List<Image> cachedIcons;

    private AppIcons() {
    }

    public static void applyTo(JFrame frame) {
        if (frame == null) {
            return;
        }
        var icons = icons();
        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        }
    }

    public static void applyToTaskbar() {
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            var icons = icons();
            if (icons.isEmpty()) {
                return;
            }
            Taskbar.getTaskbar().setIconImage(icons.getLast());
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
    }

    private static List<Image> icons() {
        var icons = cachedIcons;
        if (icons == null) {
            synchronized (AppIcons.class) {
                icons = cachedIcons;
                if (icons == null) {
                    icons = loadIcons();
                    cachedIcons = icons;
                }
            }
        }
        return icons;
    }

    private static List<Image> loadIcons() {
        try (var input = AppIcons.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (input == null) {
                return List.of();
            }
            var source = ImageIO.read(input);
            if (source == null) {
                return List.of();
            }

            var icons = new ArrayList<Image>(ICON_SIZES.length + 1);
            for (var size : ICON_SIZES) {
                icons.add(scaledIcon(source, size));
            }
            icons.add(source);
            return List.copyOf(icons);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static BufferedImage scaledIcon(BufferedImage source, int size) {
        var icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }
        return icon;
    }
}
