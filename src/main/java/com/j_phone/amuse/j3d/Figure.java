package com.j_phone.amuse.j3d;

public class Figure extends com.jblend.graphics.j3d.Figure {
    protected Figure() {
        super();
    }

    public Figure (byte[] data) {
        super(data);
    }

    public Figure (java.lang.String name) throws java.io.IOException {
        super(name);
    }


    public void setPosture (com.j_phone.amuse.j3d.ActionTable actTable, int action, int frame) {
        super.setPosture(actTable, action, frame);
    }
}
