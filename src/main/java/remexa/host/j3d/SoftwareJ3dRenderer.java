package remexa.host.j3d;

import com.jblend.graphics.j3d.AffineTrans;
import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.Texture;
import java.util.Arrays;
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
            int screenScaleX,
            int screenScaleY,
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

        float[] posedVertices = figure.vertices();
        int vertexCount = posedVertices.length / 3;
        float[] screenX = new float[vertexCount];
        float[] screenY = new float[vertexCount];
        float[] depth = new float[vertexCount];
        float scaleX = screenScaleX / 4096.0f;
        float scaleY = screenScaleY / 4096.0f;
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
            // MEXA Canvas3D screen-scale behaves like an orthographic view-space scale,
            // not a perspective focal-length divide.
            screenX[i] = screenCenterX + (tx * scaleX);
            screenY[i] = screenCenterY - (ty * scaleY);
            depth[i] = -tz;
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
            if (indices.length == 3) {
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
                        indices[0], indices[1], indices[3],
                        uv,
                        0, 2, 6
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
                        indices[0], indices[3], indices[2],
                        uv,
                        0, 6, 4
                );
            }
        }
        graphics.drawRGB(pixels, 0, surfaceWidth, 0, 0, surfaceWidth, surfaceHeight, true);
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
        float x0 = screenX[i0];
        float y0 = screenY[i0];
        float z0 = depth[i0];
        float x1 = screenX[i1];
        float y1 = screenY[i1];
        float z1 = depth[i1];
        float x2 = screenX[i2];
        float y2 = screenY[i2];
        float z2 = depth[i2];
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

        float u0 = uv == null ? 0.0f : uv[uv0];
        float v0 = uv == null ? 0.0f : uv[uv0 + 1];
        float u1 = uv == null ? 0.0f : uv[uv1];
        float v1 = uv == null ? 0.0f : uv[uv1 + 1];
        float u2 = uv == null ? 0.0f : uv[uv2];
        float v2 = uv == null ? 0.0f : uv[uv2 + 1];

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
                float w0 = edgeFunction(x1, y1, x2, y2, px, py);
                float w1 = edgeFunction(x2, y2, x0, y0, px, py);
                float w2 = edgeFunction(x0, y0, x1, y1, px, py);
                w0 /= area;
                w1 /= area;
                w2 /= area;
                float pixelDepth = w0 * z0 + w1 * z1 + w2 * z2;
                int index = y * surfaceWidth + x;
                if (pixelDepth < depthBuffer[index] - DEPTH_EPSILON) {
                    continue;
                }
                boolean writeDepth = polygonBlendMode == 0;
                int argb;
                if (texture != null && uv != null) {
                    // Canvas3D's screen-scale path is a parallel projection, so textured
                    // mascot polygons use affine UV interpolation rather than a perspective divide.
                    float u = (w0 * u0) + (w1 * u1) + (w2 * u2);
                    float v = (w0 * v0) + (w1 * v1) + (w2 * v2);
                    argb = texture.sampleColor(u, v, polygon.transparent());
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                } else {
                    argb = polygon.color();
                }
                argb = applySphereMap(argb, sphereMap, surfaceWidth, surfaceHeight, x, y);
                pixels[index] = blend(argb, pixels[index], polygonBlendMode);
                if (writeDepth) {
                    depthBuffer[index] = pixelDepth;
                }
            }
        }
    }

    private static boolean sameSide(float area, float w0, float w1, float w2, float e0, float e1, float e2) {
        if (area < 0.0f) {
            return w0 <= e0 && w1 <= e1 && w2 <= e2;
        }
        return w0 >= -e0 && w1 >= -e1 && w2 >= -e2;
    }

    private static float edgeFunction(float ax, float ay, float bx, float by, float px, float py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
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
}
