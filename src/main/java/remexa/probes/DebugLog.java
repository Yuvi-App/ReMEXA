package remexa.probes;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class DebugLog {
    private static final System.Logger LOGGER = System.getLogger("remexa");
    private static final CopyOnWriteArrayList<Consumer<LogEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private DebugLog() {
    }

    public static void addListener(Consumer<LogEvent> listener) {
        LISTENERS.add(Objects.requireNonNull(listener));
    }

    public static void removeListener(Consumer<LogEvent> listener) {
        LISTENERS.remove(listener);
    }

    public static void log(LogCategory category, String source, String message) {
        if (!LogSettings.isEnabled(category)) {
            return;
        }
        var event = new LogEvent(Instant.now(), category, source, message);
        LOGGER.log(System.Logger.Level.INFO, format(event));
        for (var listener : LISTENERS) {
            listener.accept(event);
        }
    }

    public static void sdkCall(String owner, String member, Object... arguments) {
        var packageName = owner.contains(".") ? owner.substring(0, owner.lastIndexOf('.')) : owner;
        var category = LogCategory.fromPackageName(packageName);
        log(category, owner, member + formatArguments(arguments));
    }

    private static String format(LogEvent event) {
        return "[" + event.category() + "] " + event.source() + " - " + event.message();
    }

    private static String formatArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "()";
        }
        var builder = new StringBuilder("(");
        for (int index = 0; index < arguments.length; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(arguments[index]);
        }
        return builder.append(')').toString();
    }
}
