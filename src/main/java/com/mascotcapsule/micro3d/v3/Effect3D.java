package com.mascotcapsule.micro3d.v3;

public class Effect3D extends com.jblend.graphics.j3d.Effect3D {
    public static final int NORMAL_SHADING = 0;
    public static final int TOON_SHADING = 1;

    private Light light;
    private int shading = NORMAL_SHADING;
    private int toonThreshold;
    private int toonHigh;
    private int toonLow;
    private boolean transparency = true;
    private Texture sphereTexture;

    public Effect3D() {
        super();
    }

    public Effect3D(Light light, int shading, boolean transparency, Texture sphereTexture) {
        super(light, shading, transparency, sphereTexture);
        this.light = light;
        this.shading = shading;
        this.transparency = transparency;
        this.sphereTexture = sphereTexture;
    }

    @Override
    public Light getLight() {
        return light;
    }

    public void setLight(Light light) {
        this.light = light;
    }

    @Override
    public void setLight(com.jblend.graphics.j3d.Light light) {
        if (light == null) {
            this.light = null;
        } else if (light instanceof Light mascotLight) {
            this.light = mascotLight;
        } else {
            this.light = new Light(new Vector3D(light.getDirection().x, light.getDirection().y, light.getDirection().z),
                    light.getDirIntensity(), light.getAmbIntensity());
        }
    }

    @Override
    public int getShading() {
        return shading;
    }

    public int getShadingType() {
        return shading;
    }

    @Override
    public void setShading(int shading) {
        setShadingType(shading);
    }

    public void setShadingType(int shading) {
        if ((shading & ~TOON_SHADING) != 0) {
            throw new IllegalArgumentException();
        }
        this.shading = shading;
    }

    @Override
    public int getThreshold() {
        return toonThreshold;
    }

    @Override
    public int getThresholdHigh() {
        return toonHigh;
    }

    @Override
    public int getThresholdLow() {
        return toonLow;
    }

    public int getToonThreshold() {
        return toonThreshold;
    }

    public int getToonHigh() {
        return toonHigh;
    }

    public int getToonLow() {
        return toonLow;
    }

    @Override
    public void setThreshold(int threshold, int high, int low) {
        setToonParams(threshold, high, low);
    }

    public void setToonParams(int threshold, int high, int low) {
        if (((threshold | high | low) & ~0xFF) != 0) {
            throw new IllegalArgumentException();
        }
        this.toonThreshold = threshold;
        this.toonHigh = high;
        this.toonLow = low;
    }

    @Override
    public boolean isSemiTransparentEnabled() {
        return transparency;
    }

    public boolean isTransparency() {
        return transparency;
    }

    @Override
    public void setSemiTransparentEnabled(boolean transparency) {
        this.transparency = transparency;
    }

    public void setTransparency(boolean transparency) {
        this.transparency = transparency;
    }

    @Override
    public Texture getSphereMap() {
        return sphereTexture;
    }

    public Texture getSphereTexture() {
        return sphereTexture;
    }

    @Override
    public void setSphereMap(com.jblend.graphics.j3d.Texture sphereMap) {
        this.sphereTexture = sphereMap instanceof Texture mascotTexture ? mascotTexture : null;
    }

    public void setSphereTexture(Texture sphereTexture) {
        this.sphereTexture = sphereTexture;
    }
}
