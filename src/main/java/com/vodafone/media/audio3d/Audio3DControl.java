package com.vodafone.media.audio3d;

import javax.microedition.media.Control;

public interface Audio3DControl extends Control {
    void setPosition(int x, int y, int z);

    void setVelocity(int x, int y, int z);

    void setRolloff(int minDistance, int maxDistance, int muteAfter);
}
