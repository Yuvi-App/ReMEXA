package com.jblend.media.karaoke;

public interface KaraokePlayerListener {
    public void playerStateChanged (com.jblend.media.karaoke.KaraokePlayer player, long time);
    public void eventOccurred (com.jblend.media.karaoke.KaraokePlayer player, int event);}
