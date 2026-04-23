package com.j_phone.io;

public interface VoiceRecognitionListener {
    public static final int ERROR_START_NG = 0;
    public static final int ERROR_ON_TIMEOUT = 0;
    public static final int ERROR_OFF_TIMEOUT = 0;
    public static final int ERROR_STOP = 0;
    public static final int ERROR_RECOGNIZE = 0;

    public void recognitionStarted ();
    public void recognized (int num);
    public void recognitionFailed (int reason);}
