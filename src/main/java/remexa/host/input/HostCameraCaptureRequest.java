package remexa.host.input;

import java.util.Locale;

public record HostCameraCaptureRequest(
        String title,
        int targetWidth,
        int targetHeight,
        String format,
        boolean frameOverlayPresent
) {
    public HostCameraCaptureRequest {
        title = title == null || title.isBlank() ? "Camera" : title.trim();
        targetWidth = Math.max(1, targetWidth);
        targetHeight = Math.max(1, targetHeight);
        format = format == null || format.isBlank()
                ? "jpeg"
                : format.trim().toLowerCase(Locale.ROOT);
    }
}
