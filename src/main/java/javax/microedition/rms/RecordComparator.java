package javax.microedition.rms;

public interface RecordComparator {
    int PRECEDES = -1;
    int EQUIVALENT = 0;
    int FOLLOWS = 1;

    int compare(byte[] record1, byte[] record2);
}
