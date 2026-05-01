package com.j_phone.io;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public final class FileUtility {
    public static final int WRITABLE = 0;
    public static final int EXISTS = 1;
    public static final int INSUFFICIENT = 2;
    public static final int COUNT_LIMIT = 3;
    public static final int FILETYPE_DIFFERENT = 4;
    public static final int WRITE_PROTECT = 5;
    public static final int OTHER_ERROR = 6;

    private static final FileUtility INSTANCE = new FileUtility();

    private FileUtility() {
    }

    public static com.j_phone.io.FileUtility getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "getInstance");
        return INSTANCE;
    }

    public void play (java.lang.String path) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", path);
    }

    public void play (com.j_phone.phonedata.MailData mailData, int attachedFileIndex) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", mailData, attachedFileIndex);
    }

    public void play (byte[] data, int type) throws java.io.IOException {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "play", data, type);
        if (data == null || data.length == 0) {
            throw new IOException("FileUtility.play: media buffer is empty.");
        }

        var hostFrame = remexa.host.runtime.MidletRuntime.currentHostFrame();
        if (hostFrame == null) {
            throw new IOException("FileUtility.play: no host frame is available for in-window playback.");
        }

        int payloadLength = trimmedLength(data);
        if (payloadLength <= 0) {
            throw new IOException("FileUtility.play: media buffer only contains padding.");
        }

        var mediaPath = Files.createTempFile(
                "remexa-" + sanitizeTitle(remexa.host.runtime.MidletRuntime.currentAppTitle()) + "-",
                extensionForType(type)
        );
        try {
            Files.write(mediaPath, Arrays.copyOf(data, payloadLength));

            remexa.probes.DebugLog.log(
                    remexa.probes.LogCategory.MEDIA,
                    FileUtility.class.getName(),
                    "Playing hosted media " + mediaPath.getFileName() + " (" + payloadLength + " bytes, type=" + type + ")"
            );
            hostFrame.playHostedVideoBlocking(mediaPath);
        } finally {
            try {
                Files.deleteIfExists(mediaPath);
            } catch (IOException exception) {
                remexa.probes.DebugLog.log(
                        remexa.probes.LogCategory.MEDIA,
                        FileUtility.class.getName(),
                        "Unable to remove hosted media temp file " + mediaPath.getFileName() + ": " + exception.getMessage()
                );
            }
        }
    }

    private static int trimmedLength(byte[] data) {
        int length = data.length;
        while (length > 0 && data[length - 1] == 0) {
            length--;
        }
        return length;
    }

    private static String extensionForType(int type) {
        return switch (type) {
            case 16 -> ".3gp";
            default -> ".bin";
        };
    }

    private static String sanitizeTitle(String title) {
        var source = title == null || title.isBlank() ? "media" : title;
        return source.replaceAll("[^A-Za-z0-9._-]", "_");
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
        long bytes = StoragePathSupport.getFreeSpace(rootpath);
        return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
    }

    public int precheckStorable (java.lang.String path, int size) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.FileUtility", "precheckStorable", path, size);
        try {
            var target = StoragePathSupport.resolve(path);
            var storagePath = target.realPath();
            if (java.nio.file.Files.exists(storagePath) && !java.nio.file.Files.isDirectory(storagePath)) {
                return FILETYPE_DIFFERENT;
            }

            long freeSpace = StoragePathSupport.getFreeSpace(path);
            if (size > 0 && freeSpace < size) {
                return INSUFFICIENT;
            }
            return WRITABLE;
        } catch (IOException exception) {
            return OTHER_ERROR;
        }
    }
}
