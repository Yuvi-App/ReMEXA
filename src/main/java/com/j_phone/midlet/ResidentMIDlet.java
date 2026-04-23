package com.j_phone.midlet;

public abstract class ResidentMIDlet extends javax.microedition.midlet.MIDlet implements com.j_phone.system.TelephonyListener, com.j_phone.system.MailListener, com.j_phone.system.ScheduledAlarmListener, com.j_phone.system.RingStateListener {
    protected ResidentMIDlet () {
        remexa.probes.SdkStubSupport.log("com.j_phone.midlet.ResidentMIDlet", "ResidentMIDlet");
    }


    public abstract void ring (java.lang.String name, java.lang.String number);
    public abstract void ignored ();
    public abstract void received (java.lang.String name, java.lang.String address, int detail);
    public abstract void notice (java.lang.String comment);
    public abstract void ringStarted ();
    public abstract void ringStopped ();}
