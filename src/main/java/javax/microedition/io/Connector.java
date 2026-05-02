package javax.microedition.io;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.j_phone.io.BrowserConnection;
import com.j_phone.io.InputRandomAccess;
import com.j_phone.io.StorageConnection;
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
        if ("camera".equals(scheme)) {
            return new com.j_phone.io.HostedCameraConnection();
        }
        if ("url".equals(scheme) || "urls".equals(scheme)) {
            return new BrowserConnectionAdapter(target);
        }
        if ("file".equals(scheme)) {
            return new FileStorageConnection(target, mode);
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

    private static InputStream wrapLegacyResourceStream(InputStream input) {
        return input == null ? null : new LegacyResourceInputStream(input);
    }

    private static final class LegacyResourceInputStream extends FilterInputStream {
        private LegacyResourceInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            int firstRead = super.read(buffer, offset, length);
            if (firstRead <= 0) {
                return firstRead;
            }
            int totalRead = firstRead;
            while (totalRead < length) {
                int count = super.read(buffer, offset + totalRead, length - totalRead);
                if (count <= 0) {
                    break;
                }
                totalRead += count;
            }
            return totalRead;
        }
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
            return wrapLegacyResourceStream(input);
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

    private static final class BrowserConnectionAdapter implements BrowserConnection {
        private final String target;
        private boolean closed;
        private boolean connected;

        private BrowserConnectionAdapter(String target) {
            this.target = target;
        }

        @Override
        public void connect() throws IOException {
            ensureOpen();
            String resolvedUrl = switch (extractScheme(target)) {
                case "url" -> "http:" + target.substring("url:".length());
                case "urls" -> "https:" + target.substring("urls:".length());
                default -> throw new IOException("Unsupported browser target: " + target);
            };

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("Platform browsing is not supported: " + resolvedUrl);
            }

            try {
                Desktop.getDesktop().browse(new URI(resolvedUrl));
                connected = true;
            } catch (IllegalArgumentException | SecurityException | URISyntaxException exception) {
                throw new IOException("Platform cannot handle URL: " + resolvedUrl, exception);
            }
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

    private static final class FileStorageConnection implements StorageConnection {
        private final com.j_phone.io.StoragePathSupport.StorageTarget target;
        private final int mode;
        private boolean closed;
        private boolean streamOpen;

        private FileStorageConnection(String name, int mode) throws IOException {
            this.target = com.j_phone.io.StoragePathSupport.resolve(name);
            this.mode = mode;
        }

        @Override
        public boolean exists() throws IOException {
            ensureOpen();
            return Files.exists(target.realPath());
        }

        @Override
        public int getType() throws IOException {
            ensureOpen();
            Path path = target.realPath();
            if (Files.isDirectory(path)) {
                return StorageConnection.TYPE_FOLDER;
            }
            String lowerName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".zip")) {
                return StorageConnection.TYPE_ZIP;
            }
            if (lowerName.endsWith(".htm") || lowerName.endsWith(".html")) {
                return StorageConnection.TYPE_HTML;
            }
            if (lowerName.endsWith(".png")) {
                return StorageConnection.TYPE_PNG;
            }
            if (lowerName.endsWith(".mng")) {
                return StorageConnection.TYPE_MNG;
            }
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpe") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".jpz")) {
                return StorageConnection.TYPE_JPEG;
            }
            if (lowerName.endsWith(".mmf") || lowerName.endsWith(".smaf")) {
                return StorageConnection.TYPE_SMAF;
            }
            if (lowerName.endsWith(".smd")) {
                return StorageConnection.TYPE_SMD;
            }
            if (lowerName.endsWith(".txt")) {
                return StorageConnection.TYPE_TXT;
            }
            if (lowerName.endsWith(".vcf")) {
                return StorageConnection.TYPE_VCARD;
            }
            if (lowerName.endsWith(".vbm")) {
                return StorageConnection.TYPE_VBOOKMARK;
            }
            if (lowerName.endsWith(".vcs")) {
                return StorageConnection.TYPE_VCALENDAR;
            }
            if (lowerName.endsWith(".vmg")) {
                return StorageConnection.TYPE_VMESSAGE;
            }
            if (lowerName.endsWith(".vnt")) {
                return StorageConnection.TYPE_VNOTE;
            }
            if (lowerName.endsWith(".eml")) {
                return StorageConnection.TYPE_EML;
            }
            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".m4v")) {
                return StorageConnection.TYPE_MP4;
            }
            if (lowerName.endsWith(".gif")) {
                return StorageConnection.TYPE_GIF;
            }
            if (lowerName.endsWith(".svg")) {
                return StorageConnection.TYPE_SVG;
            }
            if (lowerName.endsWith(".swf")) {
                return StorageConnection.TYPE_SWF;
            }
            if (lowerName.endsWith(".amr")) {
                return StorageConnection.TYPE_AMR;
            }
            if (lowerName.endsWith(".mp3")) {
                return StorageConnection.TYPE_MP3;
            }
            if (lowerName.endsWith(".jar") || lowerName.endsWith(".jad") || lowerName.endsWith(".class")) {
                return StorageConnection.TYPE_JAVA;
            }
            if (lowerName.endsWith(".pdf")) {
                return StorageConnection.TYPE_PDF;
            }
            if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
                return StorageConnection.TYPE_WORD;
            }
            if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
                return StorageConnection.TYPE_EXCEL;
            }
            if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
                return StorageConnection.TYPE_POWERPOINT;
            }
            return StorageConnection.TYPE_OTHER;
        }

        @Override
        public String getTypeString() throws IOException {
            return switch (getType()) {
                case StorageConnection.TYPE_FOLDER -> "folder";
                case StorageConnection.TYPE_ZIP -> "zip";
                case StorageConnection.TYPE_HTML -> "html";
                case StorageConnection.TYPE_PNG -> "png";
                case StorageConnection.TYPE_MNG -> "mng";
                case StorageConnection.TYPE_JPEG -> "jpeg";
                case StorageConnection.TYPE_SMAF -> "smaf";
                case StorageConnection.TYPE_SMD -> "smd";
                case StorageConnection.TYPE_TXT -> "txt";
                case StorageConnection.TYPE_VCARD -> "vcard";
                case StorageConnection.TYPE_VBOOKMARK -> "vbookmark";
                case StorageConnection.TYPE_VCALENDAR -> "vcalendar";
                case StorageConnection.TYPE_VMESSAGE -> "vmessage";
                case StorageConnection.TYPE_VNOTE -> "vnote";
                case StorageConnection.TYPE_EML -> "eml";
                case StorageConnection.TYPE_MP4 -> "mp4";
                case StorageConnection.TYPE_GIF -> "gif";
                case StorageConnection.TYPE_SVG -> "svg";
                case StorageConnection.TYPE_SWF -> "swf";
                case StorageConnection.TYPE_AMR -> "amr";
                case StorageConnection.TYPE_MP3 -> "mp3";
                case StorageConnection.TYPE_JAVA -> "java";
                case StorageConnection.TYPE_PDF -> "pdf";
                case StorageConnection.TYPE_WORD -> "word";
                case StorageConnection.TYPE_EXCEL -> "excel";
                case StorageConnection.TYPE_POWERPOINT -> "powerpoint";
                default -> "other";
            };
        }

        @Override
        public long getLength() throws IOException {
            ensureOpen();
            if (!Files.exists(target.realPath()) || Files.isDirectory(target.realPath())) {
                return 0L;
            }
            return Files.size(target.realPath());
        }

        @Override
        public boolean isFolder() throws IOException {
            ensureOpen();
            return Files.isDirectory(target.realPath());
        }

        @Override
        public boolean isFile() throws IOException {
            ensureOpen();
            return Files.isRegularFile(target.realPath());
        }

        @Override
        public boolean isCopyrighted() {
            return false;
        }

        @Override
        public int getCopyrightedDataVersion() {
            return 0;
        }

        @Override
        public String getCopyrightedDataContentType() {
            return null;
        }

        @Override
        public String getCopyrightedDataHeader(String name) {
            return null;
        }

        @Override
        public String[] list() throws IOException {
            ensureOpen();
            if (!Files.isDirectory(target.realPath())) {
                throw new IOException("Storage path is not a folder: " + target.logicalPath());
            }
            try (Stream<Path> children = Files.list(target.realPath())) {
                return children
                        .sorted()
                        .map(path -> {
                            String childName = path.getFileName() == null ? "" : path.getFileName().toString();
                            return Files.isDirectory(path) ? childName + "/" : childName;
                        })
                        .toArray(String[]::new);
            }
        }

        @Override
        public boolean createFolder() throws IOException {
            ensureOpen();
            if (Files.exists(target.realPath())) {
                return Files.isDirectory(target.realPath());
            }
            Files.createDirectories(target.realPath());
            return true;
        }

        @Override
        public boolean renameTo(String newName) throws IOException {
            ensureOpen();
            if (newName == null || newName.isBlank()) {
                throw new IOException("New name is empty.");
            }
            if (newName.indexOf('/') >= 0 || newName.indexOf('\\') >= 0) {
                throw new IOException("New name must not contain path separators.");
            }
            Path current = target.realPath();
            Path parent = current.getParent();
            if (parent == null) {
                throw new IOException("Cannot rename storage root.");
            }
            Files.move(current, parent.resolve(newName));
            return true;
        }

        @Override
        public boolean delete() throws IOException {
            ensureOpen();
            if (!Files.exists(target.realPath())) {
                return false;
            }
            deleteRecursively(target.realPath());
            return true;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            ensureOpen();
            ensureReadable();
            ensureNoOpenStream();
            if (!Files.isRegularFile(target.realPath())) {
                throw new IOException("Storage file does not exist: " + target.logicalPath());
            }
            return registerStream(Files.newInputStream(target.realPath(), StandardOpenOption.READ));
        }

        @Override
        public InputRandomAccess openInputRandomAccess() throws IOException {
            ensureOpen();
            ensureReadable();
            ensureNoOpenStream();
            if (!Files.isRegularFile(target.realPath())) {
                throw new IOException("Storage file does not exist: " + target.logicalPath());
            }
            streamOpen = true;
            return new FileInputRandomAccess(target.realPath(), this::onStreamClosed);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            ensureOpen();
            ensureWritable();
            ensureNoOpenStream();
            if (target.directoryHint()) {
                throw new IOException("Cannot open output stream for folder path: " + target.logicalPath());
            }
            Path parent = target.realPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return registerStream(Files.newOutputStream(
                    target.realPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        }

        @Override
        public String getApplicationDescription(String attribute) {
            return null;
        }

        @Override
        public void close() {
            closed = true;
        }

        private <T extends java.io.Closeable> T registerStream(T stream) {
            streamOpen = true;
            if (stream instanceof InputStream inputStream) {
                return (T) new StorageInputStream(inputStream, this::onStreamClosed);
            }
            if (stream instanceof OutputStream outputStream) {
                return (T) new StorageOutputStream(outputStream, this::onStreamClosed);
            }
            return stream;
        }

        private void onStreamClosed() {
            streamOpen = false;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Connection is closed.");
            }
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

        private void ensureNoOpenStream() throws IOException {
            if (streamOpen) {
                throw new IOException("A storage stream is already open.");
            }
        }

        private static void deleteRecursively(Path path) throws IOException {
            if (Files.isDirectory(path)) {
                try (Stream<Path> children = Files.list(path)) {
                    for (Path child : children.toList()) {
                        deleteRecursively(child);
                    }
                }
            }
            Files.deleteIfExists(path);
        }
    }

    private static final class StorageInputStream extends FilterInputStream {
        private final Runnable onClose;
        private boolean closed;

        private StorageInputStream(InputStream input, Runnable onClose) {
            super(input);
            this.onClose = onClose;
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
                onClose.run();
            }
        }
    }

    private static final class StorageOutputStream extends FilterOutputStream {
        private final Runnable onClose;
        private boolean closed;

        private StorageOutputStream(OutputStream output, Runnable onClose) {
            super(output);
            this.onClose = onClose;
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
                onClose.run();
            }
        }
    }

    private static final class FileInputRandomAccess extends InputRandomAccess {
        private final RandomAccessFile delegate;
        private final Runnable onClose;
        private boolean closed;

        private FileInputRandomAccess(Path path, Runnable onClose) throws IOException {
            this.delegate = new RandomAccessFile(path.toFile(), "r");
            this.onClose = onClose;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return delegate.read(buffer, offset, length);
        }

        @Override
        public long getPosition() throws IOException {
            return delegate.getFilePointer();
        }

        @Override
        public long setPosition(int from, long position) throws IOException {
            long destination = switch (from) {
                case com.j_phone.io.RandomAccess.SEEK_SET -> position;
                case com.j_phone.io.RandomAccess.SEEK_CUR -> delegate.getFilePointer() + position;
                case com.j_phone.io.RandomAccess.SEEK_END -> delegate.length() + position;
                default -> throw new IOException("Unsupported seek origin: " + from);
            };
            delegate.seek(Math.max(0L, destination));
            return delegate.getFilePointer();
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                delegate.close();
            } finally {
                onClose.run();
            }
        }
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
