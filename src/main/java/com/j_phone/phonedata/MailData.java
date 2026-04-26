package com.j_phone.phonedata;

public interface MailData extends com.j_phone.phonedata.DataElement {
    public static final int FROM_ADDRESS_INFO = 1;
    public static final int TO_ADDRESS_INFO = 2;
    public static final int CC_ADDRESS_INFO = 3;
    public static final int BCC_ADDRESS_INFO = 4;
    public static final int REPLYTO_ADDRESS_INFO = 5;
    public static final int FROM_NAME_INFO = 6;
    public static final int TO_NAME_INFO = 7;
    public static final int CC_NAME_INFO = 8;
    public static final int BCC_NAME_INFO = 9;
    public static final int SUBJECT_INFO = 10;
    public static final int BODY_INFO = 11;
    public static final int DATE_INFO = 12;
    public static final int MAIL_TYPE_INFO = 13;
    public static final int MAIL_TYPE_SUPER = 0;
    public static final int MAIL_TYPE_SKY = 1;
    public static final int MAIL_TYPE_GREETING = 2;
    public static final int PRIORITY_NOMAL = 0;
    public static final int PRIORITY_URGENT = 1;
    public static final int PRIORITY_LOW = 2;
    public static final int CONFIRM_OFF = 0;
    public static final int CONFIRM_ON = 1;
    public static final int UNREAD = 0;
    public static final int ADREAD = 1;
    public static final int SEND_STATE_MIDST = 0;
    public static final int SEND_STATE_SUCCESS = 1;
    public static final int SEND_STATE_CANCEL = 2;
    public static final int SEND_STATE_FAIL = 3;
    public static final int SEND_STATE_NO_MESSAGE = 4;

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
    public void setPriority (int priority) throws java.io.IOException;
}
