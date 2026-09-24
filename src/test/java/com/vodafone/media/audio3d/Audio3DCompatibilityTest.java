package com.vodafone.media.audio3d;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class Audio3DCompatibilityTest {
    private final List<Player> players = new ArrayList<>();
    private final Environment3D environment = Environment3D.getDefaultEnvironment3D();

    @After public void closePlayers() {
        players.forEach(Player::close);
        environment.setReverbPreset(Environment3D.REVERB_NONE);
    }

    @Test public void originalApplicationInitializationUsesTheSdkReturnDescriptor() throws Exception {
        assertEquals(int.class, Environment3D.class.getMethod("setReverbTime", int.class).getReturnType());
        environment.setOutputDevice(Environment3D.DEVICE_HEADPHONES);
        environment.setReverbPreset("None");
        assertEquals(0, environment.setReverbTime(0));
        assertEquals(0, environment.setReverbTime(2000));
        assertEquals(4, environment.getMaxSourceChannels());
    }

    @Test public void activeReverbPresetsUseNativeLimitsAndKeepTheirOwnDecayTime() {
        environment.setReverbPreset(Environment3D.REVERB_ROOM);
        int room = environment.getReverbTime();
        environment.setReverbPreset(Environment3D.REVERB_CAVE);
        int cave = environment.getReverbTime();
        try {
            environment.setReverbPreset(Environment3D.REVERB_ROOM);
            assertEquals(300, environment.setReverbTime(0));
            assertEquals(30000, environment.setReverbTime(Integer.MAX_VALUE));
            environment.setReverbPreset(Environment3D.REVERB_CAVE);
            assertEquals(1490, environment.setReverbTime(1490));
            environment.setReverbPreset(Environment3D.REVERB_ROOM);
            assertEquals(30000, environment.getReverbTime());
            assertThrows(IllegalArgumentException.class, () -> environment.setReverbTime(-1));
            assertEquals(30000, environment.getReverbTime());
        } finally {
            environment.setReverbPreset(Environment3D.REVERB_ROOM);
            environment.setReverbTime(room);
            environment.setReverbPreset(Environment3D.REVERB_CAVE);
            environment.setReverbTime(cave);
        }
    }

    @Test public void audioControlExtendsTheSdkModeInterfaceAndReturnsIndependentArrays() throws Exception {
        assertTrue(ExtendedAudioControl.class.isAssignableFrom(Audio3DControl.class));
        Player player = player();
        Audio3DControl audio = control(player);
        assertSame(audio, player.getControl(ExtendedAudioControl.class.getName()));
        audio.setPosition(-1000, 0, 0);
        audio.setVelocity(0, 100, 0);
        audio.setRolloff(1000, 1000, 100);
        audio.getPosition()[0] = 999;
        audio.getVelocity()[1] = 999;
        audio.getRolloff()[2] = 999;
        assertArrayEquals(new int[]{-1000, 0, 0}, audio.getPosition());
        assertArrayEquals(new int[]{0, 100, 0}, audio.getVelocity());
        assertArrayEquals(new int[]{1000, 1000, 100}, audio.getRolloff());
    }

    @Test public void fourChannelsAreReservedOnceAndReturnedOnDisableDeallocateAndClose() throws Exception {
        for (int index = 0; index < 4; index++) {
            Audio3DControl audio = control(player());
            audio.setMode(Audio3DControl.MODE_DYNAMIC);
            audio.setMode(Audio3DControl.MODE_DYNAMIC);
            audio.setMode(ExtendedAudioControl.MODE_EXTENDED);
            assertEquals(3 - index, environment.getAvailableSourceChannels());
        }
        Player fifth = player();
        assertThrows(MediaException.class, () -> control(fifth).setMode(Audio3DControl.MODE_DYNAMIC));
        assertEquals(ExtendedAudioControl.MODE_DISABLED, control(fifth).getMode());
        control(players.get(0)).setMode(ExtendedAudioControl.MODE_DISABLED);
        assertEquals(1, environment.getAvailableSourceChannels());
        control(fifth).setMode(Audio3DControl.MODE_DYNAMIC);
        players.get(1).deallocate();
        assertEquals(1, environment.getAvailableSourceChannels());
        Audio3DControl stale = control(players.get(2));
        players.get(2).close();
        players.get(2).close();
        assertEquals(2, environment.getAvailableSourceChannels());
        assertThrows(IllegalStateException.class, () -> stale.setMode(Audio3DControl.MODE_DYNAMIC));
    }

    @Test public void runtimeTeardownReturnsEveryReservedChannel() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader app = new ClassLoader(previous) { };
        remexa.host.runtime.MidletRuntime.registerTextInputHandler(app, request -> "");
        try {
            Thread.currentThread().setContextClassLoader(app);
            control(player()).setMode(Audio3DControl.MODE_DYNAMIC);
            assertEquals(3, environment.getAvailableSourceChannels());
            Manager.shutdownOwnedPlayers(app);
            assertEquals(4, environment.getAvailableSourceChannels());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            remexa.host.runtime.MidletRuntime.unregisterTextInputHandler(app);
        }
    }

    private Player player() throws Exception {
        Player player = Manager.createPlayer(new ByteArrayInputStream(new byte[0]), "unknown");
        players.add(player);
        player.realize();
        return player;
    }

    private static Audio3DControl control(Player player) {
        return (Audio3DControl) player.getControl(Audio3DControl.class.getName());
    }
}
