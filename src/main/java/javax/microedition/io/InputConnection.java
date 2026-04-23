package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;

public interface InputConnection extends Connection {
    InputStream openInputStream() throws IOException;
}
