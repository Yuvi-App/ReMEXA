package com.j_phone.io;

public interface OpticalDeviceConnection extends javax.microedition.io.Connection {
    public boolean isSupported (int chkType) throws java.lang.IllegalArgumentException;
    public void capture () throws java.io.IOException;}
