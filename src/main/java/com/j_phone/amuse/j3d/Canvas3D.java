package com.j_phone.amuse.j3d;

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
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "Canvas3D");
    }


    public void setAffineTrans (com.j_phone.amuse.j3d.AffineTrans t) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setAffineTrans", t);
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
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setTexture", texture);
        this.texture = texture;
    }

    public void drawFigure (com.j_phone.amuse.j3d.Figure figure) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "drawFigure", figure);
        var currentGraphics = remexa.host.runtime.MidletRuntime.currentGraphics();
        if (currentGraphics == null || texture == null || texture.image() == null) {
            return;
        }
        int width = scaled(texture.image().getWidth(), screenScaleX);
        int height = scaled(texture.image().getHeight(), screenScaleY);
        int centerX = screenCenterX == Integer.MIN_VALUE ? getWidth() / 2 : screenCenterX;
        int centerY = screenCenterY == Integer.MIN_VALUE ? getHeight() / 2 : screenCenterY;
        int drawX = centerX - width / 2;
        int drawY = centerY - height / 2;

        int oldClipX = currentGraphics.getClipX();
        int oldClipY = currentGraphics.getClipY();
        int oldClipWidth = currentGraphics.getClipWidth();
        int oldClipHeight = currentGraphics.getClipHeight();
        currentGraphics.clipRect(clipX, clipY, clipWidth == Integer.MAX_VALUE ? getWidth() : clipWidth, clipHeight == Integer.MAX_VALUE ? getHeight() : clipHeight);
        currentGraphics.drawImage(texture.image(), drawX, drawY, width, height);
        currentGraphics.setClip(oldClipX, oldClipY, oldClipWidth, oldClipHeight);
    }

    public void setClipRect (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setClipRect", x, y, width, height);
        this.clipX = x;
        this.clipY = y;
        this.clipWidth = width;
        this.clipHeight = height;
    }

    public final void sequenceStart () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "sequenceStart");
    }

    public final void sequenceStop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "sequenceStop");
    }

    private static int scaled(int size, int scale) {
        if (scale <= 0) {
            return size;
        }
        long scaled = Math.round((double) size * scale / 4096.0d);
        return (int) Math.max(1L, scaled);
    }
}
