package com.vodafone.media.audio3d;

public interface ExtendedAudioControl extends Audio3DControl {
    int MODE_DISABLE = 0;
    int MODE_NORMAL = 1;
    int MODE_POSITIONAL = 2;

    int getMode();

    void setMode(int mode);
}
