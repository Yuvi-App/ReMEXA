package javax.microedition.rms;

public class InvalidRecordIDException extends RecordStoreException {
    public InvalidRecordIDException() {
    }

    public InvalidRecordIDException(String message) {
        super(message);
    }
}
