package com.j_phone.system;

public interface PhoneStateListener {
    public static final int UPDATE_RECEIVE_MAILBOX = 0;
    public static final int UPDATE_SEND_MAILBOX = 0;
    public static final int UPDATE_ADDRESSBOOK = 0;
    public static final int UPDATE_FILE = 0;

    public void phoneStateUpdated (int result);}
