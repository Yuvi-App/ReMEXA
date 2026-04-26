package remexa.host.runtime;

import java.io.InputStream;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class LegacyRuntimeSupport {
    private static final Object SPIN_MONITOR = new Object();

    private LegacyRuntimeSupport() {
    }

    public static void spinLoopHint() {
        synchronized (SPIN_MONITOR) {
            // Acquiring and releasing a shared monitor gives legacy app threads
            // a happens-before edge inside busy-spin loops.
        }
        Thread.onSpinWait();
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
