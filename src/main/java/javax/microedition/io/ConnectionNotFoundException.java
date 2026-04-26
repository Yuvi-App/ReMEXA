package javax.microedition.io;

import java.io.IOException;

public class ConnectionNotFoundException extends IOException {
    public ConnectionNotFoundException() {
        super();
    }

    public ConnectionNotFoundException(String message) {
        super(message);
    }

    public ConnectionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
