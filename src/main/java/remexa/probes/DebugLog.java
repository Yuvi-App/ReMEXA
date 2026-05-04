package remexa.probes;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class DebugLog {
    private static final System.Logger LOGGER = System.getLogger("remexa");
    private static final CopyOnWriteArrayList<Consumer<LogEvent>> LISTENERS = new CopyOnWriteArrayList<>();
    private static final int MAX_PENDING_EVENTS = 4_096;
    private static final LinkedBlockingDeque<LogEvent> EVENT_QUEUE = new LinkedBlockingDeque<>(MAX_PENDING_EVENTS);
    private static final AtomicInteger DROPPED_EVENT_COUNT = new AtomicInteger();

    static {
        var worker = new Thread(DebugLog::drainQueue, "remexa-debug-log");
        worker.setDaemon(true);
        worker.start();
    }

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
        enqueue(event);
    }

    public static void sdkCall(String owner, String member, Object... arguments) {
        var packageName = owner.contains(".") ? owner.substring(0, owner.lastIndexOf('.')) : owner;
        var category = LogCategory.fromPackageName(packageName);
        if (!category.isEnabled()) {
            return;
        }
        log(category, owner, member + formatArguments(arguments));
    }

    private static String format(LogEvent event) {
        return "[" + event.category() + "] " + event.source() + " - " + event.message();
    }

    private static void enqueue(LogEvent event) {
        if (EVENT_QUEUE.offerLast(event)) {
            return;
        }
        while (!EVENT_QUEUE.offerLast(event)) {
            if (EVENT_QUEUE.pollFirst() != null) {
                DROPPED_EVENT_COUNT.incrementAndGet();
            } else {
                break;
            }
        }
    }

    private static void drainQueue() {
        while (true) {
            try {
                dispatchDroppedNoticeIfNeeded();
                dispatch(EVENT_QUEUE.takeFirst());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ignored) {
                // Keep the logger alive even if a listener misbehaves.
            }
        }
    }

    private static void dispatchDroppedNoticeIfNeeded() {
        int dropped = DROPPED_EVENT_COUNT.getAndSet(0);
        if (dropped <= 0) {
            return;
        }
        dispatch(new LogEvent(
                Instant.now(),
                LogCategory.HOST,
                DebugLog.class.getName(),
                "Dropped " + dropped + " queued log event(s) while the async logger was saturated."
        ));
    }

    private static void dispatch(LogEvent event) {
        LOGGER.log(System.Logger.Level.INFO, format(event));
        for (var listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // A single listener should not break the whole logging pipeline.
            }
        }
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
