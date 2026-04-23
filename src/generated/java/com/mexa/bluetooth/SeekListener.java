package com.mexa.bluetooth;

public interface SeekListener {
    public static final int COMPLETED = 0;
    public static final int ERROR = 0;
    public static final int CANCELLED = 0;
    public static final int SERVICE_NOT_FOUND = 0;
    public static final int PUSH_SERVER_FOUND = 0;
    public static final int MASK_SERVICE_CLASSES = 0;
    public static final int MASK_MAJOR_DEVICE_CLASS = 0;
    public static final int MASK_MINOR_DEVICE_CLASS = 0;

    public void foundDevice (com.mexa.bluetooth.Device dev, int codBits);
    public void terminatedDeviceSeek (int reason);
    public void foundService (com.mexa.bluetooth.RemoteService[] svc);
    public void terminatedServiceSeek (com.mexa.bluetooth.Device device, int reason);}
