package remexa.host.runtime;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

final class LegacyJarClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final Object resourceStreamLock = new Object();
    private final Set<LegacyResourceStream> resourceStreams = new HashSet<>();
    private final URL jarUrl;
    private boolean closed;

    LegacyJarClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[]{jarUrl}, parent);
        this.jarUrl = jarUrl;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        var normalizedName = normalizeResourceName(name);
        var resource = getResource(normalizedName);
        if (resource == null) {
            return openArchivedResource(normalizedName);
        }
        return openResourceStream(normalizedName, resource);
    }

    private InputStream openResourceStream(String name, URL resource) {
        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            return openResourceStream(connection.getInputStream(), connection.getContentLengthLong());
        } catch (IOException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    LegacyJarClassLoader.class.getName(),
                    "Unable to open resource stream for " + name + ": " + describeException(exception)
            );
            return null;
        }
    }

    private InputStream openResourceStream(InputStream input, long contentLength) throws IOException {
        var stream = new LegacyResourceStream(this, input, contentLength);
        if (!registerResourceStream(stream)) {
            stream.close();
            return null;
        }
        return new DataInputStream(stream);
    }

    private InputStream openArchivedResource(String name) {
        Path jarPath;
        try {
            jarPath = Path.of(jarUrl.toURI());
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return null;
        }

        try (var jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !isZipDatResource(entry.getName())) {
                    continue;
                }

                try (var zip = new ZipInputStream(jar.getInputStream(entry))) {
                    java.util.zip.ZipEntry zippedEntry;
                    while ((zippedEntry = zip.getNextEntry()) != null) {
                        if (zippedEntry.isDirectory() || !resourceNameMatches(name, zippedEntry.getName())) {
                            continue;
                        }
                        byte[] data = zip.readAllBytes();
                        DebugLog.log(
                                LogCategory.HOST,
                                LegacyJarClassLoader.class.getName(),
                                "Loaded archived resource " + name + " from " + entry.getName()
                        );
                        return openResourceStream(new ByteArrayInputStream(data), data.length);
                    }
                } catch (IOException exception) {
                    DebugLog.log(
                            LogCategory.HOST,
                            LegacyJarClassLoader.class.getName(),
                            "Unable to inspect resource archive " + entry.getName() + ": " + describeException(exception)
                    );
                }
            }
        } catch (IOException exception) {
            DebugLog.log(
                    LogCategory.HOST,
                    LegacyJarClassLoader.class.getName(),
                    "Unable to scan archived resources: " + describeException(exception)
            );
        }
        return null;
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

    private static String normalizeResourceName(String name) {
        if (name == null) {
            return "";
        }
        var normalizedName = name;
        while (normalizedName.startsWith("/")) {
            normalizedName = normalizedName.substring(1);
        }
        return normalizedName;
    }

    private static boolean isZipDatResource(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".zip.dat");
    }

    private static boolean resourceNameMatches(String requestedName, String archiveName) {
        var normalizedRequestedName = normalizeResourceName(requestedName);
        var normalizedArchiveName = normalizeResourceName(archiveName);
        if (normalizedArchiveName.equals(normalizedRequestedName)) {
            return true;
        }
        int requestedFileNameIndex = normalizedRequestedName.lastIndexOf('/');
        var requestedFileName = requestedFileNameIndex < 0
                ? normalizedRequestedName
                : normalizedRequestedName.substring(requestedFileNameIndex + 1);
        int archiveFileNameIndex = normalizedArchiveName.lastIndexOf('/');
        var archiveFileName = archiveFileNameIndex < 0
                ? normalizedArchiveName
                : normalizedArchiveName.substring(archiveFileNameIndex + 1);
        return archiveFileName.equals(requestedFileName);
    }

    private static String describeException(Throwable exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
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
