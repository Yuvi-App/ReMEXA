package com.j_phone.io;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StoragePathSupport {
    private static final Path STORAGE_ROOT = initStorageRoot();
    private static final Path EXTERNAL_ROOT = STORAGE_ROOT.resolve("mc");

    private StoragePathSupport() {
    }

    public static StorageTarget resolve(String target) throws IOException {
        String logicalPath = normalizeLogicalPath(target);
        List<String> segments = splitSegments(logicalPath);
        if (segments.isEmpty()) {
            throw new IOException("Invalid storage path: " + target);
        }

        String rootSegment = segments.get(0).toLowerCase(Locale.ROOT);
        Path storageRoot = switch (rootSegment) {
            case "ms" -> ensureRoot(EXTERNAL_ROOT);
            case "mc" -> ensureRoot(EXTERNAL_ROOT);
            default -> throw new IOException("Unsupported storage root: " + logicalPath);
        };

        Path resolved = storageRoot;
        for (int index = 1; index < segments.size(); index++) {
            resolved = resolved.resolve(segments.get(index));
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IOException("Storage path escapes root: " + logicalPath);
        }

        return new StorageTarget(logicalPath, resolved, logicalPath.endsWith("/"), storageRoot);
    }

    public static long getFreeSpace(String target) throws IOException {
        StorageTarget resolved = resolve(target);
        Path candidate = resolved.realPath();
        Path existing = Files.exists(candidate) ? candidate : candidate.getParent();
        if (existing == null) {
            existing = resolved.storageRoot();
        }
        FileStore store = Files.getFileStore(existing);
        return store.getUsableSpace();
    }

    public static Path storageRoot() throws IOException {
        return ensureRoot(STORAGE_ROOT);
    }

    private static Path initStorageRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "ReMEXA", "storage");
        }
        return Paths.get(System.getProperty("user.home"), ".remexa", "storage");
    }

    private static Path ensureRoot(Path root) throws IOException {
        return Files.createDirectories(root);
    }

    private static String normalizeLogicalPath(String target) throws IOException {
        if (target == null) {
            throw new IOException("Storage path is null.");
        }

        String normalized = target.trim().replace('\\', '/');
        if (normalized.regionMatches(true, 0, "file:", 0, "file:".length())) {
            normalized = normalized.substring("file:".length());
        }
        while (normalized.startsWith("//")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private static List<String> splitSegments(String logicalPath) throws IOException {
        String[] rawSegments = logicalPath.split("/");
        List<String> segments = new ArrayList<>();
        for (String segment : rawSegments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            if (".".equals(segment) || "..".equals(segment)) {
                throw new IOException("Invalid storage path segment: " + logicalPath);
            }
            segments.add(segment);
        }
        return segments;
    }

    public static final class StorageTarget {
        private final String logicalPath;
        private final Path realPath;
        private final boolean directoryHint;
        private final Path storageRoot;

        private StorageTarget(String logicalPath, Path realPath, boolean directoryHint, Path storageRoot) {
            this.logicalPath = logicalPath;
            this.realPath = realPath;
            this.directoryHint = directoryHint;
            this.storageRoot = storageRoot;
        }

        public String logicalPath() {
            return logicalPath;
        }

        public Path realPath() {
            return realPath;
        }

        public boolean directoryHint() {
            return directoryHint;
        }

        public Path storageRoot() {
            return storageRoot;
        }
    }
}
