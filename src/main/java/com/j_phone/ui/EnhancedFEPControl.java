package com.j_phone.ui;

import remexa.host.input.HostTextInputRequest;
import remexa.host.runtime.MidletRuntime;

public final class EnhancedFEPControl {
    private static final com.j_phone.ui.EnhancedFEPControl DEFAULT = new com.j_phone.ui.EnhancedFEPControl();

    private EnhancedFEPControl() {
    }

    public static final com.j_phone.ui.EnhancedFEPControl getDefaultEnhancedFEPControl () {
        remexa.probes.SdkStubSupport.log("com.j_phone.ui.EnhancedFEPControl", "getDefaultEnhancedFEPControl");
        return DEFAULT;
    }

    public java.lang.String getInputText (java.lang.String text, int constraints, int maxSize, boolean isWrapAllowed) {
        remexa.probes.SdkStubSupport.log("com.j_phone.ui.EnhancedFEPControl", "getInputText", text, constraints, maxSize, isWrapAllowed);
        return MidletRuntime.requestTextInput(new HostTextInputRequest(
                "Input",
                text,
                constraints,
                maxSize,
                isWrapAllowed
        ));
    }
}
