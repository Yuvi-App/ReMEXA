package com.vodafone.media.audio3d;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface ExtendedAudioControl extends Control {
    int MODE_DISABLED = 0;
    int MODE_EXTENDED = 1;
    int MODE_DISABLE = MODE_DISABLED;
    int MODE_NORMAL = MODE_EXTENDED;
    int MODE_POSITIONAL = 2;

    int getMode();

    void setMode(int mode) throws MediaException;
}
