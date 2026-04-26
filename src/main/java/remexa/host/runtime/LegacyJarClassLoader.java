package remexa.host.runtime;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
    public InputStream getResourceAsStream(String name) {
        var resource = getResource(name);
        if (resource == null) {
            return null;
        }
        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            return new LegacyResourceStream(connection.getInputStream(), connection.getContentLengthLong());
        } catch (IOException exception) {
            return null;
        }
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

    private static final class LegacyResourceStream extends FilterInputStream {
        private final long contentLength;
        private long bytesRead;

        private LegacyResourceStream(InputStream input, long contentLength) {
            super(input);
            this.contentLength = contentLength;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                bytesRead++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return isAtEnd() ? -1 : 0;
            }
            // JSCL/MIDP titles routinely call read(buf, 0, len) once and assume
            // the whole buffer was filled. loop until the buffer is full or EOF is reached.
            int total = 0;
            while (total < length) {
                int chunk = super.read(buffer, offset + total, length - total);
                if (chunk < 0) {
                    break;
                }
                total += chunk;
            }
            if (total > 0) {
                bytesRead += total;
                return total;
            }
            return -1;
        }

        private boolean isAtEnd() throws IOException {
            if (contentLength >= 0) {
                return bytesRead >= contentLength;
            }
            return super.available() <= 0;
        }
    }
}
