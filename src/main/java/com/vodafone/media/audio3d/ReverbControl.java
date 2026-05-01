package com.vodafone.media.audio3d;

import javax.microedition.media.Control;

public interface ReverbControl extends Control {
    int getLevel();

    int setLevel(int level);
}
