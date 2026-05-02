package emulator.graphics3D.lwjgl;

public final class Emulator3D {
    public static final int NumTextureUnits = 1;
    public static final int MaxTextureDimension = 4096;
    public static final int MaxViewportWidth = 4096;
    public static final int MaxViewportHeight = 4096;
    public static final int MaxSpriteCropDimension = 4096;
    public static final int MaxTransformsPerVertex = 4;

    private static final Emulator3D INSTANCE = new Emulator3D();

    private Emulator3D() {
    }

    public static Emulator3D instance() {
        return INSTANCE;
    }

    public void invalidateTexture(Object ignored) {
    }

    public void finalizeTexture(Object ignored) {
    }
}
