package com.mexa.bluetooth;

public interface SeekListener {
    public static final int COMPLETED = 0;
    public static final int ERROR = 1;
    public static final int CANCELLED = 2;
    public static final int SERVICE_NOT_FOUND = 3;
    public static final int PUSH_SERVER_FOUND = 4;
    public static final int MASK_SERVICE_CLASSES = 0xFF0000;
    public static final int MASK_MAJOR_DEVICE_CLASS = 0x1F00;
    public static final int MASK_MINOR_DEVICE_CLASS = 0x00FC;

    public void foundDevice (com.mexa.bluetooth.Device dev, int codBits);
    public void terminatedDeviceSeek (int reason);
    public void foundService (com.mexa.bluetooth.RemoteService[] svc);
    public void terminatedServiceSeek (com.mexa.bluetooth.Device device, int reason);}
