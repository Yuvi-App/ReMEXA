package com.j_phone.media;

public interface ResourceOperator {
    int getResourceType();

    int getResourceCount();

    int getResourceID(int index);

    String getResourceName(int resourceId);

    String[] getResourceNames();

    void setResourceByID(MediaPlayer player, int resourceId);

    void setResourceByTitle(MediaPlayer player, String title);

    void setResource(MediaPlayer player, int index);

    int getIndexOfResource(int resourceId);
}
