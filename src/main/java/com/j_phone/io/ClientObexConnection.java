package com.j_phone.io;

public interface ClientObexConnection extends com.j_phone.io.ObexConnection {
    public void connect () throws java.io.IOException;
    public void connect (int mode) throws java.io.IOException;
    public void setOperation (int operation);
    public void sendRequest () throws java.io.IOException;
    public int getResponseCode ();}
