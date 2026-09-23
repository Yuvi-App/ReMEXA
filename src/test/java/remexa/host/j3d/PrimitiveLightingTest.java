package remexa.host.j3d;

import com.jblend.graphics.j3d.AffineTrans;
import com.jblend.graphics.j3d.Effect3D;
import com.jblend.graphics.j3d.FigureLayout;
import com.jblend.graphics.j3d.Light;
import com.jblend.graphics.j3d.Texture;
import com.jblend.graphics.j3d.Vector3D;
import java.util.Arrays;
import javax.microedition.lcdui.Image;
import org.junit.Test;

import static com.jblend.graphics.j3d.Graphics3D.*;
import static org.junit.Assert.*;

public class PrimitiveLightingTest {
    private static final int SIZE = 32;
    private static final int LIT_QUAD = PRIMITIVE_QUADS | PDATA_NORMAL_PER_VERTEX
            | PDATA_COLOR_PER_COMMAND | PATTR_LIGHTING;
    private static final int[] QUAD = {-12, -12, 100, 12, -12, 100, 12, 12, 100, -12, 12, 100};

    @Test
    public void normalMagnitudeAndLightDirectionMatchNativeRenderer() {
        int[] normalY = {-4096, -3072, -2048, -1024, -64, -1, 0, 1024, 4096};
        int[] expected = {255, 191, 127, 63, 3, 3, 0, 0, 0};
        for (int i = 0; i < normalY.length; i++) {
            int[] pixels = render(LIT_QUAD, normals(normalY[i]), layout(), effect(0), null, 0);
            assertGray("normalY=" + normalY[i], expected[i], pixels[16 * SIZE + 16]);
        }
    }

    @Test
    public void zeroAndBackFacingNormalsReceiveOnlyAmbientLight() {
        for (int normalY : new int[]{0, 1024, 4096}) {
            int[] pixels = render(LIT_QUAD, normals(normalY), layout(), effect(1024), null, 0);
            assertGray("ambient only", 63, pixels[16 * SIZE + 16]);
        }
        assertGray("ambient plus diffuse", 191,
                render(LIT_QUAD, normals(-2048), layout(), effect(1024), null, 0)[16 * SIZE + 16]);
    }

    @Test
    public void perVertexLightingFadesSmoothlyInBothProjections() {
        int[] normals = {0, -4096, 0, 0, -4096, 0, 0, 0, 0, 0, 0, 0};
        for (boolean perspective : new boolean[]{false, true}) {
            FigureLayout layout = layout();
            if (perspective) layout.setPerspective(100, 1000, SIZE * 4096, SIZE * 4096);
            int[] pixels = render(LIT_QUAD | PATTR_BLEND_ADD, normals, layout, effect(0), null, 0xFF102030);
            int top = pixels[6 * SIZE + 16] & 255;
            int middle = pixels[16 * SIZE + 16] & 255;
            int bottom = pixels[26 * SIZE + 16] & 255;
            assertTrue("bright far edge", top > 240);
            assertTrue("smooth middle", middle > 155 && middle < 180);
            assertTrue("fades toward background", bottom > 48 && bottom < 75);
        }
    }

    @Test
    public void faceNormalsAndTexturedPrimitivesUseTheSameLighting() {
        int faceCommand = (LIT_QUAD & ~PDATA_NORMAL_PER_VERTEX) | PDATA_NORMAL_PER_FACE;
        assertGray("face normal", 127,
                render(faceCommand, new int[]{0, -2048, 0}, layout(), effect(0), null, 0)[16 * SIZE + 16]);
        Texture texture = new Texture(Image.createRGBImage(new int[]{0xFFFFFFFF}, 1, 1, false), 0, 0, 1, 1, true);
        assertGray("textured", 127,
                render(LIT_QUAD | PDATA_TEXURE_COORD, normals(-2048), layout(), effect(0), texture, 0)[16 * SIZE + 16]);
    }

