package remexa.audio;

import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

/** Host workers never inherit an appli; callbacks explicitly enter their owning appli. */
public final class AudioCallbacks {
    private AudioCallbacks() { }

    public static Thread hostThread(Runnable action, String name) {
        Thread thread = new HostWorker(action, name);
        thread.setContextClassLoader(AudioCallbacks.class.getClassLoader());
        thread.setDaemon(true);
        return thread;
    }

    /** Shared workers stay host-owned even while temporarily running an app callback. */
    public static boolean isHostWorker(Thread thread) {
        return thread instanceof HostWorker;
    }

    public static void dispatch(ClassLoader owner, String name, Runnable callback) {
        Thread thread = new Thread(() -> run(owner, callback), name);
        thread.setContextClassLoader(AudioCallbacks.class.getClassLoader());
        thread.setDaemon(true);
        thread.start();
    }

    public static void run(ClassLoader owner, Runnable callback) {
        if (!MidletRuntime.isAppActive(owner)) {
            return;
        }
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(owner == null ? AudioCallbacks.class.getClassLoader() : owner);
            callback.run();
        } catch (RuntimeException exception) {
            DebugLog.log(LogCategory.AUDIO, AudioCallbacks.class.getName(),
                    "Audio listener failed: " + exception);
        } catch (Error error) {
            if (!MidletRuntime.isExpectedShutdownThrowable(error)) {
                throw error;
            }
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private static final class HostWorker extends Thread {
        private HostWorker(Runnable action, String name) {
            super(null, action, name, 0, false);
        }
    }
}
