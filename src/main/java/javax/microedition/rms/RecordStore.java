package javax.microedition.rms;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.settings.RemexaPreferences;

public final class RecordStore {
    private static final int STORAGE_MAGIC = 0x524d5852;

    private final Path storePath;
    private final String name;
    private final Path legacyContainerPath;
    private final boolean legacyBacked;
    private final List<RecordEntry> records = new ArrayList<>();
    private long lastModified;
    private int nextRecordId = 1;
    private int version;

    private RecordStore(String name, Path storePath, Path legacyContainerPath, boolean legacyBacked, LegacyStoreRecord legacyStore)
            throws RecordStoreException {
        this.name = name;
        this.storePath = storePath;
        this.legacyContainerPath = legacyContainerPath;
        this.legacyBacked = legacyBacked;
        load(legacyStore);
    }

    public static RecordStore openRecordStore(String name, boolean createIfNecessary) throws RecordStoreException {
        try {
            Path root = rmsRoot();
            Path storePath = root.resolve(sanitize(name) + ".bin");

            LegacyStoreRecord legacyStore = null;
            Path legacyContainerPath = null;
            var legacyContainer = legacyContainerPath();
            if (legacyContainer.isPresent()) {
                legacyContainerPath = legacyContainer.get();
                legacyStore = readLegacyStore(legacyContainerPath, name);
            }
            boolean storeFileExists = Files.exists(storePath);
            boolean legacyBacked = legacyContainerPath != null;

            if (legacyBacked && legacyStore == null && !createIfNecessary) {
                throw new RecordStoreNotFoundException("RecordStore not found: " + name);
            }
            if (!legacyBacked && !storeFileExists && !createIfNecessary) {
                throw new RecordStoreNotFoundException("RecordStore not found: " + name);
            }

            if (!legacyBacked && !storeFileExists && createIfNecessary) {
                Files.createDirectories(root);
                Files.write(storePath, new byte[0]);
            }

            return new RecordStore(name, storePath, legacyContainerPath, legacyBacked, legacyStore);
        } catch (RecordStoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to open record store: " + name, exception);
        }
    }

    public static void deleteRecordStore(String name) throws RecordStoreException {
        try {
            var legacyContainer = legacyContainerPath();
            Path root = rmsRoot();
            Path storePath = root.resolve(sanitize(name) + ".bin");
            if (legacyContainer.isPresent()) {
                if (deleteLegacyStore(legacyContainer.get(), name)) {
                    Files.deleteIfExists(storePath);
                    return;
                }
                throw new RecordStoreNotFoundException("RecordStore not found: " + name);
            }

            if (Files.exists(storePath)) {
                Files.delete(storePath);
                return;
            }

            throw new RecordStoreNotFoundException("RecordStore not found: " + name);
        } catch (RecordStoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to delete record store: " + name, exception);
        }
    }

