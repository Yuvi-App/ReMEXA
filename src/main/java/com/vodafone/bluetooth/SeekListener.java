package com.vodafone.bluetooth;

public interface SeekListener {
    int COMPLETED = 0;
    int ERROR = 1;
    int CANCELLED = 2;
    int SERVICE_NOT_FOUND = 3;
    int PUSH_SERVER_FOUND = 4;
    int MASK_SERVICE_CLASSES = 0xFF0000;
    int MASK_MAJOR_DEVICE_CLASS = 0x1F00;
    int MASK_MINOR_DEVICE_CLASS = 0x00FC;

    void foundDevice(Device dev, int codBits);

    void terminatedDeviceSeek(int reason);

    void foundService(RemoteService[] svc);

    void terminatedServiceSeek(Device device, int reason);
}