    @Test
    public void primitiveLightingFlagIsRequiredEvenWhenNormalsArePresent() {
        for (int normalY : new int[]{-4096, 0, 4096}) {
            assertGray("lighting disabled", 255,
                    render(LIT_QUAD & ~PATTR_LIGHTING, normals(normalY), layout(), effect(0), null, 0)[16 * SIZE + 16]);
        }
    }

    @Test
    public void commandListLightingRequiresBothAttributeFlags() {
        for (int env : new int[]{0, ENV_ATTR_LIGHTING}) {
            for (int primitive : new int[]{0, PATTR_LIGHTING}) {
                int command = (LIT_QUAD & ~PATTR_LIGHTING) | primitive;
                int[] list = new int[32];
                list[0] = COMMAND_LIST_VERSION_1_0;
                list[1] = COMMAND_ATTRIBUTE | env;
                list[2] = COMMAND_AMBIENT_LIGHT;
                list[3] = 1024;
                list[4] = command | (1 << 16);
                System.arraycopy(QUAD, 0, list, 5, 12);
                System.arraycopy(normals(-2048), 0, list, 17, 12);
                list[29] = 0xFFFFFF;
                list[30] = COMMAND_END;
                int[] pixels = new int[SIZE * SIZE];
                float[] depth = depth();
                assertTrue(SoftwareJ3dRenderer.renderCommandListToBuffers(pixels, depth, SIZE, SIZE,
                        0, 0, SIZE, SIZE, 0, 0, layout(), effect(0), null, null, list));
                assertGray("environment and primitive flags", env != 0 && primitive != 0 ? 191 : 255,
                        pixels[16 * SIZE + 16]);
            }
        }
    }

    @Test
    public void rotationChangesLightingButScalingAndTranslationDoNot() {
        FigureLayout layout = layout();
        AffineTrans transform = new AffineTrans();
        transform.m00 = -6144;
        transform.m11 = -2048;
        transform.m22 = 8192;
        transform.m03 = 1;
        layout.setAffineTrans(transform);
        assertGray("rotated half-length normal under nonuniform scale", 127,
                render(LIT_QUAD, normals(2048), layout, effect(0), null, 0)[16 * SIZE + 16]);
    }

    @Test
    public void subtractiveBlendingUsesTheLitColor() {
        assertGray("subtractive quarter strength", 192,
                render(LIT_QUAD | PATTR_BLEND_SUB, normals(-1024), layout(), effect(0), null, 0xFFFFFFFF)[16 * SIZE + 16]);
    }

    private static FigureLayout layout() {
        return new FigureLayout(new AffineTrans(), 4096, 4096, 16, 16);
    }

    private static Effect3D effect(int ambient) {
        return new Effect3D(new Light(new Vector3D(0, 4096, 0), 4096, ambient), Effect3D.NORMAL_SHADING, true, null);
    }

    private static int[] normals(int y) {
        return new int[]{0, y, 0, 0, y, 0, 0, y, 0, 0, y, 0};
    }

    private static float[] depth() {
        float[] depth = new float[SIZE * SIZE];
        Arrays.fill(depth, Float.NEGATIVE_INFINITY);
        return depth;
    }

    private static int[] render(int command, int[] normals, FigureLayout layout, Effect3D effect, Texture texture, int background) {
        int[] pixels = new int[SIZE * SIZE];
        Arrays.fill(pixels, background);
        assertTrue(SoftwareJ3dRenderer.renderPrimitivesToBuffers(pixels, depth(), SIZE, SIZE,
                0, 0, SIZE, SIZE, 0, 0, layout, effect, texture, command, 1, QUAD,
                normals, texture == null ? null : new int[8], new int[]{0xFFFFFF}));
        return pixels;
    }

    private static void assertGray(String message, int expected, int pixel) {
        assertEquals(message, (expected << 16) | (expected << 8) | expected, pixel & 0xFFFFFF);
    }
}
