package com.j_phone.media;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

public class ResourceOperatorManager {
    public static final int MELODY_RESOURCE = 0;
    public static final int IMAGE_RESOURCE = 1;

    private static final Set<String> MELODY_EXTENSIONS = Set.of(
            ".mid", ".midi", ".mmf", ".mld", ".smaf", ".smf", ".wav", ".amr", ".imy"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp"
    );
    private static final Map<String, ResourceCatalog> CATALOGS = new ConcurrentHashMap<>();

    public static ResourceOperator getResourceOperator(int type) {
        DebugLog.sdkCall("com.j_phone.media.ResourceOperatorManager", "getResourceOperator", type);
        var jarPath = MidletRuntime.currentJarPath();
        if (jarPath == null) {
            return new JarResourceOperator(type, List.of());
        }
        var catalog = CATALOGS.computeIfAbsent(jarPath.toAbsolutePath().normalize().toString(), ignored -> loadCatalog(jarPath));
        return switch (type) {
            case MELODY_RESOURCE -> new JarResourceOperator(type, catalog.melodies());
            case IMAGE_RESOURCE -> new JarResourceOperator(type, catalog.images());
            default -> new JarResourceOperator(type, List.of());
        };
    }

    private static ResourceCatalog loadCatalog(Path jarPath) {
        var melodies = new ArrayList<ResourceEntry>();
        var images = new ArrayList<ResourceEntry>();
        try (var jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .forEach(entry -> {
                        var name = entry.getName();
                        var lowerName = name.toLowerCase(Locale.ROOT);
                        if (hasAnyExtension(lowerName, MELODY_EXTENSIONS)) {
                            melodies.add(new ResourceEntry(melodies.size(), name, displayName(name)));
                        } else if (hasAnyExtension(lowerName, IMAGE_EXTENSIONS)) {
                            images.add(new ResourceEntry(images.size(), name, displayName(name)));
                        }
                    });
        } catch (IOException exception) {
            DebugLog.log(
                    LogCategory.MEDIA,
                    ResourceOperatorManager.class.getName(),
                    "Failed to enumerate resources from " + jarPath + ": " + exception.getMessage()
            );
        }
        melodies.sort(Comparator.comparing(ResourceEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        images.sort(Comparator.comparing(ResourceEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        var normalizedMelodies = normalizeIds(melodies);
        var normalizedImages = normalizeIds(images);
        DebugLog.log(
                LogCategory.MEDIA,
                ResourceOperatorManager.class.getName(),
                "Loaded resource catalog from " + jarPath.getFileName() +
                        " (melodies=" + normalizedMelodies.size() + ", images=" + normalizedImages.size() + ")"
        );
        return new ResourceCatalog(List.copyOf(normalizedMelodies), List.copyOf(normalizedImages));
    }

    private static List<ResourceEntry> normalizeIds(List<ResourceEntry> entries) {
        var normalized = new ArrayList<ResourceEntry>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            normalized.add(new ResourceEntry(i, entry.path(), entry.displayName()));
        }
        return normalized;
    }

    private static boolean hasAnyExtension(String value, Set<String> extensions) {
        for (var extension : extensions) {
            if (value.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String displayName(String resourcePath) {
        var fileName = resourcePath;
        var separator = Math.max(resourcePath.lastIndexOf('/'), resourcePath.lastIndexOf('\\'));
        if (separator >= 0 && separator + 1 < resourcePath.length()) {
            fileName = resourcePath.substring(separator + 1);
        }
        var extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }
        return fileName;
    }

    private record ResourceCatalog(List<ResourceEntry> melodies, List<ResourceEntry> images) {
    }

    private record ResourceEntry(int id, String path, String displayName) {
    }

    private static final class JarResourceOperator implements ResourceOperator {
        private final int type;
        private final List<ResourceEntry> entries;

        private JarResourceOperator(int type, List<ResourceEntry> entries) {
            this.type = type;
            this.entries = entries;
        }

        @Override
        public int getResourceType() {
            return type;
        }

        @Override
        public int getResourceCount() {
            return entries.size();
        }

        @Override
        public int getResourceID(int index) {
            return (index >= 0 && index < entries.size()) ? entries.get(index).id() : -1;
        }

        @Override
        public String getResourceName(int resourceId) {
            var entry = entryById(resourceId);
            return entry == null ? "" : entry.displayName();
        }

        @Override
        public String[] getResourceNames() {
            var names = new String[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                names[i] = entries.get(i).displayName();
            }
            return names;
        }

        @Override
        public void setResourceByID(MediaPlayer player, int resourceId) {
            var entry = entryById(resourceId);
            if (entry != null) {
                player.setMediaData(entry.path());
            }
        }

        @Override
        public void setResourceByTitle(MediaPlayer player, String title) {
            if (title == null) {
                return;
            }
            for (var entry : entries) {
                if (entry.displayName().equals(title)) {
                    player.setMediaData(entry.path());
                    return;
                }
            }
        }

        @Override
        public void setResource(MediaPlayer player, int index) {
            if (index >= 0 && index < entries.size()) {
                player.setMediaData(entries.get(index).path());
            }
        }

        @Override
        public int getIndexOfResource(int resourceId) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).id() == resourceId) {
                    return i;
                }
            }
            return -1;
        }

        private ResourceEntry entryById(int resourceId) {
            var index = getIndexOfResource(resourceId);
            return index >= 0 ? entries.get(index) : null;
        }
    }
}
