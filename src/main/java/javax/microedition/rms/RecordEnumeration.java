package javax.microedition.rms;

public interface RecordEnumeration {
    void destroy();

    boolean hasNextElement();

    boolean hasPreviousElement();

    boolean isKeptUpdated();

    void keepUpdated(boolean keepUpdated);

    byte[] nextRecord() throws RecordStoreException;

    int nextRecordId() throws RecordStoreException;

    int numRecords();

    byte[] previousRecord() throws RecordStoreException;

    int previousRecordId() throws RecordStoreException;

    void rebuild() throws RecordStoreException;

    void reset();
}
