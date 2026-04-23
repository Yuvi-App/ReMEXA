package com.j_phone.amuse.j3d;

public abstract class Canvas3D extends javax.microedition.lcdui.Canvas implements com.jblend.ui.SequenceInterface {
    public Canvas3D () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "Canvas3D");
    }


    public void setAffineTrans (com.j_phone.amuse.j3d.AffineTrans t) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setAffineTrans", t);
    }

    public void setScreenScale (int x_scale, int y_scale) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setScreenScale", x_scale, y_scale);
    }

    public void setScreenCenter (int cx, int cy) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setScreenCenter", cx, cy);
    }

    public void setTexture (com.j_phone.amuse.j3d.Texture texture) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setTexture", texture);
    }

    public void drawFigure (com.j_phone.amuse.j3d.Figure figure) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "drawFigure", figure);
    }

    public void setClipRect (int x, int y, int width, int height) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "setClipRect", x, y, width, height);
    }

    public final void sequenceStart () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "sequenceStart");
    }

    public final void sequenceStop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Canvas3D", "sequenceStop");
    }
}
