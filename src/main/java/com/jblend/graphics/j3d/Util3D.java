package com.jblend.graphics.j3d;

import remexa.host.j3d.FixedPoint;

public class Util3D {
    public static final int sqrt (int x) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Util3D", "sqrt", x);
        return FixedPoint.sqrt(x);
    }

    public static final int sin (int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Util3D", "sin", a);
        return FixedPoint.sin(a);
    }

    public static final int cos (int a) {
        remexa.probes.SdkStubSupport.log("com.jblend.graphics.j3d.Util3D", "cos", a);
        return FixedPoint.cos(a);
    }
}
