package remexa.host.j3d;

import com.jblend.graphics.j3d.AffineTrans;
import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Light;
import com.jblend.graphics.j3d.Texture;
import com.jblend.graphics.j3d.Vector3D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.microedition.lcdui.Graphics;

public final class SoftwareJ3dRenderer {
    private static final float DEPTH_EPSILON = 0.000001f;
    private static final float TRANSLUCENT_DEPTH_BIAS = 32.0f;
    private static final int RASTER_SUBPIXEL_SHIFT = 4;
    private static final int RASTER_SUBPIXEL_SCALE = 1 << RASTER_SUBPIXEL_SHIFT;
    private static final int COMMAND_LIST_VERSION_1_0 = 0xFE000001;
    private static final int COMMAND_END = 0x80000000;
    private static final int COMMAND_NOP = 0x81000000;
    private static final int COMMAND_ATTRIBUTE = 0x83000000;
    private static final int COMMAND_CLIP = 0x84000000;
    private static final int COMMAND_FLUSH = 0x82000000;
    private static final int COMMAND_CENTER = 0x85000000;
    private static final int COMMAND_TEXTURE_INDEX = 0x86000000;
    private static final int COMMAND_AFFINE_INDEX = 0x87000000;
    private static final int COMMAND_PARALLEL_SCALE = 0x90000000;
    private static final int COMMAND_PARALLEL_SIZE = 0x91000000;
    private static final int COMMAND_PERSPECTIVE_FOV = 0x92000000;
    private static final int COMMAND_PERSPECTIVE_WH = 0x93000000;
    private static final int COMMAND_AMBIENT_LIGHT = 0xA0000000;
    private static final int COMMAND_DIRECTION_LIGHT = 0xA1000000;
    private static final int COMMAND_THRESHOLD = 0xAF000000;
    private static final int COMMAND_MASK = 0xFF000000;
    private static final int ENV_ATTR_LIGHTING = 0x01;
    private static final int ENV_ATTR_SPHERE_MAP = 0x02;
    private static final int ENV_ATTR_TOON_SHADING = 0x04;
    private static final int ENV_ATTR_SEMI_TRANSPARENT = 0x08;
    private static final int PATTR_COLORKEY = 0x10;
    private static final int PDATA_NORMAL_MASK = 0x0300;
    private static final int PDATA_NORMAL_PER_FACE = 0x0200;
    private static final int PDATA_NORMAL_PER_VERTEX = 0x0300;
    private static final int PDATA_COLOR_MASK = 0x0C00;
    private static final int PDATA_COLOR_PER_COMMAND = 0x0400;
    private static final int PDATA_COLOR_PER_FACE = 0x0800;
    private static final int PDATA_TEXCOORD_MASK = 0x3000;
    private static final int PDATA_POINT_SPRITE_PARAMS_PER_COMMAND = 0x1000;
    private static final int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 0x2000;
    private static final int PDATA_TEXCOORD_PER_VERTEX = 0x3000;
    private static final int PRIMITIVE_POINTS = 0x01;
    private static final int PRIMITIVE_LINES = 0x02;
    private static final int PRIMITIVE_TRIANGLES = 0x03;
    private static final int PRIMITIVE_QUADS = 0x04;
    private static final int PRIMITIVE_POINT_SPRITES = 0x05;

    private SoftwareJ3dRenderer() {
    }

    public enum RenderPass {
        ALL,
        OPAQUE,
        TRANSLUCENT
    }

