package com.j_phone.phonedata;

public interface AddressBook extends com.j_phone.phonedata.PhoneData {
    public static final int GROUP_SEARCH = 1;
    public static final int KANA_SEARCH = 2;
    public static final int NUMBER_SEARCH = 3;
    public static final int MAIL_ADDRESS_SEARCH = 4;

    public int[] getGroupNoList () throws java.io.IOException;
    public java.lang.String getGroupName (int groupNo) throws java.io.IOException;
    public int getPhoneNumberMaxCount () throws java.io.IOException;
    public int getMailAddressMaxCount () throws java.io.IOException;
    public com.j_phone.phonedata.DataEnumeration elements (int type, java.lang.String searchString, int from, int max) throws java.io.IOException;}
