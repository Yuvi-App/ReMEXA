package com.j_phone.amuse.j3d;

public class Texture extends com.jblend.graphics.j3d.Texture {
    protected Texture() {
        super();
    }

    public Texture (byte[] data) {
        super(data, true);
    }

    public Texture (java.lang.String name) throws java.io.IOException {
        super(name, true);
    }

    javax.microedition.lcdui.Image image() {
        return getImage();
    }
}