    public static boolean renderPrimitivesToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int originX,
            int originY,
            com.jblend.graphics.j3d.FigureLayout layout,
            Effect3D effect,
            Texture texture,
            int command,
            int numPrimitives,
            int[] vertexCoords,
            int[] normals,
            int[] textureCoords,
            int[] colors
    ) {
        return renderPrimitivesToBuffers(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                originX,
                originY,
                layout,
                effect,
                texture,
                command,
                numPrimitives,
                vertexCoords,
                normals,
                textureCoords,
                colors,
                RenderPass.ALL
        );
    }

    public static boolean renderPrimitivesToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int originX,
            int originY,
            com.jblend.graphics.j3d.FigureLayout layout,
            Effect3D effect,
            Texture texture,
            int command,
            int numPrimitives,
            int[] vertexCoords,
            int[] normals,
            int[] textureCoords,
            int[] colors,
            RenderPass pass
    ) {
        if (pixels == null || depthBuffer == null || layout == null || effect == null || vertexCoords == null) {
            return false;
        }
        if (pixels.length < surfaceWidth * surfaceHeight || depthBuffer.length < surfaceWidth * surfaceHeight) {
            throw new IllegalArgumentException("Scene buffers are smaller than the target surface.");
        }
        if (numPrimitives <= 0) {
            return false;
        }

        int primitiveType = command & COMMAND_MASK;
        int[] payload = switch (primitiveType) {
            case PRIMITIVE_LINES << 24 -> buildPrimitiveCommandList(command, numPrimitives, vertexCoords, null, null, colors);
            case PRIMITIVE_TRIANGLES << 24, PRIMITIVE_QUADS << 24 ->
                    buildPrimitiveCommandList(command, numPrimitives, vertexCoords, normals, textureCoords, colors);
            case PRIMITIVE_POINT_SPRITES << 24 ->
                    buildPrimitiveCommandList(command, numPrimitives, vertexCoords, null, textureCoords, colors);
            default -> null;
        };
        if (payload == null) {
            return false;
        }
        CommandState state = CommandState.fromLayout(
                originX,
                originY,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                layout,
                effect,
                null,
                texture,
                true
        );
        return renderPrimitiveCommand(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                state,
                payload,
                1,
                pass
        ) > 1;
    }

    public static boolean renderCommandListToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int originX,
            int originY,
            com.jblend.graphics.j3d.FigureLayout layout,
            Effect3D effect,
            Texture[] textures,
            Texture fallbackTexture,
            int[] commandList
    ) {
        return renderCommandListToBuffers(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                originX,
                originY,
                layout,
                effect,
                textures,
                fallbackTexture,
                commandList,
                RenderPass.ALL
        );
    }

    public static boolean renderCommandListToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int originX,
            int originY,
            com.jblend.graphics.j3d.FigureLayout layout,
            Effect3D effect,
            Texture[] textures,
            Texture fallbackTexture,
            int[] commandList,
            RenderPass pass
    ) {
        if (pixels == null || depthBuffer == null || layout == null || effect == null || commandList == null) {
            return false;
        }
        if (pixels.length < surfaceWidth * surfaceHeight || depthBuffer.length < surfaceWidth * surfaceHeight) {
            throw new IllegalArgumentException("Scene buffers are smaller than the target surface.");
        }
        CommandState state = CommandState.fromLayout(
                originX,
                originY,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                layout,
                effect,
                textures,
                fallbackTexture,
                true
        );
        boolean rendered = false;
        int cursor = 0;
        while (cursor < commandList.length) {
            int command = commandList[cursor++];
            if (command == COMMAND_LIST_VERSION_1_0) {
                continue;
            }
            if (command == COMMAND_END) {
                break;
            }
            if ((command & COMMAND_MASK) == COMMAND_NOP) {
                cursor = Math.min(commandList.length, cursor + (command & 0x00FFFFFF));
                continue;
            }
            if (command == COMMAND_FLUSH) {
                rendered = true;
                continue;
            }
            if ((command & COMMAND_MASK) == COMMAND_ATTRIBUTE) {
                int attributes = command & 0x00FFFFFF;
                state.lightingEnabled = (attributes & ENV_ATTR_LIGHTING) != 0;
                state.sphereMapEnabled = (attributes & ENV_ATTR_SPHERE_MAP) != 0;
                state.toonShadingEnabled = (attributes & ENV_ATTR_TOON_SHADING) != 0;
                state.semiTransparentEnabled = (attributes & ENV_ATTR_SEMI_TRANSPARENT) != 0;
                continue;
            }
            if (command == COMMAND_CLIP) {
                if (cursor + 3 >= commandList.length) {
                    break;
                }
                int left = originX + commandList[cursor++];
                int top = originY + commandList[cursor++];
                int right = originX + commandList[cursor++];
                int bottom = originY + commandList[cursor++];
                state.setCommandClip(left, top, right, bottom);
                continue;
            }
            if ((command & COMMAND_MASK) == COMMAND_TEXTURE_INDEX) {
                int textureIndex = command & 0xF;
                state.texture = textureIndex >= 0 && state.textures != null && textureIndex < state.textures.length
                        ? state.textures[textureIndex]
                        : fallbackTexture;
                continue;
            }
            if ((command & COMMAND_MASK) == COMMAND_AFFINE_INDEX) {
                state.selectAffineIndex(command & 0xFF);
                continue;
            }
            if (command == COMMAND_CENTER) {
                if (cursor + 1 >= commandList.length) {
                    break;
                }
                state.centerX = originX + commandList[cursor++];
                state.centerY = originY + commandList[cursor++];
                continue;
            }
            if (command == COMMAND_PARALLEL_SCALE) {
                if (cursor + 1 >= commandList.length) {
                    break;
                }
                state.perspective = false;
                state.projectionScaleX = commandList[cursor++] / 4096.0f;
                state.projectionScaleY = commandList[cursor++] / 4096.0f;
                continue;
            }
            if (command == COMMAND_PARALLEL_SIZE) {
                if (cursor + 1 >= commandList.length) {
                    break;
                }
                state.perspective = false;
                int width = commandList[cursor++];
                int height = commandList[cursor++];
                state.projectionScaleX = width > 0 ? (float) surfaceWidth / width : 0.0f;
                state.projectionScaleY = height > 0 ? (float) surfaceHeight / height : 0.0f;
                continue;
            }
            if (command == COMMAND_PERSPECTIVE_FOV) {
                if (cursor + 2 >= commandList.length) {
                    break;
                }
                state.perspective = true;
                state.nearClip = commandList[cursor++];
                state.farClip = commandList[cursor++];
                int angle = commandList[cursor++];
                float angleRadians = (float) (angle * (Math.PI * 2.0 / 4096.0));
                float focal = angleRadians <= 0.0f || angleRadians >= Math.PI
                        ? surfaceWidth * 0.5f
                        : (float) ((surfaceWidth * 0.5f) / Math.tan(angleRadians * 0.5f));
                state.projectionScaleX = focal;
                state.projectionScaleY = focal;
                continue;
            }
            if (command == COMMAND_PERSPECTIVE_WH) {
                if (cursor + 3 >= commandList.length) {
                    break;
                }
                state.perspective = true;
                state.nearClip = commandList[cursor++];
                state.farClip = commandList[cursor++];
                int width = commandList[cursor++];
                int height = commandList[cursor++];
                float perspectiveWidth = width / 4096.0f;
                float perspectiveHeight = height / 4096.0f;
                state.projectionScaleX = width > 0 && state.nearClip > 0
                        ? (surfaceWidth * (float) state.nearClip) / perspectiveWidth
                        : 0.0f;
                state.projectionScaleY = height > 0 && state.nearClip > 0
                        ? (surfaceHeight * (float) state.nearClip) / perspectiveHeight
                        : 0.0f;
                continue;
            }
            if (command == COMMAND_AMBIENT_LIGHT) {
                if (cursor >= commandList.length) {
                    break;
                }
                state.ambientIntensity = commandList[cursor++];
                continue;
            }
            if (command == COMMAND_DIRECTION_LIGHT) {
                if (cursor + 3 >= commandList.length) {
                    break;
                }
                state.lightDirectionX = commandList[cursor++];
                state.lightDirectionY = commandList[cursor++];
                state.lightDirectionZ = commandList[cursor++];
                state.directionalIntensity = commandList[cursor++];
                continue;
            }
            if (command == COMMAND_THRESHOLD) {
                if (cursor + 2 >= commandList.length) {
                    break;
                }
                state.toonThreshold = commandList[cursor++];
                state.toonHigh = commandList[cursor++];
                state.toonLow = commandList[cursor++];
                continue;
            }
            if (command >= 0) {
                int next = renderPrimitiveCommand(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        state,
                        commandList,
                        cursor - 1,
                        pass
                );
                if (next <= cursor - 1) {
                    break;
                }
                cursor = next;
                rendered = true;
                continue;
            }
            break;
        }
        return rendered;
    }

    private static int[] buildPrimitiveCommandList(
            int command,
            int numPrimitives,
            int[] vertexCoords,
            int[] normals,
            int[] textureCoords,
            int[] colors
    ) {
        int primitiveType = (command >>> 24) & 0xFF;
        int verticesPerPrimitive = switch (primitiveType) {
            case PRIMITIVE_POINTS, PRIMITIVE_POINT_SPRITES -> 1;
            case PRIMITIVE_LINES -> 2;
            case PRIMITIVE_TRIANGLES -> 3;
            case PRIMITIVE_QUADS -> 4;
            default -> 0;
        };
        if (verticesPerPrimitive == 0) {
            return null;
        }

        int vertexInts = numPrimitives * verticesPerPrimitive * 3;
        int normalInts = switch (command & PDATA_NORMAL_MASK) {
            case PDATA_NORMAL_PER_FACE -> numPrimitives * 3;
            case PDATA_NORMAL_PER_VERTEX -> numPrimitives * verticesPerPrimitive * 3;
            default -> 0;
        };
        int textureInts;
        if (primitiveType == PRIMITIVE_POINT_SPRITES) {
            int spriteMode = command & PDATA_TEXCOORD_MASK;
            if (spriteMode == PDATA_POINT_SPRITE_PARAMS_PER_COMMAND) {
                textureInts = 8;
            } else if (spriteMode == 0) {
                textureInts = 0;
            } else {
                textureInts = numPrimitives * 8;
            }
        } else {
            textureInts = (command & PDATA_TEXCOORD_MASK) == PDATA_TEXCOORD_PER_VERTEX
                    ? numPrimitives * verticesPerPrimitive * 2
                    : 0;
        }
        int colorInts = switch (command & PDATA_COLOR_MASK) {
            case PDATA_COLOR_PER_COMMAND -> 1;
            case PDATA_COLOR_PER_FACE -> numPrimitives;
            default -> 0;
        };

        int[] safeVertices = copyPrefix(vertexCoords, vertexInts);
        int[] safeNormals = copyPrefix(normals, normalInts);
        int[] safeTextureCoords = copyPrefix(textureCoords, textureInts);
        int[] safeColors = copyPrefix(colors, colorInts);

        int payloadLength = 1 + vertexInts + normalInts + textureInts + colorInts + 2;
        int[] commandList = new int[payloadLength];
        int cursor = 0;
        commandList[cursor++] = COMMAND_LIST_VERSION_1_0;
        commandList[cursor++] = command | ((numPrimitives & 0xFF) << 16);
        System.arraycopy(safeVertices, 0, commandList, cursor, vertexInts);
        cursor += vertexInts;
        System.arraycopy(safeNormals, 0, commandList, cursor, normalInts);
        cursor += normalInts;
        System.arraycopy(safeTextureCoords, 0, commandList, cursor, textureInts);
        cursor += textureInts;
        System.arraycopy(safeColors, 0, commandList, cursor, colorInts);
        cursor += colorInts;
        commandList[cursor++] = COMMAND_END;
        return commandList;
    }

    private static int[] copyPrefix(int[] source, int length) {
        if (length <= 0) {
            return new int[0];
        }
        int[] copy = new int[length];
        if (source != null) {
            System.arraycopy(source, 0, copy, 0, Math.min(source.length, length));
        }
        return copy;
    }

    public static void drawFigure(
            Graphics graphics,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection,
            boolean perspective,
            int nearClip,
            int farClip,
            AffineTrans affineTrans,
            MascotFigure figure,
            Texture fallbackTexture,
            Effect3D effect
    ) {
        if (graphics == null || figure == null || figure.model() == null) {
            return;
        }
        int[] pixels = new int[surfaceWidth * surfaceHeight];
        float[] depthBuffer = new float[pixels.length];
        Arrays.fill(depthBuffer, Float.NEGATIVE_INFINITY);
        renderFigureToBuffers(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                screenCenterX,
                screenCenterY,
                projectionScaleX,
                projectionScaleY,
                yDownProjection,
                perspective,
                nearClip,
                farClip,
                affineTrans,
                figure,
                fallbackTexture,
                effect,
                RenderPass.OPAQUE
        );
        renderFigureToBuffers(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                screenCenterX,
                screenCenterY,
                projectionScaleX,
                projectionScaleY,
                yDownProjection,
                perspective,
                nearClip,
                farClip,
                affineTrans,
                figure,
                fallbackTexture,
                effect,
                RenderPass.TRANSLUCENT
        );
        graphics.drawRGB(pixels, 0, surfaceWidth, 0, 0, surfaceWidth, surfaceHeight, true);
    }

    public static void renderFigureToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection,
            boolean perspective,
            int nearClip,
            int farClip,
            AffineTrans affineTrans,
            MascotFigure figure,
            Texture fallbackTexture,
            Effect3D effect
    ) {
        renderFigureToBuffers(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                screenCenterX,
                screenCenterY,
                projectionScaleX,
                projectionScaleY,
                yDownProjection,
                perspective,
                nearClip,
                farClip,
                affineTrans,
                figure,
                fallbackTexture,
                effect,
                RenderPass.ALL
        );
    }

    public static void renderFigureToBuffers(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection,
            boolean perspective,
            int nearClip,
            int farClip,
            AffineTrans affineTrans,
            MascotFigure figure,
            Texture fallbackTexture,
            Effect3D effect,
            RenderPass pass
    ) {
        if (pixels == null || depthBuffer == null || figure == null || figure.model() == null) {
            return;
        }
        if (pixels.length < surfaceWidth * surfaceHeight || depthBuffer.length < surfaceWidth * surfaceHeight) {
            throw new IllegalArgumentException("Scene buffers are smaller than the target surface.");
        }
        float[] posedVertices = figure.vertices();
        int vertexCount = posedVertices.length / 3;
        float[] viewX = new float[vertexCount];
        float[] viewY = new float[vertexCount];
        float[] viewZ = new float[vertexCount];
        float[] screenX = new float[vertexCount];
        float[] screenY = new float[vertexCount];
        float[] depth = new float[vertexCount];
        Texture sphereMap = effect == null ? null : effect.getSphereMap();
        for (int i = 0; i < vertexCount; i++) {
            int source = i * 3;
            float x = posedVertices[source];
            float y = posedVertices[source + 1];
            float z = posedVertices[source + 2];
            float tx = x;
            float ty = y;
            float tz = z;
            if (affineTrans != null) {
                tx = transformX(affineTrans, x, y, z);
                ty = transformY(affineTrans, x, y, z);
                tz = transformZ(affineTrans, x, y, z);
            }
            viewX[i] = tx;
            viewY[i] = ty;
            viewZ[i] = tz;
            if (perspective) {
                screenX[i] = Float.NaN;
                screenY[i] = Float.NaN;
                depth[i] = Float.NEGATIVE_INFINITY;
            } else {
                // Figure vertices stay in 12-bit fixed-point through posing/affine transforms.
                // Parallel projection sizes are also fixed-point, so normalize coordinates first.
                screenX[i] = screenCenterX + (tx * projectionScaleX);
                screenY[i] = screenCenterY + ((yDownProjection ? ty : -ty) * projectionScaleY);
                depth[i] = -tz;
            }
        }

        for (MbacModel.Polygon polygon : figure.model().polygons()) {
            int patternMask = polygon.patternMask();
            if (patternMask != 0 && (patternMask & figure.patternMask()) != patternMask) {
                continue;
            }
            int[] indices = polygon.indices();
            if (indices.length < 3) {
                continue;
            }
            Texture polygonTexture = polygon.textureCoords() == null ? null : figure.texture(polygon.textureIndex());
            if (polygonTexture == null) {
                polygonTexture = fallbackTexture;
            }
            int polygonBlendMode = effect != null && !effect.isSemiTransparentEnabled()
                    ? 0
                    : polygon.blendMode();
            if (!rendersInPass(polygonBlendMode, pass)) {
                continue;
            }
            if (perspective) {
                rasterizePerspectivePolygon(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        screenCenterX,
                        screenCenterY,
                        projectionScaleX,
                        projectionScaleY,
                        yDownProjection,
                        nearClip,
                        farClip,
                        polygon,
                        polygonBlendMode,
                        polygonTexture,
                        sphereMap,
                        viewX,
                        viewY,
                        viewZ,
                        indices,
                        polygon.textureCoords()
                );
            } else if (indices.length == 3) {
                rasterizeTriangle(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        polygon,
                        polygonBlendMode,
                        polygonTexture,
                        sphereMap,
                        screenX, screenY, depth,
                        indices[0], indices[1], indices[2],
                        polygon.textureCoords(),
                        0, 2, 4
                );
            } else if (indices.length == 4) {
                float[] uv = polygon.textureCoords();
                rasterizeTriangle(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        polygon,
                        polygonBlendMode,
                        polygonTexture,
                        sphereMap,
                        screenX, screenY, depth,
                        indices[0], indices[1], indices[2],
                        uv,
                        0, 2, 4
                );
                rasterizeTriangle(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        polygon,
                        polygonBlendMode,
                        polygonTexture,
                        sphereMap,
                        screenX, screenY, depth,
                        indices[2], indices[1], indices[3],
                        uv,
                        4, 2, 6
                );
            }
        }
    }

    private static void rasterizePerspectivePolygon(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection,
            int nearClip,
            int farClip,
            MbacModel.Polygon polygon,
            int polygonBlendMode,
            Texture texture,
            Texture sphereMap,
            float[] viewX,
            float[] viewY,
            float[] viewZ,
            int[] indices,
            float[] uv
    ) {
        List<PolygonVertex> vertices = new ArrayList<>(indices.length);
        for (int i = 0; i < indices.length; i++) {
            int vertexIndex = indices[i];
            float u = uv == null ? 0.0f : uv[i * 2];
            float v = uv == null ? 0.0f : uv[i * 2 + 1];
            vertices.add(new PolygonVertex(
                    viewX[vertexIndex],
                    viewY[vertexIndex],
                    viewZ[vertexIndex],
                    u,
                    v,
                    1.0f
            ));
        }
        if (vertices.size() == 3) {
            rasterizePerspectiveTriangle(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    screenCenterX,
                    screenCenterY,
                    projectionScaleX,
                    projectionScaleY,
                    nearClip,
                    farClip,
                    polygon,
                    polygonBlendMode,
                    texture,
                    sphereMap,
                    vertices.get(0),
                    vertices.get(1),
                    vertices.get(2),
                    uv != null
            );
            return;
        }
        if (vertices.size() == 4) {
            rasterizePerspectiveTriangle(
                    pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, nearClip, farClip,
                    polygon, polygonBlendMode, texture, sphereMap,
                    vertices.get(0), vertices.get(1), vertices.get(2), uv != null
            );
            rasterizePerspectiveTriangle(
                    pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, nearClip, farClip,
                    polygon, polygonBlendMode, texture, sphereMap,
                    vertices.get(2), vertices.get(1), vertices.get(3), uv != null
            );
            return;
        }
        List<PolygonVertex> clipped = clipPerspectivePolygon(vertices, nearClip, farClip);
        if (clipped.size() < 3) {
            return;
        }
        List<ProjectedVertex> projected = projectPolygon(
                clipped,
                screenCenterX,
                screenCenterY,
                projectionScaleX,
                projectionScaleY,
                yDownProjection
        );
        if (projected.size() < 3) {
            return;
        }
        ProjectedVertex first = projected.get(0);
        for (int i = 1; i < projected.size() - 1; i++) {
            rasterizeTriangleProjected(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    polygon,
                    polygonBlendMode,
                    texture,
                    sphereMap,
                    first,
                    projected.get(i),
                    projected.get(i + 1),
                    uv != null
            );
        }
    }

    private static void rasterizePerspectiveTriangle(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            int nearClip,
            int farClip,
            MbacModel.Polygon polygon,
            int polygonBlendMode,
            Texture texture,
            Texture sphereMap,
            PolygonVertex v0,
            PolygonVertex v1,
            PolygonVertex v2,
            boolean textured
    ) {
        List<PolygonVertex> clipped = new ArrayList<>(3);
        clipped.add(v0);
        clipped.add(v1);
        clipped.add(v2);
        clipped = clipPerspectivePolygon(clipped, nearClip, farClip);
        if (clipped.size() < 3) {
            return;
        }
        List<ProjectedVertex> projected = projectPolygon(
                clipped,
                screenCenterX,
                screenCenterY,
                projectionScaleX,
                projectionScaleY,
                true
        );
        if (projected.size() < 3) {
            return;
        }
        ProjectedVertex first = projected.get(0);
        for (int i = 1; i < projected.size() - 1; i++) {
            rasterizeTriangleProjected(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    polygon,
                    polygonBlendMode,
                    texture,
                    sphereMap,
                    first,
                    projected.get(i),
                    projected.get(i + 1),
                    textured
            );
        }
    }

    private static List<PolygonVertex> clipPerspectivePolygon(List<PolygonVertex> vertices, int nearClip, int farClip) {
        List<PolygonVertex> clipped = vertices;
        clipped = clipAgainstNearPlane(clipped, Math.max(DEPTH_EPSILON, nearClip > 0 ? nearClip : DEPTH_EPSILON));
        if (clipped.size() < 3) {
            return clipped;
        }
        if (farClip > 0) {
            clipped = clipAgainstFarPlane(clipped, farClip);
        }
        return clipped;
    }

    private static boolean polygonNeedsPerspectiveClipping(List<PolygonVertex> vertices, int nearClip, int farClip) {
        float nearZ = Math.max(DEPTH_EPSILON, nearClip > 0 ? nearClip : DEPTH_EPSILON);
        for (PolygonVertex vertex : vertices) {
            if (vertex.z() < nearZ) {
                return true;
            }
            if (farClip > 0 && vertex.z() > farClip) {
                return true;
            }
        }
        return false;
    }

    private static List<ProjectedVertex> projectPolygon(
            List<PolygonVertex> vertices,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection
    ) {
        List<ProjectedVertex> projected = new ArrayList<>(vertices.size());
        for (PolygonVertex vertex : vertices) {
            ProjectedVertex projectedVertex = projectVertex(
                    vertex,
                    screenCenterX,
                    screenCenterY,
                    projectionScaleX,
                    projectionScaleY,
                    yDownProjection
            );
            if (projectedVertex == null) {
                return List.of();
            }
            projected.add(projectedVertex);
        }
        return projected;
    }

    private static boolean isStripOrderedQuad(
            List<PolygonVertex> vertices,
            float[] uv,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection
    ) {
        if (uv != null && uv.length >= 8 && isSelfIntersectingQuad(
                uv[0], uv[1],
                uv[2], uv[3],
                uv[4], uv[5],
                uv[6], uv[7]
        )) {
            return true;
        }
        ProjectedVertex p0 = projectVertex(vertices.get(0), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, yDownProjection);
        ProjectedVertex p1 = projectVertex(vertices.get(1), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, yDownProjection);
        ProjectedVertex p2 = projectVertex(vertices.get(2), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, yDownProjection);
        ProjectedVertex p3 = projectVertex(vertices.get(3), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY, yDownProjection);
        if (p0 == null || p1 == null || p2 == null || p3 == null) {
            return false;
        }
        return isSelfIntersectingQuad(
                p0.screenX(), p0.screenY(),
                p1.screenX(), p1.screenY(),
                p2.screenX(), p2.screenY(),
                p3.screenX(), p3.screenY()
        );
    }

    private static void rasterizeTriangle(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            MbacModel.Polygon polygon,
            int polygonBlendMode,
            Texture texture,
            Texture sphereMap,
            float[] screenX,
            float[] screenY,
            float[] depth,
            int i0,
            int i1,
            int i2,
            float[] uv,
            int uv0,
            int uv1,
            int uv2
    ) {
        rasterizeTriangleProjected(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                polygon,
                polygonBlendMode,
                texture,
                sphereMap,
                new ProjectedVertex(screenX[i0], screenY[i0], depth[i0], 0.0f, uv == null ? 0.0f : uv[uv0], uv == null ? 0.0f : uv[uv0 + 1], 1.0f),
                new ProjectedVertex(screenX[i1], screenY[i1], depth[i1], 0.0f, uv == null ? 0.0f : uv[uv1], uv == null ? 0.0f : uv[uv1 + 1], 1.0f),
                new ProjectedVertex(screenX[i2], screenY[i2], depth[i2], 0.0f, uv == null ? 0.0f : uv[uv2], uv == null ? 0.0f : uv[uv2 + 1], 1.0f),
                uv != null
        );
    }

    private static void rasterizeTriangleProjected(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            MbacModel.Polygon polygon,
            int polygonBlendMode,
            Texture texture,
            Texture sphereMap,
            ProjectedVertex v0,
            ProjectedVertex v1,
            ProjectedVertex v2,
            boolean textured
    ) {
        float x0 = v0.screenX();
        float y0 = v0.screenY();
        float z0 = v0.depth();
        float x1 = v1.screenX();
        float y1 = v1.screenY();
        float z1 = v1.depth();
        float x2 = v2.screenX();
        float y2 = v2.screenY();
        float z2 = v2.depth();
        if (!Float.isFinite(x0) || !Float.isFinite(y0)
                || !Float.isFinite(x1) || !Float.isFinite(y1)
                || !Float.isFinite(x2) || !Float.isFinite(y2)) {
            return;
        }
        if (!polygon.doubleSided()) {
            // Mascot figures still contain some front-facing panels with mixed winding after skinning,
            // so keep single-sided faces visible here and rely on the corrected quad split instead.
        }
        int minX = Math.max(clipX, Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)))));
        int minY = Math.max(clipY, Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)))));
        int maxX = Math.min(clipX + clipWidth - 1, Math.min(surfaceWidth - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2)))));
        int maxY = Math.min(clipY + clipHeight - 1, Math.min(surfaceHeight - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2)))));
        if (minX > maxX || minY > maxY) {
            return;
        }
        int fx0 = toRasterFixed(x0);
        int fy0 = toRasterFixed(y0);
        int fx1 = toRasterFixed(x1);
        int fy1 = toRasterFixed(y1);
        int fx2 = toRasterFixed(x2);
        int fy2 = toRasterFixed(y2);
        long rasterArea = edgeFixed(fx0, fy0, fx1, fy1, fx2, fy2);
        if (rasterArea == 0L) {
            return;
        }
        boolean flipped = rasterArea < 0L;
        boolean topLeft12 = isCoverageTopLeftEdge(fx1, fy1, fx2, fy2, flipped);
        boolean topLeft20 = isCoverageTopLeftEdge(fx2, fy2, fx0, fy0, flipped);
        boolean topLeft01 = isCoverageTopLeftEdge(fx0, fy0, fx1, fy1, flipped);
        float invRasterArea = 1.0f / (float) Math.abs(rasterArea);
        boolean perspectiveCorrect = v0.reciprocalDepth() > 0.0f
                || v1.reciprocalDepth() > 0.0f
                || v2.reciprocalDepth() > 0.0f;

        for (int y = minY; y <= maxY; y++) {
            int rasterY = (y << RASTER_SUBPIXEL_SHIFT) + (RASTER_SUBPIXEL_SCALE >> 1);
            for (int x = minX; x <= maxX; x++) {
                int rasterX = (x << RASTER_SUBPIXEL_SHIFT) + (RASTER_SUBPIXEL_SCALE >> 1);
                long coverage0 = edgeFixed(fx1, fy1, fx2, fy2, rasterX, rasterY);
                long coverage1 = edgeFixed(fx2, fy2, fx0, fy0, rasterX, rasterY);
                long coverage2 = edgeFixed(fx0, fy0, fx1, fy1, rasterX, rasterY);
                if (flipped) {
                    coverage0 = -coverage0;
                    coverage1 = -coverage1;
                    coverage2 = -coverage2;
                }
                if (coverage0 < 0L || (coverage0 == 0L && !topLeft12)
                        || coverage1 < 0L || (coverage1 == 0L && !topLeft20)
                        || coverage2 < 0L || (coverage2 == 0L && !topLeft01)) {
                    continue;
                }
                float w0 = coverage0 * invRasterArea;
                float w1 = coverage1 * invRasterArea;
                float w2 = coverage2 * invRasterArea;
                float pixelDepth = w0 * z0 + w1 * z1 + w2 * z2;
                int index = y * surfaceWidth + x;
                if (pixelDepth < depthBuffer[index] - DEPTH_EPSILON) {
                    continue;
                }
                int argb;
                if (texture != null && textured) {
                    float u;
                    float v;
                    if (perspectiveCorrect) {
                        float rw0 = w0 * v0.reciprocalDepth();
                        float rw1 = w1 * v1.reciprocalDepth();
                        float rw2 = w2 * v2.reciprocalDepth();
                        float reciprocalWeight = rw0 + rw1 + rw2;
                        if (Math.abs(reciprocalWeight) <= DEPTH_EPSILON) {
                            continue;
                        }
                        float invReciprocalWeight = 1.0f / reciprocalWeight;
                        u = ((rw0 * v0.u()) + (rw1 * v1.u()) + (rw2 * v2.u())) * invReciprocalWeight;
                        v = ((rw0 * v0.v()) + (rw1 * v1.v()) + (rw2 * v2.v())) * invReciprocalWeight;
                    } else {
                        u = (w0 * v0.u()) + (w1 * v1.u()) + (w2 * v2.u());
                        v = (w0 * v0.v()) + (w1 * v1.v()) + (w2 * v2.v());
                    }
                    argb = texture.sampleColor(u, v, polygon.transparent());
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                } else {
                    argb = polygon.color();
                }
                argb = applySphereMap(argb, sphereMap, surfaceWidth, surfaceHeight, x, y);
                pixels[index] = blend(argb, pixels[index], polygonBlendMode);
                if (writesDepth(polygonBlendMode)) {
                    depthBuffer[index] = pixelDepth;
                }
            }
        }
    }

    private static float edgeFunction(float ax, float ay, float bx, float by, float px, float py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }

    private static boolean isSelfIntersectingQuad(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3
    ) {
        if (!Float.isFinite(x0) || !Float.isFinite(y0)
                || !Float.isFinite(x1) || !Float.isFinite(y1)
                || !Float.isFinite(x2) || !Float.isFinite(y2)
                || !Float.isFinite(x3) || !Float.isFinite(y3)) {
            return false;
        }
        return segmentsIntersect(x1, y1, x2, y2, x3, y3, x0, y0);
    }

    private static boolean isStripOrderedQuad(
            float[] uv,
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        return (uv != null
                && uv.length >= 8
                && isSelfIntersectingQuad(
                uv[0], uv[1],
                uv[2], uv[3],
                uv[4], uv[5],
                uv[6], uv[7]
        )) || isSelfIntersectingQuad(x0, y0, x1, y1, x2, y2, x3, y3);
    }

    private static boolean isStripOrderedQuad(
            float u0,
            float v0,
            float u1,
            float v1,
            float u2,
            float v2,
            float u3,
            float v3,
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        return isSelfIntersectingQuad(u0, v0, u1, v1, u2, v2, u3, v3)
                || isSelfIntersectingQuad(x0, y0, x1, y1, x2, y2, x3, y3);
    }

    private static boolean segmentsIntersect(
            float ax, float ay,
            float bx, float by,
            float cx, float cy,
            float dx, float dy
    ) {
        float abC = edgeFunction(ax, ay, bx, by, cx, cy);
        float abD = edgeFunction(ax, ay, bx, by, dx, dy);
        float cdA = edgeFunction(cx, cy, dx, dy, ax, ay);
        float cdB = edgeFunction(cx, cy, dx, dy, bx, by);
        return hasOppositeSigns(abC, abD) && hasOppositeSigns(cdA, cdB);
    }

    private static boolean hasOppositeSigns(float a, float b) {
        return (a < -DEPTH_EPSILON && b > DEPTH_EPSILON) || (a > DEPTH_EPSILON && b < -DEPTH_EPSILON);
    }

    private static int toRasterFixed(float value) {
        return Math.round(value * RASTER_SUBPIXEL_SCALE);
    }

    private static long edgeFixed(int ax, int ay, int bx, int by, int px, int py) {
        return (long) (px - ax) * (by - ay) - (long) (py - ay) * (bx - ax);
    }

    private static boolean isCoverageTopLeftEdge(int ax, int ay, int bx, int by, boolean flipped) {
        int dy = by - ay;
        int dx = bx - ax;
        if (!flipped) {
            return dy > 0 || (dy == 0 && dx < 0);
        }
        return dy < 0 || (dy == 0 && dx > 0);
    }

    private static float transformX(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m00) + mulRaw(y, affineTrans.m01) + mulRaw(z, affineTrans.m02) + affineTrans.m03;
    }

    private static float transformY(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m10) + mulRaw(y, affineTrans.m11) + mulRaw(z, affineTrans.m12) + affineTrans.m13;
    }

    private static float transformZ(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m20) + mulRaw(y, affineTrans.m21) + mulRaw(z, affineTrans.m22) + affineTrans.m23;
    }

    private static float mulRaw(float value, int fixed) {
        return (value * fixed) / 4096.0f;
    }

    private static int blend(int src, int dst, int blendMode) {
        return switch (blendMode) {
            case 32 -> average(src, dst);
            case 64 -> add(src, dst);
            case 96 -> subtract(src, dst);
            case 2 -> average(src, dst);
            case 4 -> add(src, dst);
            case 6 -> subtract(src, dst);
            default -> alphaBlend(src, dst);
        };
    }

    private static int alphaBlend(int src, int dst) {
        int alpha = (src >>> 24) & 0xFF;
        if (alpha <= 0) {
            return dst;
        }
        if (alpha >= 255) {
            return src;
        }
        int inv = 255 - alpha;
        int red = (((src >> 16) & 0xFF) * alpha + ((dst >> 16) & 0xFF) * inv) / 255;
        int green = (((src >> 8) & 0xFF) * alpha + ((dst >> 8) & 0xFF) * inv) / 255;
        int blue = ((src & 0xFF) * alpha + (dst & 0xFF) * inv) / 255;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int average(int src, int dst) {
        int red = (((src >> 16) & 0xFF) + ((dst >> 16) & 0xFF)) >> 1;
        int green = (((src >> 8) & 0xFF) + ((dst >> 8) & 0xFF)) >> 1;
        int blue = ((src & 0xFF) + (dst & 0xFF)) >> 1;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int add(int src, int dst) {
        int red = Math.min(255, ((src >> 16) & 0xFF) + ((dst >> 16) & 0xFF));
        int green = Math.min(255, ((src >> 8) & 0xFF) + ((dst >> 8) & 0xFF));
        int blue = Math.min(255, (src & 0xFF) + (dst & 0xFF));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int subtract(int src, int dst) {
        int red = Math.max(0, ((dst >> 16) & 0xFF) - ((src >> 16) & 0xFF));
        int green = Math.max(0, ((dst >> 8) & 0xFF) - ((src >> 8) & 0xFF));
        int blue = Math.max(0, (dst & 0xFF) - (src & 0xFF));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int applySphereMap(int color, Texture sphereMap, int targetWidth, int targetHeight, int x, int y) {
        if (sphereMap == null || targetWidth <= 0 || targetHeight <= 0) {
            return color;
        }
        float u = (x + 0.5f) * sphereMap.getWidth() / targetWidth;
        float v = (y + 0.5f) * sphereMap.getHeight() / targetHeight;
        int sphereColor = sphereMap.sampleColor(u, v, false);
        int alpha = (color >>> 24) & 0xFF;
        int red = ((((color >>> 16) & 0xFF) + ((sphereColor >>> 16) & 0xFF)) >> 1);
        int green = ((((color >>> 8) & 0xFF) + ((sphereColor >>> 8) & 0xFF)) >> 1);
        int blue = (((color & 0xFF) + (sphereColor & 0xFF)) >> 1);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int modulateShade(int color, float shade) {
        float clampedShade = clamp01(shade);
        int alpha = (color >>> 24) & 0xFF;
        int red = Math.min(255, Math.max(0, Math.round(((color >>> 16) & 0xFF) * clampedShade)));
        int green = Math.min(255, Math.max(0, Math.round(((color >>> 8) & 0xFF) * clampedShade)));
        int blue = Math.min(255, Math.max(0, Math.round((color & 0xFF) * clampedShade)));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int sampleCommandTexture(Texture texture, float u, float v, boolean transparent) {
        if (texture == null || texture.getWidth() <= 0 || texture.getHeight() <= 0) {
            return 0;
        }
        // Command-list texture coordinates in these Vodafone titles are authored as absolute
        // texel positions inside an atlas, so preserve the integer boundaries the content uses
        // and only clamp tiny overshoots back inside the sheet.
        float maxU = Math.nextDown((float) texture.getWidth());
        float maxV = Math.nextDown((float) texture.getHeight());
        return texture.sampleColor(
                Math.max(0.0f, Math.min(u, maxU)),
                Math.max(0.0f, Math.min(v, maxV)),
                transparent
        );
    }

    private static List<PolygonVertex> clipAgainstNearPlane(List<PolygonVertex> vertices, float nearZ) {
        return clipPolygon(vertices, vertex -> vertex.z() >= nearZ, (start, end) -> interpolateAtZ(start, end, nearZ));
    }

    private static List<PolygonVertex> clipAgainstFarPlane(List<PolygonVertex> vertices, float farZ) {
        return clipPolygon(vertices, vertex -> vertex.z() <= farZ, (start, end) -> interpolateAtZ(start, end, farZ));
    }

    private static List<PolygonVertex> clipPolygon(
            List<PolygonVertex> vertices,
            java.util.function.Predicate<PolygonVertex> insideTest,
            java.util.function.BiFunction<PolygonVertex, PolygonVertex, PolygonVertex> intersection
    ) {
        if (vertices.isEmpty()) {
            return vertices;
        }
        List<PolygonVertex> output = new ArrayList<>(vertices.size() + 2);
        PolygonVertex previous = vertices.get(vertices.size() - 1);
        boolean previousInside = insideTest.test(previous);
        for (PolygonVertex current : vertices) {
            boolean currentInside = insideTest.test(current);
            if (currentInside != previousInside) {
                output.add(intersection.apply(previous, current));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static PolygonVertex interpolateAtZ(PolygonVertex start, PolygonVertex end, float targetZ) {
        float delta = end.z() - start.z();
        if (Math.abs(delta) <= DEPTH_EPSILON) {
            return start;
        }
        float t = (targetZ - start.z()) / delta;
        return new PolygonVertex(
                lerp(start.x(), end.x(), t),
                lerp(start.y(), end.y(), t),
                targetZ,
                lerp(start.u(), end.u(), t),
                lerp(start.v(), end.v(), t),
                lerp(start.shade(), end.shade(), t)
        );
    }

    private static ProjectedVertex projectVertex(
            PolygonVertex vertex,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY,
            boolean yDownProjection
    ) {
        if (vertex.z() <= DEPTH_EPSILON) {
            return null;
        }
        return new ProjectedVertex(
                screenCenterX + ((vertex.x() * projectionScaleX) / vertex.z()),
                screenCenterY + (((yDownProjection ? vertex.y() : -vertex.y()) * projectionScaleY) / vertex.z()),
                -vertex.z(),
                1.0f / vertex.z(),
                vertex.u(),
                vertex.v(),
                vertex.shade()
        );
    }

    private static float lerp(float start, float end, float amount) {
        return start + ((end - start) * amount);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float transformNormalX(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m00) + mulRaw(y, affineTrans.m01) + mulRaw(z, affineTrans.m02);
    }

    private static float transformNormalY(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m10) + mulRaw(y, affineTrans.m11) + mulRaw(z, affineTrans.m12);
    }

    private static float transformNormalZ(AffineTrans affineTrans, float x, float y, float z) {
        return mulRaw(x, affineTrans.m20) + mulRaw(y, affineTrans.m21) + mulRaw(z, affineTrans.m22);
    }

    private static float computeCommandShade(CommandState state, float nx, float ny, float nz) {
        if (state == null || !state.lightingEnabled) {
            return 1.0f;
        }
        float tx = nx;
        float ty = ny;
        float tz = nz;
        if (state.affineTrans != null) {
            tx = transformNormalX(state.affineTrans, nx, ny, nz);
            ty = transformNormalY(state.affineTrans, nx, ny, nz);
            tz = transformNormalZ(state.affineTrans, nx, ny, nz);
        }
        float normalLength = (float) Math.sqrt((tx * tx) + (ty * ty) + (tz * tz));
        if (normalLength <= DEPTH_EPSILON) {
            return 1.0f;
        }
        tx /= normalLength;
        ty /= normalLength;
        tz /= normalLength;

        float lightX = state.lightDirectionX;
        float lightY = state.lightDirectionY;
        float lightZ = state.lightDirectionZ;
        float lightLength = (float) Math.sqrt((lightX * lightX) + (lightY * lightY) + (lightZ * lightZ));
        float directional = 0.0f;
        if (lightLength > DEPTH_EPSILON && state.directionalIntensity > 0) {
            lightX /= lightLength;
            lightY /= lightLength;
            lightZ /= lightLength;
            directional = Math.max(0.0f, (tx * lightX) + (ty * lightY) + (tz * lightZ));
        }
        float ambient = state.ambientIntensity / 4096.0f;
        float diffuse = directional * (state.directionalIntensity / 4096.0f);
        return applyCommandShading(state, ambient + diffuse);
    }

    private static float applyCommandShading(CommandState state, float shade) {
        float clamped = clamp01(shade);
        if (state == null || !state.toonShadingEnabled) {
            return clamped;
        }
        int threshold = clampByte(state.toonThreshold);
        int high = clampByte(state.toonHigh);
        int low = clampByte(state.toonLow);
        int level = Math.round(clamped * 255.0f);
        return (level >= threshold ? high : low) / 255.0f;
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int renderPrimitiveCommand(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            CommandState state,
            int[] commandList,
            int commandIndex,
            RenderPass pass
    ) {
        int command = commandList[commandIndex];
        int primitiveType = (command >>> 24) & 0xFF;
        int primitiveCount = (command >>> 16) & 0xFF;
        if (primitiveCount <= 0) {
            return commandIndex + 1;
        }
        int effectiveClipX = Math.max(clipX, state.clipX);
        int effectiveClipY = Math.max(clipY, state.clipY);
        int effectiveClipRight = Math.min(clipX + clipWidth, state.clipX + state.clipWidth);
        int effectiveClipBottom = Math.min(clipY + clipHeight, state.clipY + state.clipHeight);
        int effectiveClipWidth = Math.max(0, effectiveClipRight - effectiveClipX);
        int effectiveClipHeight = Math.max(0, effectiveClipBottom - effectiveClipY);
        int cursor = commandIndex + 1;
        int blendMode = state.semiTransparentEnabled ? (command & 0x60) : 0;
        boolean sphereMapEnabled = state.sphereMapEnabled && (command & ENV_ATTR_SPHERE_MAP) != 0;
        boolean colorKeyEnabled = (command & PATTR_COLORKEY) != 0;
        switch (primitiveType) {
            case PRIMITIVE_LINES -> {
                int vertexInts = primitiveCount * 6;
                if (cursor + vertexInts > commandList.length) {
                    return commandIndex;
                }
                float[] vertices = new float[vertexInts];
                for (int i = 0; i < vertexInts; i++) {
                    vertices[i] = commandList[cursor++];
                }
                int[] colors = null;
                int colorCount = switch (command & PDATA_COLOR_MASK) {
                    case PDATA_COLOR_PER_COMMAND -> 1;
                    case PDATA_COLOR_PER_FACE -> primitiveCount;
                    default -> 0;
                };
                if (colorCount > 0) {
                    if (cursor + colorCount > commandList.length) {
                        return commandIndex;
                    }
                    colors = new int[colorCount];
                    for (int i = 0; i < colorCount; i++) {
                        colors[i] = commandList[cursor++];
                    }
                }
                if (rendersInPass(blendMode, pass)) {
                    for (int i = 0; i < primitiveCount; i++) {
                        int base = i * 6;
                        ProjectedVertex v0 = transformAndProject(
                                state,
                                vertices[base],
                                vertices[base + 1],
                                vertices[base + 2],
                                0.0f,
                                0.0f,
                                1.0f
                        );
                        ProjectedVertex v1 = transformAndProject(
                                state,
                                vertices[base + 3],
                                vertices[base + 4],
                                vertices[base + 5],
                                0.0f,
                                0.0f,
                                1.0f
                        );
                        int colorValue = 0xFFFFFFFF;
                        if (colors != null) {
                            colorValue = (colors.length == 1 ? colors[0] : colors[i]) | 0xFF000000;
                        }
                        drawLine(
                                pixels,
                                depthBuffer,
                                surfaceWidth,
                                surfaceHeight,
                                effectiveClipX,
                                effectiveClipY,
                                effectiveClipWidth,
                                effectiveClipHeight,
                                v0,
                                v1,
                                colorValue,
                                blendMode
                        );
                    }
                }
                return cursor;
            }
            case PRIMITIVE_TRIANGLES -> {
                return renderTriangleLikePrimitive(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        effectiveClipX,
                        effectiveClipY,
                        effectiveClipWidth,
                        effectiveClipHeight,
                        state,
                        commandList,
                        cursor,
                        command,
                        primitiveCount,
                        3,
                        blendMode,
                        sphereMapEnabled ? state.sphereMap : null,
                        colorKeyEnabled,
                        pass
                );
            }
            case PRIMITIVE_QUADS -> {
                return renderTriangleLikePrimitive(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        effectiveClipX,
                        effectiveClipY,
                        effectiveClipWidth,
                        effectiveClipHeight,
                        state,
                        commandList,
                        cursor,
                        command,
                        primitiveCount,
                        4,
                        blendMode,
                        sphereMapEnabled ? state.sphereMap : null,
                        colorKeyEnabled,
                        pass
                );
            }
            case PRIMITIVE_POINT_SPRITES -> {
                int vertexInts = primitiveCount * 3;
                int spriteMode = command & PDATA_TEXCOORD_MASK;
                int spriteInts = spriteMode == PDATA_POINT_SPRITE_PARAMS_PER_COMMAND
                        ? 8
                        : (spriteMode == 0 ? 0 : primitiveCount * 8);
                if (cursor + vertexInts + spriteInts > commandList.length) {
                    return commandIndex;
                }
                float[] vertices = new float[vertexInts];
                for (int i = 0; i < vertexInts; i++) {
                    vertices[i] = commandList[cursor++];
                }
                int[] spriteParams = new int[spriteInts];
                for (int i = 0; i < spriteInts; i++) {
                    spriteParams[i] = commandList[cursor++];
                }
                if (rendersInPass(blendMode, pass)) {
                    for (int i = 0; i < primitiveCount; i++) {
                        int vertexBase = i * 3;
                        int spriteBase = spriteMode == PDATA_POINT_SPRITE_PARAMS_PER_COMMAND ? 0 : i * 8;
                        renderPointSprite(
                                pixels,
                                depthBuffer,
                                surfaceWidth,
                                surfaceHeight,
                                effectiveClipX,
                                effectiveClipY,
                                effectiveClipWidth,
                                effectiveClipHeight,
                                state,
                                vertices[vertexBase],
                                vertices[vertexBase + 1],
                                vertices[vertexBase + 2],
                                spriteParams,
                                spriteBase,
                                blendMode,
                                sphereMapEnabled ? state.sphereMap : null,
                                colorKeyEnabled
                        );
                    }
                }
                return cursor;
            }
            default -> {
                return commandIndex;
            }
        }
    }

    private static int renderTriangleLikePrimitive(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            CommandState state,
            int[] commandList,
            int cursor,
            int command,
            int primitiveCount,
            int verticesPerPrimitive,
            int blendMode,
            Texture sphereMap,
            boolean colorKeyEnabled,
            RenderPass pass
    ) {
        int vertexInts = primitiveCount * verticesPerPrimitive * 3;
        if (cursor + vertexInts > commandList.length) {
            return cursor - 1;
        }
        float[] vertices = new float[vertexInts];
        for (int i = 0; i < vertexInts; i++) {
            vertices[i] = commandList[cursor++];
        }
        int normalInts = switch (command & PDATA_NORMAL_MASK) {
            case PDATA_NORMAL_PER_FACE -> primitiveCount * 3;
            case PDATA_NORMAL_PER_VERTEX -> primitiveCount * verticesPerPrimitive * 3;
            default -> 0;
        };
        if (normalInts > 0) {
            if (cursor + normalInts > commandList.length) {
                return cursor - 1 - vertexInts;
            }
        }
        float[] normals = null;
        if (normalInts > 0) {
            normals = new float[normalInts];
            for (int i = 0; i < normalInts; i++) {
                normals[i] = commandList[cursor++];
            }
        }
        boolean hasTextureCoords = (command & PDATA_TEXCOORD_MASK) == PDATA_TEXCOORD_PER_VERTEX;
        float[] texCoords = null;
        if (hasTextureCoords) {
            int texInts = primitiveCount * verticesPerPrimitive * 2;
            if (cursor + texInts > commandList.length) {
                return cursor - 1 - vertexInts - normalInts;
            }
            texCoords = new float[texInts];
            for (int i = 0; i < texInts; i++) {
                texCoords[i] = commandList[cursor++];
            }
        }
        int[] colors = null;
        int colorCount = switch (command & PDATA_COLOR_MASK) {
            case PDATA_COLOR_PER_COMMAND -> 1;
            case PDATA_COLOR_PER_FACE -> primitiveCount;
            default -> 0;
        };
        if (colorCount > 0) {
            if (cursor + colorCount > commandList.length) {
                return cursor - 1 - vertexInts - normalInts;
            }
            colors = new int[colorCount];
            for (int i = 0; i < colorCount; i++) {
                colors[i] = commandList[cursor++];
            }
        }
        if (!rendersInPass(blendMode, pass)) {
            return cursor;
        }
        for (int i = 0; i < primitiveCount; i++) {
            int vertexBase = i * verticesPerPrimitive * 3;
            int texBase = texCoords == null ? 0 : i * verticesPerPrimitive * 2;
            int normalBase = 0;
            float faceShade = 1.0f;
            if (normals != null) {
                if ((command & PDATA_NORMAL_MASK) == PDATA_NORMAL_PER_FACE) {
                    normalBase = i * 3;
                    faceShade = computeCommandShade(
                            state,
                            normals[normalBase],
                            normals[normalBase + 1],
                            normals[normalBase + 2]
                    );
                } else if ((command & PDATA_NORMAL_MASK) == PDATA_NORMAL_PER_VERTEX) {
                    normalBase = i * verticesPerPrimitive * 3;
                }
            }
            int color = colors == null ? 0xFFFFFFFF : 0xFF000000 | (colors.length == 1 ? colors[0] : colors[i]);
            if (state.perspective) {
                List<PolygonVertex> polygonVertices = new ArrayList<>(verticesPerPrimitive);
                for (int vertex = 0; vertex < verticesPerPrimitive; vertex++) {
                    int source = vertexBase + vertex * 3;
                    float u = texCoords == null ? 0.0f : texCoords[texBase + vertex * 2];
                    float v = texCoords == null ? 0.0f : texCoords[texBase + vertex * 2 + 1];
                    float shade = faceShade;
                    if (normals != null && (command & PDATA_NORMAL_MASK) == PDATA_NORMAL_PER_VERTEX) {
                        int vertexNormalBase = normalBase + vertex * 3;
                        shade = computeCommandShade(
                                state,
                                normals[vertexNormalBase],
                                normals[vertexNormalBase + 1],
                                normals[vertexNormalBase + 2]
                        );
                    }
                    polygonVertices.add(transformVertex(
                            state,
                            vertices[source],
                            vertices[source + 1],
                            vertices[source + 2],
                            u,
                            v,
                            shade
                    ));
                }
                rasterizePerspectiveCommandPolygon(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        state,
                        polygonVertices,
                        state.texture,
                        color,
                        blendMode,
                        sphereMap,
                        colorKeyEnabled,
                        texCoords != null
                );
                continue;
            }
            ProjectedVertex[] projected = new ProjectedVertex[verticesPerPrimitive];
            for (int vertex = 0; vertex < verticesPerPrimitive; vertex++) {
                int source = vertexBase + vertex * 3;
                float u = texCoords == null ? 0.0f : texCoords[texBase + vertex * 2];
                float v = texCoords == null ? 0.0f : texCoords[texBase + vertex * 2 + 1];
                float shade = faceShade;
                if (normals != null && (command & PDATA_NORMAL_MASK) == PDATA_NORMAL_PER_VERTEX) {
                    int vertexNormalBase = normalBase + vertex * 3;
                    shade = computeCommandShade(
                            state,
                            normals[vertexNormalBase],
                            normals[vertexNormalBase + 1],
                            normals[vertexNormalBase + 2]
                    );
                }
                projected[vertex] = transformAndProject(
                        state,
                        vertices[source],
                        vertices[source + 1],
                        vertices[source + 2],
                        u,
                        v,
                        shade
                );
            }
            if (verticesPerPrimitive == 3) {
                rasterizeCommandTriangle(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        projected[0],
                        projected[1],
                        projected[2],
                        state.texture,
                        color,
                        blendMode,
                        sphereMap,
                        colorKeyEnabled,
                        texCoords != null
                );
            } else {
                rasterizeCommandQuad(
                        pixels,
                        depthBuffer,
                        surfaceWidth,
                        surfaceHeight,
                        clipX,
                        clipY,
                        clipWidth,
                        clipHeight,
                        projected,
                        state.texture,
                        color,
                        blendMode,
                        sphereMap,
                        colorKeyEnabled,
                        texCoords != null
                );
            }
        }
        return cursor;
    }

    private static void rasterizePerspectiveCommandPolygon(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            CommandState state,
            List<PolygonVertex> vertices,
            Texture texture,
            int color,
            int blendMode,
            Texture sphereMap,
            boolean colorKeyEnabled,
            boolean textured
    ) {
        List<PolygonVertex> clipped = clipPerspectivePolygon(vertices, state.nearClip, state.farClip);
        if (clipped.size() < 3) {
            return;
        }
        List<ProjectedVertex> projected = new ArrayList<>(clipped.size());
        for (PolygonVertex vertex : clipped) {
            ProjectedVertex projectedVertex = projectVertex(
                    vertex,
                    state.centerX,
                    state.centerY,
                    state.projectionScaleX,
                    state.projectionScaleY,
                    state.yDownProjection
            );
            if (projectedVertex == null) {
                return;
            }
            projected.add(projectedVertex);
        }
        ProjectedVertex first = projected.get(0);
        for (int i = 1; i + 1 < projected.size(); i++) {
            rasterizeTriangleProjected(
                    pixels,
                    depthBuffer,
                    surfaceWidth,
                    surfaceHeight,
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    blendMode,
                    texture,
                    sphereMap,
                    color,
                    colorKeyEnabled,
                    textured,
                    first,
                    projected.get(i),
                    projected.get(i + 1)
            );
        }
    }

    private static void rasterizeCommandQuad(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            ProjectedVertex[] projected,
            Texture texture,
            int color,
            int blendMode,
            Texture sphereMap,
            boolean colorKeyEnabled,
            boolean textured
    ) {
        if (projected.length < 4 || projected[0] == null || projected[1] == null || projected[2] == null || projected[3] == null) {
            return;
        }
        boolean stripOrderedQuad = isStripOrderedQuad(
                projected[0].u(), projected[0].v(),
                projected[1].u(), projected[1].v(),
                projected[2].u(), projected[2].v(),
                projected[3].u(), projected[3].v(),
                projected[0].screenX(), projected[0].screenY(),
                projected[1].screenX(), projected[1].screenY(),
                projected[2].screenX(), projected[2].screenY(),
                projected[3].screenX(), projected[3].screenY()
        );
        if (stripOrderedQuad) {
            rasterizeCommandTriangle(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    projected[0], projected[1], projected[2], texture, color, blendMode, sphereMap, colorKeyEnabled, textured);
            rasterizeCommandTriangle(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    projected[1], projected[3], projected[2], texture, color, blendMode, sphereMap, colorKeyEnabled, textured);
        } else {
            rasterizeCommandTriangle(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    projected[0], projected[1], projected[2], texture, color, blendMode, sphereMap, colorKeyEnabled, textured);
            rasterizeCommandTriangle(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight,
                    projected[0], projected[2], projected[3], texture, color, blendMode, sphereMap, colorKeyEnabled, textured);
        }
    }

    private static void rasterizeCommandTriangle(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            ProjectedVertex v0,
            ProjectedVertex v1,
            ProjectedVertex v2,
            Texture texture,
            int color,
            int blendMode,
            Texture sphereMap,
            boolean colorKeyEnabled,
            boolean textured
    ) {
        if (v0 == null || v1 == null || v2 == null) {
            return;
        }
        rasterizeTriangleProjected(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                blendMode,
                texture,
                sphereMap,
                color,
                colorKeyEnabled,
                textured,
                v0,
                v1,
                v2
        );
    }

    private static void renderPointSprite(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            CommandState state,
            float x,
            float y,
            float z,
            int[] spriteParams,
            int spriteBase,
            int blendMode,
            Texture sphereMap,
            boolean colorKeyEnabled
    ) {
        ProjectedVertex center = transformAndProject(state, x, y, z, 0.0f, 0.0f, 1.0f);
        if (center == null || state.texture == null) {
            return;
        }
        float scaleX = state.projectionScaleX;
        float scaleY = state.projectionScaleY;
        if (state.perspective) {
            // Command-list point sprites use world-space billboard sizes, so they must
            // shrink with distance just like projected geometry instead of staying at a
            // fixed screen-space size.
            scaleX *= center.reciprocalDepth();
            scaleY *= center.reciprocalDepth();
        }
        float halfWidth = Math.abs(spriteParams[spriteBase] * scaleX) * 0.5f;
        float halfHeight = Math.abs(spriteParams[spriteBase + 1] * scaleY) * 0.5f;
        if (halfWidth <= DEPTH_EPSILON || halfHeight <= DEPTH_EPSILON) {
            return;
        }
        float angle = (float) (spriteParams[spriteBase + 2] * (Math.PI * 2.0 / 4096.0));
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float left = spriteParams[spriteBase + 3];
        float top = spriteParams[spriteBase + 4];
        float right = spriteParams[spriteBase + 5];
        float bottom = spriteParams[spriteBase + 6];
        ProjectedVertex[] quad = new ProjectedVertex[]{
                rotateSpriteCorner(center, -halfWidth, -halfHeight, cos, sin, left, top),
                rotateSpriteCorner(center, halfWidth, -halfHeight, cos, sin, right, top),
                rotateSpriteCorner(center, halfWidth, halfHeight, cos, sin, right, bottom),
                rotateSpriteCorner(center, -halfWidth, halfHeight, cos, sin, left, bottom)
        };
        rasterizeCommandQuad(
                pixels,
                depthBuffer,
                surfaceWidth,
                surfaceHeight,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                quad,
                state.texture,
                0xFFFFFFFF,
                blendMode,
                sphereMap,
                colorKeyEnabled,
                true
        );
    }

    private static ProjectedVertex rotateSpriteCorner(
            ProjectedVertex center,
            float offsetX,
            float offsetY,
            float cos,
            float sin,
            float u,
            float v
    ) {
        float rotatedX = (offsetX * cos) - (offsetY * sin);
        float rotatedY = (offsetX * sin) + (offsetY * cos);
        return new ProjectedVertex(
                center.screenX() + rotatedX,
                center.screenY() + rotatedY,
                center.depth(),
                center.reciprocalDepth(),
                u,
                v,
                center.shade()
        );
    }

    private static PolygonVertex transformVertex(
            CommandState state,
            float x,
            float y,
            float z,
            float u,
            float v,
            float shade
    ) {
        float tx = x;
        float ty = y;
        float tz = z;
        if (state.affineTrans != null) {
            tx = transformX(state.affineTrans, x, y, z);
            ty = transformY(state.affineTrans, x, y, z);
            tz = transformZ(state.affineTrans, x, y, z);
        }
        return new PolygonVertex(tx, ty, tz, u, v, shade);
    }

    private static ProjectedVertex transformAndProject(
            CommandState state,
            float x,
            float y,
            float z,
            float u,
            float v,
            float shade
    ) {
        float tx = x;
        float ty = y;
        float tz = z;
        if (state.affineTrans != null) {
            tx = transformX(state.affineTrans, x, y, z);
            ty = transformY(state.affineTrans, x, y, z);
            tz = transformZ(state.affineTrans, x, y, z);
        }
        if (state.perspective) {
            return projectVertex(
                    new PolygonVertex(tx, ty, tz, u, v, shade),
                    state.centerX,
                    state.centerY,
                    state.projectionScaleX,
                    state.projectionScaleY,
                    state.yDownProjection
            );
        }
        return new ProjectedVertex(
                state.centerX + (tx * state.projectionScaleX),
                state.centerY + ((state.yDownProjection ? ty : -ty) * state.projectionScaleY),
                -tz,
                0.0f,
                u,
                v,
                shade
        );
    }

    private static void drawLine(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            ProjectedVertex start,
            ProjectedVertex end,
            int color,
            int blendMode
    ) {
        if (start == null || end == null) {
            return;
        }
        int x0 = Math.round(start.screenX());
        int y0 = Math.round(start.screenY());
        int x1 = Math.round(end.screenX());
        int y1 = Math.round(end.screenY());
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            plot(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight, x0, y0, start.depth(), color, blendMode);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            float amount = step / (float) steps;
            int x = Math.round(lerp(x0, x1, amount));
            int y = Math.round(lerp(y0, y1, amount));
            float depth = lerp(start.depth(), end.depth(), amount);
            plot(pixels, depthBuffer, surfaceWidth, surfaceHeight, clipX, clipY, clipWidth, clipHeight, x, y, depth, color, blendMode);
        }
    }

    private static void plot(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int x,
            int y,
            float depth,
            int color,
            int blendMode
    ) {
        if (x < clipX || y < clipY || x >= clipX + clipWidth || y >= clipY + clipHeight) {
            return;
        }
        if (x < 0 || y < 0 || x >= surfaceWidth || y >= surfaceHeight) {
            return;
        }
        int index = y * surfaceWidth + x;
        if (depth < depthBuffer[index] - DEPTH_EPSILON) {
            return;
        }
        pixels[index] = blend(color, pixels[index], blendMode);
        if (writesDepth(blendMode)) {
            depthBuffer[index] = depth;
        }
    }

    private static void rasterizeTriangleProjected(
            int[] pixels,
            float[] depthBuffer,
            int surfaceWidth,
            int surfaceHeight,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight,
            int blendMode,
            Texture texture,
            Texture sphereMap,
            int flatColor,
            boolean colorKeyEnabled,
            boolean textured,
            ProjectedVertex v0,
            ProjectedVertex v1,
            ProjectedVertex v2
    ) {
        float x0 = v0.screenX();
        float y0 = v0.screenY();
        float z0 = v0.depth();
        float x1 = v1.screenX();
        float y1 = v1.screenY();
        float z1 = v1.depth();
        float x2 = v2.screenX();
        float y2 = v2.screenY();
        float z2 = v2.depth();
        if (!Float.isFinite(x0) || !Float.isFinite(y0)
                || !Float.isFinite(x1) || !Float.isFinite(y1)
                || !Float.isFinite(x2) || !Float.isFinite(y2)) {
            return;
        }
        int minX = Math.max(clipX, Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)))));
        int minY = Math.max(clipY, Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)))));
        int maxX = Math.min(clipX + clipWidth - 1, Math.min(surfaceWidth - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2)))));
        int maxY = Math.min(clipY + clipHeight - 1, Math.min(surfaceHeight - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2)))));
        if (minX > maxX || minY > maxY) {
            return;
        }
        int fx0 = toRasterFixed(x0);
        int fy0 = toRasterFixed(y0);
        int fx1 = toRasterFixed(x1);
        int fy1 = toRasterFixed(y1);
        int fx2 = toRasterFixed(x2);
        int fy2 = toRasterFixed(y2);
        long rasterArea = edgeFixed(fx0, fy0, fx1, fy1, fx2, fy2);
        if (rasterArea == 0L) {
            return;
        }
        boolean flipped = rasterArea < 0L;
        boolean topLeft12 = isCoverageTopLeftEdge(fx1, fy1, fx2, fy2, flipped);
        boolean topLeft20 = isCoverageTopLeftEdge(fx2, fy2, fx0, fy0, flipped);
        boolean topLeft01 = isCoverageTopLeftEdge(fx0, fy0, fx1, fy1, flipped);
        float invRasterArea = 1.0f / (float) Math.abs(rasterArea);
        boolean perspectiveCorrect = v0.reciprocalDepth() > 0.0f
                || v1.reciprocalDepth() > 0.0f
                || v2.reciprocalDepth() > 0.0f;

        for (int y = minY; y <= maxY; y++) {
            int rasterY = (y << RASTER_SUBPIXEL_SHIFT) + (RASTER_SUBPIXEL_SCALE >> 1);
            for (int x = minX; x <= maxX; x++) {
                int rasterX = (x << RASTER_SUBPIXEL_SHIFT) + (RASTER_SUBPIXEL_SCALE >> 1);
                long coverage0 = edgeFixed(fx1, fy1, fx2, fy2, rasterX, rasterY);
                long coverage1 = edgeFixed(fx2, fy2, fx0, fy0, rasterX, rasterY);
                long coverage2 = edgeFixed(fx0, fy0, fx1, fy1, rasterX, rasterY);
                if (flipped) {
                    coverage0 = -coverage0;
                    coverage1 = -coverage1;
                    coverage2 = -coverage2;
                }
                if (coverage0 < 0L || (coverage0 == 0L && !topLeft12)
                        || coverage1 < 0L || (coverage1 == 0L && !topLeft20)
                        || coverage2 < 0L || (coverage2 == 0L && !topLeft01)) {
                    continue;
                }
                float w0 = coverage0 * invRasterArea;
                float w1 = coverage1 * invRasterArea;
                float w2 = coverage2 * invRasterArea;
                float pixelDepth = (w0 * z0) + (w1 * z1) + (w2 * z2);
                float pixelShade = clamp01((w0 * v0.shade()) + (w1 * v1.shade()) + (w2 * v2.shade()));
                int index = y * surfaceWidth + x;
                float depthTest = blendMode == 0 ? pixelDepth : pixelDepth + TRANSLUCENT_DEPTH_BIAS;
                if (depthTest < depthBuffer[index] - DEPTH_EPSILON) {
                    continue;
                }
                int argb;
                if (texture != null && textured) {
                    float u;
                    float v;
                    if (perspectiveCorrect) {
                        float rw0 = w0 * v0.reciprocalDepth();
                        float rw1 = w1 * v1.reciprocalDepth();
                        float rw2 = w2 * v2.reciprocalDepth();
                        float reciprocalWeight = rw0 + rw1 + rw2;
                        if (Math.abs(reciprocalWeight) <= DEPTH_EPSILON) {
                            continue;
                        }
                        float invReciprocalWeight = 1.0f / reciprocalWeight;
                        u = ((rw0 * v0.u()) + (rw1 * v1.u()) + (rw2 * v2.u())) * invReciprocalWeight;
                        v = ((rw0 * v0.v()) + (rw1 * v1.v()) + (rw2 * v2.v())) * invReciprocalWeight;
                    } else {
                        u = (w0 * v0.u()) + (w1 * v1.u()) + (w2 * v2.u());
                        v = (w0 * v0.v()) + (w1 * v1.v()) + (w2 * v2.v());
                    }
                    // Command-list road meshes are opaque by default; some games use palette index 0
                    // as visible black in indexed atlases, so globally color-keying command lists
                    // punches holes through the track. Blended command-list effects, however, rely on
                    // that same palette key for billboard/sprite backgrounds, so keep color-keying for
                    // additive/average/subtractive primitives.
                    argb = sampleCommandTexture(texture, u, v, colorKeyEnabled || blendMode != 0);
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                    argb = modulateShade(argb, pixelShade);
                } else {
                    argb = modulateShade(flatColor, pixelShade);
                }
                argb = applySphereMap(argb, sphereMap, surfaceWidth, surfaceHeight, x, y);
                pixels[index] = blend(argb, pixels[index], blendMode);
                if (writesDepth(blendMode)) {
                    depthBuffer[index] = pixelDepth;
                }
            }
        }
    }

    private static boolean writesDepth(int blendMode) {
        return blendMode == 0;
    }

    private static boolean rendersInPass(int blendMode, RenderPass pass) {
        if (pass == null || pass == RenderPass.ALL) {
            return true;
        }
        return blendMode == 0 ? pass == RenderPass.OPAQUE : pass == RenderPass.TRANSLUCENT;
    }

    private static final class CommandState {
        private final com.jblend.graphics.j3d.FigureLayout layout;
        private final Texture[] textures;
        private final Texture sphereMap;
        private final int baseClipX;
        private final int baseClipY;
        private final int baseClipWidth;
        private final int baseClipHeight;
        private int nearClip;
        private int farClip;
        private final boolean yDownProjection;
        private boolean lightingEnabled;
        private boolean semiTransparentEnabled;
        private boolean sphereMapEnabled;
        private boolean toonShadingEnabled;
        private float lightDirectionX;
        private float lightDirectionY;
        private float lightDirectionZ;
        private int directionalIntensity;
        private int ambientIntensity;
        private int toonThreshold;
        private int toonHigh;
        private int toonLow;
        private int clipX;
        private int clipY;
        private int clipWidth;
        private int clipHeight;
        private AffineTrans affineTrans;
        private Texture texture;
        private int centerX;
        private int centerY;
        private float projectionScaleX;
        private float projectionScaleY;
        private boolean perspective;

        private CommandState(
                com.jblend.graphics.j3d.FigureLayout layout,
                Texture[] textures,
                Texture sphereMap,
                int baseClipX,
                int baseClipY,
                int baseClipWidth,
                int baseClipHeight,
                int nearClip,
                int farClip,
                boolean yDownProjection,
                boolean lightingEnabled,
                boolean semiTransparentEnabled,
                boolean sphereMapEnabled,
                boolean toonShadingEnabled,
                float lightDirectionX,
                float lightDirectionY,
                float lightDirectionZ,
                int directionalIntensity,
                int ambientIntensity,
                int toonThreshold,
                int toonHigh,
                int toonLow,
                AffineTrans affineTrans,
                Texture texture,
                int centerX,
                int centerY,
                float projectionScaleX,
                float projectionScaleY,
                boolean perspective
        ) {
            this.layout = layout;
            this.textures = textures;
            this.sphereMap = sphereMap;
            this.baseClipX = baseClipX;
            this.baseClipY = baseClipY;
            this.baseClipWidth = baseClipWidth;
            this.baseClipHeight = baseClipHeight;
            this.nearClip = nearClip;
            this.farClip = farClip;
            this.yDownProjection = yDownProjection;
            this.lightingEnabled = lightingEnabled;
            this.semiTransparentEnabled = semiTransparentEnabled;
            this.sphereMapEnabled = sphereMapEnabled;
            this.toonShadingEnabled = toonShadingEnabled;
            this.lightDirectionX = lightDirectionX;
            this.lightDirectionY = lightDirectionY;
            this.lightDirectionZ = lightDirectionZ;
            this.directionalIntensity = directionalIntensity;
            this.ambientIntensity = ambientIntensity;
            this.toonThreshold = toonThreshold;
            this.toonHigh = toonHigh;
            this.toonLow = toonLow;
            this.clipX = baseClipX;
            this.clipY = baseClipY;
            this.clipWidth = Math.max(0, baseClipWidth);
            this.clipHeight = Math.max(0, baseClipHeight);
            this.affineTrans = affineTrans;
            this.texture = texture;
            this.centerX = centerX;
            this.centerY = centerY;
            this.projectionScaleX = projectionScaleX;
            this.projectionScaleY = projectionScaleY;
            this.perspective = perspective;
        }

        private void selectAffineIndex(int index) {
            if (layout == null) {
                return;
            }
            try {
                layout.selectAffineTrans(index);
                affineTrans = layout.getAffineTrans();
            } catch (ArrayIndexOutOfBoundsException ignored) {
                // Ignore malformed command lists and keep the current transform.
            }
        }

        private void setCommandClip(int left, int top, int right, int bottom) {
            int commandLeft = Math.min(left, right);
            int commandTop = Math.min(top, bottom);
            int commandRight = Math.max(left, right);
            int commandBottom = Math.max(top, bottom);
            int baseRight = baseClipX + baseClipWidth;
            int baseBottom = baseClipY + baseClipHeight;
            clipX = Math.max(baseClipX, commandLeft);
            clipY = Math.max(baseClipY, commandTop);
            int clippedRight = Math.min(baseRight, commandRight);
            int clippedBottom = Math.min(baseBottom, commandBottom);
            clipWidth = Math.max(0, clippedRight - clipX);
            clipHeight = Math.max(0, clippedBottom - clipY);
        }

        private static CommandState fromLayout(
                int originX,
                int originY,
                int surfaceWidth,
                int surfaceHeight,
                int clipX,
                int clipY,
                int clipWidth,
                int clipHeight,
                com.jblend.graphics.j3d.FigureLayout layout,
                Effect3D effect,
                Texture[] textures,
                Texture fallbackTexture,
                boolean yDownProjection
        ) {
            float projectionScaleX;
            float projectionScaleY;
            boolean perspective = layout.isPerspective();
            if (perspective) {
                int nearClip = layout.getPerspectiveNear();
                if (layout.getPerspectiveWidth() > 0 && layout.getPerspectiveHeight() > 0) {
                    float perspectiveWidth = layout.getPerspectiveWidth() / 4096.0f;
                    float perspectiveHeight = layout.getPerspectiveHeight() / 4096.0f;
                    projectionScaleX = nearClip > 0
                            ? (surfaceWidth * (float) nearClip) / perspectiveWidth
                            : 0.0f;
                    projectionScaleY = nearClip > 0
                            ? (surfaceHeight * (float) nearClip) / perspectiveHeight
                            : 0.0f;
                } else {
                    float angleRadians = (float) (layout.getPerspectiveAngle() * (Math.PI * 2.0 / 4096.0));
                    float focal = angleRadians <= 0.0f || angleRadians >= Math.PI
                            ? surfaceWidth * 0.5f
                            : (float) ((surfaceWidth * 0.5f) / Math.tan(angleRadians * 0.5f));
                    projectionScaleX = focal;
                    projectionScaleY = focal;
                }
            } else if (layout.getParallelWidth() > 0 || layout.getParallelHeight() > 0) {
                projectionScaleX = layout.getParallelWidth() > 0 ? (float) surfaceWidth / layout.getParallelWidth() : 1.0f;
                projectionScaleY = layout.getParallelHeight() > 0 ? (float) surfaceHeight / layout.getParallelHeight() : 1.0f;
            } else {
                int scaleX = layout.getScaleX();
                int scaleY = layout.getScaleY();
                projectionScaleX = (scaleX == 0 ? 512 : scaleX) / 4096.0f;
                projectionScaleY = (scaleY == 0 ? 512 : scaleY) / 4096.0f;
            }
            Texture currentTexture = fallbackTexture;
            if (currentTexture == null && textures != null && textures.length > 0) {
                currentTexture = textures[0];
            }
            Light light = effect == null ? null : effect.getLight();
            Vector3D lightDirection = light == null ? null : light.getDirection();
            float lightDirectionX = lightDirection == null ? 0.0f : lightDirection.x;
            float lightDirectionY = lightDirection == null ? 0.0f : lightDirection.y;
            float lightDirectionZ = lightDirection == null ? 4096.0f : lightDirection.z;
            int directionalIntensity = light == null ? 0 : light.getDirIntensity();
            int ambientIntensity = light == null ? 4096 : light.getAmbIntensity();
            int toonThreshold = effect == null ? 0 : effect.getThreshold();
            int toonHigh = effect == null ? 255 : effect.getThresholdHigh();
            int toonLow = effect == null ? 0 : effect.getThresholdLow();
            int centerX = layout.hasExplicitCenter()
                    ? originX + layout.getCenterX()
                    : originX + (perspective ? surfaceWidth / 2 : layout.getCenterX());
            int centerY = layout.hasExplicitCenter()
                    ? originY + layout.getCenterY()
                    : originY + (perspective ? surfaceHeight / 2 : layout.getCenterY());
            return new CommandState(
                    layout,
                    textures,
                    effect == null ? null : effect.getSphereMap(),
                    clipX,
                    clipY,
                    clipWidth,
                    clipHeight,
                    layout.getPerspectiveNear(),
                    layout.getPerspectiveFar(),
                    yDownProjection,
                    effect != null,
                    effect == null || effect.isSemiTransparentEnabled(),
                    effect != null && effect.getSphereMap() != null,
                    effect != null && effect.getShading() == Effect3D.TOON_SHADING,
                    lightDirectionX,
                    lightDirectionY,
                    lightDirectionZ,
                    directionalIntensity,
                    ambientIntensity,
                    toonThreshold,
                    toonHigh,
                    toonLow,
                    layout.getAffineTrans(),
                    currentTexture,
                    centerX,
                    centerY,
                    projectionScaleX,
                    projectionScaleY,
                    perspective
            );
        }
    }

    private record PolygonVertex(float x, float y, float z, float u, float v, float shade) {
    }

    private record ProjectedVertex(float screenX, float screenY, float depth, float reciprocalDepth, float u, float v, float shade) {
    }
}
