package remexa.host.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import javax.microedition.media.Manager;
import remexa.host.JadFrame;
import remexa.host.input.HostTextInputRequest;
import remexa.host.jad.JadDescriptor;
import remexa.host.jad.JadManifestOverlay;
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
            Consumer<DisplayMetrics> displayListener,
            HostTextInputRequest.Handler textInputHandler,
            JadFrame hostFrame
    ) throws LaunchException {
        var jarPath = descriptor.resolveJarPath()
                .orElseThrow(() -> new LaunchException("No JAR path was found in the JAD."));
        var mergedDescriptor = mergeDescriptorWithJarManifest(descriptor, jarPath);
        var entryCandidates = entryClassCandidates(descriptor, jarPath);
        if (entryCandidates.isEmpty()) {
            throw new LaunchException("No entry class was found in the JAD or JAR manifest.");
        }

        if (!jarPath.toFile().exists()) {
            throw new LaunchException("Resolved JAR does not exist: " + jarPath);
        }

        DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Launching " + descriptor.title() + " from " + jarPath);
        DebugLog.log(
                LogCategory.HOST,
                AppRuntime.class.getName(),
                "Using profile " + launchProfile.profile().displayName() + " with initial display " + launchProfile.initialDisplay().dimensions()
        );

        LegacyJarClassLoader classLoader = null;
        var launched = false;
        try {
            classLoader = new LegacyJarClassLoader(jarPath.toUri().toURL(), getClass().getClassLoader());
            MidletRuntime.registerTextInputHandler(classLoader, textInputHandler);
            MidletRuntime.registerHostFrame(classLoader, hostFrame);
            var loadedEntryClass = loadEntryClass(classLoader, entryCandidates);
            var resolvedEntryClass = loadedEntryClass.className();
            var appClass = loadedEntryClass.appClass();
            Object instance;
            MIDlet midlet = null;
            SystemPropertyProfile.apply(launchProfile.profile());
            MidletRuntime.beginInstantiation(mergedDescriptor, launchProfile, classLoader, displayListener);
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

            launched = true;
            return new LaunchResult(mergedDescriptor, launchProfile, jarPath.toAbsolutePath().toString(), resolvedEntryClass, classLoader, instance, midlet);
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof MIDletStateChangeException stateChangeException) {
                    throw new LaunchException("MIDlet refused to start.", stateChangeException);
            }
            throw new LaunchException("App constructor threw an exception.", exception.getTargetException());
        } catch (LinkageError exception) {
            throw new LaunchException("App class verification failed.", exception);
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new LaunchException("Failed to launch app.", exception);
        } finally {
            if (!launched && classLoader != null) {
                MidletRuntime.unregisterTextInputHandler(classLoader);
                MidletRuntime.unregisterHostFrame(classLoader);
                closeClassLoader(classLoader, descriptor.title());
            }
        }
    }

    public void shutdown(LaunchResult result) {
        if (result == null) {
            return;
        }

        var classLoader = result.classLoader();
        MidletRuntime.beginShutdown(classLoader);
        shutdownAudioPlayers(classLoader);

        var midlet = result.midlet();
        if (midlet != null) {
            invokeLifecycle(midlet, "pauseApp");
            invokeLifecycle(midlet, "destroyApp", true);
            midlet.notifyDestroyed();
            MidletRuntime.detach(midlet);
        }

        shutdownAudioPlayers(classLoader);
        shutdownAppThreads(classLoader);
        shutdownAudioPlayers(classLoader);
        MidletRuntime.unregisterTextInputHandler(classLoader);

        closeClassLoader(classLoader, result.descriptor().title());
    }

    private void shutdownAudioPlayers(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        shutdownPhrasePlayer(classLoader);
        shutdownJblendMediaPlayers(classLoader);
        shutdownJphoneMediaPlayers(classLoader);
        shutdownMediaPlayers(classLoader);
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
                .filter(thread -> isAppThread(thread, classLoader))
                .toList();

        installExpectedShutdownHandlers(appThreads);

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

        var stubbornThreads = appThreads.stream()
                .filter(Thread::isAlive)
                .toList();

        for (var thread : stubbornThreads) {
            DebugLog.log(
                        LogCategory.HOST,
                        AppRuntime.class.getName(),
                    "Escalating shutdown for app thread " + thread.getName()
            );
            nudgeThreadForShutdown(thread);
        }

        for (var thread : stubbornThreads) {
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

    private boolean isAppThread(Thread thread, ClassLoader classLoader) {
        if (thread.getContextClassLoader() == classLoader) {
            return true;
        }
        for (var frame : thread.getStackTrace()) {
            if (isClassOwnedBy(frame.getClassName(), classLoader)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassOwnedBy(String className, ClassLoader classLoader) {
        if (className == null || className.isBlank()) {
            return false;
        }
        try {
            return Class.forName(className, false, classLoader).getClassLoader() == classLoader;
        } catch (LinkageError | ClassNotFoundException ignored) {
            return false;
        }
    }

    private void installExpectedShutdownHandlers(java.util.List<Thread> appThreads) {
        for (var thread : appThreads) {
            var previousHandler = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler((currentThread, throwable) -> {
                if (MidletRuntime.isExpectedShutdownThrowable(throwable)) {
                    return;
                }
                if (previousHandler != null) {
                    previousHandler.uncaughtException(currentThread, throwable);
                    return;
                }
                var threadGroup = currentThread.getThreadGroup();
                if (threadGroup != null) {
                    threadGroup.uncaughtException(currentThread, throwable);
                }
            });
        }
    }

    private void shutdownPhrasePlayer(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        var disposed = false;
        for (var phrasePlayerClassName : new String[]{
                "com.j_phone.amuse.PhrasePlayer",
                "com.jblend.media.smaf.phrase.PhrasePlayer"
        }) {
            try {
                var phrasePlayerClass = classLoader.loadClass(phrasePlayerClassName);
                var getPlayer = phrasePlayerClass.getMethod("getPlayer");
                var player = getPlayer.invoke(null);
                invokePhrasePlayerKill(phrasePlayerClass, player, classLoader);
                disposed = true;
            } catch (ClassNotFoundException ignored) {
                // This app did not use this phrase player facade.
            } catch (ReflectiveOperationException exception) {
                DebugLog.log(
                        LogCategory.HOST,
                        AppRuntime.class.getName(),
                        "Failed to dispose phrase player during shutdown (" + phrasePlayerClassName + "): " + exception.getMessage()
                );
            }
        }
        if (disposed) {
            DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Disposed phrase player during shutdown.");
        }
    }

    private void invokePhrasePlayerKill(Class<?> phrasePlayerClass, Object player, ClassLoader classLoader)
            throws ReflectiveOperationException {
        try {
            var killOwnedBy = phrasePlayerClass.getMethod("killOwnedBy", ClassLoader.class);
            killOwnedBy.invoke(player, classLoader);
            return;
        } catch (NoSuchMethodException ignored) {
            // Older facades only expose kill(); keep the compatibility fallback.
        }
        var kill = phrasePlayerClass.getMethod("kill");
        kill.invoke(player);
    }

    private void shutdownJblendMediaPlayers(ClassLoader classLoader) {
        try {
            com.jblend.media.MediaPlayer.shutdownOwnedPlayers(classLoader);
        } catch (RuntimeException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Failed to dispose JBlend media players during shutdown: " + exception.getMessage()
            );
        }
    }

    private void shutdownJphoneMediaPlayers(ClassLoader classLoader) {
        try {
            com.j_phone.media.MediaPlayer.shutdownOwnedPlayers(classLoader);
        } catch (RuntimeException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Failed to dispose J-Phone media players during shutdown: " + exception.getMessage()
            );
        }
    }

    private void shutdownMediaPlayers(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Manager.shutdownOwnedPlayers(classLoader);
            DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Disposed media players during shutdown.");
        } catch (RuntimeException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "Failed to dispose media players during shutdown: " + exception.getMessage()
            );
        }
    }

    private void closeClassLoader(ClassLoader classLoader, String title) {
        if (!(classLoader instanceof URLClassLoader urlClassLoader)) {
            return;
        }
        try {
            urlClassLoader.close();
        } catch (java.io.IOException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    AppRuntime.class.getName(),
                    "ClassLoader close failed for " + title + ": " + exception.getMessage()
            );
        }
    }

    private JadDescriptor mergeDescriptorWithJarManifest(JadDescriptor descriptor, java.nio.file.Path jarPath) throws LaunchException {
        try {
            return JadManifestOverlay.merge(descriptor, jarPath);
        } catch (IOException exception) {
            throw new LaunchException("Failed to read JAR manifest.", exception);
        }
    }

    private List<String> entryClassCandidates(JadDescriptor descriptor, java.nio.file.Path jarPath) throws LaunchException {
        var candidates = new LinkedHashSet<String>();
        descriptor.entryClassName().ifPresent(candidates::add);
        entryClassFromJarManifest(jarPath).ifPresent(candidates::add);
        return List.copyOf(candidates);
    }

    private LoadedEntryClass loadEntryClass(LegacyJarClassLoader classLoader, List<String> entryCandidates)
            throws ClassNotFoundException {
        var missingClassNames = new ArrayList<String>();
        for (int index = 0; index < entryCandidates.size(); index++) {
            var className = entryCandidates.get(index);
            try {
                var appClass = classLoader.loadClass(className);
                if (index > 0) {
                    DebugLog.log(
                            LogCategory.HOST,
                            AppRuntime.class.getName(),
                            "Fell back to JAR manifest entry class: " + className
                    );
                }
                return new LoadedEntryClass(className, appClass);
            } catch (ClassNotFoundException exception) {
                missingClassNames.add(className);
            }
        }
        throw new ClassNotFoundException("Entry class not found: " + String.join(", ", missingClassNames));
    }

    private Optional<String> entryClassFromJarManifest(java.nio.file.Path jarPath) throws LaunchException {
        try (var jarFile = new JarFile(jarPath.toFile())) {
            var manifest = jarFile.getManifest();
            if (manifest == null) {
                return Optional.empty();
            }
            var attributes = manifest.getMainAttributes();
            var manifestMidlet = firstPresentAttribute(attributes, "MIDlet-1", "MIDlet-2", "MIDlet-3", "MIDlet-4");
            if (manifestMidlet.isPresent()) {
                var parsed = parseMidletClassName(manifestMidlet.get());
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
            return firstPresentAttribute(attributes, "AppClass", "KVM-Class-Name", "Main-Class");
        } catch (IOException exception) {
            throw new LaunchException("Failed to read JAR manifest.", exception);
        }
    }

    private static Optional<String> firstPresentAttribute(
            java.util.jar.Attributes attributes,
            String... keys
    ) {
        for (var key : keys) {
            var value = attributes.getValue(key);
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> parseMidletClassName(String manifestMidlet) {
        var parts = manifestMidlet.split(",");
        if (parts.length == 0) {
            return Optional.empty();
        }
        var className = parts[parts.length - 1].trim();
        return className.isEmpty() ? Optional.empty() : Optional.of(className);
    }

    private record LoadedEntryClass(String className, Class<?> appClass) {
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

    private static void nudgeThreadForShutdown(Thread thread) {
        if (thread == null || !thread.isAlive()) {
            return;
        }
        thread.interrupt();
        LockSupport.unpark(thread);
    }

}
