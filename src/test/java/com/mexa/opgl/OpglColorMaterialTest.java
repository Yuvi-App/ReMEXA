package com.mexa.opgl;

import java.awt.image.BufferedImage;
import javax.microedition.lcdui.Graphics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.mexa.opgl.OpglGraphics.*;
import static org.junit.Assert.*;

/** OpenGL ES 1.1 sections 2.8, 2.12.3 and appendix B, item 5. */
public class OpglColorMaterialTest {
    private OpglGraphics gl;
    private BufferedImage image;
    private java.awt.Graphics2D graphics;
    private Graphics target;

    @Before
    public void createContext() throws Exception {
        var constructor = OpglGraphics.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        gl = constructor.newInstance();
        image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        target = new Graphics(graphics, 32, 32);
        gl.bind(target);
        gl.glEnableClientState(GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL_FLOAT, 0, floats(0, 0, -0.5f));
        gl.glEnable(GL_LIGHTING);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {1, 1, 1, 1});
    }

    @After
    public void releaseContext() {
        if (gl != null) {
            gl.release();
        }
        if (graphics != null) {
            graphics.dispose();
        }
    }

    @Test
    public void enablingTrackingImmediatelyCopiesCurrentColorToBothMaterials() {
        gl.glColor4f(0.25f, 0.5f, 0.75f, 0.4f);
        gl.glEnable(GL_COLOR_MATERIAL);
        assertTrackedMaterials(0.25f, 0.5f, 0.75f, 0.4f);
        assertRgb(0x4080BF, drawPoint());
    }

    @Test
    public void enablingTrackingUsesDefaultWhiteWithoutAColorCall() {
        gl.glEnable(GL_COLOR_MATERIAL);
        assertTrackedMaterials(1, 1, 1, 1);
        assertRgb(0xFFFFFF, drawPoint());
    }

    @Test
    public void explicitColorsTrackBackMaterialEvenWithOneSidedLighting() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColor4f(0.1f, 0.2f, 0.3f, 0.4f);
        assertTrackedMaterials(0.1f, 0.2f, 0.3f, 0.4f);
        gl.glColor4ub((byte) 128, (byte) 192, (byte) 255, (byte) 64);
        assertTrackedMaterials(128 / 255f, 192 / 255f, 1, 64 / 255f);
    }

    @Test
    public void trackedPropertiesIgnoreMaterialCallsButOtherPropertiesStillWork() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColor4f(0.2f, 0.4f, 0.6f, 0.8f);
        for (int property : new int[] {GL_AMBIENT, GL_DIFFUSE, GL_AMBIENT_AND_DIFFUSE}) {
            gl.glMaterialfv(GL_FRONT_AND_BACK, property, new float[] {0, 0, 0, 0});
            assertTrackedMaterials(0.2f, 0.4f, 0.6f, 0.8f);
        }
        float[] emission = {0.1f, 0, 0, 1};
        float[] specular = {0.3f, 0.5f, 0.7f, 1};
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, emission);
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, specular);
        gl.glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 32);
        for (int face : new int[] {GL_FRONT, GL_BACK}) {
            assertMaterial(face, GL_EMISSION, emission);
            assertMaterial(face, GL_SPECULAR, specular);
            assertMaterial(face, GL_SHININESS, 32);
        }
        assertRgb(0x4D6699, drawPoint());
    }

    @Test
    public void disablingTrackingRetainsMaterialAndAllowsExplicitChanges() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColor4f(0.25f, 0.5f, 0.75f, 1);
        gl.glDisable(GL_COLOR_MATERIAL);
        gl.glColor4f(1, 0, 0, 1);
        assertTrackedMaterials(0.25f, 0.5f, 0.75f, 1);
        assertRgb(0x4080BF, drawPoint());
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, new float[] {0, 1, 0, 1});
        assertTrackedMaterials(0, 1, 0, 1);
        assertRgb(0x00FF00, drawPoint());
        gl.glEnable(GL_COLOR_MATERIAL);
        assertTrackedMaterials(1, 0, 0, 1);
    }

    @Test
    public void floatColorArrayControlsLitVerticesIndependently() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glVertexPointer(3, GL_FLOAT, 0, floats(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f));
        colors(floats(1, 0, 0, 1, 0, 1, 0, 1), GL_FLOAT);
        gl.glDrawArrays(GL_POINTS, 0, 2);
        present();
        assertRgb(0xFF0000, image.getRGB(8, 16));
        assertRgb(0x00FF00, image.getRGB(24, 16));
    }

    @Test
    public void unsignedByteColorArrayIsNormalizedForLighting() {
        gl.glEnable(GL_COLOR_MATERIAL);
        colors(bytes(128, 192, 255, 255), GL_UNSIGNED_BYTE);
        assertRgb(0x80C0FF, drawPoint());
    }

    @Test
    public void indexedClientArraysUseTheIndexedColor() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glVertexPointer(3, GL_FLOAT, 0, floats(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f));
        colors(floats(1, 0, 0, 1, 0, 0, 1, 1), GL_FLOAT);
        var indices = ShortBuffer.allocateDirect(1);
        indices.put(0, new short[] {1}, 0, 1);
        gl.glDrawElements(GL_POINTS, GL_UNSIGNED_SHORT, indices);
        present();
        assertRgb(0x0000FF, image.getRGB(24, 16));
        assertRgb(0, image.getRGB(8, 16));
    }

    @Test
    public void indexedVbosHonorColorStrideAndOffset() {
        gl.glEnable(GL_COLOR_MATERIAL);
        int[] buffers = new int[3];
        gl.glGenBuffers(buffers);
        gl.glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
        gl.glBufferData(GL_ARRAY_BUFFER, floats(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f), GL_STATIC_DRAW);
        gl.glVertexPointer(3, GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, buffers[1]);
        gl.glBufferData(GL_ARRAY_BUFFER, bytes(99, 99, 99, 99, 255, 0, 0, 255,
                99, 99, 99, 99, 32, 192, 128, 255), GL_STATIC_DRAW);
        gl.glColorPointer(4, GL_UNSIGNED_BYTE, 8, 4);
        gl.glEnableClientState(GL_COLOR_ARRAY);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, bytes(0, 1), GL_STATIC_DRAW);
        gl.glDrawElements(GL_POINTS, 1, GL_UNSIGNED_BYTE, 1);
        present();
        assertRgb(0x20C080, image.getRGB(24, 16));
        assertRgb(0, image.getRGB(8, 16));
    }

    @Test
    public void floatVbosPreserveColorComponents() {
        gl.glEnable(GL_COLOR_MATERIAL);
        int[] buffers = new int[2];
        gl.glGenBuffers(buffers);
        gl.glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
        gl.glBufferData(GL_ARRAY_BUFFER, floats(0, 0, -0.5f), GL_STATIC_DRAW);
        gl.glVertexPointer(3, GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, buffers[1]);
        gl.glBufferData(GL_ARRAY_BUFFER, floats(0.25f, 0.5f, 0.75f, 1), GL_STATIC_DRAW);
        gl.glColorPointer(4, GL_FLOAT, 0, 0);
        gl.glEnableClientState(GL_COLOR_ARRAY);
        assertRgb(0x4080BF, drawPoint());
    }

    @Test
    public void disabledTrackingKeepsExplicitMaterialDespiteColorArray() {
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, new float[] {0, 1, 0, 1});
        colors(floats(1, 0, 0, 1), GL_FLOAT);
        assertRgb(0x00FF00, drawPoint());
        assertTrackedMaterials(0, 1, 0, 1);
    }

    @Test
    public void disabledColorArrayUsesExplicitCurrentColor() {
        gl.glEnable(GL_COLOR_MATERIAL);
        colors(floats(1, 0, 0, 1), GL_FLOAT);
        assertRgb(0xFF0000, drawPoint());
        gl.glDisableClientState(GL_COLOR_ARRAY);
        // Current color after an array draw is unspecified; set it explicitly.
        gl.glColor4f(0, 0, 1, 1);
        assertRgb(0x0000FF, drawPoint());
    }

    @Test
    public void unlitColorsBypassMaterialAndLighting() {
        gl.glDisable(GL_LIGHTING);
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, new float[] {0, 1, 0, 1});
        colors(floats(0.25f, 0.5f, 0.75f, 1), GL_FLOAT);
        assertRgb(0x4080BF, drawPoint());
        gl.glEnable(GL_COLOR_MATERIAL);
        assertRgb(0x4080BF, drawPoint());
    }

    @Test
    public void trackingStateUpdatesWhileLightingIsDisabled() {
        gl.glDisable(GL_LIGHTING);
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColor4f(0.25f, 0.5f, 0.75f, 1);
        assertTrackedMaterials(0.25f, 0.5f, 0.75f, 1);
        gl.glEnable(GL_LIGHTING);
        assertRgb(0x4080BF, drawPoint());
    }

    @Test
    public void trackedDiffuseStillRespondsToNormalDirection() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {0, 0, 0, 1});
        gl.glLightfv(GL_LIGHT0, GL_POSITION, new float[] {0, 0, 1, 0});
        gl.glEnable(GL_LIGHT0);
        colors(floats(0.25f, 0.5f, 0.75f, 1), GL_FLOAT);
        assertRgb(0x4080BF, drawPoint());
        gl.glNormal3f(0, 0, -1);
        assertRgb(0, drawPoint());
    }

    @Test
    public void floatArrayIsNotQuantizedBeforeLighting() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {100, 100, 100, 1});
        colors(floats(0.001f, 0.002f, 0.003f, 1), GL_FLOAT);
        assertRgb(0x1A334D, drawPoint());
    }

    @Test
    public void floatArrayIsClampedAfterLighting() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {0.25f, 0.25f, 0.25f, 1});
        colors(floats(2, 1, -1, 1), GL_FLOAT);
        assertRgb(0x804000, drawPoint());
    }

    @Test
    public void explicitFloatColorIsClampedAfterLighting() {
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {0.25f, 0.25f, 0.25f, 1});
        gl.glColor4f(2, 1, -1, 1);
        assertRgb(0x804000, drawPoint());
    }

    @Test
    public void trackedAlphaMultipliesTextureAlphaBeforeBlending() {
        gl.glEnable(GL_COLOR_MATERIAL);
        colors(floats(1, 1, 1, 0.5f), GL_FLOAT);
        texture(255, 128, 64, 128);
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        assertRgb(0x402010, drawPoint());
    }

    @Test
    public void disabledTrackingUsesMaterialAlphaInsteadOfArrayAlpha() {
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, new float[] {1, 1, 1, 0.5f});
        colors(floats(1, 0, 0, 0), GL_FLOAT);
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        assertRgb(0x808080, drawPoint());
    }

    @Test
    public void texturedBillboardUsesTrackedWhiteUnderAmbientAndDirectionalLight() {
        // A synthetic reproduction of issue #60's API usage, with no game assets.
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] {0.2f, 0.2f, 0.2f, 1});
        gl.glLightfv(GL_LIGHT0, GL_AMBIENT, new float[] {0.75f, 0.75f, 0.75f, 1});
        gl.glLightfv(GL_LIGHT0, GL_POSITION, new float[] {1.75f, 1.75f, 1.75f, 0});
        gl.glEnable(GL_LIGHT0);
        gl.glVertexPointer(3, GL_FLOAT, 0, floats(-0.8f, -0.8f, -0.5f, 0.8f, -0.8f, -0.5f,
                -0.8f, 0.8f, -0.5f, 0.8f, 0.8f, -0.5f));
        colors(floats(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), GL_FLOAT);
        texture(240, 160, 80, 255);
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        present();
        assertRgb(0xF0A050, image.getRGB(16, 16));
    }

    private void assertTrackedMaterials(float... expected) {
        for (int face : new int[] {GL_FRONT, GL_BACK}) {
            assertMaterial(face, GL_AMBIENT, expected);
            assertMaterial(face, GL_DIFFUSE, expected);
        }
    }

    private void assertMaterial(int face, int property, float... expected) {
        float[] actual = new float[expected.length];
        gl.glGetMaterialfv(face, property, actual);
        assertArrayEquals("material " + face + "/" + property, expected, actual, 0.000001f);
    }

    private void colors(Buffer buffer, int type) {
        gl.glColorPointer(4, type, 0, buffer);
        gl.glEnableClientState(GL_COLOR_ARRAY);
    }

    private void texture(int... rgba) {
        int[] names = new int[1];
        gl.glGenTextures(names);
        gl.glBindTexture(GL_TEXTURE_2D, names[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, bytes(rgba));
        gl.glEnable(GL_TEXTURE_2D);
        gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    }

    private int drawPoint() {
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL_COLOR_BUFFER_BIT);
        gl.glDrawArrays(GL_POINTS, 0, 1);
        present();
        assertEquals(GL_NO_ERROR, gl.glGetError());
        return image.getRGB(16, 16);
    }

    private void present() {
        gl.glFlush();
        // MEXA publishes the offscreen surface to its Graphics target on release.
        gl.release();
        gl.bind(target);
    }

    private static void assertRgb(int expected, int actual) {
        // Allow a one-LSB difference for floating-point conversion/rasterization.
        for (int shift : new int[] {16, 8, 0}) {
            assertEquals(String.format("expected #%06X, got #%06X", expected, actual & 0xFFFFFF),
                    (expected >> shift) & 255, (actual >> shift) & 255, 1);
        }
    }

    private static FloatBuffer floats(float... values) {
        var buffer = FloatBuffer.allocateDirect(values.length);
        buffer.put(0, values, 0, values.length);
        return buffer;
    }

    private static ByteBuffer bytes(int... values) {
        byte[] data = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
        }
        var buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(0, data, 0, data.length);
        return buffer;
    }
}
