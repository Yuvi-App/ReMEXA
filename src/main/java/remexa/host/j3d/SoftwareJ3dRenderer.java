package remexa.host.j3d;

import com.jblend.graphics.j3d.AffineTrans;
import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Texture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.microedition.lcdui.Graphics;

public final class SoftwareJ3dRenderer {
    private static final float DEPTH_EPSILON = 0.000001f;
    private static final int RASTER_SUBPIXEL_SHIFT = 4;
    private static final int RASTER_SUBPIXEL_SCALE = 1 << RASTER_SUBPIXEL_SHIFT;

    private SoftwareJ3dRenderer() {
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
                perspective,
                nearClip,
                farClip,
                affineTrans,
                figure,
                fallbackTexture,
                effect
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
            boolean perspective,
            int nearClip,
            int farClip,
            AffineTrans affineTrans,
            MascotFigure figure,
            Texture fallbackTexture,
            Effect3D effect
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
                // MEXA Canvas3D screen-scale behaves like an orthographic view-space scale,
                // not a perspective focal-length divide.
                screenX[i] = screenCenterX + (tx * projectionScaleX);
                screenY[i] = screenCenterY - (ty * projectionScaleY);
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
                boolean stripOrderedQuad = isSelfIntersectingQuad(
                        screenX[indices[0]], screenY[indices[0]],
                        screenX[indices[1]], screenY[indices[1]],
                        screenX[indices[2]], screenY[indices[2]],
                        screenX[indices[3]], screenY[indices[3]]
                );
                if (stripOrderedQuad) {
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
                            indices[1], indices[3], indices[2],
                            uv,
                            2, 6, 4
                    );
                } else {
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
                            indices[0], indices[2], indices[3],
                            uv,
                            0, 4, 6
                    );
                }
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
                    v
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
            boolean stripOrderedQuad = isStripOrderedQuad(vertices, uv, screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
            if (stripOrderedQuad) {
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
                        vertices.get(1), vertices.get(3), vertices.get(2), uv != null
                );
            } else {
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
                        vertices.get(0), vertices.get(2), vertices.get(3), uv != null
                );
            }
            return;
        }
        List<PolygonVertex> clipped = clipPerspectivePolygon(vertices, nearClip, farClip);
        if (clipped.size() < 3) {
            return;
        }
        List<ProjectedVertex> projected = projectPolygon(clipped, screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
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
        List<ProjectedVertex> projected = projectPolygon(clipped, screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
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

    private static List<ProjectedVertex> projectPolygon(
            List<PolygonVertex> vertices,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY
    ) {
        List<ProjectedVertex> projected = new ArrayList<>(vertices.size());
        for (PolygonVertex vertex : vertices) {
            ProjectedVertex projectedVertex = projectVertex(vertex, screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
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
            float projectionScaleY
    ) {
        if (uv != null && uv.length >= 8 && isSelfIntersectingQuad(
                uv[0], uv[1],
                uv[2], uv[3],
                uv[4], uv[5],
                uv[6], uv[7]
        )) {
            return true;
        }
        ProjectedVertex p0 = projectVertex(vertices.get(0), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
        ProjectedVertex p1 = projectVertex(vertices.get(1), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
        ProjectedVertex p2 = projectVertex(vertices.get(2), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
        ProjectedVertex p3 = projectVertex(vertices.get(3), screenCenterX, screenCenterY, projectionScaleX, projectionScaleY);
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
                new ProjectedVertex(screenX[i0], screenY[i0], depth[i0], 0.0f, uv == null ? 0.0f : uv[uv0], uv == null ? 0.0f : uv[uv0 + 1]),
                new ProjectedVertex(screenX[i1], screenY[i1], depth[i1], 0.0f, uv == null ? 0.0f : uv[uv1], uv == null ? 0.0f : uv[uv1 + 1]),
                new ProjectedVertex(screenX[i2], screenY[i2], depth[i2], 0.0f, uv == null ? 0.0f : uv[uv2], uv == null ? 0.0f : uv[uv2 + 1]),
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
        float area = edgeFunction(x0, y0, x1, y1, x2, y2);
        if (area == 0.0f) {
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

        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            int rasterY = (y << RASTER_SUBPIXEL_SHIFT) + (RASTER_SUBPIXEL_SCALE >> 1);
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
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
                float w0 = edgeFunction(x1, y1, x2, y2, px, py) / area;
                float w1 = edgeFunction(x2, y2, x0, y0, px, py) / area;
                float w2 = edgeFunction(x0, y0, x1, y1, px, py) / area;
                float pixelDepth = w0 * z0 + w1 * z1 + w2 * z2;
                int index = y * surfaceWidth + x;
                if (pixelDepth < depthBuffer[index] - DEPTH_EPSILON) {
                    continue;
                }
                int argb;
                if (texture != null && textured) {
                    float u;
                    float v;
                    if (v0.reciprocalDepth() > 0.0f || v1.reciprocalDepth() > 0.0f || v2.reciprocalDepth() > 0.0f) {
                        float rw0 = w0 * v0.reciprocalDepth();
                        float rw1 = w1 * v1.reciprocalDepth();
                        float rw2 = w2 * v2.reciprocalDepth();
                        float reciprocalWeight = rw0 + rw1 + rw2;
                        if (Math.abs(reciprocalWeight) <= DEPTH_EPSILON) {
                            continue;
                        }
                        u = ((rw0 * v0.u()) + (rw1 * v1.u()) + (rw2 * v2.u())) / reciprocalWeight;
                        v = ((rw0 * v0.v()) + (rw1 * v1.v()) + (rw2 * v2.v())) / reciprocalWeight;
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
                depthBuffer[index] = pixelDepth;
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
        if (dst == 0) {
            return src;
        }
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
                lerp(start.v(), end.v(), t)
        );
    }

    private static ProjectedVertex projectVertex(
            PolygonVertex vertex,
            int screenCenterX,
            int screenCenterY,
            float projectionScaleX,
            float projectionScaleY
    ) {
        if (vertex.z() <= DEPTH_EPSILON) {
            return null;
        }
        return new ProjectedVertex(
                screenCenterX + ((vertex.x() * projectionScaleX) / vertex.z()),
                screenCenterY - ((vertex.y() * projectionScaleY) / vertex.z()),
                -vertex.z(),
                1.0f / vertex.z(),
                vertex.u(),
                vertex.v()
        );
    }

    private static float lerp(float start, float end, float amount) {
        return start + ((end - start) * amount);
    }

    private record PolygonVertex(float x, float y, float z, float u, float v) {
    }

    private record ProjectedVertex(float screenX, float screenY, float depth, float reciprocalDepth, float u, float v) {
    }
}
