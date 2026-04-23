package remexa.host.jad;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public final class RecentJadsRepository {
    private static final int LIMIT = 10;
    private static final Preferences PREFERENCES = Preferences.userRoot().node("remexa/recent-jads");

    public List<RecentJadEntry> load() {
        var entries = new ArrayList<RecentJadEntry>();
        for (int index = 0; index < LIMIT; index++) {
            var value = PREFERENCES.get("entry." + index, null);
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
            var key = "entry." + index;
            if (index < entries.size()) {
                var entry = entries.get(index);
                PREFERENCES.put(key, entry.title() + "|" + entry.jadPath());
            } else {
                PREFERENCES.remove(key);
            }
        }
    }
}
