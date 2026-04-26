package com.jblend.media;

public abstract class MediaData {
    private byte[] data;

    public MediaData () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData");
    }

    public MediaData (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData", name);
        if (name == null) {
            throw new NullPointerException("MediaData: resource name is null");
        }
        this.data = loadResource(name);
    }

    public MediaData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "MediaData", data);
        this.data = data == null ? null : data.clone();
    }

    public abstract java.lang.String getMediaType ();

    public void setData (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaData", "setData", data);
        this.data = data == null ? null : data.clone();
    }

    protected final byte[] rawData() {
        return data;
    }

    private static byte[] loadResource(String name) throws java.io.IOException {
        var loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = MediaData.class.getClassLoader();
        }
        var path = name.startsWith("/") ? name.substring(1) : name;
        try (var stream = loader == null
                ? ClassLoader.getSystemResourceAsStream(path)
                : loader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new java.io.IOException("MediaData: resource not found: " + name);
            }
            return stream.readAllBytes();
        }
    }
}
