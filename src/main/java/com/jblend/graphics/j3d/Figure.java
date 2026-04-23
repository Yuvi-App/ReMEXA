package com.jblend.graphics.j3d;

public class Figure {
    protected Figure() {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "Figure");
    }

    public Figure (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "Figure", data);
    }

    public Figure (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "Figure", name);
    }


    public void setPosture (com.jblend.graphics.j3d.ActionTable actTable, int action, int frame) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "setPosture", actTable, action, frame);
    }

    public void setTexture (com.jblend.graphics.j3d.Texture texture) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "setTexture", texture);
    }

    public void setTexture (com.jblend.graphics.j3d.Texture[] textures) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "setTexture", textures);
    }

    public int getNumTextures () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "getNumTextures");
        return 0;
    }

    public int getNumPattern () {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "getNumPattern");
        return 0;
    }

    public void setPattern (int pattern) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Figure", "setPattern", pattern);
    }
}
