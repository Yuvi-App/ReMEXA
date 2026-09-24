package remexa.host.media;

import com.sun.jna.NativeLibrary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

/** Shared discovery for hosted video and audio-only decoding. */
public final class VlcRuntime {
    private static boolean configured;

    private VlcRuntime() { }

    public static synchronized void configure() throws IOException {
        if (configured) return;
        try {
            Path directory = locateDirectory();
            if (directory != null) {
                NativeLibrary.addSearchPath("libvlc", directory.toString());
                System.setProperty("jna.library.path", directory.toString());
            } else if (!new NativeDiscovery().discover()) {
                throw new IOException("VLC 3.x was not found. Install VLC to enable MP4/3GP playback.");
            }
            configured = true;
        } catch (RuntimeException | LinkageError exception) {
            throw new IOException("VLC 3.x could not be loaded.", exception);
        }
    }

    private static Path locateDirectory() {
        String home = System.getenv("VLC_HOME");
        if (home != null && !home.isBlank()) {
            Path path = Path.of(home.trim());
            if (Files.isDirectory(path)) return path;
        }
        for (String root : new String[]{System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)")}) {
            if (root != null) {
                Path path = Path.of(root, "VideoLAN", "VLC");
                if (Files.isRegularFile(path.resolve("libvlc.dll"))) return path;
            }
        }
        return null;
    }
}
