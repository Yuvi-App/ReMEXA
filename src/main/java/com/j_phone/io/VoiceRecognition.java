package com.j_phone.io;

public class VoiceRecognition {
    private static final VoiceRecognition INSTANCE = new VoiceRecognition();

    private VoiceRecognitionListener listener;

    public static com.j_phone.io.VoiceRecognition getInstance () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "getInstance");
        return INSTANCE;
    }

    public void recognize (com.j_phone.io.VoiceRecognitionDictionary dict, int voiceOffTimeOut, int voiceOnTimeOut, int maxCandidate) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "recognize", dict, voiceOffTimeOut, voiceOnTimeOut, maxCandidate);
    }

    public void recognize (java.lang.String dict, int voiceOffTimeOut, int voiceOnTimeOut, int maxCandidate) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "recognize", dict, voiceOffTimeOut, voiceOnTimeOut, maxCandidate);
    }

    public void recognize (java.lang.String language, com.j_phone.io.VoiceRecognitionDictionary dict, int voiceOffTimeOut, int voiceOnTimeOut, int maxCandidate) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "recognize", language, dict, voiceOffTimeOut, voiceOnTimeOut, maxCandidate);
    }

    public void recognize (java.lang.String language, java.lang.String dict, int voiceOffTimeOut, int voiceOnTimeOut, int maxCandidate) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "recognize", language, dict, voiceOffTimeOut, voiceOnTimeOut, maxCandidate);
    }

    public java.util.Enumeration getAvailableLanguages () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "getAvailableLanguages");
        return java.util.Collections.emptyEnumeration();
    }

    public void stop () {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "stop");
    }

    public int getCandidateWord (int order) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "getCandidateWord", order);
        return 0;
    }

    public int getCandidateScore (int order) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "getCandidateScore", order);
        return 0;
    }

    public void setVoiceRecognitionListener (com.j_phone.io.VoiceRecognitionListener listener) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.VoiceRecognition", "setVoiceRecognitionListener", listener);
        this.listener = listener;
    }
}
