package com.j_phone.ui;

import remexa.host.input.HostTextInputRequest;
import remexa.host.runtime.MidletRuntime;

public final class FEPControl {
    private static final com.j_phone.ui.FEPControl DEFAULT = new com.j_phone.ui.FEPControl();

    private FEPControl() {
    }

    public static final com.j_phone.ui.FEPControl getDefaultFEPControl () {
        remexa.probes.SdkStubSupport.log("com.j_phone.ui.FEPControl", "getDefaultFEPControl");
        return DEFAULT;
    }

    public java.lang.String getInputText (java.lang.String text, int constraints, int maxSize, boolean isWrapAllowed) {
        remexa.probes.SdkStubSupport.log("com.j_phone.ui.FEPControl", "getInputText", text, constraints, maxSize, isWrapAllowed);
        return MidletRuntime.requestTextInput(new HostTextInputRequest(
                "Input",
                text,
                constraints,
                maxSize,
                isWrapAllowed
        ));
    }
}
