package remexa.probes;

public enum LogCategory {
    HOST,
    FRONTEND,
    JAD,
    MIDLET,
    UI,
    IO,
    AUDIO,
    MEDIA,
    J3D,
    BLUETOOTH,
    RMS,
    PHONE_DATA,
    SYSTEM,
    SDK_MISC;

    public static LogCategory fromPackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return SDK_MISC;
        }
        if (packageName.startsWith("remexa.frontend")) {
            return FRONTEND;
        }
        if (packageName.startsWith("remexa.host")) {
            return HOST;
        }
        if (packageName.contains(".bluetooth")) {
            return BLUETOOTH;
        }
        if (packageName.contains(".rms")) {
            return RMS;
        }
        if (packageName.contains(".j3d") || packageName.contains(".opgl")) {
            return J3D;
        }
        if (packageName.contains(".media") || packageName.contains(".amuse")) {
            return MEDIA;
        }
        if (packageName.contains(".lcdui") || packageName.contains(".ui")) {
            return UI;
        }
        if (packageName.contains(".io")) {
            return IO;
        }
        if (packageName.contains(".midlet")) {
            return MIDLET;
        }
        if (packageName.contains(".phonedata")) {
            return PHONE_DATA;
        }
        if (packageName.contains(".system")) {
            return SYSTEM;
        }
        return SDK_MISC;
    }
}
