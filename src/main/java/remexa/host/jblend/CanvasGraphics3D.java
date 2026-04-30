package remexa.host.jblend;

import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Figure;
import com.jblend.graphics.j3d.FigureLayout;
import com.jblend.graphics.j3d.Graphics3D;
import com.jblend.graphics.j3d.Texture;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import remexa.host.j3d.SoftwareJ3dRenderer;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.SdkStubSupport;

public final class CanvasGraphics3D extends Graphics implements Graphics3D {
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final BufferedImage backingImage;
    private int[] scenePixels;
    private float[] sceneDepth;
    private boolean sceneDirty;

    public CanvasGraphics3D(java.awt.Graphics2D delegate, int surfaceWidth, int surfaceHeight, boolean disposable) {
        this(delegate, surfaceWidth, surfaceHeight, disposable, null);
    }

    public CanvasGraphics3D(
            java.awt.Graphics2D delegate,
            int surfaceWidth,
            int surfaceHeight,
            boolean disposable,
            BufferedImage backingImage
    ) {
        super(delegate, surfaceWidth, surfaceHeight, disposable);
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.backingImage = backingImage;
    }

    @Override
    public void renderPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int command, int numPrimitives, int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors) {
        if (layout == null || effect == null || vertexCoords == null) {
            throw new NullPointerException();
        }
        DebugLog.log(
                LogCategory.J3D,
                CanvasGraphics3D.class.getName(),
                "renderPrimitives cmd=0x" + Integer.toHexString(command)
                        + " prims=" + numPrimitives
                        + " origin=" + x + "," + y
                        + " scale=" + layout.getScaleX() + "," + layout.getScaleY()
                        + " center=" + layout.getCenterX() + "," + layout.getCenterY()
                        + " explicitCenter=" + layout.hasExplicitCenter()
        );
        ensureSceneBuffers();
        if (SoftwareJ3dRenderer.renderPrimitivesToBuffers(
                scenePixels,
                sceneDepth,
                surfaceWidth,
                surfaceHeight,
                getClipX(),
                getClipY(),
                getClipWidth(),
                getClipHeight(),
                x,
                y,
                layout,
                effect,
                texture,
                command,
                numPrimitives,
                vertexCoords,
                normals,
                textureCoords,
                colors
        )) {
            sceneDirty = true;
        }
    }

    @Override
    public void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] commandlist) {
        if (layout == null || effect == null || commandlist == null) {
            throw new NullPointerException();
        }
        // JSCL allows null slots in the texture array as long as the command
        // list does not select them via COMMAND_TEXTURE_INDEX.
        ensureSceneBuffers();
        if (SoftwareJ3dRenderer.renderCommandListToBuffers(
                scenePixels,
                sceneDepth,
                surfaceWidth,
                surfaceHeight,
                getClipX(),
                getClipY(),
                getClipWidth(),
                getClipHeight(),
                x,
                y,
                layout,
                effect,
                textures,
                null,
                commandlist
        )) {
            sceneDirty = true;
        }
    }

    @Override
    public void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int[] commandlist) {
        if (texture == null) {
            drawCommandList((Texture[]) null, x, y, layout, effect, commandlist);
            return;
        }
        drawCommandList(new Texture[]{texture}, x, y, layout, effect, commandlist);
    }

    @Override
    public void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        drawFigure(figure, x, y, layout, effect);
    }

    @Override
    public void flush() {
        if (!sceneDirty || scenePixels == null) {
            return;
        }
        super.drawRGB(scenePixels, 0, surfaceWidth, 0, 0, surfaceWidth, surfaceHeight, true);
        clearSceneBuffers();
    }

    @Override
    public void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        if (figure == null || layout == null) {
            return;
        }
        var affine = layout.getAffineTrans();
        int centerX = x + (layout.hasExplicitCenter() ? layout.getCenterX() : surfaceWidth / 2);
        int centerY = y + (layout.hasExplicitCenter() ? layout.getCenterY() : surfaceHeight / 2);
        boolean perspective = layout.isPerspective();
        int nearClip = 0;
        int farClip = 0;
        float projectionScaleX;
        float projectionScaleY;
        if (perspective) {
            nearClip = layout.getPerspectiveNear();
            farClip = layout.getPerspectiveFar();
            if (layout.getPerspectiveWidth() > 0 && layout.getPerspectiveHeight() > 0) {
                projectionScaleX = nearClip > 0
                        ? (surfaceWidth * (float) nearClip) / layout.getPerspectiveWidth()
                        : 0.0f;
                projectionScaleY = nearClip > 0
                        ? (surfaceHeight * (float) nearClip) / layout.getPerspectiveHeight()
                        : 0.0f;
            } else {
                float angleRadians = (float) (layout.getPerspectiveAngle() * (Math.PI * 2.0 / 4096.0));
                // JSCL's angle-based perspective behaves like a horizontal field-of-view,
                // matching the explicit near-plane width overload in the Vodafone docs.
                float focal = angleRadians <= 0.0f || angleRadians >= Math.PI
                        ? surfaceWidth * 0.5f
                        : (float) ((surfaceWidth * 0.5f) / Math.tan(angleRadians * 0.5f));
                projectionScaleX = focal;
                projectionScaleY = focal;
            }
        } else if (layout.getParallelWidth() > 0 || layout.getParallelHeight() > 0) {
            // setParallelSize(W, H) declares the view rectangle in raw vertex
            // units; pixels-per-vertex-unit = surface / parallelSize. The earlier
            // *4096 multiplier rasterized figures 4096x oversized — the cause of
            // Burning Fortress's red-screen-fill on the title.
            projectionScaleX = layout.getParallelWidth() > 0
                    ? (float) surfaceWidth / layout.getParallelWidth()
                    : 512.0f / 4096.0f;
            projectionScaleY = layout.getParallelHeight() > 0
                    ? (float) surfaceHeight / layout.getParallelHeight()
                    : 512.0f / 4096.0f;
        } else {
            int scaleX = layout.getScaleX();
            int scaleY = layout.getScaleY();
            projectionScaleX = (scaleX == 0 ? 512 : scaleX) / 4096.0f;
            projectionScaleY = (scaleY == 0 ? 512 : scaleY) / 4096.0f;
        }
        ensureSceneBuffers();
        SoftwareJ3dRenderer.renderFigureToBuffers(
                scenePixels,
                sceneDepth,
                surfaceWidth,
                surfaceHeight,
                getClipX(),
                getClipY(),
                getClipWidth(),
                getClipHeight(),
                centerX,
                centerY,
                projectionScaleX,
                projectionScaleY,
                true,
                perspective,
                nearClip,
                farClip,
                affine,
                figure.mascotFigure(),
                null,
                effect
        );
        sceneDirty = true;
    }

    public BufferedImage backingImage() {
        return backingImage;
    }

    @Override
    public void resetState() {
        flush();
        super.resetState();
    }

    @Override
    public void dispose() {
        flush();
        super.dispose();
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        flush();
        super.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        flush();
        super.drawRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        flush();
        super.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        flush();
        super.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        flush();
        super.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        flush();
        super.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        flush();
        super.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        flush();
        super.fillOval(x, y, width, height);
    }

    @Override
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        flush();
        super.fillTriangle(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        flush();
        super.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawString(String string, int x, int y, int anchor) {
        flush();
        super.drawString(string, x, y, anchor);
    }

    @Override
    public void drawChar(char character, int x, int y, int anchor) {
        flush();
        super.drawChar(character, x, y, anchor);
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        flush();
        super.drawChars(data, offset, length, x, y, anchor);
    }

    @Override
    public void drawSubstring(String string, int offset, int len, int x, int y, int anchor) {
        flush();
        super.drawSubstring(string, offset, len, x, y, anchor);
    }

    @Override
    public void drawImage(Image image, int x, int y, int anchor) {
        flush();
        super.drawImage(image, x, y, anchor);
    }

    @Override
    public void drawImage(Image image, int x, int y, int width, int height) {
        flush();
        super.drawImage(image, x, y, width, height);
    }

    @Override
    public void drawRegion(Image image, int xSrc, int ySrc, int width, int height, int transform, int xDest, int yDest, int anchor) {
        flush();
        super.drawRegion(image, xSrc, ySrc, width, height, transform, xDest, yDest, anchor);
    }

    @Override
    public void drawRegion(Image image, int xSrc, int ySrc, int width, int height, int transform, int xDest, int yDest, int widthDest, int heightDest, int anchor) {
        flush();
        super.drawRegion(image, xSrc, ySrc, width, height, transform, xDest, yDest, widthDest, heightDest, anchor);
    }

    @Override
    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        flush();
        super.drawRGB(rgbData, offset, scanlength, x, y, width, height, processAlpha);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        flush();
        super.setClip(x, y, width, height);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        flush();
        super.clipRect(x, y, width, height);
    }

    @Override
    public void setFont(Font font) {
        flush();
        super.setFont(font);
    }

    @Override
    public void translate(int x, int y) {
        flush();
        super.translate(x, y);
    }

    private void ensureSceneBuffers() {
        int size = surfaceWidth * surfaceHeight;
        if (scenePixels == null || scenePixels.length != size) {
            scenePixels = new int[size];
        }
        if (sceneDepth == null || sceneDepth.length != size) {
            sceneDepth = new float[size];
        }
        if (!sceneDirty) {
            if (backingImage != null) {
                backingImage.getRGB(0, 0, surfaceWidth, surfaceHeight, scenePixels, 0, surfaceWidth);
            } else {
                Arrays.fill(scenePixels, 0);
            }
            Arrays.fill(sceneDepth, Float.NEGATIVE_INFINITY);
        }
    }

    private void clearSceneBuffers() {
        sceneDirty = false;
        if (scenePixels != null) {
            Arrays.fill(scenePixels, 0);
        }
        if (sceneDepth != null) {
            Arrays.fill(sceneDepth, Float.NEGATIVE_INFINITY);
        }
    }
}
