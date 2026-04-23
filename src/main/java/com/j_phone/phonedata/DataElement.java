package com.j_phone.phonedata;

public interface DataElement {
    public static final int STRING = 0;
    public static final int INT = 0;
    public static final int DATE = 0;
    public static final int BOOLEAN = 0;

    public java.lang.String getType ();
    public int getElementCount (int id) throws java.io.IOException;
    public int getDataType (int id);
    public java.lang.String getString (int id, int index) throws java.io.IOException;
    public java.lang.Integer getInt (int id, int index) throws java.io.IOException;
    public java.util.Date getDate (int id, int index) throws java.io.IOException;
    public java.lang.Boolean getBoolean (int id, int index) throws java.io.IOException;
    public void setString (int id, int index, java.lang.String value) throws java.io.IOException;
    public void setInt (int id, int index, java.lang.Integer value) throws java.io.IOException;
    public void setBoolean (int id, int index, java.lang.Boolean value) throws java.io.IOException;
    public boolean isListElement ();
    public com.j_phone.phonedata.DataElement createClone () throws java.io.IOException;}
