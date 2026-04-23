package remexa.host.jblend;

import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Figure;
import com.jblend.graphics.j3d.FigureLayout;
import com.jblend.graphics.j3d.Graphics3D;
import com.jblend.graphics.j3d.Texture;
import javax.microedition.lcdui.Graphics;
import remexa.host.j3d.SoftwareJ3dRenderer;
import remexa.probes.SdkStubSupport;

public final class CanvasGraphics3D extends Graphics implements Graphics3D {
    private final int surfaceWidth;
    private final int surfaceHeight;

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
        SdkStubSupport.log("com.jblend.graphics.j3d.Graphics3D", "drawCommandList", textures, x, y, layout, effect, commandlist);
    }

    @Override
    public void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int[] commandlist) {
        SdkStubSupport.log("com.jblend.graphics.j3d.Graphics3D", "drawCommandList", texture, x, y, layout, effect, commandlist);
    }

    @Override
    public void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        drawFigure(figure, x, y, layout, effect);
    }

    @Override
    public void flush() {
        // Software-backed canvas graphics draw immediately.
    }

    @Override
    public void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        if (figure == null || layout == null) {
            return;
        }
        var affine = layout.getAffineTrans();
        int centerX = x + layout.getCenterX();
        int centerY = y + layout.getCenterY();
        int scaleX = layout.getScaleX();
        int scaleY = layout.getScaleY();
        if (scaleX == 0) {
            scaleX = 4096;
        }
        if (scaleY == 0) {
            scaleY = 4096;
        }
        SoftwareJ3dRenderer.drawFigure(
                this,
                surfaceWidth,
                surfaceHeight,
                getClipX(),
                getClipY(),
                getClipWidth(),
                getClipHeight(),
                centerX,
                centerY,
                scaleX,
                scaleY,
                affine,
                figure.mascotFigure(),
                null,
                effect
        );
    }
}
