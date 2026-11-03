package javax.microedition.lcdui.game;

import java.util.ArrayList;
import java.util.Objects;
import javax.microedition.lcdui.Graphics;

public class LayerManager {
    private final java.util.List<Layer> layers = new ArrayList<>();
    private int viewX;
    private int viewY;
    private int viewWidth = Integer.MAX_VALUE;
    private int viewHeight = Integer.MAX_VALUE;

    public void append(Layer layer) {
        remove(layer);
        layers.add(layer);
    }

    public void insert(Layer layer, int index) {
        Objects.requireNonNull(layer, "layer");
        var previous = indexOf(layer);
        var size = layers.size() - (previous >= 0 ? 1 : 0);
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Layer index out of range: " + index);
        }
        if (previous >= 0) {
            layers.remove(previous);
        }
        layers.add(index, layer);
    }

    public Layer getLayerAt(int index) {
        return layers.get(index);
    }

    public int getSize() {
        return layers.size();
    }

    public void remove(Layer layer) {
        Objects.requireNonNull(layer, "layer");
        var index = indexOf(layer);
        if (index >= 0) {
            layers.remove(index);
        }
    }

    public void setViewWindow(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("View window size must be non-negative.");
        }
        viewX = x;
        viewY = y;
        viewWidth = width;
        viewHeight = height;
    }

    public void paint(Graphics graphics, int x, int y) {
        Objects.requireNonNull(graphics, "graphics");
        var translateX = graphics.getTranslateX();
        var translateY = graphics.getTranslateY();
        var clipX = graphics.getClipX();
        var clipY = graphics.getClipY();
        var clipWidth = graphics.getClipWidth();
        var clipHeight = graphics.getClipHeight();
        try {
            graphics.clipRect(x, y, viewWidth, viewHeight);
            graphics.translate(x - viewX, y - viewY);
            for (var index = layers.size() - 1; index >= 0; index--) {
                var layer = layers.get(index);
                if (layer.isVisible() && intersectsView(layer)) {
                    layer.paint(graphics);
                }
            }
        } finally {
            graphics.translate(translateX - graphics.getTranslateX(), translateY - graphics.getTranslateY());
            graphics.setClip(clipX, clipY, clipWidth, clipHeight);
        }
    }

    private boolean intersectsView(Layer layer) {
        return viewWidth > 0 && viewHeight > 0 && layer.getWidth() > 0 && layer.getHeight() > 0
                && (long) layer.getX() + layer.getWidth() > viewX
                && (long) layer.getY() + layer.getHeight() > viewY
                && layer.getX() < (long) viewX + viewWidth
                && layer.getY() < (long) viewY + viewHeight;
    }

    private int indexOf(Layer layer) {
        for (var index = 0; index < layers.size(); index++) {
            if (layers.get(index) == layer) {
                return index;
            }
        }
        return -1;
    }
}
