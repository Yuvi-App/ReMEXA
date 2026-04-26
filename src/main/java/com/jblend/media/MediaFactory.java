package com.jblend.media;

public class MediaFactory {
    public static final int MEDIA_TYPE_SMAF = 1;
    public static final int MEDIA_TYPE_KARAOKE = 11;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] MNG_MAGIC = {(byte) 0x8A, 'M', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] SMAF_MAGIC = {'M', 'M', 'M', 'D'};

    public static com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String name) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", name);
        return getMediaPlayer(loadResource(name));
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (byte[] data) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", data);
        if (data == null) {
            throw new NullPointerException("MediaFactory.getMediaPlayer: data is null");
        }
        if (startsWith(data, JPEG_MAGIC)) {
            return new com.jblend.media.jpeg.JpegPlayer(data);
        }
        if (startsWith(data, PNG_MAGIC)) {
            return new com.jblend.media.png.PngPlayer(data);
        }
        if (startsWith(data, MNG_MAGIC)) {
            return new com.jblend.media.mng.MngPlayer(data);
        }
        if (startsWith(data, SMAF_MAGIC)) {
            return new com.jblend.media.smaf.SmafPlayer(data);
        }
        throw new IllegalArgumentException("MediaFactory.getMediaPlayer: unrecognised media format");
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String name, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", name, type);
        return getMediaPlayer(loadResource(name), type);
    }

    public static com.jblend.media.MediaPlayer getMediaPlayer (byte[] data, int type) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.MediaFactory", "getMediaPlayer", data, type);
        if (data == null) {
            throw new NullPointerException("MediaFactory.getMediaPlayer: data is null");
        }
        if (startsWith(data, SMAF_MAGIC)) {
            // For SMAF data the type argument selects the player flavour. Per spec,
            // any unknown type falls back to SmafPlayer.
            return type == MEDIA_TYPE_KARAOKE
                    ? new com.jblend.media.karaoke.KaraokePlayer(data)
                    : new com.jblend.media.smaf.SmafPlayer(data);
        }
        return getMediaPlayer(data);
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] loadResource(String name) throws java.io.IOException {
        if (name == null) {
            throw new NullPointerException("MediaFactory.getMediaPlayer: resource name is null");
        }
        var loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = MediaFactory.class.getClassLoader();
        }
        var path = name.startsWith("/") ? name.substring(1) : name;
        try (var stream = loader == null
                ? ClassLoader.getSystemResourceAsStream(path)
                : loader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new java.io.IOException("MediaFactory.getMediaPlayer: resource not found: " + name);
            }
            return stream.readAllBytes();
        }
    }
}
