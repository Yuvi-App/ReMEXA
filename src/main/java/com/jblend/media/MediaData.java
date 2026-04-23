package com.jblend.media;

public abstract class MediaData {
    public MediaData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData");
    }

    public MediaData (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData", name);
    }

    public MediaData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData", data);
    }


    public abstract java.lang.String getMediaType ();
    public abstract void setData (byte[] data);}
