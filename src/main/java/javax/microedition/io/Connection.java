package javax.microedition.io;

import java.io.IOException;

public interface Connection extends AutoCloseable {
    @Override
    void close() throws IOException;
}
