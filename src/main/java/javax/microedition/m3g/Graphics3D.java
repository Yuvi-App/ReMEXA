package javax.microedition.m3g;

import com.mexa.opgl.ByteBuffer;
import com.mexa.opgl.FloatBuffer;
import com.mexa.opgl.OpglGraphics;
import com.mexa.opgl.ShortBuffer;
import emulator.graphics3D.G3DUtils;
import emulator.graphics3D.Vector4f;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import javax.microedition.lcdui.Graphics;

public final class Graphics3D {
    public static final int ANTIALIAS = 2;
    public static final int DITHER = 4;
    public static final int TRUE_COLOR = 8;
    public static final int OVERWRITE = 16;

    private static final Graphics3D INSTANCE = new Graphics3D();

    private final OpglGraphics gl = OpglGraphics.getInstance();
    private final IdentityHashMap<Image2D, Integer> textureIds = new IdentityHashMap<>();
    private final List<LightBinding> lights = new ArrayList<>();
    private final Transform cameraTransform = new Transform();
    private final Transform scratchTransform = new Transform();

    private Object boundTarget;
    private Graphics boundGraphics;
    private Camera currentCamera;
    private int viewportX;
    private int viewportY;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private int bindOriginX;
    private int bindOriginY;
    private int bindClipX;
    private int bindClipY;
    private int bindClipWidth;
    private int bindClipHeight;
    private int surfaceWidth;
    private int surfaceHeight;

    private Graphics3D() {
    }

    public static Graphics3D getInstance() {
        return INSTANCE;
    }

    public static Hashtable getProperties() {
        Hashtable properties = new Hashtable();
        properties.put("supportAntialiasing", Boolean.FALSE);
        properties.put("supportTrueColor", Boolean.TRUE);
        properties.put("supportDithering", Boolean.FALSE);
        properties.put("supportMipmapping", Boolean.FALSE);
        properties.put("supportPerspectiveCorrection", Boolean.TRUE);
        properties.put("supportLocalCameraLighting", Boolean.FALSE);
        properties.put("maxLights", Integer.valueOf(8));
        properties.put("maxViewportDimension", Integer.valueOf(4096));
        properties.put("maxTextureDimension", Integer.valueOf(4096));
        properties.put("maxSpriteCropDimension", Integer.valueOf(4096));
        properties.put("numTextureUnits", Integer.valueOf(1));
        properties.put("maxTransformsPerVertex", Integer.valueOf(4));
        properties.put("maxViewportWidth", Integer.valueOf(4096));
        properties.put("maxViewportHeight", Integer.valueOf(4096));
        return properties;
    }

    public static Object getImpl() {
        return emulator.graphics3D.lwjgl.Emulator3D.instance();
    }

    public Object getTarget() {
        return boundTarget;
    }

    public synchronized void bindTarget(Object target) {
        bindTarget(target, true, 0);
    }

