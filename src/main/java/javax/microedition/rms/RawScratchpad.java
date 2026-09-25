package javax.microedition.rms;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RawScratchpad {
    private static final int FORMAT_VERSION = 0x2778;
    private static final int STORE_HEADER_SIZE = 8;
    private static final int STORE_ENTRY_SIZE = 94;
    private static final int INDEX_ENTRY_SIZE = 16;

    private RawScratchpad() {
    }

    /** Returns null when no companion is present; incomplete or invalid dumps fail closed. */
    static List<Store> readIfPresent(Path rmsPath) throws IOException {
        String fileName = rmsPath.getFileName().toString();
        String base = fileName.substring(0, fileName.length() - 4);
        Path directory = rmsPath.getParent();
        Path rsd = findFile(directory.resolve(base + ".rsd"));
        Path rsi = findFile(directory.resolve(base + ".rsi"));
        Path rsr = findFile(directory.resolve(base + ".rsr"));
        Path rsp = findFile(directory.resolve(base + ".rsp"));
        if (rsd == null && rsi == null && rsr == null && rsp == null) {
            return null;
        }
        if (rsd == null || rsi == null || rsr == null) {
            throw invalid("Incomplete raw scratchpad for " + base + ": RSD, RSI and RSR are required");
        }

        byte[] data = Files.readAllBytes(rsd);
        byte[] index = Files.readAllBytes(rsi);
        byte[] registry = Files.readAllBytes(rsr);
        int dataLength = data.length;
        int indexLength;
        int registryLength;
        if (rsp != null) {
            byte[] properties = Files.readAllBytes(rsp);
            if (properties.length != 20) {
                throw invalid("RSP must contain five 32-bit fields");
            }
            ByteBuffer limits = ByteBuffer.wrap(properties).order(ByteOrder.LITTLE_ENDIAN);
            // The first field is unidentified (zero in observed dumps); the last is capacity,
            // not the used data length. Garbage can make the data file exceed that capacity.
            indexLength = checkedLength(limits.getInt(4), index.length, "RSI");
            registryLength = checkedLength(limits.getInt(8), registry.length, "RSR");
            dataLength = checkedLength(limits.getInt(12), data.length, "RSD");
        } else {
            indexLength = unpaddedLength(index, 0, INDEX_ENTRY_SIZE);
            registryLength = unpaddedLength(registry, STORE_HEADER_SIZE, STORE_ENTRY_SIZE);
        }
        if (registryLength < STORE_HEADER_SIZE
                || (registryLength - STORE_HEADER_SIZE) % STORE_ENTRY_SIZE != 0
                || (registryLength - STORE_HEADER_SIZE) / STORE_ENTRY_SIZE > 4096
                || indexLength % INDEX_ENTRY_SIZE != 0) {
            throw invalid("Truncated raw scratchpad table");
        }

        ByteBuffer names = ByteBuffer.wrap(registry); // Java RMS fields are big endian.
        if (names.getInt(0) != FORMAT_VERSION) {
            throw invalid("Unsupported RSR format version");
        }
        Map<Integer, Store> stores = new LinkedHashMap<>();
        var storeNames = new java.util.HashSet<String>();
        Map<Integer, Integer> expectedSizes = new LinkedHashMap<>();
        for (int offset = STORE_HEADER_SIZE; offset < registryLength; offset += STORE_ENTRY_SIZE) {
            int id = names.getInt(offset);
            if (id == -1) {
                continue; // Deleted store slot.
            }
            int nameLength = Short.toUnsignedInt(names.getShort(offset + 4));
            if (id < 0 || nameLength < 1 || nameLength > 32) {
                throw invalid("Invalid RSR store ID or name length");
            }
            String name = new String(registry, offset + 6, nameLength * 2, StandardCharsets.UTF_16BE);
            int count = names.getInt(offset + 70);
            int lastId = names.getInt(offset + 74);
            int size = names.getInt(offset + 78);
            int version = names.getInt(offset + 82);
            long modified = names.getLong(offset + 86);
            if (count < 0 || count > 65535 || count > indexLength / INDEX_ENTRY_SIZE || lastId < 0
                    || lastId == Integer.MAX_VALUE || size < 0 || version < 0) {
                throw invalid("Invalid RSR metadata for " + name);
            }
            Store store = new Store(name, version, modified, count, lastId + 1, new LinkedHashMap<>());
            if (stores.putIfAbsent(id, store) != null || !storeNames.add(name)) {
                throw invalid("Duplicate RSR store ID or name");
            }
            expectedSizes.put(id, size);
        }

        ByteBuffer entries = ByteBuffer.wrap(index);
        var liveRanges = new java.util.TreeMap<Integer, Integer>();
        for (int offset = 0; offset < indexLength; offset += INDEX_ENTRY_SIZE) {
            int storeId = entries.getInt(offset);
            int recordId = entries.getInt(offset + 4);
            if (recordId == -1) {
                continue; // Free/garbage entries may point outside the live data.
            }
            int start = entries.getInt(offset + 8);
            int length = entries.getInt(offset + 12);
            Store store = stores.get(storeId);
            if (store == null || recordId <= 0 || recordId >= store.nextRecordId()
                    || start < 0 || length < 0 || start > dataLength || length > dataLength - start) {
                throw invalid("Invalid RSI record ID, store reference or data range");
            }
            if (length > 0) {
                var preceding = liveRanges.floorEntry(start);
                var following = liveRanges.ceilingEntry(start);
                if (preceding != null && preceding.getValue() > start
                        || following != null && following.getKey() < start + length) {
                    throw invalid("Overlapping live RSI records");
                }
                liveRanges.put(start, start + length);
            }
            if (store.records().putIfAbsent(recordId, Arrays.copyOfRange(data, start, start + length)) != null) {
                throw invalid("Duplicate RSI record ID in " + store.name());
            }
        }
        for (var entry : stores.entrySet()) {
            Store store = entry.getValue();
            long storedSize = store.records().values().stream().mapToLong(bytes -> bytes.length + 16L).sum();
            if (store.records().size() != store.recordCount() || storedSize != expectedSizes.get(entry.getKey())) {
                throw invalid("RSR and RSI metadata disagree for " + store.name());
            }
        }
        return new ArrayList<>(stores.values());
    }

    static Path findFile(Path candidate) throws IOException {
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        if (!Files.isDirectory(candidate.getParent())) {
            return null;
        }
        try (var files = Files.list(candidate.getParent())) {
            var matches = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(candidate.getFileName().toString()))
                    .sorted().toList();
            if (matches.size() > 1) {
                throw invalid("Ambiguous companion filename: " + candidate.getFileName());
            }
            return matches.isEmpty() ? null : matches.getFirst();
        }
    }

    private static int checkedLength(int length, int available, String extension) throws IOException {
        if (length < 0 || length > available) {
            throw invalid("RSP " + extension + " length exceeds the file size");
        }
        return length;
    }

    private static int unpaddedLength(byte[] bytes, int header, int stride) throws IOException {
        int end = bytes.length;
        while (end > header && bytes[end - 1] == (byte) 0xff) {
            end--;
        }
        // Restore any trailing 0xff bytes that belong to a live entry (e.g. a timestamp).
        long aligned = header + ((long) Math.max(0, end - header) + stride - 1) / stride * stride;
        if (aligned > bytes.length) {
            throw invalid("Truncated raw scratchpad table");
        }
        return (int) aligned;
    }

    private static IOException invalid(String message) {
        return new IOException(message);
    }

    record Store(String name, int version, long lastModified, int recordCount, int nextRecordId,
                 Map<Integer, byte[]> records) {
    }
}
