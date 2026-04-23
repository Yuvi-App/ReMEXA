package com.j_phone.amuse.j3d;

public class Texture extends com.jblend.graphics.j3d.Texture {
    private final javax.microedition.lcdui.Image image;

    protected Texture() {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Texture", "Texture");
        this.image = null;
    }

    public Texture (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Texture", "Texture", data);
        this.image = javax.microedition.lcdui.Image.createImage(data, 0, data == null ? 0 : data.length);
    }

    public Texture (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.amuse.j3d.Texture", "Texture", name);
        this.image = javax.microedition.lcdui.Image.createImage(name);
    }

    javax.microedition.lcdui.Image image() {
        return image;
    }
}
