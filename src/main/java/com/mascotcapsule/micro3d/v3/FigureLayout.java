package com.mascotcapsule.micro3d.v3;

public class FigureLayout extends com.jblend.graphics.j3d.FigureLayout {
    public FigureLayout() {
        super();
    }

    public FigureLayout(AffineTrans trans, int xScale, int yScale, int centerX, int centerY) {
        super(trans, xScale, yScale, centerX, centerY);
    }

    @Override
    public AffineTrans getAffineTrans() {
        com.jblend.graphics.j3d.AffineTrans trans = super.getAffineTrans();
        if (trans instanceof AffineTrans mascotTrans) {
            return mascotTrans;
        }
        return new AffineTrans(
                trans.m00, trans.m01, trans.m02, trans.m03,
                trans.m10, trans.m11, trans.m12, trans.m13,
                trans.m20, trans.m21, trans.m22, trans.m23
        );
    }

    public void setAffineTrans(AffineTrans trans) {
        super.setAffineTrans(trans);
    }

    public void setAffineTrans(AffineTrans[] trans) {
        super.setAffineTransArray(trans);
    }

    public void setAffineTransArray(AffineTrans[] trans) {
        setAffineTrans(trans);
    }
}
