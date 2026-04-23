package com.j_phone.phonedata;

public interface MailData extends com.j_phone.phonedata.DataElement {
    public static final int FROM_ADDRESS_INFO = 0;
    public static final int TO_ADDRESS_INFO = 0;
    public static final int CC_ADDRESS_INFO = 0;
    public static final int BCC_ADDRESS_INFO = 0;
    public static final int REPLYTO_ADDRESS_INFO = 0;
    public static final int FROM_NAME_INFO = 0;
    public static final int TO_NAME_INFO = 0;
    public static final int CC_NAME_INFO = 0;
    public static final int BCC_NAME_INFO = 0;
    public static final int SUBJECT_INFO = 0;
    public static final int BODY_INFO = 0;
    public static final int DATE_INFO = 0;
    public static final int MAIL_TYPE_INFO = 0;
    public static final int MAIL_TYPE_SUPER = 0;
    public static final int MAIL_TYPE_SKY = 0;
    public static final int MAIL_TYPE_GREETING = 0;
    public static final int PRIORITY_NOMAL = 0;
    public static final int PRIORITY_URGENT = 0;
    public static final int PRIORITY_LOW = 0;
    public static final int CONFIRM_OFF = 0;
    public static final int CONFIRM_ON = 0;
    public static final int UNREAD = 0;
    public static final int ADREAD = 0;
    public static final int SEND_STATE_MIDST = 0;
    public static final int SEND_STATE_SUCCESS = 0;
    public static final int SEND_STATE_CANCEL = 0;
    public static final int SEND_STATE_FAIL = 0;
    public static final int SEND_STATE_NO_MESSAGE = 0;

    public boolean isUnRead () throws java.io.IOException;
    public boolean hasRemainder () throws java.io.IOException;
    public int hasSendState () throws java.io.IOException;
    public int getAttachedFileCount () throws java.io.IOException;
    public java.lang.String getAttachedFileName (int index) throws java.io.IOException;
    public byte[] getAttachedFileData (int index) throws java.io.IOException;
    public void setState (int state) throws java.io.IOException;
    public int setAttachedFile (java.lang.String pathname) throws java.io.IOException;
    public int setAttachedData (byte[] data, java.lang.String attachedFileName, int fileType) throws java.io.IOException;
    public void removeAttachedFile (int index) throws java.io.IOException;
    public void setConfirm (int confirm) throws java.io.IOException;
    public void setPriority (int priority) throws java.io.IOException;}
