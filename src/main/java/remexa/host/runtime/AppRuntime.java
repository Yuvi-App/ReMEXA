package remexa.host.runtime;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Consumer;
import remexa.host.jad.JadDescriptor;
import remexa.host.profile.DisplayMetrics;
import remexa.host.profile.LaunchProfile;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public final class AppRuntime {
    public LaunchResult launch(
            JadDescriptor descriptor,
            LaunchProfile launchProfile,
            Consumer<DisplayMetrics> displayListener
    ) throws LaunchException {
        var jarPath = descriptor.resolveJarPath()
                .orElseThrow(() -> new LaunchException("No JAR path was found in the JAD."));
        var entryClass = descriptor.entryClassName()
                .orElseThrow(() -> new LaunchException("No entry class was found in the JAD."));

        if (!jarPath.toFile().exists()) {
            throw new LaunchException("Resolved JAR does not exist: " + jarPath);
        }

        DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Launching " + descriptor.title() + " from " + jarPath);
        DebugLog.log(
                LogCategory.HOST,
                AppRuntime.class.getName(),
                "Using profile " + launchProfile.profile().displayName() + " with initial display " + launchProfile.initialDisplay().dimensions()
        );

        try {
            var classLoader = new LegacyJarClassLoader(jarPath.toUri().toURL(), getClass().getClassLoader());
            var appClass = classLoader.loadClass(entryClass);
            Object instance;
            MIDlet midlet = null;
            SystemPropertyProfile.apply(launchProfile.profile());
            MidletRuntime.beginInstantiation(descriptor, launchProfile, classLoader, displayListener);
            var originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                instance = appClass.getDeclaredConstructor().newInstance();
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                MidletRuntime.endInstantiation();
            }

            if (instance instanceof MIDlet typedMidlet) {
                midlet = typedMidlet;
                try {
                    var startApp = MIDlet.class.getDeclaredMethod("startApp");
                    startApp.setAccessible(true);
                    var originalStartContextClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(classLoader);
                        startApp.invoke(midlet);
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalStartContextClassLoader);
                    }
                } catch (InvocationTargetException exception) {
                    if (exception.getTargetException() instanceof MIDletStateChangeException stateChangeException) {
                        throw new LaunchException("MIDlet refused to start.", stateChangeException);
                    }
                    throw new LaunchException("MIDlet start failed.", exception.getTargetException());
                } catch (ReflectiveOperationException exception) {
                    throw new LaunchException("Failed to invoke MIDlet lifecycle.", exception);
                }
            }

            return new LaunchResult(descriptor, launchProfile, jarPath.toAbsolutePath().toString(), entryClass, classLoader, instance, midlet);
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof MIDletStateChangeException stateChangeException) {
                    throw new LaunchException("MIDlet refused to start.", stateChangeException);
            }
            throw new LaunchException("App constructor threw an exception.", exception.getTargetException());
        } catch (LinkageError exception) {
            throw new LaunchException("App class verification failed.", exception);
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new LaunchException("Failed to launch app.", exception);
        }
    }

    public void shutdown(LaunchResult result) {
        if (result == null) {
            return;
        }

        var midlet = result.midlet();
        if (midlet != null) {
            invokeLifecycle(midlet, "pauseApp");
            invokeLifecycle(midlet, "destroyApp", true);
            midlet.notifyDestroyed();
            MidletRuntime.detach(midlet);
        }

        shutdownPhrasePlayer(result.classLoader());
        shutdownAppThreads(result.classLoader());

        if (result.classLoader() instanceof URLClassLoader urlClassLoader) {
            try {
                urlClassLoader.close();
            } catch (java.io.IOException exception) {
                DebugLog.log(
                        LogCategory.HOST,
                        AppRuntime.class.getName(),
                        "ClassLoader close failed for " + result.descriptor().title() + ": " + exception.getMessage()
                );
            }
        }
    }

    private void shutdownAppThreads(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }

        var currentThread = Thread.currentThread();
        var appThreads = Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(thread -> thread != currentThread)
                .filter(Thread::isAlive)
                .filter(thread -> thread.getContextClassLoader() == classLoader)
                .toList();

        for (var thread : appThreads) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Interrupting app thread " + thread.getName()
            );
            thread.interrupt();
        }

        for (var thread : appThreads) {
            try {
                thread.join(500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        for (var thread : appThreads) {
            if (!thread.isAlive()) {
                continue;
            }
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "App thread still alive after interrupt: " + thread.getName()
            );
        }
    }

    private void shutdownPhrasePlayer(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            var phrasePlayerClass = classLoader.loadClass("com.jblend.media.smaf.phrase.PhrasePlayer");
            var getPlayer = phrasePlayerClass.getMethod("getPlayer");
            var player = getPlayer.invoke(null);
            var kill = phrasePlayerClass.getMethod("kill");
            kill.invoke(player);
            DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Disposed phrase player during shutdown.");
        } catch (ClassNotFoundException ignored) {
            // This app did not use the phrase player classes.
        } catch (ReflectiveOperationException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Failed to dispose phrase player during shutdown: " + exception.getMessage()
            );
        }
    }

    private void invokeLifecycle(MIDlet midlet, String methodName, Object... arguments) {
        try {
            Class<?>[] parameterTypes;
            if (arguments.length == 0) {
                parameterTypes = new Class<?>[0];
            } else {
                parameterTypes = new Class<?>[]{boolean.class};
            }
            var method = MIDlet.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            var originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(midlet.getClass().getClassLoader());
                method.invoke(midlet, arguments);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        } catch (InvocationTargetException exception) {
            var target = exception.getTargetException();
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "MIDlet " + methodName + " failed for " + midlet.getClass().getName() + ": " + target.getMessage()
            );
        } catch (ReflectiveOperationException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Failed to invoke MIDlet " + methodName + " for " + midlet.getClass().getName() + ": " + exception.getMessage()
            );
        }
    }

}
