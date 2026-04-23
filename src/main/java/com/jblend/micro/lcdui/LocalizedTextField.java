package com.jblend.micro.lcdui;

public class LocalizedTextField extends javax.microedition.lcdui.TextField {
    public static final int INTERNET = 0;
    public static final int HANKAKU = 0;

    protected LocalizedTextField() {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.lcdui.LocalizedTextField", "LocalizedTextField");
    }

    public LocalizedTextField (java.lang.String label, java.lang.String text, int maxSize, int constraints) {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.lcdui.LocalizedTextField", "LocalizedTextField", label, text, maxSize, constraints);
    }


    public void setCharConstraints (int charConstraints) {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.lcdui.LocalizedTextField", "setCharConstraints", charConstraints);
    }

    public void setInputMode (int mode) {
        remexa.probes.SdkStubSupport.log("com.jblend.micro.lcdui.LocalizedTextField", "setInputMode", mode);
    }
}
