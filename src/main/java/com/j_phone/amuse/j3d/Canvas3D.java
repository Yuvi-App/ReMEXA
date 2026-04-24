package com.j_phone.amuse.j3d;

import remexa.host.j3d.SoftwareJ3dRenderer;

public abstract class Canvas3D extends javax.microedition.lcdui.Canvas implements com.jblend.ui.SequenceInterface {
    private AffineTrans affineTrans;
    private int screenScaleX = 4096;
    private int screenScaleY = 4096;
    private int screenCenterX = Integer.MIN_VALUE;
    private int screenCenterY = Integer.MIN_VALUE;
    private Texture texture;
    private int clipX;
    private int clipY;
    private int clipWidth = Integer.MAX_VALUE;
    private int clipHeight = Integer.MAX_VALUE;

    public Canvas3D () {
    }


    public void setAffineTrans (com.j_phone.amuse.j3d.AffineTrans t) {
        if (t == null) {
            throw new NullPointerException();
        }
        this.affineTrans = t;
    }

    public void setScreenScale (int x_scale, int y_scale) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setScreenScale", x_scale, y_scale);
        this.screenScaleX = x_scale;
        this.screenScaleY = y_scale;
    }

    public void setScreenCenter (int cx, int cy) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setScreenCenter", cx, cy);
        this.screenCenterX = cx;
        this.screenCenterY = cy;
    }

    public void setTexture (com.j_phone.amuse.j3d.Texture texture) {
        if (texture == null) {
            throw new NullPointerException();
        }
        this.texture = texture;
    }

    public void drawFigure (com.j_phone.amuse.j3d.Figure figure) {
        if (figure == null) {
            throw new NullPointerException();
        }
        if (texture == null && figure.getNumTextures() == 0) {
            throw new NullPointerException();
        }
        var currentGraphics = remexa.host.runtime.MidletRuntime.currentGraphics();
        if (currentGraphics == null) {
            return;
        }
        int centerX = screenCenterX == Integer.MIN_VALUE ? getWidth() / 2 : screenCenterX;
        int centerY = screenCenterY == Integer.MIN_VALUE ? getHeight() / 2 : screenCenterY;
        int actualClipWidth = clipWidth == Integer.MAX_VALUE ? getWidth() : clipWidth;
        int actualClipHeight = clipHeight == Integer.MAX_VALUE ? getHeight() : clipHeight;
        SoftwareJ3dRenderer.drawFigure(
                currentGraphics,
                getWidth(),
                getHeight(),
                clipX,
                clipY,
                actualClipWidth,
                actualClipHeight,
                centerX,
                centerY,
                screenScaleX / 4096.0f,
                screenScaleY / 4096.0f,
                false,
                0,
                0,
                affineTrans,
                figure.mascotFigure(),
                texture,
                null
        );
    }

    public void setClipRect (int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0 || x + width > getWidth() || y + height > getHeight()) {
            throw new IllegalArgumentException();
        }
        this.clipX = x;
        this.clipY = y;
        this.clipWidth = width;
        this.clipHeight = height;
    }

    public final void sequenceStart () {
    }

    public final void sequenceStop () {
    }
}
