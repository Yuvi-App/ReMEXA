package com.j_phone.phonedata;

public interface PhoneData {
    public static final int SORT_ASCENDING = 0;
    public static final int SORT_DESCENDING = 0;

    public void close ();
    public java.lang.String getListType ();
    public com.j_phone.phonedata.DataEnumeration elements (int position, int max, int sortType) throws java.io.IOException;
    public void createElement (com.j_phone.phonedata.DataElement element) throws java.io.IOException;
    public void delete (com.j_phone.phonedata.DataElement element) throws java.io.IOException;
    public void importElementRawData (byte[] data) throws java.io.IOException;
    public byte[] exportElementRawData (com.j_phone.phonedata.DataElement exportElement) throws java.io.IOException;
    public int getListMaxCount () throws java.io.IOException;}
