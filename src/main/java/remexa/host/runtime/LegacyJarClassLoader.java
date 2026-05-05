package remexa.host.runtime;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Set;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

final class LegacyJarClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final Object resourceStreamLock = new Object();
    private final Set<LegacyResourceStream> resourceStreams = new HashSet<>();
    private boolean closed;

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
            var stream = new LegacyResourceStream(this, connection.getInputStream(), connection.getContentLengthLong());
            if (!registerResourceStream(stream)) {
                stream.close();
                return null;
            }
            return new DataInputStream(stream);
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        LegacyResourceStream[] streams;
        synchronized (resourceStreamLock) {
            closed = true;
            streams = resourceStreams.toArray(new LegacyResourceStream[0]);
            resourceStreams.clear();
        }

        IOException failure = null;
        for (var stream : streams) {
            try {
                stream.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        try {
            super.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String resourceName = name.replace('.', '/') + ".class";
        URL resource = findResource(resourceName);
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }

        try (var input = openUncachedStream(resource)) {
            byte[] original = input.readAllBytes();
            var switchResult = ClassFileSanitizer.zeroSwitchPadding(original);
            if (switchResult.changes() > 0) {
                DebugLog.log(
                        LogCategory.HOST,
                        LegacyJarClassLoader.class.getName(),
                        "Sanitized " + switchResult.changes() + " switch padding byte(s) in " + name
                );
            }
            var compatibilityResult = ClassFileSanitizer.applyLegacyRuntimeFixes(switchResult.classBytes());
            if (compatibilityResult.changes() > 0) {
                DebugLog.log(
                        LogCategory.HOST,
                        LegacyJarClassLoader.class.getName(),
                        "Applied " + compatibilityResult.changes() + " legacy runtime compatibility rewrite(s) to " + name
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
                    compatibilityResult.classBytes(),
                    0,
                    compatibilityResult.classBytes().length,
                    new CodeSource(resource, (java.security.cert.Certificate[]) null)
            );
        } catch (IOException exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }

    private boolean registerResourceStream(LegacyResourceStream stream) {
        synchronized (resourceStreamLock) {
            if (closed) {
                return false;
            }
            resourceStreams.add(stream);
            return true;
        }
    }

    private void unregisterResourceStream(LegacyResourceStream stream) {
        synchronized (resourceStreamLock) {
            resourceStreams.remove(stream);
        }
    }

    private static InputStream openUncachedStream(URL resource) throws IOException {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private static final class LegacyResourceStream extends FilterInputStream {
        private final LegacyJarClassLoader owner;
        private final long contentLength;
        private long bytesRead;
        private boolean closed;

        private LegacyResourceStream(LegacyJarClassLoader owner, InputStream input, long contentLength) {
            super(input);
            this.owner = owner;
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

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                super.close();
            } finally {
                owner.unregisterResourceStream(this);
            }
        }
    }
}
