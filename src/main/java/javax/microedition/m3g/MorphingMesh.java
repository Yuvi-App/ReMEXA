package javax.microedition.m3g;

public class MorphingMesh extends Mesh {
    private final VertexBuffer[] targets;
    private float[] weights;

    public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] submeshes, Appearance[] appearances) {
        super(base, submeshes, appearances);
        this.targets = targets == null ? new VertexBuffer[0] : targets.clone();
        this.weights = new float[this.targets.length];
    }

    public void setWeights(float[] weights) {
        if (weights == null) {
            throw new NullPointerException();
        }
        this.weights = weights.clone();
    }

    public int getMorphTargetCount() {
        return targets.length;
    }
}
