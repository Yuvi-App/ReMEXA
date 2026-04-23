package com.j_phone.media;

public interface MediaPlayerListener {
    public static final int PLAYED = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;

    public void mediaStateChanged (int state);}
