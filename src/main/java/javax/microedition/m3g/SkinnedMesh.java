package javax.microedition.m3g;

public class SkinnedMesh extends Mesh {
    private final Group skeleton;

    public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton) {
        super(vertices, submeshes, appearances);
        if (skeleton == null) {
            throw new NullPointerException();
        }
        this.skeleton = skeleton;
        addReference(skeleton);
    }

    public Group getSkeleton() {
        return skeleton;
    }

    public void addTransform(Node bone, int weight, int firstVertex, int numVertices) {
        if (bone != null) {
            bone.setSkinnedMeshBone();
            addReference(bone);
        }
    }
}
