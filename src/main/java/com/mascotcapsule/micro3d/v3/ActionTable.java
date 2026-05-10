package com.mascotcapsule.micro3d.v3;

import java.io.IOException;

public class ActionTable extends com.jblend.graphics.j3d.ActionTable {
    public ActionTable(byte[] data) {
        super(data);
    }

    public ActionTable(String name) throws IOException {
        super(name);
    }

    public void dispose() {
        // Host-side resources are managed by the JVM.
    }

    public int getNumActions() {
        return getNumAction();
    }

    public int getNumFrames(int action) {
        return getNumFrame(action);
    }
}
