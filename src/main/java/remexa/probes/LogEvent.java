package remexa.probes;

import java.time.Instant;

public record LogEvent(
        Instant timestamp,
        LogCategory category,
        String source,
        String message
) {
}
