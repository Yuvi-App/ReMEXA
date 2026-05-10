package com.mascotcapsule.micro3d.v3;

import java.io.IOException;

public class Figure extends com.jblend.graphics.j3d.Figure {
    private Texture texture;
    private Texture[] textures;

    public Figure(byte[] data) {
        super(data);
    }

    public Figure(String name) throws IOException {
        super(name);
    }

    public void dispose() {
        // Host-side resources are managed by the JVM.
    }

    public void setPosture(ActionTable actionTable, int action, int frame) {
        super.setPosture(actionTable, action, frame);
    }

    public Texture getTexture() {
        return texture;
    }

    @Override
    public void setTexture(com.jblend.graphics.j3d.Texture texture) {
        super.setTexture(texture);
        this.texture = texture instanceof Texture mascotTexture ? mascotTexture : null;
        this.textures = this.texture == null ? null : new Texture[] {this.texture};
    }

    public void setTexture(Texture texture) {
        setTexture((com.jblend.graphics.j3d.Texture) texture);
    }

    @Override
    public void setTexture(com.jblend.graphics.j3d.Texture[] textures) {
        super.setTexture(textures);
        if (textures == null) {
            this.textures = null;
            this.texture = null;
            return;
        }
        this.textures = new Texture[textures.length];
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] instanceof Texture mascotTexture) {
                this.textures[i] = mascotTexture;
            }
        }
        this.texture = this.textures.length == 0 ? null : this.textures[0];
    }

    public void setTexture(Texture[] textures) {
        setTexture((com.jblend.graphics.j3d.Texture[]) textures);
    }

    public void selectTexture(int index) {
        if (textures == null || index < 0 || index >= textures.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        texture = textures[index];
        super.setTexture(texture);
    }
}
