package com.j_phone.system;

public interface LocationUpdateListener {
    public static final int UPDATE_SUCCEEDED = 0;
    public static final int UPDATE_FAILED = 0;

    public void locationUpdated (int result);}
