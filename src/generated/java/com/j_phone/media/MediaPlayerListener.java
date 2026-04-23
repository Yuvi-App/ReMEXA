package com.j_phone.media;

public interface MediaPlayerListener {
    public static final int PLAYED = 0;
    public static final int STOPPED = 0;
    public static final int PAUSED = 0;

    public void mediaStateChanged (int state);}
