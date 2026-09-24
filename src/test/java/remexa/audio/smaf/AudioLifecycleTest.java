package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhrasePlayer;
import com.jblend.media.smaf.phrase.PhraseTrack;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.midi.*;
import javax.sound.sampled.SourceDataLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import remexa.audio.AudioCallbacks;
import remexa.audio.pcm.RenderedPcmAudio;
import remexa.audio.pcm.RenderedPcmPlayer;
import remexa.host.runtime.AppRuntime;
import remexa.host.runtime.MidletRuntime;

import static org.junit.Assert.*;

/** Real player/mixer lifecycles with an injected output line, without an audio device. */
public class AudioLifecycleTest {
    private static final ClassLoader HOST = AudioLifecycleTest.class.getClassLoader();
    private static final int RATE = 8000;
    private final List<ClassLoader> owners = new ArrayList<>();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final List<Object> engines = new ArrayList<>();
    private final AtomicInteger writes = new AtomicInteger();
    private ClassLoader previousContext;

    @Before public void setup() {
        previousContext = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(HOST);
    }

    @After public void cleanup() throws Exception {
        Thread.currentThread().setContextClassLoader(HOST);
        for (AutoCloseable resource : resources) {
            resource.close();
        }
        for (ClassLoader owner : owners) {
            MidletRuntime.beginShutdown(owner);
            com.j_phone.amuse.PhrasePlayer.getPlayer().killOwnedBy(owner);
            com.jblend.media.MediaPlayer.shutdownOwnedPlayers(owner);
            javax.microedition.media.Manager.shutdownOwnedPlayers(owner);
            com.mitsubishielectric.carnavi.Sound.shutdownOwnedPlayers(owner);
            MidletRuntime.unregisterTextInputHandler(owner);
        }
        for (Object engine : engines) {
            synchronized (get(engine, "engineLock")) {
                call(engine, "closeLineLocked");
                get(engine, "engineLock").notifyAll();
            }
            Thread worker = (Thread) get(engine, "worker");
            if (worker != null) {
                worker.interrupt();
                worker.join(2000);
                assertFalse("Mixer failed to shut down", worker.isAlive());
            }
        }
        Thread.currentThread().setContextClassLoader(previousContext);
    }

    @Test public void switchingAppsCreatesFreshTracksAndInvalidatesOldReferences() throws Exception {
        ClassLoader a = owner("A"), b = owner("B");
        enter(a);
        PhrasePlayer oldPool = PhrasePlayer.getPlayer();
        PhraseTrack old = oldPool.getTrack(0);
        old.setVolume(7); old.setPanpot(2); old.mute(true);
        old.setEventListener(event -> fail("Closed game's listener executed"));
        MidletRuntime.beginShutdown(a);
        enter(HOST);
        PhrasePlayer.getPlayer().killOwnedBy(a);
        enter(b);
        PhrasePlayer pool = PhrasePlayer.getPlayer();
        PhraseTrack next = pool.getTrack(0);
        assertNotSame(oldPool, pool);
        assertNotSame(old, next);
        assertEquals(127, next.getVolume()); assertEquals(64, next.getPanpot()); assertFalse(next.isMute());
        assertEquals(PhraseTrack.NO_DATA, old.getState());
        assertThrows(IllegalStateException.class, () -> old.setVolume(0));
        assertThrows(IllegalStateException.class, () -> oldPool.getTrack(0));
        old.removePhrase(); old.stop();
        assertEquals(127, next.getVolume());
    }

    @Test public void concurrentAppsAndJphoneWrappersHaveIndependentPools() {
        enter(owner("A"));
        var a = com.j_phone.amuse.PhrasePlayer.getPlayer();
        var old = a.getTrack(0); old.mute(true);
        enter(owner("B"));
        var b = com.j_phone.amuse.PhrasePlayer.getPlayer();
        var next = b.getTrack(0);
        assertNotSame(a, b); assertNotSame(old, next); assertFalse(next.isMute());
        a.disposeTrack(old);
        assertFalse(next.isMute());
    }

    @Test public void disposedTrackCannotOperateOnItsReplacementWithinSameApp() {
        enter(owner("A"));
        PhrasePlayer pool = PhrasePlayer.getPlayer();
        PhraseTrack old = pool.getTrack(0);
        old.mute(true);
        pool.disposeTrack(old);
        PhraseTrack replacement = pool.getTrack(0);
        assertNotSame(old, replacement);
        assertFalse(replacement.isMute());
        assertThrows(IllegalStateException.class, () -> old.mute(true));
        pool.disposeTrack(old);
        assertSame(replacement, pool.getTrack(0));
    }

