package remexa.host.jad;

import java.nio.file.Path;

public record RecentJadEntry(
        String title,
        Path jadPath
) {
    @Override
    public String toString() {
        return title + " [" + jadPath + "]";
    }
}
