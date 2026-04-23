package com.j_phone.system;

public interface MailListener {
    public static final int SKYMAIL = 0;
    public static final int RELAY = 0;
    public static final int GREETING = 0;
    public static final int LONGMAIL = 0;
    public static final int WEB = 0;
    public static final int CBS_DEFINE = 0;
    public static final int CBS_PL = 0;

    public void received (java.lang.String name, java.lang.String address, int detail);}