    @Test public void inGameKillKeepsCachedJphoneTracksAndListenersUsable() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        enter(owner("screen-transitions"));
        var pool = com.j_phone.amuse.PhrasePlayer.getPlayer();
        var cached = new com.j_phone.amuse.PhraseTrack[pool.getTrackCount()];
        AtomicInteger completions = new AtomicInteger();
        for (int i = 0; i < cached.length; i++) {
            cached[i] = pool.getTrack(i);
            cached[i].setEventListener(id -> { if (id == -1) completions.incrementAndGet(); });
        }
        // Initial D stores these handles once, then kill()/removePhrase() between screens.
        for (int screen = 0; screen < 3; screen++) {
            PhraseTrack master = (PhraseTrack) get(cached[0], "delegate");
            SmafPlayback previous = attachPlayback(master, new Session());
            cached[1].setSubjectTo(cached[0]);
            cached[0].setVolume(81);
            pool.kill();
            assertTrue((Boolean) get(previous, "closed"));
            assertEquals(0, completions.get());
            for (int i = 0; i < cached.length; i++) {
                assertSame("kill() replaced the game's cached track", cached[i], pool.getTrack(i));
                assertNull(cached[i].getPhrase());
                assertNull(cached[i].getSyncMaster());
                cached[i].removePhrase();
            }
            assertEquals(81, master.getVolume());
            cached[0].setPhrase(null); // The same handle must accept the next screen's phrase.
            Session gameplay = new Session();
            attachPlayback(master, gameplay);
            cached[0].play(2);
            assertTrue("Gameplay completion listener was lost", await(() -> completions.get() == 1, 2000));
            assertEquals(2, gameplay.rewinds.get());
            completions.set(0);
        }
    }

    @Test public void inGameKillPreservesAudioTracksButAppShutdownStillRetiresThem() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        ClassLoader app = owner("audio-tracks"); enter(app);
        PhrasePlayer pool = PhrasePlayer.getPlayer();
        var cached = pool.getAudioTrack(0);
        PhraseTrack track = (PhraseTrack) get(cached, "delegate");
        SmafPlayback old = attachPlayback(track, new Session());
        CountDownLatch completed = new CountDownLatch(1);
        cached.setEventListener(id -> completed.countDown());
        pool.kill();
        assertTrue((Boolean) get(old, "closed"));
        assertSame(cached, pool.getAudioTrack(0));
        cached.setVolume(70);
        attachPlayback(track, new Session());
        cached.play(1);
        assertTrue(completed.await(2, TimeUnit.SECONDS));

        MidletRuntime.beginShutdown(app); enter(HOST);
        PhrasePlayer.getPlayer().killOwnedBy(app);
        pool.kill(); // A game's destroyApp() may repeat its normal audio cleanup.
        assertThrows(IllegalStateException.class, () -> cached.setVolume(70));
        enter(owner("next-app"));
        assertNotSame(cached, PhrasePlayer.getPlayer().getAudioTrack(0));
    }

    @Test public void queuedCompletionIsDiscardedAfterClose() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        SmafStreamingPlayer player = stream(new Session(), List.of());
        AtomicInteger callbacks = new AtomicInteger();
        player.setListener(event -> callbacks.incrementAndGet());
        Object sink = queueCompletion(player);
        player.close();
        call(sink, "runAll");
        assertFalse(await(() -> callbacks.get() != 0, 100));
        assertThrows(IllegalStateException.class, () -> player.play(1));
    }

    @Test public void userEventFromPreviousPlaybackCannotReachReplacementListener() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        SmafStreamingPlayer player = stream(new Session(), List.of());
        AtomicInteger callbacks = new AtomicInteger();
        player.setListener(event -> callbacks.incrementAndGet());
        Object handle = get(player, "handle");
        long oldEpoch = (Long) get(handle, "playbackEpoch");
        player.stop();
        player.setListener(event -> callbacks.addAndGet(10));
        call(handle, "dispatchUserEvent", 42, oldEpoch);
        assertEquals(0, callbacks.get());
    }

    @Test public void pcmQueuedCompletionIsDiscardedAfterClose() throws Exception {
        installEngine(RenderedPcmPlayer.class, RATE);
        RenderedPcmPlayer player = keep(new RenderedPcmPlayer(new RenderedPcmAudio(RATE, 2, 8, new byte[32])));
        AtomicInteger callbacks = new AtomicInteger();
        player.setCompletionListener(callbacks::incrementAndGet);
        Object handle = get(player, "handle");
        set(handle, "completionPending", true); set(handle, "completionTargetFrame", 0L);
        List<Runnable> notifications = new ArrayList<>();
        call(handle, "dispatchReadyCompletion", 0L, notifications);
        player.close(); notifications.forEach(Runnable::run);
        assertFalse(await(() -> callbacks.get() != 0, 100));
    }

    @Test public void listenerFailureDoesNotKillSharedMixerOrSubsequentPlayback() throws Exception {
        Object engine = installEngine(SmafStreamingPlayer.class, RATE);
        SmafStreamingPlayer first = stream(new Session(), List.of(new SMAFDecoder.SequenceUserEvent(0, 1)));
        CountDownLatch event = new CountDownLatch(1);
        first.setListener(id -> {
            if (id == 1) { event.countDown(); throw new IllegalStateException("Game listener failure"); }
        });
        first.play(1);
        assertTrue(event.await(2, TimeUnit.SECONDS));
        SmafStreamingPlayer next = stream(new Session(), List.of());
        CountDownLatch complete = new CountDownLatch(1);
        next.setListener(id -> complete.countDown()); next.play(1);
        assertTrue(complete.await(2, TimeUnit.SECONDS));
        assertTrue(((Thread) get(engine, "worker")).isAlive());
    }

    @Test public void deadWorkerReferenceIsReplaced() throws Exception {
        Object engine = installEngine(SmafStreamingPlayer.class, RATE);
        Thread dead = new Thread(() -> { }); dead.start(); dead.join();
        set(engine, "worker", dead);
        SmafStreamingPlayer player = stream(new Session(), List.of());
        CountDownLatch complete = new CountDownLatch(1);
        player.setListener(id -> complete.countDown()); player.play(1);
        assertTrue(complete.await(2, TimeUnit.SECONDS));
        assertNotSame(dead, get(engine, "worker"));
    }

    @Test public void warmupAndMixerAreHostOwnedButCallbacksEnterTheCorrectApp() throws Exception {
        Object engine = installEngine(SmafStreamingPlayer.class, RATE);
        ClassLoader a = owner("A"), b = owner("B"); enter(a);
        ExecutorService executor = (ExecutorService) field(SmafPlayback.class, "WARMUP_EXECUTOR").get(null);
        Thread warmup = executor.submit(Thread::currentThread).get(2, TimeUnit.SECONDS);
        assertSame(HOST, warmup.getContextClassLoader());
        assertFalse((Boolean) call(new AppRuntime(), "isAppThread", warmup, a));
        MidletRuntime.beginShutdown(a); enter(b);
        SmafStreamingPlayer player = keep(executor.submit(() ->
                new SmafStreamingPlayer(new Session(), List.of(), b)).get(2, TimeUnit.SECONDS));
        assertSame(HOST, ((Thread) get(engine, "worker")).getContextClassLoader());
        CountDownLatch complete = new CountDownLatch(2);
        AtomicReference<ClassLoader> callbackOwner = new AtomicReference<>();
        AtomicInteger iterations = new AtomicInteger();
        player.setListener(id -> {
            MidletRuntime.ensureThreadActive();
            callbackOwner.set(MidletRuntime.currentAppClassLoader());
            if (iterations.incrementAndGet() == 1) { player.play(1); }
            complete.countDown();
        });
        player.play(1);
        assertTrue("Callback-driven looping stopped", complete.await(2, TimeUnit.SECONDS));
        assertSame(b, callbackOwner.get());
    }

    @Test public void closedAppCallbacksAreDroppedAndHostContextIsRestored() {
        ClassLoader app = owner("A");
        AudioCallbacks.run(app, () -> assertSame(app, MidletRuntime.currentAppClassLoader()));
        assertSame(HOST, Thread.currentThread().getContextClassLoader());
        MidletRuntime.beginShutdown(app);
        AudioCallbacks.run(app, () -> fail("Closed app callback ran"));
    }

    @Test public void sharedMixerIsNotAnAppThreadWhileDeliveringAUserEvent() throws Exception {
        Object engine = installEngine(SmafStreamingPlayer.class, RATE);
        ClassLoader app = owner("callback-owner"); enter(app);
        SmafStreamingPlayer player = stream(new Session(), List.of(new SMAFDecoder.SequenceUserEvent(0, 1)));
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        player.setListener(id -> {
            if (id != 1) return;
            entered.countDown();
            try { release.await(2, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        });
        try {
            player.play(1);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Thread worker = (Thread) get(engine, "worker");
            assertSame(app, worker.getContextClassLoader());
            assertFalse((Boolean) call(new AppRuntime(), "isAppThread", worker, app));
        } finally {
            release.countDown();
        }
    }

    @Test public void synchronizedPhraseTracksFinishAllRepeatsAndCanPlayAgain() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        enter(owner("group"));
        PhrasePlayer pool = PhrasePlayer.getPlayer();
        PhraseTrack master = pool.getTrack(0), slave = pool.getTrack(1);
        Session masterSession = new Session(), slaveSession = new Session();
        attachPlayback(master, masterSession);
        attachPlayback(slave, slaveSession);
        slave.setSubjectTo(master);
        CountDownLatch complete = new CountDownLatch(2);
        master.setEventListener(id -> complete.countDown());
        slave.setEventListener(id -> complete.countDown());
        master.play(3);
        assertTrue("Grouped repeats stopped early", complete.await(2, TimeUnit.SECONDS));
        assertEquals(3, masterSession.rewinds.get());
        assertEquals(3, slaveSession.rewinds.get());
        assertEquals(PhraseTrack.READY, master.getState());
        assertEquals(PhraseTrack.READY, slave.getState());
        CountDownLatch again = new CountDownLatch(2);
        master.setEventListener(id -> again.countDown());
        slave.setEventListener(id -> again.countDown());
        master.play(1);
        assertTrue(again.await(2, TimeUnit.SECONDS));
    }

    @Test public void aReplacedPhraseCannotCompleteTheCurrentPhrase() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        enter(owner("replacement"));
        PhraseTrack track = PhrasePlayer.getPlayer().getTrack(0);
        SmafPlayback old = attachPlayback(track, new Session());
        SmafPlayback current = attachPlayback(track, new Session());
        AtomicInteger events = new AtomicInteger();
        track.setEventListener(id -> events.incrementAndGet());
        call(track, "handlePlaybackEvent", old, -1);
        call(track, "handlePlaybackEvent", old, 42);
        assertEquals(0, events.get());
        call(track, "handlePlaybackEvent", current, -1);
        assertEquals(1, events.get());
    }

    @Test public void midiLeaseIsReleasedOnlyByItsOwnerAndOnlyOnce() throws Exception {
        AtomicInteger closes = new AtomicInteger();
        Receiver receiver = new Receiver() {
            public void send(MidiMessage message, long stamp) { }
            public void close() { closes.incrementAndGet(); }
        };
        field(SmafPlayback.class, "sharedReceiver").set(null, receiver);
        SmafPlayback a = keep(SmafPlayback.create(new byte[0]));
        SmafPlayback b = keep(SmafPlayback.create(new byte[0]));
        SmafPlayback unopened = keep(SmafPlayback.create(new byte[0]));
        call(a, "acquireSharedMidi"); call(b, "acquireSharedMidi"); call(a, "acquireSharedMidi");
        assertEquals(2, field(SmafPlayback.class, "sharedSynthUsers").getInt(null));
        unopened.close(); a.close(); a.close();
        assertEquals(0, closes.get());
        assertEquals(1, field(SmafPlayback.class, "sharedSynthUsers").getInt(null));
        b.close();
        assertEquals(1, closes.get());
        assertEquals(0, field(SmafPlayback.class, "sharedSynthUsers").getInt(null));
    }

    @Test public void finiteRepeatsFinishAndAllowNextPlay() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        enter(owner("A"));
        SmafStreamingPlayer output = stream(new Session(), List.of());
        SmafPlayback playback = keep(SmafPlayback.create(new byte[0]));
        set(playback, "audioPlayer", output);
        var facade = new com.jblend.media.smaf.SmafPlayer(new byte[]{0});
        set(facade, "playback", playback);
        AtomicInteger completed = new AtomicInteger();
        output.setListener(id -> {
            try { call(facade, "handlePlaybackEvent", id); } catch (Exception ex) { throw new RuntimeException(ex); }
            completed.incrementAndGet();
        });
        facade.play(2);
        assertTrue(await(() -> completed.get() == 1, 2000));
        assertEquals(com.jblend.media.MediaPlayer.READY, facade.getState());
        facade.play(1);
        assertTrue(await(() -> completed.get() == 2, 2000));
        assertEquals(com.jblend.media.MediaPlayer.READY, facade.getState());
    }

    @Test public void audioPhraseResumeActuallyResumes() throws Exception {
        installEngine(SmafStreamingPlayer.class, RATE);
        enter(owner("A"));
        var track = PhrasePlayer.getPlayer().getAudioTrack(0);
        SmafStreamingPlayer output = stream(new Session(), List.of());
        SmafPlayback playback = keep(SmafPlayback.create(new byte[0])); set(playback, "audioPlayer", output);
        set(get(track, "delegate"), "playback", playback);
        set(get(output, "handle"), "paused", true);
        int before = writes.get(); track.resume();
        assertTrue(await(() -> writes.get() > before, 2000));
        assertNotEquals(PhraseTrack.PAUSED, track.getState());
    }

    @Test public void soundControlsDoNotCarryOverToAnotherApp() {
        enter(owner("A"));
        com.mitsubishielectric.carnavi.Sound.setVolume(7);
        com.mitsubishielectric.carnavi.Sound.setMute(true);
        enter(owner("B"));
        assertEquals(100, com.mitsubishielectric.carnavi.Sound.getVolume());
        assertFalse(com.mitsubishielectric.carnavi.Sound.isMuted());
    }

    @Test public void repeatedRuntimeShutdownsLeaveNoOldTracksOrMixerHandles() throws Exception {
        var descriptor = new remexa.host.jad.JadDescriptor(java.nio.file.Path.of("audio-test.jad"),
                Map.of("MIDlet-Name", "audio-test"), List.of());
        for (int index = 0; index < 30; index++) {
            Object engine = installEngine(SmafStreamingPlayer.class, RATE);
            ClassLoader app = owner("cycle-" + index); enter(app);
            PhraseTrack track = PhrasePlayer.getPlayer().getTrack(0);
            assertFalse(track.isMute()); assertEquals(127, track.getVolume());
            SmafStreamingPlayer output = stream(new Session(), List.of());
            SmafPlayback playback = keep(SmafPlayback.create(new byte[0]));
            set(playback, "audioPlayer", output); set(track, "playback", playback);
            output.setListener(id -> {
                try { call(track, "handlePlaybackEvent", playback, id); }
                catch (Exception ex) { throw new RuntimeException(ex); }
            });
            CountDownLatch twice = new CountDownLatch(2);
            track.setEventListener(id -> {
                assertSame(app, MidletRuntime.currentAppClassLoader());
                if (twice.getCount() == 2) { track.play(1); }
                twice.countDown();
            });
            track.play(1);
            assertTrue("Cycle " + index + " did not loop", twice.await(2, TimeUnit.SECONDS));
            track.mute(true); track.setVolume(7);
            enter(HOST);
            new AppRuntime().shutdown(new remexa.host.runtime.LaunchResult(
                    descriptor, null, "test.jar", "test", app, null, null));
            assertEquals(PhraseTrack.NO_DATA, track.getState());
            assertTrue((Boolean) get(get(output, "handle"), "closed"));
            synchronized (get(engine, "engineLock")) {
                assertTrue(((List<?>) get(engine, "handles")).isEmpty());
            }
        }
    }

    @Test public void aChunkRenderedDuringCloseCannotWriteAfterTheHandleIsRetired() throws Exception {
        Object engine = installEngine(SmafStreamingPlayer.class, RATE);
        CountDownLatch rendering = new CountDownLatch(1), finishRender = new CountDownLatch(1);
        SmafStreamingSession session = new SmafStreamingSession() {
            public int sampleRate() { return RATE; }
            public int channelCount() { return 2; }
            public void rewind() { }
            public int render(float[] buffer, int frames) throws Exception {
                rendering.countDown();
                if (!finishRender.await(2, TimeUnit.SECONDS)) throw new AssertionError("render blocked");
                return 8;
            }
        };
        SmafStreamingPlayer player = keep(new SmafStreamingPlayer(session, List.of()));
        player.play(1);
        assertTrue(rendering.await(2, TimeUnit.SECONDS));
        Object handle = get(player, "handle");
        Thread closer = AudioCallbacks.hostThread(player::close, "test-close");
        synchronized (get(engine, "engineLock")) {
            closer.start();
            finishRender.countDown();
            assertTrue(await(() -> {
                try {
                    synchronized (get(handle, "stateLock")) { return (Boolean) get(handle, "closed"); }
                } catch (Exception ex) { throw new RuntimeException(ex); }
            }, 2000));
        }
        closer.join(2000);
        assertFalse(closer.isAlive());
        assertEquals(0, writes.get());
    }

    @Test public void streamingControlsDoNotWaitForTheAudioDevice() throws Exception {
        verifyResponsiveControls(SmafStreamingPlayer.class);
    }

    @Test public void pcmControlsDoNotWaitForTheAudioDevice() throws Exception {
        verifyResponsiveControls(RenderedPcmPlayer.class);
    }

    @Test public void renderedSmafControlsDoNotWaitForTheAudioDevice() throws Exception {
        verifyResponsiveControls(SmafRenderedPlayer.class);
    }

    @Test public void closingDuringADeviceWriteDiscardsTheRetiredOutput() throws Exception {
        for (Class<?> type : List.of(SmafStreamingPlayer.class, RenderedPcmPlayer.class, SmafRenderedPlayer.class)) {
            CountDownLatch writing = new CountDownLatch(1), releaseWrite = new CountDownLatch(1);
            AtomicInteger lineCloses = new AtomicInteger();
            Object engine = installEngine(type, RATE, (proxy, method, args) -> {
                if (method.getName().equals("write")) {
                    writing.countDown();
                    if (!releaseWrite.await(3, TimeUnit.SECONDS)) throw new AssertionError("Write never released");
                }
                if (method.getName().equals("close")) lineCloses.incrementAndGet();
                return fakeLineResult(method.getName(), args);
            });
            Object output = newOutput(type);
            call(output, "play", 0);
            assertTrue(writing.await(2, TimeUnit.SECONDS));
            Thread mixer;
            synchronized (get(engine, "engineLock")) { mixer = (Thread) get(engine, "worker"); }
            FutureTask<Void> closing = new FutureTask<>(() -> {
                ((AutoCloseable) output).close();
                if (type == SmafStreamingPlayer.class) call(engine, "closeIdleNow");
                return null;
            });
            Thread closer = AudioCallbacks.hostThread(closing, "test-close-during-device-write");
            closer.start();
            try {
                closing.get(500, TimeUnit.MILLISECONDS);
                assertEquals(1, lineCloses.get());
            } finally {
                releaseWrite.countDown();
                closer.join(2000);
                mixer.join(2000);
            }
            assertFalse("Closed mixer kept running: " + type.getSimpleName(), mixer.isAlive());
            synchronized (get(engine, "engineLock")) {
                assertNull(get(engine, "line"));
                assertEquals(0L, get(engine, "writtenFrames"));
            }
            assertEquals(1, lineCloses.get());
        }
    }

    private void verifyResponsiveControls(Class<?> type) throws Exception {
        CountDownLatch writing = new CountDownLatch(1), releaseWrite = new CountDownLatch(1);
        installEngine(type, RATE, (proxy, method, args) -> {
            if (method.getName().equals("write")) {
                writing.countDown();
                if (!releaseWrite.await(3, TimeUnit.SECONDS)) throw new AssertionError("Write never released");
            }
            return fakeLineResult(method.getName(), args);
        });
        Object music = newOutput(type);
        call(music, "play", 0);
        assertTrue(writing.await(2, TimeUnit.SECONDS));
        FutureTask<Void> effect = new FutureTask<>(() -> {
            Object output = newOutput(type);
            try {
                call(output, "play", 1);
                call(output, "pause");
                call(output, "resume");
                call(output, "stop");
            } finally {
                ((AutoCloseable) output).close();
            }
            return null;
        });
        Thread game = AudioCallbacks.hostThread(effect, "test-game-audio-control");
        game.start();
        try {
            effect.get(500, TimeUnit.MILLISECONDS);
        } finally {
            releaseWrite.countDown();
            game.join(2000);
        }
    }

    @Test public void drainedPcmAndRenderedSmafWaitInsteadOfBusySpinning() throws Exception {
        for (Class<?> type : List.of(RenderedPcmPlayer.class, SmafRenderedPlayer.class)) {
            AtomicLong playedFrames = new AtomicLong();
            AtomicInteger positionPolls = new AtomicInteger();
            CountDownLatch polling = new CountDownLatch(1), completed = new CountDownLatch(1);
            installEngine(type, RATE, (proxy, method, args) -> {
                if (method.getName().equals("getLongFramePosition")) {
                    positionPolls.incrementAndGet(); polling.countDown();
                    return playedFrames.get();
                }
                return fakeLineResult(method.getName(), args);
            });
            Object output = newOutput(type);
            if (output instanceof RenderedPcmPlayer pcm) pcm.setCompletionListener(completed::countDown);
            else ((SmafRenderedPlayer) output).setListener(id -> completed.countDown());
            call(output, "play", 1);
            assertTrue(polling.await(2, TimeUnit.SECONDS));
            assertFalse("Completion polling spins while the device drains: " + type.getSimpleName(),
                    await(() -> positionPolls.get() > 100, 100));
            playedFrames.set(Long.MAX_VALUE);
            assertTrue(completed.await(2, TimeUnit.SECONDS));
        }
    }

    private Object newOutput(Class<?> type) {
        if (type == SmafStreamingPlayer.class) return stream(new Session(), List.of());
        if (type == RenderedPcmPlayer.class)
            return keep(new RenderedPcmPlayer(new RenderedPcmAudio(RATE, 2, 8, new byte[32])));
        return keep(new SmafRenderedPlayer(new SmafRenderedAudio(RATE, 2, 8, new byte[32])));
    }

    @Test public void spatialPcmAndStreamingMixersPreserveLoopsAndProduceTheSameStereoTail() throws Exception {
        byte[] expected = null;
        for (Class<?> type : List.of(RenderedPcmPlayer.class, SmafStreamingPlayer.class)) {
            var captured = new java.io.ByteArrayOutputStream();
            installEngine(type, RATE, (proxy, method, args) -> {
                if (method.getName().equals("write")) captured.write((byte[]) args[0], (Integer) args[1], (Integer) args[2]);
                return fakeLineResult(method.getName(), args);
            });
            var scene = new remexa.audio.spatial.Audio3DScene();
            var source = scene.createSource();
            scene.setReverb(8, 300);
            source.setMode(2); source.setPosition(-100, 0, -100); source.setReverbLevel(100);
            byte[] pcm = new byte[1200];
            for (int i = 0; i < 600; i++) {
                int sample = (int) (10000 * Math.sin(2 * Math.PI * 400 * i / RATE));
                pcm[i * 2] = (byte) sample; pcm[i * 2 + 1] = (byte) (sample >> 8);
            }
            CountDownLatch completion = new CountDownLatch(1);
            Object output;
            if (type == RenderedPcmPlayer.class) {
                var player = keep(new RenderedPcmPlayer(new RenderedPcmAudio(RATE, 1, 600, pcm), source));
                player.setCompletionListener(completion::countDown); output = player;
            } else {
                SmafStreamingSession session = new SmafStreamingSession() {
                    int position;
                    public int sampleRate() { return RATE; }
                    public int channelCount() { return 1; }
                    public void rewind() { position = 0; }
                    public int render(float[] buffer, int count) {
                        int frames = Math.min(count, 600 - position);
                        for (int i = 0; i < frames; i++, position++) {
                            buffer[i] = (short) ((pcm[position * 2] & 255) | (pcm[position * 2 + 1] << 8)) / 32768f;
                        }
                        return frames;
                    }
                };
                var player = keep(new SmafStreamingPlayer(session, List.of(), HOST, source));
                player.setListener(id -> { if (id == -1) completion.countDown(); }); output = player;
            }
            call(output, "play", 3);
            assertTrue("Effects failed to drain", completion.await(3, TimeUnit.SECONDS));
            byte[] actual = captured.toByteArray();
            assertEquals("Three iterations, then one room tail", (1800 + 6200) * 4, actual.length);
            assertTrue(stereoEnergy(actual, 0) > stereoEnergy(actual, 1) * 2);
            if (expected != null) assertArrayEquals(expected, actual);
            expected = actual;
        }
    }

    @Test public void spatialMuteIncludesReverbAndPauseAndCloseRemainResponsive() throws Exception {
        for (Class<?> type : List.of(RenderedPcmPlayer.class, SmafStreamingPlayer.class)) {
            CountDownLatch writing = new CountDownLatch(1), release = new CountDownLatch(1), mutedWrite = new CountDownLatch(1);
            AtomicBoolean muted = new AtomicBoolean();
            AtomicInteger nonzeroMutedSamples = new AtomicInteger();
            installEngine(type, RATE, (proxy, method, args) -> {
                if (method.getName().equals("write")) {
                    if (muted.get()) {
                        byte[] data = (byte[]) args[0];
                        for (int i = (Integer) args[1]; i < (Integer) args[1] + (Integer) args[2]; i++)
                            if (data[i] != 0) nonzeroMutedSamples.incrementAndGet();
                        mutedWrite.countDown();
                    } else {
                        writing.countDown();
                        if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Write never released");
                    }
                }
                return fakeLineResult(method.getName(), args);
            });
            var scene = new remexa.audio.spatial.Audio3DScene();
            scene.setReverb(2, 1000);
            var source = scene.createSource(); source.setMode(2); source.setReverbLevel(100);
            Object output;
            if (type == RenderedPcmPlayer.class) {
                byte[] pcm = new byte[16000]; Arrays.fill(pcm, (byte) 32);
                output = keep(new RenderedPcmPlayer(new RenderedPcmAudio(RATE, 1, 8000, pcm), source));
            } else {
                SmafStreamingSession session = new SmafStreamingSession() {
                    public int sampleRate() { return RATE; }
                    public int channelCount() { return 1; }
                    public void rewind() { }
                    public int render(float[] data, int count) { Arrays.fill(data, 0, count, .25f); return count; }
                };
                output = keep(new SmafStreamingPlayer(session, List.of(), HOST, source));
            }
            try {
                call(output, "play", 0);
                assertTrue(writing.await(2, TimeUnit.SECONDS));
                call(output, "pause");
                assertEquals(RenderedPcmPlayer.PAUSED, call(output, "getState"));
                call(output, "setVolume", 0);
                call(output, "resume");
                muted.set(true); release.countDown();
                assertTrue(mutedWrite.await(2, TimeUnit.SECONDS));
                ((AutoCloseable) output).close();
                assertEquals(0, nonzeroMutedSamples.get());
            } finally { release.countDown(); }
        }
    }

    @Test public void bothYamahaSynthesizersFeedAudibleOutputThroughTheEffectsStage() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 1000);
        var track = sequence.createTrack();
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0), 500));
        var midi = new java.io.ByteArrayOutputStream(); MidiSystem.write(sequence, 0, midi);
        for (String synth : List.of("ma3", "ma5")) {
            var captured = new java.io.ByteArrayOutputStream();
            int rate = synth.equals("ma3") ? 32000 : Integer.getInteger("remexa.ma5SampleRate", 48000);
            installEngine(SmafStreamingPlayer.class, rate, (proxy, method, args) -> {
                if (method.getName().equals("write")) captured.write((byte[]) args[0], (Integer) args[1], (Integer) args[2]);
                return fakeLineResult(method.getName(), args);
            });
            var scene = new remexa.audio.spatial.Audio3DScene();
            var source = scene.createSource(); source.setMode(2); source.setPosition(1000, 0, -200);
            var playback = keep(YamahaMidiPlayback.create(midi.toByteArray(), synth, source));
            CountDownLatch completion = new CountDownLatch(1); playback.setCompletionListener(completion::countDown);
            playback.play(1);
            assertTrue(synth + " failed to finish", completion.await(5, TimeUnit.SECONDS));
            byte[] pcm = captured.toByteArray();
            assertTrue(synth + " was silent", stereoEnergy(pcm, 1) > 100000);
            assertTrue(synth + " bypassed spatial positioning", stereoEnergy(pcm, 1) > stereoEnergy(pcm, 0) * 10);
        }
    }

    private static double stereoEnergy(byte[] pcm, int channel) {
        double energy = 0;
        for (int i = channel * 2; i < pcm.length; i += 4) {
            double value = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8)); energy += value * value;
        }
        return energy;
    }

    @Test public void asynchronouslyOpenedSmafKeepsTheCreatingAppsIdentity() throws Exception {
        installEngine(SmafStreamingPlayer.class, 32000);
        String synth = System.getProperty("remexa.smafSynth");
        try {
            System.setProperty("remexa.smafSynth", "ma3");
            ClassLoader app = owner("async-owner"); enter(app);
            SmafPlayback playback = keep(SmafPlayback.create(new byte[0]));
            Sequence sequence = new Sequence(Sequence.PPQ, 1000);
            var track = sequence.createTrack();
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 80), 0));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0), 20));
            // Supply decoded events so this lifecycle test is independent of SMAF parsing.
            set(playback, "sequence", sequence); set(playback, "midiSequence", sequence);
            set(playback, "decodedLoaded", true);
            AtomicReference<ClassLoader> callbackOwner = new AtomicReference<>();
            CountDownLatch complete = new CountDownLatch(1);
            playback.setListener(id -> { callbackOwner.set(MidletRuntime.currentAppClassLoader()); complete.countDown(); });
            playback.prepareAsync();
            assertTrue(await(() -> {
                try { return get(playback, "audioPlayer") != null; }
                catch (Exception ex) { throw new RuntimeException(ex); }
            }, 2000));
            playback.play(1);
            assertTrue(complete.await(2, TimeUnit.SECONDS));
            assertSame(app, callbackOwner.get());
        } finally {
            if (synth == null) System.clearProperty("remexa.smafSynth"); else System.setProperty("remexa.smafSynth", synth);
        }
    }

    @Test public void aFailedLaunchRollsBackCreatedPlayersAndTracks() throws Exception {
        java.nio.file.Path jar = java.nio.file.Files.createTempFile("remexa-audio-launch-test", ".jar");
        try {
            var manifest = new java.util.jar.Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            try (var stream = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar), manifest)) { }
            var descriptor = new remexa.host.jad.JadDescriptor(jar.resolveSibling("audio.jad"),
                    Map.of("MIDlet-Jar-URL", jar.toUri().toString(), "MIDlet-Name", "failing-audio"),
                    List.of(new remexa.host.jad.MidletEntry(1, "failing-audio", "", FailingMidlet.class.getName())));
            assertThrows(remexa.host.runtime.LaunchException.class, () -> new AppRuntime().launch(
                    descriptor, remexa.host.profile.LaunchProfileResolver.resolve(descriptor), metrics -> { }, request -> "", null));
            assertNotNull(FailingMidlet.createdPlayer);
            assertEquals(javax.microedition.media.Player.CLOSED, FailingMidlet.createdPlayer.getState());
            assertThrows(IllegalStateException.class, () -> FailingMidlet.createdTrack.setVolume(0));
            assertFalse(MidletRuntime.isAppActive(FailingMidlet.owner));
        } finally {
            java.nio.file.Files.deleteIfExists(jar);
        }
    }

    public static final class FailingMidlet extends javax.microedition.midlet.MIDlet {
        static javax.microedition.media.Player createdPlayer;
        static PhraseTrack createdTrack;
        static ClassLoader owner;
        public FailingMidlet() throws Exception {
            owner = MidletRuntime.currentAppClassLoader();
            createdPlayer = javax.microedition.media.Manager.createPlayer(new java.io.ByteArrayInputStream(new byte[]{0}), "unknown");
            createdTrack = PhrasePlayer.getPlayer().getTrack(0);
        }
        protected void startApp() { throw new IllegalStateException("Expected startup failure"); }
        protected void pauseApp() { }
        protected void destroyApp(boolean unconditional) { }
    }

    private ClassLoader owner(String name) {
        ClassLoader owner = new ClassLoader(name, HOST) { };
        owners.add(owner); MidletRuntime.registerTextInputHandler(owner, request -> ""); return owner;
    }
    private void enter(ClassLoader owner) { Thread.currentThread().setContextClassLoader(owner); }
    private <T extends AutoCloseable> T keep(T resource) { resources.add(resource); return resource; }
    private SmafStreamingPlayer stream(Session session, List<SMAFDecoder.SequenceUserEvent> events) {
        return keep(new SmafStreamingPlayer(session, events));
    }
    private SmafPlayback attachPlayback(PhraseTrack track, Session session) throws Exception {
        SmafStreamingPlayer output = stream(session, List.of());
        SmafPlayback playback = keep(SmafPlayback.create(new byte[0]));
        set(playback, "audioPlayer", output);
        set(track, "playback", playback);
        output.setListener(id -> {
            try { call(track, "handlePlaybackEvent", playback, id); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        return playback;
    }
    private Object queueCompletion(SmafStreamingPlayer player) throws Exception {
        Object handle = get(player, "handle");
        Object sink = construct(SmafStreamingPlayer.class.getName() + "$NotificationSink");
        set(handle, "completionPending", true); set(handle, "completionTargetFrame", 0L);
        call(handle, "dispatchReadyCompletion", 0L, sink); return sink;
    }
    private Object installEngine(Class<?> type, int rate) throws Exception {
        return installEngine(type, rate, (proxy, method, args) -> fakeLineResult(method.getName(), args));
    }
    @SuppressWarnings("unchecked")
    private Object installEngine(Class<?> type, int rate, InvocationHandler output) throws Exception {
        Object key = construct(type.getName() + "$OutputFormatKey", rate, 2);
        Object engine = construct(type.getName() + "$SharedEngine", key);
        SourceDataLine line = (SourceDataLine) Proxy.newProxyInstance(HOST, new Class<?>[]{SourceDataLine.class},
                output);
        set(engine, "line", line);
        ((Map<Object, Object>) field(type, "ENGINES").get(null)).put(key, engine);
        engines.add(engine); return engine;
    }
    private Object fakeLineResult(String method, Object[] args) {
        return switch (method) {
            case "isRunning", "isActive", "isOpen" -> true;
            case "write" -> { writes.incrementAndGet(); yield (Integer) args[2]; }
            case "getLongFramePosition", "getMicrosecondPosition" -> Long.MAX_VALUE;
            case "getLevel" -> 0f;
            case "getControls" -> new javax.sound.sampled.Control[0];
            case "isControlSupported" -> false;
            case "getFramePosition", "available", "getBufferSize" -> 0;
            case "toString" -> "FakeAudioLine";
            default -> null;
        };
    }
    private static final class Session implements SmafStreamingSession {
        private int position;
        private final AtomicInteger rewinds = new AtomicInteger();
        public int sampleRate() { return RATE; }
        public int channelCount() { return 2; }
        public int render(float[] output, int maxFrames) { return position++ == 0 ? 8 : 0; }
        public void rewind() { position = 0; rewinds.incrementAndGet(); }
    }
    private static boolean await(java.util.function.BooleanSupplier condition, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        do { if (condition.getAsBoolean()) return true; Thread.sleep(1); } while (System.nanoTime() < deadline);
        return condition.getAsBoolean();
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object get(Object object, String name) throws Exception { return field(object.getClass(), name).get(object); }
    private static void set(Object object, String name, Object value) throws Exception { field(object.getClass(), name).set(object, value); }
    private static Object call(Object object, String name, Object... args) throws Exception {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                try { return method.invoke(object, args); }
                catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof Exception exception) throw exception;
                    if (ex.getCause() instanceof Error error) throw error;
                    throw ex;
                }
            }
        }
        throw new NoSuchMethodException(name);
    }
    private static Object construct(String name, Object... args) throws Exception {
        for (Constructor<?> constructor : Class.forName(name).getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                constructor.setAccessible(true); return constructor.newInstance(args);
            }
        }
        throw new NoSuchMethodException(name);
    }
}
