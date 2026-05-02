package com.jblend.media;

import remexa.host.runtime.MidletRuntime;

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
        try (var stream = MidletRuntime.openResource(normalizeResourceName(name))) {
            if (stream == null) {
                throw new java.io.IOException("MediaData: resource not found: " + name);
            }
            return stream.readAllBytes();
        }
    }

    private static String normalizeResourceName(String name) {
        if (name == null) {
            return "";
        }
        var normalized = name;
        if (normalized.regionMatches(true, 0, "resource://", 0, "resource://".length())) {
            normalized = normalized.substring("resource://".length());
        } else if (normalized.regionMatches(true, 0, "resource:/", 0, "resource:/".length())) {
            normalized = normalized.substring("resource:/".length());
        } else if (normalized.regionMatches(true, 0, "resource:", 0, "resource:".length())) {
            normalized = normalized.substring("resource:".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
