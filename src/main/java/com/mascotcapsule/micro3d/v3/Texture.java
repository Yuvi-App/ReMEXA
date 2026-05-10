package com.mascotcapsule.micro3d.v3;

import java.io.IOException;
import javax.microedition.lcdui.Image;

public class Texture extends com.jblend.graphics.j3d.Texture {
    public Texture(byte[] data, boolean isForModel) {
        super(data, isForModel);
    }

    public Texture(String name, boolean isForModel) throws IOException {
        super(name, isForModel);
    }

    public Texture(Image image, int x, int y, int width, int height, boolean isForModel) {
        super(image, x, y, width, height, isForModel);
    }

    public void dispose() {
        // Host-side resources are managed by the JVM.
    }
}
