package remexa.host.j3d;

public final class MbacModel {
    private final int[] modelVertices;
    private final Polygon[] polygons;
    private final int numPatterns;
    private final Bone[] bones;

    public MbacModel(int[] modelVertices, Polygon[] polygons, int numPatterns, Bone[] bones) {
        this.modelVertices = modelVertices == null ? new int[0] : modelVertices.clone();
        this.polygons = polygons == null ? new Polygon[0] : polygons;
        this.numPatterns = Math.max(1, numPatterns);
        this.bones = bones == null ? new Bone[0] : bones;
    }

    public int[] modelVertices() {
        return modelVertices.clone();
    }

    public Polygon[] polygons() {
        return polygons;
    }

    public int numPatterns() {
        return numPatterns;
    }

    public Bone[] bones() {
        return bones;
    }

    public static final class Bone {
        private final int length;
        private final int parent;
        private final int[] matrix;

        public Bone(int length, int parent, int[] matrix) {
            this.length = length;
            this.parent = parent;
            this.matrix = matrix;
        }

        public int length() {
            return length;
        }

        public int parent() {
            return parent;
        }

        public int[] fixedMatrix() {
            return matrix;
        }
    }

    public static final class Polygon {
        private final int[] indices;
        private final float[] textureCoords;
        private final int color;
        private final int textureIndex;
        private final int patternMask;
        private final int attributes;
        private final int blendMode;
        private final boolean doubleSided;
        private final boolean transparent;

        public Polygon(
                int[] indices,
                float[] textureCoords,
                int color,
                int textureIndex,
                int patternMask,
                int attributes,
                int blendMode,
                boolean doubleSided,
                boolean transparent
        ) {
            this.indices = indices;
            this.textureCoords = textureCoords;
            this.color = color;
            this.textureIndex = textureIndex;
            this.patternMask = patternMask;
            this.attributes = attributes;
            this.blendMode = blendMode;
            this.doubleSided = doubleSided;
            this.transparent = transparent;
        }

        public int[] indices() {
            return indices;
        }

        public float[] textureCoords() {
            return textureCoords;
        }

        public int color() {
            return color;
        }

        public int textureIndex() {
            return textureIndex;
        }

        public int patternMask() {
            return patternMask;
        }

        public int attributes() {
            return attributes;
        }

        public int blendMode() {
            return blendMode;
        }

        public boolean doubleSided() {
            return doubleSided;
        }

        public boolean transparent() {
            return transparent;
        }
    }
}
