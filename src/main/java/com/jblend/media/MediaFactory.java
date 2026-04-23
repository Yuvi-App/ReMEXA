package com.jblend.media;

public class MediaFactory {
    public static final int MEDIA_TYPE_SMAF = 0;
    public static final int MEDIA_TYPE_KARAOKE = 0;

    public static com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", name);
        return null;
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", data);
        return null;
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String name, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", name, type);
        return null;
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (byte[] data, int type) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", data, type);
        return null;
    }
}
