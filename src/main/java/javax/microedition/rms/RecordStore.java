package javax.microedition.rms;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import remexa.host.runtime.MidletRuntime;

public final class RecordStore {
    private static final int STORAGE_MAGIC = 0x524d5852;

    private final Path storePath;
    private final String name;
    private final List<RecordEntry> records = new ArrayList<>();
    private long lastModified;
    private int nextRecordId = 1;
    private int version;

    private RecordStore(String name, Path storePath) throws RecordStoreException {
        this.name = name;
        this.storePath = storePath;
        load();
    }

    public static RecordStore openRecordStore(String name, boolean createIfNecessary) throws RecordStoreException {
        try {
            Path root = rmsRoot();
            Files.createDirectories(root);
            Path storePath = root.resolve(sanitize(name) + ".bin");
            if (!Files.exists(storePath) && !createIfNecessary) {
                throw new RecordStoreNotFoundException("RecordStore not found: " + name);
            }
            if (!Files.exists(storePath)) {
                Files.write(storePath, new byte[0]);
            }
            return new RecordStore(name, storePath);
        } catch (RecordStoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to open record store: " + name, exception);
        }
    }

    public static void deleteRecordStore(String name) throws RecordStoreException {
        try {
            Path root = rmsRoot();
            Path storePath = root.resolve(sanitize(name) + ".bin");
            if (!Files.exists(storePath)) {
                throw new RecordStoreNotFoundException("RecordStore not found: " + name);
            }
            Files.delete(storePath);
        } catch (RecordStoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to delete record store: " + name, exception);
        }
    }

    public static String[] listRecordStores() throws RecordStoreException {
        try {
            Path root = rmsRoot();
            if (!Files.isDirectory(root)) {
                return null;
            }
            try (Stream<Path> stream = Files.list(root)) {
                String[] stores = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(fileName -> fileName.toLowerCase(Locale.ROOT).endsWith(".bin"))
                        .sorted(Comparator.naturalOrder())
                        .map(fileName -> fileName.substring(0, fileName.length() - 4))
                        .toArray(String[]::new);
                return stores.length == 0 ? null : stores;
            }
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to list record stores", exception);
        }
    }