    public synchronized void bindTarget(Object target, boolean depthBuffer, int hints) {
        if (boundTarget != null) {
            throw new IllegalStateException();
        }
        if (!(target instanceof Graphics graphics)) {
            throw new IllegalArgumentException("Only LCDUI Graphics targets are supported.");
        }
        boundTarget = target;
        boundGraphics = graphics;
        bindOriginX = graphics.getTranslateX();
        bindOriginY = graphics.getTranslateY();
        bindClipX = bindOriginX + graphics.getClipX();
        bindClipY = bindOriginY + graphics.getClipY();
        bindClipWidth = graphics.getClipWidth();
        bindClipHeight = graphics.getClipHeight();
        surfaceWidth = Math.max(bindClipWidth, bindClipX + bindClipWidth);
        surfaceHeight = Math.max(bindClipHeight, bindClipY + bindClipHeight);
        if (surfaceWidth <= 0) {
            surfaceWidth = 1;
        }
        if (surfaceHeight <= 0) {
            surfaceHeight = 1;
        }
        gl.bind(graphics);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewportX = bindClipX;
            viewportY = bindClipY;
            viewportWidth = bindClipWidth;
            viewportHeight = bindClipHeight;
        }
        applyViewportAndScissor();
        gl.glDisable(OpglGraphics.GL_LIGHTING);
        gl.glDisable(OpglGraphics.GL_FOG);
    }

    public synchronized void releaseTarget() {
        if (boundTarget == null) {
            return;
        }
        gl.release();
        boundTarget = null;
        boundGraphics = null;
    }

    public synchronized void setViewport(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        viewportX = x;
        viewportY = y;
        viewportWidth = width;
        viewportHeight = height;
        if (boundTarget != null) {
            applyViewportAndScissor();
        }
    }

    public int getViewportX() {
        return viewportX;
    }

    public int getViewportY() {
        return viewportY;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public synchronized void clear(Background background) {
        ensureBound();
        applyViewportAndScissor();
        int mask = 0;
        if (background == null || background.isColorClearEnabled()) {
            int color = background == null ? 0 : background.getColor();
            gl.glClearColor(
                    G3DUtils.getFloatColor(color, 16),
                    G3DUtils.getFloatColor(color, 8),
                    G3DUtils.getFloatColor(color, 0),
                    resolvedSurfaceAlpha(color)
            );
            mask |= OpglGraphics.GL_COLOR_BUFFER_BIT;
        }
        if (background == null || background.isDepthClearEnabled()) {
            gl.glClearDepthf(1.0f);
            mask |= OpglGraphics.GL_DEPTH_BUFFER_BIT;
        }
        if (mask != 0) {
            gl.glClear(mask);
        }
    }

    public synchronized void render(World world) {
        if (world == null) {
            throw new NullPointerException();
        }
        ensureBound();
        Camera camera = world.getActiveCamera();
        if (camera == null) {
            throw new IllegalStateException("World has no active camera.");
        }
        clear(world.getBackground());
        currentCamera = camera;
        cameraTransform.set(computeWorldTransform(camera));
        lights.clear();
        collectLights(world, new Transform());
        renderGroup(world, new Transform());
    }

    public synchronized void render(Node node, Transform transform) {
        if (node == null) {
            throw new NullPointerException();
        }
        ensureBound();
        if (currentCamera == null) {
            throw new IllegalStateException("No current camera is set.");
        }
        Transform worldTransform = new Transform();
        if (transform != null) {
            worldTransform.set(transform);
        } else {
            worldTransform.setIdentity();
        }
        Transform local = new Transform();
        node.getCompositeTransform(local);
        worldTransform.postMultiply(local);
        renderNode(node, worldTransform);
    }

    public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform) {
        render(vertices, triangles, appearance, transform, -1);
    }

    public synchronized void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform, int scope) {
        if (vertices == null || triangles == null) {
            throw new NullPointerException();
        }
        ensureBound();
        if (currentCamera == null) {
            throw new IllegalStateException("No current camera is set.");
        }
        Transform worldTransform = new Transform();
        if (transform != null) {
            worldTransform.set(transform);
        } else {
            worldTransform.setIdentity();
        }
        renderPrimitive(vertices, triangles, appearance, worldTransform, 1.0f);
    }

    public Camera getCamera(Transform transform) {
        if (transform != null) {
            transform.set(cameraTransform);
        }
        return currentCamera;
    }

    public void setCamera(Camera camera, Transform transform) {
        if (camera == null) {
            throw new NullPointerException();
        }
        currentCamera = camera;
        if (transform == null) {
            cameraTransform.setIdentity();
        } else {
            cameraTransform.set(transform);
        }
    }

    public int addLight(Light light, Transform transform) {
        if (light == null) {
            throw new NullPointerException();
        }
        Transform stored = new Transform();
        if (transform == null) {
            stored.setIdentity();
        } else {
            stored.set(transform);
        }
        lights.add(new LightBinding(light, stored));
        return lights.size() - 1;
    }

    public void setLight(int index, Light light, Transform transform) {
        while (lights.size() <= index) {
            lights.add(null);
        }
        if (light == null) {
            lights.set(index, null);
            return;
        }
        Transform stored = new Transform();
        if (transform == null) {
            stored.setIdentity();
        } else {
            stored.set(transform);
        }
        lights.set(index, new LightBinding(light, stored));
    }

    public Light getLight(int index, Transform transform) {
        if (index < 0 || index >= lights.size()) {
            throw new IndexOutOfBoundsException();
        }
        LightBinding binding = lights.get(index);
        if (binding == null) {
            return null;
        }
        if (transform != null) {
            transform.set(binding.transform());
        }
        return binding.light();
    }

    public int getLightCount() {
        return lights.size();
    }

    public void resetLights() {
        lights.clear();
    }

    public float getDepthRangeNear() {
        return 0.0f;
    }

    public float getDepthRangeFar() {
        return 1.0f;
    }

    public boolean isDepthBufferEnabled() {
        return true;
    }

    public int getHints() {
        return 0;
    }

    private void ensureBound() {
        if (boundTarget == null) {
            throw new IllegalStateException();
        }
    }

    private void applyViewportAndScissor() {
        gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
        gl.glScissor(
                bindClipX,
                Math.max(0, surfaceHeight - (bindClipY + bindClipHeight)),
                bindClipWidth,
                bindClipHeight
        );
        gl.glEnable(OpglGraphics.GL_SCISSOR_TEST);
    }

    private void collectLights(Group group, Transform parentTransform) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            Node child = group.getChild(i);
            Transform worldTransform = compose(parentTransform, child);
            if (child instanceof Light light && child.isRenderingEnabled()) {
                lights.add(new LightBinding(light, worldTransform));
            }
            if (child instanceof Group childGroup) {
                collectLights(childGroup, worldTransform);
            }
        }
    }

    private void renderGroup(Group group, Transform parentTransform) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Node child = group.getChild(i);
            Transform worldTransform = compose(parentTransform, child);
            renderNode(child, worldTransform);
        }
    }

    private void renderNode(Node node, Transform worldTransform) {
        if (!node.isRenderingEnabled()) {
            return;
        }
        if (node == currentCamera || node instanceof Light || node instanceof Sprite3D) {
            if (node instanceof Group group) {
                renderGroup(group, worldTransform);
            }
            return;
        }
        if (node instanceof Mesh mesh) {
            renderMesh(mesh, worldTransform);
        }
        if (node instanceof Group group) {
            renderGroup(group, worldTransform);
        }
    }

    private void renderMesh(Mesh mesh, Transform worldTransform) {
        for (int i = 0; i < mesh.getSubmeshCount(); i++) {
            Appearance appearance = mesh.getAppearance(i);
            if (appearance == null) {
                continue;
            }
            renderPrimitive(mesh.getVertexBuffer(), mesh.getIndexBuffer(i), appearance, worldTransform, mesh.getAlphaFactor());
        }
    }

    private void renderPrimitive(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform worldTransform, float alphaFactor) {
        applyCamera(worldTransform);
        applyAppearance(appearance, alphaFactor);

        VertexData vertexData = buildVertexData(vertices, alphaFactor);
        FloatBuffer vertexBuffer = FloatBuffer.allocateDirect(vertexData.positions.length);
        vertexBuffer.put(0, vertexData.positions, 0, vertexData.positions.length);
        gl.glEnableClientState(OpglGraphics.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, OpglGraphics.GL_FLOAT, 0, vertexBuffer);

        if (vertexData.normals != null) {
            FloatBuffer normalBuffer = FloatBuffer.allocateDirect(vertexData.normals.length);
            normalBuffer.put(0, vertexData.normals, 0, vertexData.normals.length);
            gl.glEnableClientState(OpglGraphics.GL_NORMAL_ARRAY);
            gl.glNormalPointer(OpglGraphics.GL_FLOAT, 0, normalBuffer);
        } else {
            gl.glDisableClientState(OpglGraphics.GL_NORMAL_ARRAY);
        }

        if (vertexData.texCoords != null) {
            FloatBuffer texBuffer = FloatBuffer.allocateDirect(vertexData.texCoords.length);
            texBuffer.put(0, vertexData.texCoords, 0, vertexData.texCoords.length);
            gl.glEnableClientState(OpglGraphics.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, OpglGraphics.GL_FLOAT, 0, texBuffer);
        } else {
            gl.glDisableClientState(OpglGraphics.GL_TEXTURE_COORD_ARRAY);
        }

        if (vertexData.colors != null) {
            ByteBuffer colorBuffer = ByteBuffer.allocateDirect(vertexData.colors.length);
            colorBuffer.put(0, vertexData.colors, 0, vertexData.colors.length);
            gl.glEnableClientState(OpglGraphics.GL_COLOR_ARRAY);
            gl.glColorPointer(4, OpglGraphics.GL_UNSIGNED_BYTE, 0, colorBuffer);
        } else {
            gl.glDisableClientState(OpglGraphics.GL_COLOR_ARRAY);
            int argb = vertices.getDefaultColor();
            float alpha = ((argb >>> 24) & 0xFF) / 255.0f;
            if (((argb >>> 24) & 0xFF) == 0) {
                alpha = 1.0f;
            }
            alpha *= alphaFactor;
            gl.glColor4f(
                    ((argb >>> 16) & 0xFF) / 255.0f,
                    ((argb >>> 8) & 0xFF) / 255.0f,
                    (argb & 0xFF) / 255.0f,
                    alpha
            );
        }

        short[] expandedIndices = expandIndices(triangles);
        ShortBuffer indexBuffer = ShortBuffer.allocateDirect(expandedIndices.length);
        indexBuffer.put(0, expandedIndices, 0, expandedIndices.length);
        gl.glDrawElements(OpglGraphics.GL_TRIANGLES, OpglGraphics.GL_UNSIGNED_SHORT, indexBuffer);

        gl.glDisableClientState(OpglGraphics.GL_VERTEX_ARRAY);
        gl.glDisableClientState(OpglGraphics.GL_NORMAL_ARRAY);
        gl.glDisableClientState(OpglGraphics.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(OpglGraphics.GL_COLOR_ARRAY);
    }

    private void applyCamera(Transform worldTransform) {
        Transform projection = new Transform();
        currentCamera.getProjection(projection);
        Transform view = new Transform(cameraTransform);
        view.invert();
        Transform modelView = new Transform(view);
        modelView.postMultiply(worldTransform);

        gl.glMatrixMode(OpglGraphics.GL_PROJECTION);
        gl.glLoadMatrixf(toOpglMatrix(projection));
        gl.glMatrixMode(OpglGraphics.GL_MODELVIEW);
        gl.glLoadMatrixf(toOpglMatrix(modelView));
        gl.glMatrixMode(OpglGraphics.GL_TEXTURE);
        gl.glLoadIdentity();
        gl.glMatrixMode(OpglGraphics.GL_MODELVIEW);
    }

    private void applyAppearance(Appearance appearance, float alphaFactor) {
        gl.glDisable(OpglGraphics.GL_LIGHTING);
        gl.glDisable(OpglGraphics.GL_FOG);
        gl.glDisable(OpglGraphics.GL_BLEND);
        gl.glDisable(OpglGraphics.GL_ALPHA_TEST);
        gl.glDisable(OpglGraphics.GL_TEXTURE_2D);
        gl.glDepthMask(true);
        gl.glEnable(OpglGraphics.GL_DEPTH_TEST);
        gl.glColorMask(true, true, true, true);
        gl.glFrontFace(OpglGraphics.GL_CCW);
        gl.glDisable(OpglGraphics.GL_CULL_FACE);
        gl.glTexEnvi(OpglGraphics.GL_TEXTURE_ENV, OpglGraphics.GL_TEXTURE_ENV_MODE, OpglGraphics.GL_MODULATE);

        if (appearance == null) {
            return;
        }

        PolygonMode polygonMode = appearance.getPolygonMode();
        if (polygonMode != null) {
            if (polygonMode.getCulling() == PolygonMode.CULL_NONE) {
                gl.glDisable(OpglGraphics.GL_CULL_FACE);
            } else {
                gl.glEnable(OpglGraphics.GL_CULL_FACE);
                gl.glCullFace(polygonMode.getCulling() == PolygonMode.CULL_FRONT
                        ? OpglGraphics.GL_FRONT
                        : OpglGraphics.GL_BACK);
            }
            gl.glFrontFace(polygonMode.getWinding() == PolygonMode.WINDING_CW
                    ? OpglGraphics.GL_CW
                    : OpglGraphics.GL_CCW);
        }

        CompositingMode compositingMode = appearance.getCompositingMode();
        if (compositingMode != null) {
            gl.glDepthMask(compositingMode.isDepthWriteEnabled());
            if (compositingMode.isDepthTestEnabled()) {
                gl.glEnable(OpglGraphics.GL_DEPTH_TEST);
            } else {
                gl.glDisable(OpglGraphics.GL_DEPTH_TEST);
            }
            gl.glColorMask(
                    compositingMode.isColorWriteEnabled(),
                    compositingMode.isColorWriteEnabled(),
                    compositingMode.isColorWriteEnabled(),
                    compositingMode.isAlphaWriteEnabled()
            );
            if (compositingMode.getAlphaThreshold() > 0.0f) {
                gl.glEnable(OpglGraphics.GL_ALPHA_TEST);
                gl.glAlphaFunc(OpglGraphics.GL_GREATER, compositingMode.getAlphaThreshold());
            }
            switch (compositingMode.getBlending()) {
                case CompositingMode.ALPHA -> {
                    gl.glEnable(OpglGraphics.GL_BLEND);
                    gl.glBlendFunc(OpglGraphics.GL_SRC_ALPHA, OpglGraphics.GL_ONE_MINUS_SRC_ALPHA);
                }
                case CompositingMode.ALPHA_ADD -> {
                    gl.glEnable(OpglGraphics.GL_BLEND);
                    gl.glBlendFunc(OpglGraphics.GL_SRC_ALPHA, OpglGraphics.GL_ONE);
                }
                default -> gl.glDisable(OpglGraphics.GL_BLEND);
            }
        }

        Fog fog = appearance.getFog();
        if (fog != null) {
            gl.glEnable(OpglGraphics.GL_FOG);
            gl.glFogf(OpglGraphics.GL_FOG_MODE,
                    fog.getMode() == Fog.EXPONENTIAL ? OpglGraphics.GL_EXP : OpglGraphics.GL_LINEAR);
            gl.glFogf(OpglGraphics.GL_FOG_DENSITY, fog.getDensity());
            gl.glFogf(OpglGraphics.GL_FOG_START, fog.getNearDistance());
            gl.glFogf(OpglGraphics.GL_FOG_END, fog.getFarDistance());
            gl.glFogfv(OpglGraphics.GL_FOG_COLOR, colorToFloats(fog.getColor(), alphaFactor));
        }

        Texture2D texture = appearance.getTexture(0);
        if (texture != null && texture.getImage() != null) {
            int textureId = ensureTexture(texture.getImage());
            gl.glEnable(OpglGraphics.GL_TEXTURE_2D);
            gl.glBindTexture(OpglGraphics.GL_TEXTURE_2D, textureId);
            gl.glTexParameteri(
                    OpglGraphics.GL_TEXTURE_2D,
                    OpglGraphics.GL_TEXTURE_WRAP_S,
                    texture.getWrappingS() == Texture2D.WRAP_CLAMP ? OpglGraphics.GL_CLAMP_TO_EDGE : OpglGraphics.GL_REPEAT
            );
            gl.glTexParameteri(
                    OpglGraphics.GL_TEXTURE_2D,
                    OpglGraphics.GL_TEXTURE_WRAP_T,
                    texture.getWrappingT() == Texture2D.WRAP_CLAMP ? OpglGraphics.GL_CLAMP_TO_EDGE : OpglGraphics.GL_REPEAT
            );
            gl.glTexParameteri(
                    OpglGraphics.GL_TEXTURE_2D,
                    OpglGraphics.GL_TEXTURE_MAG_FILTER,
                    texture.getImageFilter() == Texture2D.FILTER_LINEAR ? OpglGraphics.GL_LINEAR : OpglGraphics.GL_NEAREST
            );
            gl.glTexParameteri(
                    OpglGraphics.GL_TEXTURE_2D,
                    OpglGraphics.GL_TEXTURE_MIN_FILTER,
                    texture.getImageFilter() == Texture2D.FILTER_LINEAR ? OpglGraphics.GL_LINEAR : OpglGraphics.GL_NEAREST
            );
            gl.glTexEnvi(OpglGraphics.GL_TEXTURE_ENV, OpglGraphics.GL_TEXTURE_ENV_MODE, switch (texture.getBlending()) {
                case Texture2D.FUNC_REPLACE -> OpglGraphics.GL_REPLACE;
                case Texture2D.FUNC_BLEND -> OpglGraphics.GL_BLEND;
                case Texture2D.FUNC_DECAL -> OpglGraphics.GL_DECAL;
                case Texture2D.FUNC_ADD -> OpglGraphics.GL_ADD;
                default -> OpglGraphics.GL_MODULATE;
            });
            if (texture.getBlending() == Texture2D.FUNC_BLEND) {
                gl.glTexEnvfv(OpglGraphics.GL_TEXTURE_ENV, OpglGraphics.GL_TEXTURE_ENV_COLOR, colorToFloats(texture.getBlendColor(), 1.0f));
            }
            texture.getCompositeTransform(scratchTransform);
            gl.glMatrixMode(OpglGraphics.GL_TEXTURE);
            gl.glLoadMatrixf(toOpglMatrix(scratchTransform));
            gl.glMatrixMode(OpglGraphics.GL_MODELVIEW);
        }
    }

    private int ensureTexture(Image2D image) {
        Integer existing = textureIds.get(image);
        if (existing != null && !image.isDirty()) {
            return existing;
        }
        int textureId = existing == null ? createTextureId() : existing;
        ByteBuffer pixels = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        byte[] rgba = image.rgbaData();
        pixels.put(0, rgba, 0, rgba.length);
        gl.glBindTexture(OpglGraphics.GL_TEXTURE_2D, textureId);
        gl.glTexImage2D(
                OpglGraphics.GL_TEXTURE_2D,
                0,
                OpglGraphics.GL_RGBA,
                image.getWidth(),
                image.getHeight(),
                0,
                OpglGraphics.GL_RGBA,
                OpglGraphics.GL_UNSIGNED_BYTE,
                pixels
        );
        textureIds.put(image, textureId);
        image.markClean(textureId);
        return textureId;
    }

    private int createTextureId() {
        int[] ids = new int[1];
        gl.glGenTextures(ids);
        return ids[0];
    }

    private VertexData buildVertexData(VertexBuffer vertexBuffer, float alphaFactor) {
        int vertexCount = vertexBuffer.getVertexCount();
        float[] positions = new float[vertexCount * 3];
        float[] texCoords = null;
        float[] normals = null;
        byte[] colors = null;

        boolean hasTexCoords = vertexBuffer.getTexCoords(0, null) != null;
        boolean hasNormals = vertexBuffer.getNormals() != null;
        VertexArray colorArray = vertexBuffer.getColors();
        if (hasTexCoords) {
            texCoords = new float[vertexCount * 2];
        }
        if (hasNormals) {
            normals = new float[vertexCount * 3];
        }
        if (colorArray != null) {
            colors = new byte[vertexCount * 4];
        }

        Vector4f vector = new Vector4f();
        for (int i = 0; i < vertexCount; i++) {
            vertexBuffer.getVertex(i, vector);
            positions[i * 3] = vector.x;
            positions[i * 3 + 1] = vector.y;
            positions[i * 3 + 2] = vector.z;
            if (texCoords != null && vertexBuffer.getTexVertex(i, 0, vector)) {
                texCoords[i * 2] = vector.x;
                texCoords[i * 2 + 1] = vector.y;
            }
            if (normals != null && vertexBuffer.getNormalVertex(i, vector)) {
                normals[i * 3] = vector.x;
                normals[i * 3 + 1] = vector.y;
                normals[i * 3 + 2] = vector.z;
            }
        }

        if (colors != null) {
            fillColorArray(colors, colorArray, alphaFactor);
        }
        return new VertexData(positions, texCoords, normals, colors);
    }

    private void fillColorArray(byte[] colors, VertexArray colorArray, float alphaFactor) {
        int componentCount = colorArray.getComponentCount();
        byte[] source = new byte[colorArray.getVertexCount() * componentCount];
        colorArray.get(0, colorArray.getVertexCount(), source);
        for (int i = 0; i < colorArray.getVertexCount(); i++) {
            int srcIndex = i * componentCount;
            int dstIndex = i * 4;
            colors[dstIndex] = source[srcIndex];
            colors[dstIndex + 1] = source[srcIndex + 1];
            colors[dstIndex + 2] = source[srcIndex + 2];
            int alpha = componentCount == 4 ? source[srcIndex + 3] & 0xFF : 0xFF;
            alpha = Math.max(0, Math.min(255, Math.round(alpha * alphaFactor)));
            colors[dstIndex + 3] = (byte) alpha;
        }
    }

    private short[] expandIndices(IndexBuffer buffer) {
        int[] indices = new int[buffer.getIndexCount()];
        buffer.getIndices(indices);
        short[] packed = new short[indices.length];
        for (int i = 0; i < indices.length; i++) {
            packed[i] = (short) indices[i];
        }
        return packed;
    }

    private Transform computeWorldTransform(Node node) {
        ArrayList<Node> chain = new ArrayList<>();
        Node current = node;
        while (current != null) {
            chain.add(0, current);
            current = current.getParent();
        }
        Transform world = new Transform();
        world.setIdentity();
        Transform local = new Transform();
        for (Node element : chain) {
            element.getCompositeTransform(local);
            world.postMultiply(local);
        }
        return world;
    }

    private Transform compose(Transform parent, Node child) {
        Transform combined = new Transform();
        combined.set(parent);
        Transform local = new Transform();
        child.getCompositeTransform(local);
        combined.postMultiply(local);
        return combined;
    }

    private static float[] toOpglMatrix(Transform transform) {
        float[] rowMajor = new float[16];
        transform.get(rowMajor);
        float[] columnMajor = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                columnMajor[col * 4 + row] = rowMajor[row * 4 + col];
            }
        }
        return columnMajor;
    }

    private static float[] colorToFloats(int color, float alphaFactor) {
        float alpha = G3DUtils.getFloatColor(color, 24);
        if (((color >>> 24) & 0xFF) == 0) {
            alpha = 1.0f;
        }
        return new float[] {
                G3DUtils.getFloatColor(color, 16),
                G3DUtils.getFloatColor(color, 8),
                G3DUtils.getFloatColor(color, 0),
                Math.max(0.0f, Math.min(1.0f, alpha * alphaFactor))
        };
    }

    private static float resolvedSurfaceAlpha(int color) {
        float alpha = G3DUtils.getFloatColor(color, 24);
        return ((color >>> 24) & 0xFF) == 0 ? 1.0f : alpha;
    }

    private record LightBinding(Light light, Transform transform) {
    }

    private record VertexData(float[] positions, float[] texCoords, float[] normals, byte[] colors) {
    }
}
