package com.j_phone.io;

public final class FileUtility {
    public static final int WRITABLE = 0;
    public static final int EXISTS = 0;
    public static final int INSUFFICIENT = 0;
    public static final int COUNT_LIMIT = 0;
    public static final int FILETYPE_DIFFERENT = 0;
    public static final int WRITE_PROTECT = 0;
    public static final int OTHER_ERROR = 0;

    public static com.j_phone.io.FileUtility getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getInstance");
        return null;
    }

    public void play (java.lang.String path) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", path);
    }

    public void play (com.j_phone.phonedata.MailData mailData, int attachedFileIndex) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", mailData, attachedFileIndex);
    }

    public void play (byte[] data, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", data, type);
    }

    public com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String path) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getMediaPlayer", path);
        return null;
    }

    public com.jblend.media.MediaPlayer getMediaPlayer (java.lang.String path, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getMediaPlayer", path, type);
        return null;
    }

    public com.jblend.media.MediaData getMediaData (java.lang.String path) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getMediaData", path);
        return null;
    }

    public com.jblend.media.MediaData getMediaData (java.lang.String path, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getMediaData", path, type);
        return null;
    }

    public int getFreeSpace (java.lang.String rootpath) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getFreeSpace", rootpath);
        return 0;
    }

    public int precheckStorable (java.lang.String path, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "precheckStorable", path, size);
        return 0;
    }
}
