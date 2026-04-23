package remexa.host.runtime;

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
}
