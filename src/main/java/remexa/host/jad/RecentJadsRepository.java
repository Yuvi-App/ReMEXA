package remexa.host.jad;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import remexa.settings.RemexaPreferences;

public final class RecentJadsRepository {
    private static final int LIMIT = 10;

    public List<RecentJadEntry> load() {
        var entries = new ArrayList<RecentJadEntry>();
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
            entries.add(new RecentJadEntry(title, path));
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
        for (int index = 0; index < LIMIT; index++) {
            var key = RemexaPreferences.RECENT_ENTRY_PREFIX + index;
            if (index < entries.size()) {
                var entry = entries.get(index);
                RemexaPreferences.recentJads().put(key, entry.title() + "|" + entry.jadPath());
            } else {
                RemexaPreferences.recentJads().remove(key);
            }
        }
    }
}
