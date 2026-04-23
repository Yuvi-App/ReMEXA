package com.j_phone.io;

public interface RandomAccess {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 0;
    public static final int SEEK_END = 0;

    public long getPosition () throws java.io.IOException;
    public long setPosition (int from, long position) throws java.io.IOException;}
