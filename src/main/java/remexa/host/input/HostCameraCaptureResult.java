package remexa.host.input;

import java.nio.file.Path;

public record HostCameraCaptureResult(Path sourcePath, boolean accepted) {
    public static HostCameraCaptureResult cancelled() {
        return new HostCameraCaptureResult(null, false);
    }
}