    public synchronized int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        byte[] record = slice(data, offset, numBytes, "data");
        int recordId = nextRecordId++;
        records.add(new RecordEntry(recordId, record));
        flush();
        return recordId;
    }

    public synchronized void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {
        RecordEntry entry = entryForId(recordId);
        entry.data = slice(newData, offset, numBytes, "newData");
        flush();
    }

    public synchronized byte[] getRecord(int recordId) throws RecordStoreException {
        return copyRecord(recordId);
    }

    public synchronized int getRecord(int recordId, byte[] buffer, int offset) throws RecordStoreException {
        byte[] record = getRecord(recordId);
        System.arraycopy(record, 0, buffer, offset, record.length);
        return record.length;
    }

    public synchronized int getRecordSize(int recordId) throws RecordStoreException {
        return getRecord(recordId).length;
    }

    public synchronized int getNumRecords() {
        return records.size();
    }

    public synchronized void deleteRecord(int recordId) throws RecordStoreException {
        records.remove(entryForId(recordId));
        flush();
    }

    public synchronized long getLastModified() {
        return lastModified;
    }

    public synchronized int getNextRecordID() {
        return nextRecordId;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized int getSize() {
        return records.stream().mapToInt(entry -> entry.data.length).sum();
    }

    public synchronized int getSizeAvailable() {
        return Math.max(0, 16 * 1024 * 1024 - getSize());
    }

    public synchronized int getVersion() {
        return version;
    }

    public synchronized RecordEnumeration enumerateRecords(
            RecordFilter filter,
            RecordComparator comparator,
            boolean keepUpdated
    ) throws RecordStoreException {
        return new SimpleRecordEnumeration(this, filter, comparator, keepUpdated);
    }

    public void closeRecordStore() {
    }

    synchronized byte[] copyRecord(int recordId) throws RecordStoreException {
        return entryForId(recordId).data.clone();
    }

    synchronized List<Integer> snapshotRecordIds(RecordFilter filter, RecordComparator comparator) throws RecordStoreException {
        List<RecordEntry> entries = new ArrayList<>(records);
        if (filter != null) {
            entries.removeIf(entry -> !filter.matches(entry.data.clone()));
        }
        if (comparator != null) {
            entries.sort((left, right) -> normalizeComparator(comparator.compare(left.data.clone(), right.data.clone())));
        }
        List<Integer> recordIds = new ArrayList<>(entries.size());
        for (RecordEntry entry : entries) {
            recordIds.add(entry.id);
        }
        return recordIds;
    }

    synchronized int version() {
        return version;
    }

    private void load() throws RecordStoreException {
        records.clear();
        nextRecordId = 1;
        version = 0;
        try {
            if (!Files.exists(storePath) || Files.size(storePath) == 0L) {
                lastModified = System.currentTimeMillis();
                return;
            }
            byte[] data = Files.readAllBytes(storePath);
            try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data))) {
                int marker = in.readInt();
                if (marker == STORAGE_MAGIC) {
                    int formatVersion = in.readUnsignedByte();
                    if (formatVersion != 1) {
                        throw new RecordStoreException("Unsupported record store format: " + formatVersion);
                    }
                    nextRecordId = in.readInt();
                    version = in.readInt();
                    int count = in.readInt();
                    for (int i = 0; i < count; i++) {
                        int recordId = in.readInt();
                        int size = in.readInt();
                        if (size < 0) {
                            throw new RecordStoreException("Corrupt record store: negative size");
                        }
                        byte[] record = new byte[size];
                        in.readFully(record);
                        records.add(new RecordEntry(recordId, record));
                    }
                } else {
                    loadLegacyRecords(marker, in, data);
                }
            } catch (IOException exception) {
                if (data.length > 0) {
                    records.add(new RecordEntry(nextRecordId++, data));
                }
            }
            lastModified = Files.getLastModifiedTime(storePath).toMillis();
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to load record store", exception);
        }
    }

    private void flush() throws RecordStoreException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream dataOut = new DataOutputStream(out)) {
                dataOut.writeInt(STORAGE_MAGIC);
                dataOut.writeByte(1);
                dataOut.writeInt(nextRecordId);
                dataOut.writeInt(++version);
                dataOut.writeInt(records.size());
                for (RecordEntry entry : records) {
                    dataOut.writeInt(entry.id);
                    dataOut.writeInt(entry.data.length);
                    dataOut.write(entry.data);
                }
            }
            Files.write(storePath, out.toByteArray());
            lastModified = Files.getLastModifiedTime(storePath).toMillis();
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to persist record store", exception);
        }
    }

    private void loadLegacyRecords(int count, DataInputStream in, byte[] originalData) throws IOException {
        if (count < 0) {
            throw new IOException("Corrupt legacy record store");
        }
        for (int i = 0; i < count; i++) {
            int size = in.readInt();
            if (size < 0) {
                throw new IOException("Corrupt legacy record store");
            }
            byte[] record = new byte[size];
            in.readFully(record);
            records.add(new RecordEntry(nextRecordId++, record));
        }
        if (records.isEmpty() && originalData.length > 0) {
            records.add(new RecordEntry(nextRecordId++, originalData));
        }
    }

    private RecordEntry entryForId(int recordId) throws InvalidRecordIDException {
        for (RecordEntry entry : records) {
            if (entry.id == recordId) {
                return entry;
            }
        }
        throw new InvalidRecordIDException("Unknown record id: " + recordId);
    }

    private static byte[] slice(byte[] data, int offset, int numBytes, String label) {
        byte[] record = new byte[numBytes];
        if (numBytes > 0) {
            if (data == null) {
                throw new NullPointerException(label);
            }
            System.arraycopy(data, offset, record, 0, numBytes);
        }
        return record;
    }

    private static int normalizeComparator(int result) {
        if (result < 0) {
            return -1;
        }
        if (result > 0) {
            return 1;
        }
        return 0;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path rmsRoot() {
        return MidletRuntime.appStorageRoot().resolve("rms");
    }

    private static final class RecordEntry {
        private final int id;
        private byte[] data;

        private RecordEntry(int id, byte[] data) {
            this.id = id;
            this.data = data;
        }
    }
}