    public static String[] listRecordStores() throws RecordStoreException {
        try {
            var legacyContainer = legacyContainerPath();
            if (legacyContainer.isPresent()) {
                var legacyNames = readLegacyStoreNames(legacyContainer.get());
                return legacyNames.isEmpty() ? null : legacyNames.toArray(String[]::new);
            }

            Path root = rmsRoot();
            var names = new LinkedHashSet<String>();
            if (Files.isDirectory(root)) {
                try (Stream<Path> stream = Files.list(root)) {
                    stream
                            .filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .filter(fileName -> fileName.toLowerCase(Locale.ROOT).endsWith(".bin"))
                            .sorted(Comparator.naturalOrder())
                            .map(fileName -> fileName.substring(0, fileName.length() - 4))
                            .forEach(names::add);
                }
            }

            return names.isEmpty() ? null : names.toArray(String[]::new);
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

    private void load(LegacyStoreRecord legacyStore) throws RecordStoreException {
        records.clear();
        nextRecordId = 1;
        version = 0;
        try {
            if (legacyBacked) {
                if (legacyStore != null) {
                    loadLegacyStore(legacyStore);
                    if (dumpLegacyMirrorEnabled()) {
                        writeBinaryStore();
                        DebugLog.log(
                                LogCategory.RMS,
                                RecordStore.class.getName(),
                                "Dumped legacy RecordStore \"" + name + "\" to " + storePath
                        );
                    }
                    return;
                }
                lastModified = System.currentTimeMillis();
                return;
            }

            if (Files.exists(storePath) && Files.size(storePath) > 0L) {
                loadBinaryStore();
                return;
            }
            lastModified = Files.exists(storePath)
                    ? Files.getLastModifiedTime(storePath).toMillis()
                    : System.currentTimeMillis();
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to load record store", exception);
        }
    }

    private void loadBinaryStore() throws IOException, RecordStoreException {
        byte[] data = Files.readAllBytes(storePath);
        try {
            if (!loadStructuredStoreBytes(data)) {
                throw new IOException("Unsupported record store bytes");
            }
        } catch (IOException exception) {
            if (data.length > 0) {
                records.add(new RecordEntry(nextRecordId++, data));
            }
        }
        lastModified = Files.getLastModifiedTime(storePath).toMillis();
    }

    private void loadLegacyStore(LegacyStoreRecord legacyStore) {
        version = Math.max(legacyStore.version(), 0);
        lastModified = legacyStore.lastModified();
        records.clear();
        int maxRecordId = 0;
        for (LegacyRecord legacyRecord : legacyStore.records()) {
            records.add(new RecordEntry(legacyRecord.id(), legacyRecord.data().clone()));
            maxRecordId = Math.max(maxRecordId, legacyRecord.id());
        }
        nextRecordId = Math.max(maxRecordId + 1, records.size() + 1);
        if (records.size() != legacyStore.recordCount()) {
            DebugLog.log(
                    LogCategory.RMS,
                    RecordStore.class.getName(),
                    "Legacy RMS store \"" + name + "\" declared " + legacyStore.recordCount()
                            + " record(s) but decoded " + records.size() + '.'
            );
        }
    }

    private void flush() throws RecordStoreException {
        version++;
        lastModified = System.currentTimeMillis();
        try {
            if (legacyBacked) {
                writeLegacyStore();
                if (dumpLegacyMirrorEnabled()) {
                    writeBinaryStore();
                }
            } else {
                writeBinaryStore();
            }
        } catch (IOException exception) {
            throw new RecordStoreException("Unable to persist record store", exception);
        }
    }

    private void writeBinaryStore() throws IOException {
        Files.createDirectories(storePath.getParent());
        Files.write(storePath, encodeBinaryStore());
        lastModified = Files.getLastModifiedTime(storePath).toMillis();
    }

    private void writeLegacyStore() throws IOException, RecordStoreException {
        if (legacyContainerPath == null) {
            writeBinaryStore();
            return;
        }

        List<LegacyRecord> legacyRecords = new ArrayList<>(records.size());
        for (RecordEntry entry : records) {
            legacyRecords.add(new LegacyRecord(entry.id, entry.data.clone()));
        }
        Map<String, LegacyStoreRecord> stores = readLegacyStores(legacyContainerPath);
        stores.put(name, new LegacyStoreRecord(
                name,
                version,
                lastModified,
                records.size(),
                legacyRecords
        ));
        writeLegacyStores(legacyContainerPath, stores);
    }

    private byte[] encodeBinaryStore() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream dataOut = new DataOutputStream(out)) {
            dataOut.writeInt(STORAGE_MAGIC);
            dataOut.writeByte(1);
            dataOut.writeInt(nextRecordId);
            dataOut.writeInt(version);
            dataOut.writeInt(records.size());
            for (RecordEntry entry : records) {
                dataOut.writeInt(entry.id);
                dataOut.writeInt(entry.data.length);
                dataOut.write(entry.data);
            }
        }
        return out.toByteArray();
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

    private boolean loadStructuredStoreBytes(byte[] data) {
        if (data == null || data.length < Integer.BYTES) {
            return false;
        }

        var parsedRecords = new ArrayList<RecordEntry>();
        int parsedNextRecordId = nextRecordId;
        int parsedVersion = version;
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data))) {
            int marker = in.readInt();
            if (marker == STORAGE_MAGIC) {
                int formatVersion = in.readUnsignedByte();
                if (formatVersion != 1) {
                    return false;
                }
                parsedNextRecordId = in.readInt();
                parsedVersion = in.readInt();
                int count = in.readInt();
                if (count < 0) {
                    return false;
                }
                for (int i = 0; i < count; i++) {
                    int recordId = in.readInt();
                    int size = in.readInt();
                    if (size < 0) {
                        return false;
                    }
                    byte[] record = new byte[size];
                    in.readFully(record);
                    parsedRecords.add(new RecordEntry(recordId, record));
                }
            } else {
                int count = marker;
                if (count < 0 || count > 65535) {
                    return false;
                }
                int parsedRecordId = 1;
                for (int i = 0; i < count; i++) {
                    int size = in.readInt();
                    if (size < 0) {
                        return false;
                    }
                    byte[] record = new byte[size];
                    in.readFully(record);
                    parsedRecords.add(new RecordEntry(parsedRecordId++, record));
                }
                parsedNextRecordId = Math.max(parsedRecordId, parsedRecords.size() + 1);
            }

            if (in.read() != -1) {
                return false;
            }
        } catch (IOException exception) {
            return false;
        }

        records.clear();
        records.addAll(parsedRecords);
        nextRecordId = Math.max(parsedNextRecordId, parsedRecords.size() + 1);
        version = Math.max(parsedVersion, 0);
        return true;
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
        return appDataRmsRoot().resolve(sanitizeAppFolderName(MidletRuntime.currentAppTitle()));
    }

    private static Path appDataRmsRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "ReMEXA", "rms");
        }
        return Path.of(System.getProperty("user.home"), ".remexa", "rms");
    }

    private static String sanitizeAppFolderName(String title) {
        String sanitized = title == null ? "" : title.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return sanitized.isEmpty() ? "Unknown Game" : sanitized;
    }

    private static boolean dumpLegacyMirrorEnabled() {
        return RemexaPreferences.debug().getBoolean(RemexaPreferences.DUMP_RMS_KEY, false);
    }

    private static Optional<Path> legacyContainerPath() {
        Path sourcePath = MidletRuntime.currentSourcePath();
        Path jarPath = MidletRuntime.currentJarPath();
        Path appDirectory = sourcePath != null ? sourcePath.getParent() : jarPath != null ? jarPath.getParent() : null;
        if (appDirectory == null) {
            try {
                appDirectory = MidletRuntime.appStorageRoot().getParent();
            } catch (IllegalStateException ignored) {
                return Optional.empty();
            }
        }
        if (appDirectory == null || !Files.isDirectory(appDirectory)) {
            return Optional.empty();
        }

        var candidates = new LinkedHashSet<Path>();
        addLegacyCandidates(candidates, sourcePath);
        addLegacyCandidates(candidates, jarPath);
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }

        try (Stream<Path> stream = Files.list(appDirectory)) {
            var rmsFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rms"))
                    .sorted()
                    .toList();
            if (rmsFiles.size() == 1) {
                return Optional.of(rmsFiles.getFirst());
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static void addLegacyCandidates(LinkedHashSet<Path> candidates, Path sourcePath) {
        if (sourcePath == null) {
            return;
        }
        Path parent = sourcePath.getParent();
        Path fileName = sourcePath.getFileName();
        if (parent == null || fileName == null) {
            return;
        }
        String rawName = fileName.toString();
        int extensionIndex = rawName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? rawName.substring(0, extensionIndex) : rawName;
        candidates.add(parent.resolve(baseName + ".rms"));
        candidates.add(parent.resolve(baseName + ".RMS"));
    }

    private static List<String> readLegacyStoreNames(Path legacyContainer) throws IOException {
        return new ArrayList<>(readLegacyStores(legacyContainer).keySet());
    }

    private static LegacyStoreRecord readLegacyStore(Path legacyContainer, String storeName) throws IOException {
        return readLegacyStores(legacyContainer).get(storeName);
    }

    private static Map<String, LegacyStoreRecord> readLegacyStores(Path legacyContainer) throws IOException {
        Map<String, LegacyStoreRecord> stores = new LinkedHashMap<>();
        if (!Files.isRegularFile(legacyContainer)) {
            return stores;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(legacyContainer))) {
            int entryCount = in.readInt();
            if (entryCount < 0 || entryCount > 4096) {
                throw new IOException("Invalid legacy RMS entry count: " + entryCount);
            }
            for (int index = 0; index < entryCount; index++) {
                String entryName = in.readUTF();
                int importedVersion = in.readInt();
                long importedLastModified = in.readLong();
                int recordCount = in.readInt();
                if (recordCount < 0 || recordCount > 65535) {
                    throw new IOException("Invalid legacy RMS record count: " + recordCount);
                }
                List<LegacyRecord> importedRecords = new ArrayList<>(recordCount);
                for (int recordIndex = 0; recordIndex < recordCount; recordIndex++) {
                    int recordId = in.readInt();
                    int dataLength = in.readInt();
                    if (dataLength < 0) {
                        throw new IOException("Negative legacy RMS record length: " + dataLength);
                    }
                    byte[] recordData = in.readNBytes(dataLength);
                    if (recordData.length != dataLength) {
                        throw new IOException("Truncated legacy RMS store: " + entryName);
                    }
                    importedRecords.add(new LegacyRecord(recordId, recordData));
                }
                stores.put(entryName, new LegacyStoreRecord(
                        entryName,
                        Math.max(importedVersion, 0),
                        importedLastModified,
                        Math.max(recordCount, 0),
                        importedRecords
                ));
            }
        } catch (IOException exception) {
            DebugLog.log(
                    LogCategory.RMS,
                    RecordStore.class.getName(),
                    "Legacy RMS fallback skipped for \"" + legacyContainer.getFileName() + "\": " + exception.getMessage()
            );
            throw exception;
        }
        return stores;
    }

    private static void writeLegacyStores(Path legacyContainer, Map<String, LegacyStoreRecord> stores) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream dataOut = new DataOutputStream(out)) {
            dataOut.writeInt(stores.size());
            for (LegacyStoreRecord store : stores.values()) {
                dataOut.writeUTF(store.name());
                dataOut.writeInt(store.version());
                dataOut.writeLong(store.lastModified());
                dataOut.writeInt(store.recordCount());
                for (LegacyRecord record : store.records()) {
                    dataOut.writeInt(record.id());
                    dataOut.writeInt(record.data().length);
                    dataOut.write(record.data());
                }
            }
        }
        Files.write(legacyContainer, out.toByteArray());
    }

    private static boolean deleteLegacyStore(Path legacyContainer, String storeName) throws IOException {
        Map<String, LegacyStoreRecord> stores = readLegacyStores(legacyContainer);
        if (stores.remove(storeName) == null) {
            return false;
        }
        writeLegacyStores(legacyContainer, stores);
        return true;
    }

    private static final class RecordEntry {
        private final int id;
        private byte[] data;

        private RecordEntry(int id, byte[] data) {
            this.id = id;
            this.data = data;
        }
    }

    private record LegacyStoreRecord(
            String name,
            int version,
            long lastModified,
            int recordCount,
            List<LegacyRecord> records
    ) {
    }

    private record LegacyRecord(
            int id,
            byte[] data
    ) {
    }
}
