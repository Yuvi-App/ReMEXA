package remexa.host.jblend;

import com.jblend.graphics.j3d.AffineTrans;
import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Figure;
import com.jblend.graphics.j3d.FigureLayout;
import com.jblend.graphics.j3d.Graphics3D;
import com.jblend.graphics.j3d.Light;
import com.jblend.graphics.j3d.Texture;
import com.jblend.graphics.j3d.Vector3D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.microedition.lcdui.Graphics;
import remexa.host.j3d.MascotFigure;
import remexa.host.j3d.SoftwareJ3dRenderer;
import remexa.host.j3d.SoftwareJ3dRenderer.RenderPass;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class CanvasGraphics3D extends Graphics implements Graphics3D {
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final BufferedImage backingImage;
    private final boolean retainDepthAcrossFlushes;
    private final List<QueuedDraw> pendingDraws = new ArrayList<>();
    private int[] scenePixels;
    private float[] sceneDepth;
    private boolean sceneDepthValid;

    public CanvasGraphics3D(java.awt.Graphics2D delegate, int surfaceWidth, int surfaceHeight, boolean disposable) {
        this(delegate, surfaceWidth, surfaceHeight, disposable, null, true);
    }

    public CanvasGraphics3D(
            java.awt.Graphics2D delegate,
            int surfaceWidth,
            int surfaceHeight,
            boolean disposable,
            BufferedImage backingImage
    ) {
        this(delegate, surfaceWidth, surfaceHeight, disposable, backingImage, true);
    }

    public CanvasGraphics3D(
            java.awt.Graphics2D delegate,
            int surfaceWidth,
            int surfaceHeight,
            boolean disposable,
            BufferedImage backingImage,
            boolean retainDepthAcrossFlushes
    ) {
        super(delegate, surfaceWidth, surfaceHeight, disposable);
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.backingImage = backingImage;
        this.retainDepthAcrossFlushes = retainDepthAcrossFlushes;
    }

    @Override
    public void renderPrimitives(
            Texture texture,
            int x,
            int y,
            FigureLayout layout,
            Effect3D effect,
            int command,
            int numPrimitives,
            int[] vertexCoords,
            int[] normals,
            int[] textureCoords,
            int[] colors
    ) {
        if (layout == null || effect == null || vertexCoords == null) {
            throw new NullPointerException();
        }
        int translateX = getTranslateX();
        int translateY = getTranslateY();
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
        pendingDraws.add(new PrimitivesDraw(
                texture,
                x + translateX,
                y + translateY,
                snapshotLayout(layout),
                snapshotEffect(effect),
                getClipX() + translateX,
                getClipY() + translateY,
                getClipWidth(),
                getClipHeight(),
                command,
                numPrimitives,
                vertexCoords.clone(),
                normals == null ? null : normals.clone(),
                textureCoords == null ? null : textureCoords.clone(),
                colors == null ? null : colors.clone()
        ));
    }

    @Override
    public void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] commandlist) {
        if (layout == null || effect == null || commandlist == null) {
            throw new NullPointerException();
        }
        int translateX = getTranslateX();
        int translateY = getTranslateY();
        pendingDraws.add(new CommandListDraw(
                textures == null ? null : textures.clone(),
                x + translateX,
                y + translateY,
                snapshotLayout(layout),
                snapshotEffect(effect),
                getClipX() + translateX,
                getClipY() + translateY,
                getClipWidth(),
                getClipHeight(),
                commandlist.clone()
        ));
        if (containsCommandFlush(commandlist)) {
            flush();
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
        enqueueFigure(figure, x, y, layout, effect);
    }

    @Override
    public void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        enqueueFigure(figure, x, y, layout, effect);
        flush();
    }

    @Override
    public void flush() {
        if (pendingDraws == null || pendingDraws.isEmpty()) {
            return;
        }
        prepareSceneBuffers();
        // Match Mascot/JBlend's render order: opaque geometry fills depth before additive sprites.
        for (QueuedDraw draw : pendingDraws) {
            draw.rasterize(scenePixels, sceneDepth, RenderPass.OPAQUE);
        }
        for (QueuedDraw draw : pendingDraws) {
            draw.rasterize(scenePixels, sceneDepth, RenderPass.TRANSLUCENT);
        }
        sceneDepthValid = true;

        int clipX = getClipX();
        int clipY = getClipY();
        int clipWidth = getClipWidth();
        int clipHeight = getClipHeight();
        int translateX = getTranslateX();
        int translateY = getTranslateY();
        try {
            super.setClip(-translateX, -translateY, surfaceWidth, surfaceHeight);
            super.drawRGB(
                    scenePixels,
                    0,
                    surfaceWidth,
                    -translateX,
                    -translateY,
                    surfaceWidth,
                    surfaceHeight,
                    true
            );
        } finally {
            super.setClip(clipX, clipY, clipWidth, clipHeight);
            clearPendingScene();
            if (!retainDepthAcrossFlushes) {
                clearDepthBuffer();
            }
        }
    }

    public BufferedImage backingImage() {
        return backingImage;
    }

    @Override
    public void resetState() {
        flush();
        clearDepthBuffer();
        super.resetState();
    }

    @Override
    public void dispose() {
        flush();
        clearDepthBuffer();
        super.dispose();
    }

    @Override
    public void clearSurface(int argbColor) {
        flush();
        clearDepthBuffer();
        super.clearSurface(argbColor);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        boolean changed = x != getClipX()
                || y != getClipY()
                || width != getClipWidth()
                || height != getClipHeight();
        flush();
        if (changed) {
            clearDepthBuffer();
        }
        super.setClip(x, y, width, height);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        int oldX = getClipX();
        int oldY = getClipY();
        int oldWidth = getClipWidth();
        int oldHeight = getClipHeight();
        flush();
        super.clipRect(x, y, width, height);
        if (oldX != getClipX()
                || oldY != getClipY()
                || oldWidth != getClipWidth()
                || oldHeight != getClipHeight()) {
            clearDepthBuffer();
        }
    }

    private void enqueueFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        if (figure == null || layout == null) {
            return;
        }
        int translateX = getTranslateX();
        int translateY = getTranslateY();
        pendingDraws.add(new FigureDraw(
                figure.mascotFigure().snapshot(),
                x + translateX,
                y + translateY,
                snapshotLayout(layout),
                snapshotEffect(effect),
                getClipX() + translateX,
                getClipY() + translateY,
                getClipWidth(),
                getClipHeight()
        ));
    }

    private void prepareSceneBuffers() {
        int size = surfaceWidth * surfaceHeight;
        if (scenePixels == null || scenePixels.length != size) {
            scenePixels = new int[size];
        }
        if (sceneDepth == null || sceneDepth.length != size) {
            sceneDepth = new float[size];
            sceneDepthValid = false;
        }
        if (backingImage != null) {
            backingImage.getRGB(0, 0, surfaceWidth, surfaceHeight, scenePixels, 0, surfaceWidth);
        } else {
            Arrays.fill(scenePixels, 0);
        }
        if (!sceneDepthValid) {
            Arrays.fill(sceneDepth, Float.NEGATIVE_INFINITY);
        }
    }

    private void clearPendingScene() {
        pendingDraws.clear();
    }

    private void clearDepthBuffer() {
        sceneDepthValid = false;
    }

    private static FigureLayout snapshotLayout(FigureLayout layout) {
        FigureLayout copy = new FigureLayout();
        copy.setAffineTrans(copyAffine(layout.getAffineTrans()));
        AffineTrans[] affineArray = layout.getAffineTransArray();
        if (affineArray.length > 0) {
            AffineTrans[] affineArrayCopy = new AffineTrans[affineArray.length];
            for (int i = 0; i < affineArray.length; i++) {
                affineArrayCopy[i] = copyAffine(affineArray[i]);
            }
            copy.setAffineTransArray(affineArrayCopy);
            int selectedIndex = layout.getSelectedAffineIndex();
            if (selectedIndex >= 0 && selectedIndex < affineArrayCopy.length) {
                copy.selectAffineTrans(selectedIndex);
            }
        }
        if (layout.isPerspective()) {
            if (layout.getPerspectiveWidth() > 0 && layout.getPerspectiveHeight() > 0) {
                copy.setPerspective(
                        layout.getPerspectiveNear(),
                        layout.getPerspectiveFar(),
                        layout.getPerspectiveWidth(),
                        layout.getPerspectiveHeight()
                );
            } else {
                copy.setPerspective(
                        layout.getPerspectiveNear(),
                        layout.getPerspectiveFar(),
                        layout.getPerspectiveAngle()
                );
            }
        } else if (layout.getParallelWidth() > 0 || layout.getParallelHeight() > 0) {
            copy.setParallelSize(layout.getParallelWidth(), layout.getParallelHeight());
        } else {
            copy.setScale(layout.getScaleX(), layout.getScaleY());
        }
        if (layout.hasExplicitCenter()) {
            copy.setCenter(layout.getCenterX(), layout.getCenterY());
        }
        return copy;
    }

    private static Effect3D snapshotEffect(Effect3D effect) {
        if (effect == null) {
            return null;
        }
        Effect3D copy = new Effect3D();
        copy.setLight(copyLight(effect.getLight()));
        copy.setShading(effect.getShading());
        copy.setSemiTransparentEnabled(effect.isSemiTransparentEnabled());
        copy.setSphereMap(effect.getSphereMap());
        copy.setThreshold(effect.getThreshold(), effect.getThresholdHigh(), effect.getThresholdLow());
        return copy;
    }

    private static boolean containsCommandFlush(int[] commandList) {
        for (int command : commandList) {
            if (command == COMMAND_FLUSH) {
                return true;
            }
        }
        return false;
    }

    private static Light copyLight(Light light) {
        if (light == null) {
            return null;
        }
        return new Light(copyVector(light.getDirection()), light.getDirIntensity(), light.getAmbIntensity());
    }

    private static Vector3D copyVector(Vector3D vector) {
        if (vector == null) {
            return null;
        }
        return new Vector3D(vector.x, vector.y, vector.z);
    }

    private static AffineTrans copyAffine(AffineTrans affine) {
        if (affine == null) {
            return new AffineTrans();
        }
        AffineTrans copy = new AffineTrans();
        copy.m00 = affine.m00;
        copy.m01 = affine.m01;
        copy.m02 = affine.m02;
        copy.m03 = affine.m03;
        copy.m10 = affine.m10;
        copy.m11 = affine.m11;
        copy.m12 = affine.m12;
        copy.m13 = affine.m13;
        copy.m20 = affine.m20;
        copy.m21 = affine.m21;
        copy.m22 = affine.m22;
        copy.m23 = affine.m23;
        return copy;
    }

    private interface QueuedDraw {
        void rasterize(int[] pixels, float[] depthBuffer, RenderPass pass);
    }

    private final class FigureDraw implements QueuedDraw {
        private final MascotFigure figure;
        private final int x;
        private final int y;
        private final FigureLayout layout;
        private final Effect3D effect;
        private final int clipX;
        private final int clipY;
        private final int clipWidth;
        private final int clipHeight;

        private FigureDraw(MascotFigure figure, int x, int y, FigureLayout layout, Effect3D effect, int clipX, int clipY, int clipWidth, int clipHeight) {
            this.figure = figure;
            this.x = x;
            this.y = y;
            this.layout = layout;
            this.effect = effect;
            this.clipX = clipX;
            this.clipY = clipY;
            this.clipWidth = clipWidth;
            this.clipHeight = clipHeight;
        }

        @Override
        public void rasterize(int[] pixels, float[] depthBuffer, RenderPass pass) {
            AffineTrans affine = layout.getAffineTrans();
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
                    float focal = angleRadians <= 0.0f || angleRadians >= Math.PI
                            ? surfaceWidth * 0.5f
                            : (float) ((surfaceWidth * 0.5f) / Math.tan(angleRadians * 0.5f));
                    projectionScaleX = focal;
                    projectionScaleY = focal;
                }
            } else if (layout.getParallelWidth() > 0 || layout.getParallelHeight() > 0) {
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
            SoftwareJ3dRenderer.renderFigureToBuffers(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    centerX,
                    centerY,
                    projectionScaleX,
                    projectionScaleY,
                    true,
                    perspective,
                    nearClip,
                    farClip,
                    affine,
                    figure,
                    null,
                    effect,
                    pass
            );
        }
    }

    private final class PrimitivesDraw implements QueuedDraw {
        private final Texture texture;
        private final int x;
        private final int y;
        private final FigureLayout layout;
        private final Effect3D effect;
        private final int clipX;
        private final int clipY;
        private final int clipWidth;
        private final int clipHeight;
        private final int command;
        private final int numPrimitives;
        private final int[] vertexCoords;
        private final int[] normals;
        private final int[] textureCoords;
        private final int[] colors;

        private PrimitivesDraw(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int clipX, int clipY, int clipWidth, int clipHeight, int command, int numPrimitives, int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.layout = layout;
            this.effect = effect;
            this.clipX = clipX;
            this.clipY = clipY;
            this.clipWidth = clipWidth;
            this.clipHeight = clipHeight;
            this.command = command;
            this.numPrimitives = numPrimitives;
            this.vertexCoords = vertexCoords;
            this.normals = normals;
            this.textureCoords = textureCoords;
            this.colors = colors;
        }

        @Override
        public void rasterize(int[] pixels, float[] depthBuffer, RenderPass pass) {
            SoftwareJ3dRenderer.renderPrimitivesToBuffers(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
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
                    colors,
                    pass
            );
        }
    }

    private final class CommandListDraw implements QueuedDraw {
        private final Texture[] textures;
        private final int x;
        private final int y;
        private final FigureLayout layout;
        private final Effect3D effect;
        private final int clipX;
        private final int clipY;
        private final int clipWidth;
        private final int clipHeight;
        private final int[] commandList;

        private CommandListDraw(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int clipX, int clipY, int clipWidth, int clipHeight, int[] commandList) {
            this.textures = textures;
            this.x = x;
            this.y = y;
            this.layout = layout;
            this.effect = effect;
            this.clipX = clipX;
            this.clipY = clipY;
            this.clipWidth = clipWidth;
            this.clipHeight = clipHeight;
            this.commandList = commandList;
        }

        @Override
        public void rasterize(int[] pixels, float[] depthBuffer, RenderPass pass) {
            SoftwareJ3dRenderer.renderCommandListToBuffers(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    x,
                    y,
                    layout,
                    effect,
                    textures,
                    null,
                    commandList,
                    pass
            );
        }
    }
}
