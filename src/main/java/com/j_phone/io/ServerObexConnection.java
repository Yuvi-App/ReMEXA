package com.j_phone.io;

public interface ServerObexConnection extends com.j_phone.io.ObexConnection {
    public void accept () throws java.io.IOException;
    public void receiveRequest () throws java.io.IOException;
    public int getOperation ();
    public void sendResponse (int code) throws java.io.IOException;}
