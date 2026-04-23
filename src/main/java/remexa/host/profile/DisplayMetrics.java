package remexa.host.profile;

public record DisplayMetrics(int width, int height, String source) {
    public DisplayMetrics {
        if (width <= 0) {
            throw new IllegalArgumentException("Display width must be positive.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Display height must be positive.");
        }
        if (source == null || source.isBlank()) {
            source = "runtime";
        }
    }

    public String dimensions() {
        return width + "x" + height;
    }
}
