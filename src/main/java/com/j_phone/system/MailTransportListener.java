package com.j_phone.system;

public interface MailTransportListener {
    public static final int MAIL_SUCCEEDED = 0;
    public static final int MAIL_FAILED = 0;
    public static final int MAIL_STOP = 0;
    public static final int MAIL_PART_FAILED = 0;
    public static final int MAIL_UNKNOWN = 0;

    public void mailSent (int result);
    public void messageReceived (int result);}
