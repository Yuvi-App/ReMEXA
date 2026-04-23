package javax.microedition.io;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import org.recompile.mobile.Mobile;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public final class Connector {
    private static final String HTTP_LOG_SOURCE = Connector.class.getName() + "$HttpConnectionAdapter";

    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    private Connector() {
    }

    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) throws IOException {
        return open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts) throws IOException {
        String target = normalizeName(name);
        if (target.regionMatches(true, 0, "resource:", 0, "resource:".length())) {
            return new ResourceConnection(target);
        }
        String scheme = extractScheme(target);
        if ("jar".equals(scheme)) {
            return new JarStoreConnection(target);
        }
        if ("http".equals(scheme) || "https".equals(scheme)) {
            return new HttpConnectionAdapter(target, mode, timeouts);
        }
        throw new IOException("Unsupported Connector target: " + name);
    }

    public static InputStream openInputStream(String name) throws IOException {
        Connection connection = open(name, READ, false);
        if (!(connection instanceof InputConnection inputConnection)) {
            connection.close();
            throw new IOException("Connection does not support input: " + name);
        }
        return inputConnection.openInputStream();
    }

    public static OutputStream openOutputStream(String name) throws IOException {
        Connection connection = open(name, WRITE, false);
        if (!(connection instanceof OutputConnection outputConnection)) {
            connection.close();
            throw new IOException("Connection does not support output: " + name);
        }
        return outputConnection.openOutputStream();
    }

    public static DataInputStream openDataInputStream(String name) throws IOException {
        return new DataInputStream(openInputStream(name));
    }

    public static DataOutputStream openDataOutputStream(String name) throws IOException {
        return new DataOutputStream(openOutputStream(name));
    }

    private static String normalizeName(String name) throws IOException {
        if (name == null) {
            throw new IOException("Connection target is null.");
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new IOException("Connection target is empty.");
        }
        return normalized;
    }

    private static String extractScheme(String name) {
        int separator = name.indexOf(':');
        if (separator <= 0) {
            return "";
        }
        return name.substring(0, separator).toLowerCase(Locale.ROOT);
    }

    private static final class ResourceConnection implements InputConnection {
        private final String resourceName;
        private boolean closed;

        private ResourceConnection(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            ensureOpen();
            InputStream input = Mobile.getMIDletResourceAsStream(resourceName);
            if (input == null) {
                throw new IOException("Missing resource: " + resourceName);
            }
            return input;
        }

        @Override
        public void close() {
            closed = true;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Connection is closed.");
            }
        }
    }

    private static final class HttpConnectionAdapter implements HttpConnection {
        private final URL url;
        private final int mode;
        private final boolean timeouts;
        private HttpURLConnection delegate;
        private String requestMethod = GET;
        private boolean requestLogged;
        private boolean responseLogged;

        private HttpConnectionAdapter(String target, int mode, boolean timeouts) throws IOException {
            this.url = new URL(target);
            this.mode = mode;
            this.timeouts = timeouts;
            log("open(" + url.toExternalForm() + ", mode=" + modeName(mode) + ", timeouts=" + timeouts + ")");
        }

        @Override
        public long getDate() throws IOException {
            return connection().getDate();
        }

        @Override
        public String getEncoding() {
            return connection().getContentEncoding();
        }

        @Override
        public long getExpiration() throws IOException {
            return connection().getExpiration();
        }

        @Override
        public String getFile() {
            return url.getFile();
        }

        @Override
        public String getHeaderField(int n) throws IOException {
            return connection().getHeaderField(n);
        }

        @Override
        public String getHeaderField(String name) throws IOException {
            return connection().getHeaderField(name);
        }

        @Override
        public long getHeaderFieldDate(String name, long def) throws IOException {
            return connection().getHeaderFieldDate(name, def);
        }

        @Override
        public int getHeaderFieldInt(String name, int def) throws IOException {
            return connection().getHeaderFieldInt(name, def);
        }

        @Override
        public String getHeaderFieldKey(int n) throws IOException {
            return connection().getHeaderFieldKey(n);
        }

        @Override
        public String getHost() {
            return url.getHost();
        }

        @Override
        public long getLastModified() throws IOException {
            return connection().getLastModified();
        }

        @Override
        public long getLength() {
            return connection().getContentLengthLong();
        }

        @Override
        public int getPort() {
            return url.getPort();
        }

        @Override
        public String getProtocol() {
            return url.getProtocol();
        }

        @Override
        public String getQuery() {
            return url.getQuery();
        }

        @Override
        public String getRef() {
            return url.getRef();
        }

        @Override
        public String getRequestMethod() {
            return requestMethod;
        }

        @Override
        public String getRequestProperty(String key) {
            return connection().getRequestProperty(key);
        }

        @Override
        public int getResponseCode() throws IOException {
            int responseCode = connection().getResponseCode();
            logResponse("response " + responseCode + " " + safeResponseMessage());
            return responseCode;
        }

        @Override
        public String getResponseMessage() throws IOException {
            return connection().getResponseMessage();
        }

        @Override
        public String getType() {
            return connection().getContentType();
        }

        @Override
        public String getURL() {
            return url.toExternalForm();
        }

        @Override
        public void setRequestMethod(String method) throws IOException {
            requestMethod = method;
            connection().setRequestMethod(method);
            log("setRequestMethod(" + method + ")");
        }

        @Override
        public void setRequestProperty(String key, String value) throws IOException {
            connection().setRequestProperty(key, value);
            log("setRequestProperty(" + key + "=" + value + ")");
        }

        @Override
        public InputStream openInputStream() throws IOException {
            ensureReadable();
            logRequest();
            return new LoggingInputStream(connection().getInputStream(), this);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            ensureWritable();
            logRequest();
            return new LoggingOutputStream(connection().getOutputStream(), this);
        }

        @Override
        public void close() {
            log("close()");
            if (delegate != null) {
                delegate.disconnect();
            }
        }

        private HttpURLConnection connection() {
            if (delegate == null) {
                try {
                    delegate = (HttpURLConnection) url.openConnection();
                    delegate.setRequestMethod(requestMethod);
                    delegate.setDoInput((mode & READ) != 0 || mode == READ_WRITE);
                    delegate.setDoOutput((mode & WRITE) != 0 || mode == READ_WRITE);
                    if (timeouts) {
                        delegate.setConnectTimeout(30000);
                        delegate.setReadTimeout(30000);
                    }
                    log("delegate opened for " + url.toExternalForm());
                } catch (IOException exception) {
                    log("delegate open failed: " + exception.getMessage());
                    throw new IllegalStateException("Failed to open HTTP connection for " + url, exception);
                }
            }
            return delegate;
        }

        private void ensureReadable() throws IOException {
            if (mode != READ && mode != READ_WRITE) {
                throw new IOException("Connection is not open for input.");
            }
        }

        private void ensureWritable() throws IOException {
            if (mode != WRITE && mode != READ_WRITE) {
                throw new IOException("Connection is not open for output.");
            }
        }

        private void logRequest() {
            if (requestLogged) {
                return;
            }
            requestLogged = true;
            log("request " + requestMethod + " " + url.toExternalForm());
        }

        private void logResponse(String message) {
            if (responseLogged) {
                return;
            }
            responseLogged = true;
            log(message);
        }

        private String safeResponseMessage() {
            try {
                return String.valueOf(connection().getResponseMessage());
            } catch (IOException exception) {
                return "<unavailable:" + exception.getMessage() + ">";
            }
        }

        private void log(String message) {
            DebugLog.log(LogCategory.IO, HTTP_LOG_SOURCE, message);
        }
    }

    private static final class LoggingInputStream extends FilterInputStream {
        private final HttpConnectionAdapter owner;
        private long bytesRead;
        private boolean closed;

        private LoggingInputStream(InputStream delegate, HttpConnectionAdapter owner) {
            super(delegate);
            this.owner = owner;
            owner.log("openInputStream()");
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
            int count = super.read(buffer, offset, length);
            if (count > 0) {
                bytesRead += count;
            }
            return count;
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
                owner.log("input stream closed after " + bytesRead + " byte(s)");
            }
        }
    }

    private static final class LoggingOutputStream extends FilterOutputStream {
        private final HttpConnectionAdapter owner;
        private long bytesWritten;
        private boolean closed;

        private LoggingOutputStream(OutputStream delegate, HttpConnectionAdapter owner) {
            super(delegate);
            this.owner = owner;
            owner.log("openOutputStream()");
        }

        @Override
        public void write(int value) throws IOException {
            super.write(value);
            bytesWritten++;
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            super.write(buffer, offset, length);
            bytesWritten += length;
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
                owner.log("output stream closed after " + bytesWritten + " byte(s)");
            }
        }
    }

    private static String modeName(int mode) {
        return switch (mode) {
            case READ -> "READ";
            case WRITE -> "WRITE";
            case READ_WRITE -> "READ_WRITE";
            default -> Integer.toString(mode);
        };
    }

    private static final class JarStoreConnection implements InputConnection {
        private final String storeName;
        private final String entryName;
        private boolean closed;

        private JarStoreConnection(String target) throws IOException {
            String spec = target.substring("jar://".length());
            int separator = spec.indexOf('/');
            if (separator <= 0 || separator == spec.length() - 1) {
                throw new IOException("Invalid jar connector target: " + target);
            }
            this.storeName = spec.substring(0, separator);
            this.entryName = normalizeEntryName(spec.substring(separator + 1));
        }

        @Override
        public InputStream openInputStream() throws IOException {
            ensureOpen();
            byte[] archive = readArchive();
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (!entryName.equals(normalizeEntryName(entry.getName()))) {
                        continue;
                    }
                    return new ByteArrayInputStream(zip.readAllBytes());
                }
            }
            throw new IOException("Missing jar entry: " + entryName + " in " + storeName);
        }

        @Override
        public void close() {
            closed = true;
        }

        private byte[] readArchive() throws IOException {
            try {
                RecordStore store = RecordStore.openRecordStore(storeName, false);
                try {
                    if (store.getNumRecords() <= 0) {
                        throw new IOException("Jar store is empty: " + storeName);
                    }
                    return store.getRecord(1);
                } finally {
                    store.closeRecordStore();
                }
            } catch (RecordStoreException exception) {
                throw new IOException("Unable to open jar store: " + storeName, exception);
            }
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Connection is closed.");
            }
        }

        private static String normalizeEntryName(String entryName) {
            String normalized = entryName.replace('\\', '/');
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }
    }
}
