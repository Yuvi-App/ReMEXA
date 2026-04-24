package javax.microedition.media;

public class MediaException extends Exception {
    public MediaException() {
    }

    public MediaException(String message) {
        super(message);
    }

    public MediaException(String message, Throwable cause) {
        super(message, cause);
    }
}
