package com.mascotcapsule.micro3d.v3;

public class Light extends com.jblend.graphics.j3d.Light {
    private Vector3D direction = new Vector3D(0, 0, 4096);
    private int parallelIntensity = 4096;
    private int ambientIntensity;

    public Light() {
        super();
    }

    public Light(Vector3D direction, int parallelIntensity, int ambientIntensity) {
        super(direction, parallelIntensity, ambientIntensity);
        setDirection(direction);
        this.parallelIntensity = parallelIntensity;
        this.ambientIntensity = ambientIntensity;
    }

    @Override
    public Vector3D getDirection() {
        return direction;
    }

    public Vector3D getParallelLightDirection() {
        return getDirection();
    }

    @Override
    public void setDirection(com.jblend.graphics.j3d.Vector3D direction) {
        if (direction == null) {
            this.direction = new Vector3D(0, 0, 4096);
        } else if (direction instanceof Vector3D mascotDirection) {
            this.direction = mascotDirection;
        } else {
            this.direction = new Vector3D(direction.x, direction.y, direction.z);
        }
    }

    public void setParallelLightDirection(Vector3D direction) {
        setDirection(direction);
    }

    @Override
    public int getDirIntensity() {
        return parallelIntensity;
    }

    public int getParallelLightIntensity() {
        return parallelIntensity;
    }

    @Override
    public void setDirIntensity(int intensity) {
        this.parallelIntensity = intensity;
    }

    public void setParallelLightIntensity(int intensity) {
        setDirIntensity(intensity);
    }

    @Override
    public int getAmbIntensity() {
        return ambientIntensity;
    }

    public int getAmbientIntensity() {
        return ambientIntensity;
    }

    @Override
    public void setAmbIntensity(int intensity) {
        this.ambientIntensity = intensity;
    }

    public void setAmbientIntensity(int intensity) {
        setAmbIntensity(intensity);
    }
}
