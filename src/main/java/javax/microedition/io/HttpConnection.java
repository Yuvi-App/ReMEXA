package javax.microedition.io;

import java.io.IOException;

public interface HttpConnection extends ContentConnection {
    String GET = "GET";
    String POST = "POST";
    String HEAD = "HEAD";

    long getDate() throws IOException;

    String getEncoding();

    long getExpiration() throws IOException;

    String getFile();

    String getHeaderField(int n) throws IOException;

    String getHeaderField(String name) throws IOException;

    long getHeaderFieldDate(String name, long def) throws IOException;

    int getHeaderFieldInt(String name, int def) throws IOException;

    String getHeaderFieldKey(int n) throws IOException;

    String getHost();

    long getLastModified() throws IOException;

    int getPort();

    String getProtocol();

    String getQuery();

    String getRef();

    String getRequestMethod();

    String getRequestProperty(String key);

    int getResponseCode() throws IOException;

    String getResponseMessage() throws IOException;

    String getURL();

    void setRequestMethod(String method) throws IOException;

    void setRequestProperty(String key, String value) throws IOException;
}
