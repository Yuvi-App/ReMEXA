package javax.microedition.m3g;

import emulator.graphics3D.Vector4f;

public class RayIntersection {
    private float distance = Float.POSITIVE_INFINITY;

    void startPick(float[] ray, float x, float y, Camera camera) {
        distance = Float.POSITIVE_INFINITY;
    }

    boolean testDistance(float candidate) {
        return candidate < distance;
    }

    boolean endPick(float candidate, float[] textureS, float[] textureT, int submesh, Node node, float[] normal) {
        if (!testDistance(candidate)) {
            return false;
        }
        distance = candidate;
        return true;
    }

    public float getDistance() {
        return distance;
    }

    public Node getIntersected() {
        return null;
    }

    public float getTextureS(int index) {
        return 0.0f;
    }

    public float getTextureT(int index) {
        return 0.0f;
    }

    public float[] getNormal(float[] normal) {
        if (normal != null && normal.length >= 3) {
            normal[0] = 0.0f;
            normal[1] = 0.0f;
            normal[2] = 1.0f;
        }
        return normal;
    }

    public int getSubmeshIndex() {
        return 0;
    }

    public void getRay(float[] ray) {
        if (ray != null && ray.length >= 6) {
            for (int i = 0; i < 6; i++) {
                ray[i] = 0.0f;
            }
        }
    }

    Vector4f intersectedNormal() {
        return new Vector4f(0.0f, 0.0f, 1.0f, 0.0f);
    }
}
