package com.jblend.graphics.j3d;

public class FigureLayout {
    private AffineTrans affineTrans = new AffineTrans();
    private AffineTrans[] affineTransArray = new AffineTrans[0];
    private int selectedAffineIndex = -1;
    private int scaleX = 512;
    private int scaleY = 512;
    private int parallelWidth;
    private int parallelHeight;
    private int centerX;
    private int centerY;
    private boolean perspective;
    private int perspectiveNear;
    private int perspectiveFar;
    private int perspectiveAngle;
    private int perspectiveWidth;
    private int perspectiveHeight;

    public FigureLayout () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "FigureLayout");
    }

    public FigureLayout (com.jblend.graphics.j3d.AffineTrans trans, int x_scale, int y_scale, int cx, int cy) {
        this.affineTrans = trans == null ? new AffineTrans() : trans;
        this.scaleX = x_scale;
        this.scaleY = y_scale;
        this.centerX = cx;
        this.centerY = cy;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "FigureLayout", trans, x_scale, y_scale, cx, cy);
    }


    public com.jblend.graphics.j3d.AffineTrans getAffineTrans () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getAffineTrans");
        if (selectedAffineIndex >= 0 && selectedAffineIndex < affineTransArray.length) {
            return affineTransArray[selectedAffineIndex];
        }
        return affineTrans;
    }

    public void setAffineTrans (com.jblend.graphics.j3d.AffineTrans at) {
        this.affineTrans = at == null ? new AffineTrans() : at;
        this.selectedAffineIndex = -1;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setAffineTrans", at);
    }

    public void setAffineTransArray (com.jblend.graphics.j3d.AffineTrans[] at) {
        this.affineTransArray = at == null ? new AffineTrans[0] : at.clone();
        this.selectedAffineIndex = this.affineTransArray.length == 0 ? -1 : Math.min(Math.max(selectedAffineIndex, 0), this.affineTransArray.length - 1);
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setAffineTransArray", at);
    }

    public void selectAffineTrans (int index) {
        if (index < 0 || index >= affineTransArray.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        this.selectedAffineIndex = index;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "selectAffineTrans", index);
    }

    public int getScaleX () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getScaleX");
        return scaleX;
    }

    public int getScaleY () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getScaleY");
        return scaleY;
    }

    public void setScale (int x_scale, int y_scale) {
        this.scaleX = x_scale;
        this.scaleY = y_scale;
        this.parallelWidth = 0;
        this.parallelHeight = 0;
        this.perspective = false;
        this.perspectiveNear = 0;
        this.perspectiveFar = 0;
        this.perspectiveAngle = 0;
        this.perspectiveWidth = 0;
        this.perspectiveHeight = 0;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setScale", x_scale, y_scale);
    }

    public int getParallelWidth () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getParallelWidth");
        return parallelWidth;
    }

    public int getParallelHeight () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getParallelHeight");
        return parallelHeight;
    }

    public void setParallelSize (int width, int height) {
        this.parallelWidth = width;
        this.parallelHeight = height;
        this.perspective = false;
        this.perspectiveNear = 0;
        this.perspectiveFar = 0;
        this.perspectiveAngle = 0;
        this.perspectiveWidth = 0;
        this.perspectiveHeight = 0;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setParallelSize", width, height);
    }

    public int getCenterX () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getCenterX");
        return centerX;
    }

    public int getCenterY () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "getCenterY");
        return centerY;
    }

    public void setCenter (int cx, int cy) {
        this.centerX = cx;
        this.centerY = cy;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setCenter", cx, cy);
    }

    public void setPerspective (int zNear, int zFar, int angle) {
        this.perspective = true;
        this.parallelWidth = 0;
        this.parallelHeight = 0;
        this.perspectiveNear = zNear;
        this.perspectiveFar = zFar;
        this.perspectiveAngle = angle;
        this.perspectiveWidth = 0;
        this.perspectiveHeight = 0;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setPerspective", zNear, zFar, angle);
    }

    public void setPerspective (int zNear, int zFar, int width, int height) {
        this.perspective = true;
        this.parallelWidth = 0;
        this.parallelHeight = 0;
        this.perspectiveNear = zNear;
        this.perspectiveFar = zFar;
        this.perspectiveWidth = width;
        this.perspectiveHeight = height;
        this.perspectiveAngle = 0;
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.FigureLayout", "setPerspective", zNear, zFar, width, height);
    }

    public boolean isPerspective() {
        return perspective;
    }

    public int getPerspectiveNear() {
        return perspectiveNear;
    }

    public int getPerspectiveFar() {
        return perspectiveFar;
    }

    public int getPerspectiveAngle() {
        return perspectiveAngle;
    }

    public int getPerspectiveWidth() {
        return perspectiveWidth;
    }

    public int getPerspectiveHeight() {
        return perspectiveHeight;
    }
}
