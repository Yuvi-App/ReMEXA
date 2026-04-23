package remexa.host.runtime;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import remexa.host.jad.JadDescriptor;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public final class AppRuntime {
    public LaunchResult launch(JadDescriptor descriptor) throws LaunchException {
        var jarPath = descriptor.resolveJarPath()
                .orElseThrow(() -> new LaunchException("No JAR path was found in the JAD."));
        var entryClass = descriptor.entryClassName()
                .orElseThrow(() -> new LaunchException("No entry class was found in the JAD."));

        if (!jarPath.toFile().exists()) {
            throw new LaunchException("Resolved JAR does not exist: " + jarPath);
        }

        DebugLog.log(LogCategory.HOST, AppRuntime.class.getName(), "Launching " + descriptor.title() + " from " + jarPath);

        try {
            var classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());
            var appClass = classLoader.loadClass(entryClass);
            Object instance;
            MIDlet midlet = null;
            MidletRuntime.beginInstantiation(descriptor);
            try {
                instance = appClass.getDeclaredConstructor().newInstance();
            } finally {
                MidletRuntime.endInstantiation();
            }

            if (instance instanceof MIDlet typedMidlet) {
                midlet = typedMidlet;
                try {
                    var startApp = MIDlet.class.getDeclaredMethod("startApp");
                    startApp.setAccessible(true);
                    startApp.invoke(midlet);
                } catch (InvocationTargetException exception) {
                    if (exception.getTargetException() instanceof MIDletStateChangeException stateChangeException) {
                        throw new LaunchException("MIDlet refused to start.", stateChangeException);
                    }
                    throw new LaunchException("MIDlet start failed.", exception.getTargetException());
                } catch (ReflectiveOperationException exception) {
                    throw new LaunchException("Failed to invoke MIDlet lifecycle.", exception);
                }
            }

            return new LaunchResult(descriptor, jarPath.toAbsolutePath().toString(), entryClass, classLoader, instance, midlet);
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof MIDletStateChangeException stateChangeException) {
                    throw new LaunchException("MIDlet refused to start.", stateChangeException);
            }
            throw new LaunchException("App constructor threw an exception.", exception.getTargetException());
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new LaunchException("Failed to launch app.", exception);
        }
    }
}
