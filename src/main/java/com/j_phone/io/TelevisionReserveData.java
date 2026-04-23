package com.j_phone.io;

public interface TelevisionReserveData {
    public static final int TYPE_REC = 0;
    public static final int TYPE_WATCH = 0;

    public int getReserveNumber ();
    public int getReserveType ();
    public int getReserveCategory ();
    public int getReserveChannel ();
    public java.lang.String getReserveStartTime ();
    public java.lang.String getReserveEndTime ();
    public java.lang.String getReserveTitle ();
    public java.lang.String getReserveStation ();
    public void setReserveType (int type);
    public void setReserveCategory (int category);
    public void setReserveChannel (int channel);
    public void setReserveStartTime (java.lang.String time);
    public void setReserveEndTime (java.lang.String time);
    public void setReserveTitle (java.lang.String title);
    public void setReserveStation (java.lang.String station);}
