package remexa.host.jad;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import remexa.settings.RemexaPreferences;

public final class RecentJadsRepository {
    private static final int LIMIT = 10;
    private static final String FORMAT_VERSION_KEY = "format.version";
    private static final int CURRENT_FORMAT_VERSION = 3;

    public List<RecentJadEntry> load() {
        var entries = new ArrayList<RecentJadEntry>();
        var needsMigration = storedFormatVersion() < CURRENT_FORMAT_VERSION;
        for (int index = 0; index < LIMIT; index++) {
            var value = RemexaPreferences.recentJads().get(RemexaPreferences.RECENT_ENTRY_PREFIX + index, null);
            if (value == null || value.isBlank()) {
                continue;
            }
            var separator = value.indexOf('|');
            if (separator < 0) {
                continue;
            }
            var title = value.substring(0, separator);
            var path = Path.of(value.substring(separator + 1));
            var entry = new RecentJadEntry(title, path);
            entries.add(needsMigration ? refresh(entry) : entry);
        }
        if (needsMigration) {
            store(entries);
        }
        return List.copyOf(entries);
    }

    public void remember(JadDescriptor descriptor) {
        var entries = new ArrayList<>(load());
        entries.removeIf(entry -> entry.jadPath().equals(descriptor.sourcePath()));
        entries.addFirst(new RecentJadEntry(descriptor.title(), descriptor.sourcePath()));
        while (entries.size() > LIMIT) {
            entries.removeLast();
        }
        store(entries);
    }

    private static RecentJadEntry refresh(RecentJadEntry entry) {
        if (!Files.exists(entry.jadPath())) {
            return entry;
        }
        try {
            var descriptor = JadParser.parse(entry.jadPath());
            var refreshedTitle = descriptor.title();
            if (refreshedTitle == null || refreshedTitle.isBlank()) {
                return entry;
            }
            return new RecentJadEntry(refreshedTitle, entry.jadPath());
        } catch (Exception ignored) {
            return entry;
        }
    }

    private static void store(List<RecentJadEntry> entries) {
        for (int index = 0; index < LIMIT; index++) {
            var key = RemexaPreferences.RECENT_ENTRY_PREFIX + index;
            if (index < entries.size()) {
                var entry = entries.get(index);
                RemexaPreferences.recentJads().put(key, entry.title() + "|" + entry.jadPath());
            } else {
                RemexaPreferences.recentJads().remove(key);
            }
        }
        RemexaPreferences.recentJads().putInt(FORMAT_VERSION_KEY, CURRENT_FORMAT_VERSION);
    }

    private static int storedFormatVersion() {
        return RemexaPreferences.recentJads().getInt(FORMAT_VERSION_KEY, 0);
    }
}
