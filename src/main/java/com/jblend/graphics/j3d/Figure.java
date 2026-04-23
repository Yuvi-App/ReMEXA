package com.jblend.graphics.j3d;

import java.io.IOException;
import java.io.InputStream;
import remexa.host.j3d.MascotFigure;
import remexa.host.j3d.MascotLoader;
import remexa.host.j3d.MbacModel;
import remexa.host.runtime.MidletRuntime;

public class Figure {
    private final MbacModel model;
    private final MascotFigure mascotFigure;

    protected Figure() {
        this.model = null;
        this.mascotFigure = new MascotFigure(null);
    }

    public Figure (byte[] data) {
        if (data == null) {
            throw new NullPointerException();
        }
        try {
            this.model = MascotLoader.loadFigure(data);
            this.mascotFigure = new MascotFigure(this.model);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load figure", exception);
        }
    }

    public Figure (java.lang.String name) throws java.io.IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        String resourceName = name.endsWith(".mbac") ? name : name + ".mbac";
        try (InputStream stream = MidletRuntime.openResource(resourceName)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            this.model = MascotLoader.loadFigure(stream);
            this.mascotFigure = new MascotFigure(this.model);
        }
    }


    public void setPosture (com.jblend.graphics.j3d.ActionTable actTable, int action, int frame) {
        if (actTable == null) {
            throw new NullPointerException();
        }
        mascotFigure.setAction(actTable.data(), action);
        mascotFigure.setTime(frame);
    }

    public void setTexture (com.jblend.graphics.j3d.Texture texture) {
        mascotFigure.setTexture(texture);
    }

    public void setTexture (com.jblend.graphics.j3d.Texture[] textures) {
        mascotFigure.setTextures(textures);
    }

    public int getNumTextures () {
        return mascotFigure.numTextures();
    }

    public int getNumPattern () {
        return mascotFigure.numPatterns();
    }

    public void setPattern (int pattern) {
        mascotFigure.setPattern(pattern);
    }

    public MbacModel model() {
        return model;
    }

    public MascotFigure mascotFigure() {
        return mascotFigure;
    }
}
