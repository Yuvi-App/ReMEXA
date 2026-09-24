package remexa.audio.spatial;

import com.vodafone.media.audio3d.Environment3D;
import org.junit.Test;
import remexa.audio.spatial.Audio3DScene.Vector;
import remexa.host.runtime.MidletRuntime;
import static org.junit.Assert.*;

public class Audio3DSceneTest {
    @Test public void defaultsMatchTheNativeSourceInitializer() {
        var source = new Audio3DScene().createSource();
        assertTrue(source.isRelative());
        assertArrayEquals(new int[]{100, 100000, 100}, source.getRolloff());
        assertEquals(0, source.snapshot().mode());
        assertEquals(0, source.getReverbLevel());
    }

    @Test public void deferredPlacementAndListenerCommitTogetherWhileOtherControlsAreImmediate() {
        var scene = new Audio3DScene();
        var a = scene.createSource(); var b = scene.createSource();
        scene.setDeferred(true);
        a.setPosition(10, 20, 30); b.setVelocity(40, 50, 60);
        scene.setListener(new Vector(100, 0, 0), Vector.ZERO, new Vector(0, 0, -1), new Vector(0, 1, 0));
        assertArrayEquals(new int[]{10, 20, 30}, a.getPosition());
        assertEquals(Vector.ZERO, a.snapshot().placement().position());
        assertEquals(Vector.ZERO, b.snapshot().placement().velocity());
        assertEquals(Vector.ZERO, a.snapshot().listener().position());
        a.setMode(2); a.setReverbLevel(70); a.setRolloff(500, 1000, 200); scene.setReverb(2, 2910);
        assertEquals(70, a.snapshot().reverbLevel());
        assertEquals(500, a.snapshot().minDistance());
        assertEquals(2, a.snapshot().preset());
        scene.commit();
        assertEquals(new Vector(10, 20, 30), a.snapshot().placement().position());
        assertEquals(new Vector(40, 50, 60), b.snapshot().placement().velocity());
        assertEquals(new Vector(100, 0, 0), a.snapshot().listener().position());
        a.setPosition(90, 80, 70);
        scene.setDeferred(false);
        assertEquals(new Vector(90, 80, 70), a.snapshot().placement().position());
    }

    @Test public void changingCoordinateFramesPreservesWorldPositionAndVelocity() {
        var scene = new Audio3DScene();
        scene.setListener(new Vector(1000, 2000, 3000), new Vector(10, 20, 30),
                new Vector(2, 0, 0), new Vector(0, 3, 0));
        var source = scene.createSource();
        source.setPosition(100, 200, -300); source.setVelocity(1, 2, -3);
        source.setRelative(false);
        assertArrayEquals(new int[]{1300, 2200, 3100}, source.getPosition());
        assertArrayEquals(new int[]{13, 22, 31}, source.getVelocity());
        source.setRelative(true);
        assertArrayEquals(new int[]{100, 200, -300}, source.getPosition());
        assertArrayEquals(new int[]{1, 2, -3}, source.getVelocity());
        assertThrows(IllegalArgumentException.class, () -> source.setRolloff(0, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> source.setRolloff(100, 50, 100));
        assertThrows(IllegalArgumentException.class, () -> source.setRolloff(100, 500, -1));
    }

    @Test public void environmentStateDoesNotCrossAppliClassLoaders() {
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        ClassLoader first = new ClassLoader(previous) { };
        ClassLoader second = new ClassLoader(previous) { };
        MidletRuntime.registerTextInputHandler(first, request -> "");
        MidletRuntime.registerTextInputHandler(second, request -> "");
        try {
            thread.setContextClassLoader(first);
            var a = Environment3D.getDefaultEnvironment3D();
            a.setReverbPreset("Cave"); a.setListenerPosition(1000, 0, 0);
            thread.setContextClassLoader(second);
            var b = Environment3D.getDefaultEnvironment3D();
            assertNotSame(a, b);
            assertEquals("None", b.getReverbPreset());
            assertArrayEquals(new int[]{0, 0, 0}, b.getListenerPosition());
            thread.setContextClassLoader(first);
            assertSame(a, Environment3D.getDefaultEnvironment3D());
        } finally {
            thread.setContextClassLoader(previous);
            MidletRuntime.unregisterTextInputHandler(first);
            MidletRuntime.unregisterTextInputHandler(second);
        }
    }
}
