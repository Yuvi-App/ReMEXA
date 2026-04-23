package javax.microedition.rms;

import java.util.ArrayList;
import java.util.List;

final class SimpleRecordEnumeration implements RecordEnumeration {
    private final RecordStore store;
    private final RecordFilter filter;
    private final RecordComparator comparator;
    private boolean keepUpdated;
    private List<Integer> recordIds;
    private int nextIndex;
    private int observedVersion;
    private boolean destroyed;

    SimpleRecordEnumeration(
            RecordStore store,
            RecordFilter filter,
            RecordComparator comparator,
            boolean keepUpdated
    ) throws RecordStoreException {
        this.store = store;
        this.filter = filter;
        this.comparator = comparator;
        this.keepUpdated = keepUpdated;
        this.recordIds = new ArrayList<>();
        rebuild();
    }

    @Override
    public void destroy() {
        destroyed = true;
        recordIds = List.of();
        nextIndex = 0;
    }

    @Override
    public boolean hasNextElement() {
        syncIfNeeded();
        return !destroyed && nextIndex < recordIds.size();
    }

    @Override
    public boolean hasPreviousElement() {
        syncIfNeeded();
        return !destroyed && nextIndex > 0 && !recordIds.isEmpty();
    }

    @Override
    public boolean isKeptUpdated() {
        return keepUpdated;
    }

    @Override
    public void keepUpdated(boolean keepUpdated) {
        this.keepUpdated = keepUpdated;
        syncIfNeeded();
    }

    @Override
    public byte[] nextRecord() throws RecordStoreException {
        return store.copyRecord(nextRecordId());
    }

    @Override
    public int nextRecordId() throws RecordStoreException {
        syncIfNeeded();
        if (!hasNextElement()) {
            throw new InvalidRecordIDException("No next record");
        }
        return recordIds.get(nextIndex++);
    }

    @Override
    public int numRecords() {
        syncIfNeeded();
        return destroyed ? 0 : recordIds.size();
    }

    @Override
    public byte[] previousRecord() throws RecordStoreException {
        return store.copyRecord(previousRecordId());
    }

    @Override
    public int previousRecordId() throws RecordStoreException {
        syncIfNeeded();
        if (!hasPreviousElement()) {
            throw new InvalidRecordIDException("No previous record");
        }
        return recordIds.get(--nextIndex);
    }

    @Override
    public void rebuild() throws RecordStoreException {
        if (destroyed) {
            return;
        }
        int resumeId = nextIndex > 0 && nextIndex <= recordIds.size() ? recordIds.get(nextIndex - 1) : -1;
        recordIds = new ArrayList<>(store.snapshotRecordIds(filter, comparator));
        observedVersion = store.version();
        if (resumeId < 0) {
            nextIndex = 0;
            return;
        }
        int position = recordIds.indexOf(resumeId);
        nextIndex = position < 0 ? Math.min(nextIndex, recordIds.size()) : position + 1;
    }

    @Override
    public void reset() {
        nextIndex = 0;
        syncIfNeeded();
    }

    private void syncIfNeeded() {
        if (destroyed || !keepUpdated || observedVersion == store.version()) {
            return;
        }
        try {
            rebuild();
        } catch (RecordStoreException exception) {
            throw new IllegalStateException("Unable to rebuild record enumeration", exception);
        }
    }
}
