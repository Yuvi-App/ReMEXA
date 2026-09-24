package com.vodafone.media.audio3d;

public interface Audio3DControl extends ExtendedAudioControl {
    int MODE_DYNAMIC = 2;

    int[] getPosition();

    int[] getVelocity();

    int[] getRolloff();

    boolean isListenerRelative();

    void setListenerRelative(boolean relative);

    void setPosition(int x, int y, int z);

    void setVelocity(int x, int y, int z);

    void setRolloff(int minDistance, int maxDistance, int factor);
}
