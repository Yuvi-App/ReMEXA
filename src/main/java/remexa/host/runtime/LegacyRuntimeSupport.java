package remexa.host.runtime;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class LegacyRuntimeSupport {
    private static final Object SPIN_MONITOR = new Object();
    private static final Set<String> COMPLETED_BOOTSTRAP_TARGETS = ConcurrentHashMap.newKeySet();
    private static final long LEGACY_GC_INTERVAL_NANOS = 250_000_000L;
    private static final AtomicLong LAST_LEGACY_GC_NANOS = new AtomicLong();

    private LegacyRuntimeSupport() {
    }

    public static void spinLoopHint() {
        synchronized (SPIN_MONITOR) {
            // Acquiring and releasing a shared monitor gives legacy app threads
            // a happens-before edge inside busy-spin loops.
        }
        Thread.onSpinWait();
    }

    public static void gcHint() {
        // CLDC-era apps often call System.gc() in tight loading loops as an
        // advisory memory hint. Honor it occasionally so memory-settle loops
        // can complete, but avoid repeatedly stopping the whole desktop VM.
        long now = System.nanoTime();
        long previous = LAST_LEGACY_GC_NANOS.get();
        if (now - previous >= LEGACY_GC_INTERVAL_NANOS
                && LAST_LEGACY_GC_NANOS.compareAndSet(previous, now)) {
            System.gc();
            return;
        }
        Thread.onSpinWait();
    }

    public static void legacyMemorySettle() {
        gcHint();
        Thread.yield();
    }

    public static void publishLegacyState() {
        synchronized (SPIN_MONITOR) {
            // Pair with spinLoopHint() so UI-thread callbacks publish state changes
            // to legacy worker threads that busy-spin without volatile fields.
        }
    }

    public static InputStream getResourceAsStream(Class<?> anchor, String name) {
        if (anchor == null || name == null) {
            return null;
        }

        String resolvedName = resolveResourceName(anchor, name);
        ClassLoader loader = anchor.getClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(resolvedName);
        }

        InputStream stream = MidletRuntime.openResource(resolvedName);
        if (stream != null) {
            return stream;
        }
        return ClassLoader.getSystemResourceAsStream(resolvedName);
    }

    public static void logCaughtThrowable(Throwable throwable) {
        if (throwable == null) {
            return;
        }
        if (MidletRuntime.isExpectedShutdownThrowable(throwable)) {
            return;
        }
        var stack = new StringBuilder();
        stack.append(throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            stack.append(": ").append(throwable.getMessage());
        }
        for (var element : throwable.getStackTrace()) {
            stack.append("\n  at ").append(element);
        }
        var cause = throwable.getCause();
        while (cause != null) {
            stack.append("\nCaused by: ").append(cause.getClass().getName());
            if (cause.getMessage() != null) {
                stack.append(": ").append(cause.getMessage());
            }
            for (var element : cause.getStackTrace()) {
                stack.append("\n  at ").append(element);
            }
            cause = cause.getCause();
        }
        DebugLog.log(LogCategory.HOST, LegacyRuntimeSupport.class.getName(), "Legacy catch swallowed: " + stack);
    }

    public static boolean completeBootstrapStub(String target) {
        if (target != null && COMPLETED_BOOTSTRAP_TARGETS.add(target)) {
            DebugLog.log(
                    LogCategory.HOST,
                    LegacyRuntimeSupport.class.getName(),
                    "Completed legacy bootstrap stub for " + target
            );
        }
        return true;
    }

    private static String resolveResourceName(Class<?> anchor, String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }

        String className = anchor.getName();
        int packageSeparator = className.lastIndexOf('.');
        if (packageSeparator < 0) {
            return name;
        }
        return className.substring(0, packageSeparator).replace('.', '/') + "/" + name;
    }
}
