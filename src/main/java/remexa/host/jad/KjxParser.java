package remexa.host.jad;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.jar.JarFile;

public final class KjxParser {
    private KjxParser() {
    }

    public static JadDescriptor parse(Path path) throws IOException {
        var localAppData = System.getenv("LOCALAPPDATA");
        var cacheRoot = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".remexa", "cache", "kjx")
                : Path.of(localAppData, "ReMEXA", "cache", "kjx");
        return parse(path, cacheRoot);
    }

    static JadDescriptor parse(Path path, Path cacheRoot) throws IOException {
        var source = path.toAbsolutePath().normalize();
        var digest = newDigest();
        Path temporaryJar = null;
        try (var input = new DataInputStream(new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(source)), digest))) {
            if (input.readUnsignedByte() != 'K' || input.readUnsignedByte() != 'J' || input.readUnsignedByte() != 'X') {
                throw new IOException("Invalid KJX signature.");
            }
            var headerLength = input.readUnsignedByte();
            if (headerLength < 8) {
                throw new IOException("Invalid KJX header length.");
            }
            var header = readBytes(input, headerLength - 4);
            int jadLength;
            try (var fields = new DataInputStream(new ByteArrayInputStream(header))) {
                // Embedded names are metadata, never host filesystem paths.
                fields.skipNBytes(fields.readUnsignedByte());
                jadLength = fields.readUnsignedShort();
                fields.skipNBytes(fields.readUnsignedByte());
            }
            var descriptor = JadParser.parse(source, readBytes(input, jadLength));
            if (descriptor.properties().isEmpty()) {
                throw new IOException("KJX contains an empty or invalid JAD.");
            }
            Files.createDirectories(cacheRoot);
            temporaryJar = Files.createTempFile(cacheRoot, ".kjx-", ".jar");
            long jarLength;
            try (var output = Files.newOutputStream(temporaryJar)) {
                jarLength = input.transferTo(output);
            }
            validateJarSize(descriptor, jarLength);
            try (var jar = new JarFile(temporaryJar.toFile())) {
                jar.getManifest();
            }
            var cacheDirectory = cacheRoot.resolve(HexFormat.of().formatHex(digest.digest()));
            Files.createDirectories(cacheDirectory);
            var cachedJar = cacheDirectory.resolve("app.jar");
            if (!Files.isRegularFile(cachedJar) || Files.mismatch(temporaryJar, cachedJar) != -1) {
                try {
                    Files.move(temporaryJar, cachedJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporaryJar, cachedJar, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return new JadDescriptor(source, descriptor.properties(), descriptor.midlets(), cachedJar.toAbsolutePath());
        } catch (EOFException exception) {
            throw new IOException("Truncated KJX header or JAD.", exception);
        } finally {
            if (temporaryJar != null) {
                Files.deleteIfExists(temporaryJar);
            }
        }
    }

    private static byte[] readBytes(DataInputStream input, int length) throws IOException {
        var bytes = new byte[length];
        input.readFully(bytes);
        return bytes;
    }

    private static void validateJarSize(JadDescriptor descriptor, long actualSize) throws IOException {
        var declaredSize = descriptor.property("MIDlet-Jar-Size");
        if (declaredSize.isEmpty()) {
            return;
        }
        try {
            if (Long.parseLong(declaredSize.get()) != actualSize) {
                throw new IOException("KJX JAR size does not match MIDlet-Jar-Size.");
            }
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid MIDlet-Jar-Size in KJX descriptor.", exception);
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
