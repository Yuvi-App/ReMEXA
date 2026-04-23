package remexa.host.runtime;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

final class LegacyJarClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    LegacyJarClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[]{jarUrl}, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String resourceName = name.replace('.', '/') + ".class";
        URL resource = findResource(resourceName);
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }

        try (var input = resource.openStream()) {
            byte[] original = input.readAllBytes();
            var switchResult = ClassFileSanitizer.zeroSwitchPadding(original);
            if (switchResult.changes() > 0) {
                DebugLog.log(
                        LogCategory.HOST,
                        LegacyJarClassLoader.class.getName(),
                        "Sanitized " + switchResult.changes() + " switch padding byte(s) in " + name
                );
            }
            var spinResult = ClassFileSanitizer.injectSpinLoopHints(switchResult.classBytes());
            if (spinResult.changes() > 0) {
                DebugLog.log(
                        LogCategory.HOST,
                        LegacyJarClassLoader.class.getName(),
                        "Injected " + spinResult.changes() + " spin loop hint(s) into " + name
                );
            }
            int packageSeparator = name.lastIndexOf('.');
            if (packageSeparator > 0) {
                String packageName = name.substring(0, packageSeparator);
                if (getDefinedPackage(packageName) == null) {
                    definePackage(packageName, null, null, null, null, null, null, null);
                }
            }
            return defineClass(
                    name,
                    spinResult.classBytes(),
                    0,
                    spinResult.classBytes().length,
                    new CodeSource(resource, (java.security.cert.Certificate[]) null)
            );
        } catch (IOException exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }
}
