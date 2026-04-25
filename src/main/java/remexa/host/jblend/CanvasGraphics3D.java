package remexa.host.jblend;

import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Figure;
import com.jblend.graphics.j3d.FigureLayout;
import com.jblend.graphics.j3d.Graphics3D;
import com.jblend.graphics.j3d.Texture;
import java.util.Arrays;
import javax.microedition.lcdui.Graphics;
import remexa.host.j3d.SoftwareJ3dRenderer;
import remexa.probes.SdkStubSupport;

public final class CanvasGraphics3D extends Graphics implements Graphics3D {
    private final int surfaceWidth;
    private final int surfaceHeight;
    private int[] scenePixels;
    private float[] sceneDepth;
    private boolean sceneDirty;

    public CanvasGraphics3D(java.awt.Graphics2D delegate, int surfaceWidth, int surfaceHeight, boolean disposable) {
        super(delegate, surfaceWidth, surfaceHeight, disposable);
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
    }

    @Override
    public void renderPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int command, int numPrimitives, int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors) {
        SdkStubSupport.log("com.jblend.graphics.j3d.Graphics3D", "renderPrimitives", texture, x, y, layout, effect, command, numPrimitives, vertexCoords, normals, textureCoords, colors);
    }

    @Override
    public void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] commandlist) {
        if (layout == null || effect == null || commandlist == null) {
            throw new NullPointerException();
        }
        // JSCL allows null slots in the texture array as long as the command
        // list does not select them via COMMAND_TEXTURE_INDEX. Trial builds
        // (e.g. Burning Fortress) routinely pass partially-populated arrays
        // because some textures are downloaded lazily.
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

    private void ensureSceneBuffers() {
        int size = surfaceWidth * surfaceHeight;
        if (scenePixels == null || scenePixels.length != size) {
            scenePixels = new int[size];
        }
        if (sceneDepth == null || sceneDepth.length != size) {
            sceneDepth = new float[size];
        }
        if (!sceneDirty) {
            Arrays.fill(scenePixels, 0);
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
