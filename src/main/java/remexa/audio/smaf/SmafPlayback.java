package remexa.audio.smaf;

import com.jblend.media.smaf.phrase.PhraseTrackListener;
import org.recompile.mobile.Mobile;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;

import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Lightweight phrase playback wrapper for MEXA SMAF/SPF data.
 *
 * <p>SMAF phrases prefer a clean Yamaha render path selected by packet family,
 * while host MIDI output remains available as a fallback when no rendered
 * backend can be opened.</p>
 */
public final class SmafPlayback implements AutoCloseable {
    private static final String MIDI_DEVICE_AUTO = "auto";
    private static final String MIDI_DEVICE_GERVILL = "gervill";
    private static final String MIDI_DEVICE_DEFAULT = "default";
    private static final String SMAF_SYNTH_AUTO = "auto";
    private static final String SMAF_SYNTH_MA3 = "ma3";
    private static final String SMAF_SYNTH_MA5 = "ma5";
    private static final String SMAF_SYNTH_MA7 = "ma7";
    private static final String SMAF_SYNTH_MIDI = "midi";
    private static final int USER_EVENT_META_TYPE = 0x7F;
    private static final byte[] USER_EVENT_META_PREFIX = new byte[]{'R', 'X'};
    public static final int NO_DATA = 1;
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 5;
    private static final Ma3SmafAudioEngine MA3_ENGINE = new Ma3SmafAudioEngine();
    private static final Ma5SmafAudioEngine MA5_ENGINE = new Ma5SmafAudioEngine();
    private static final Ma7PlaceholderAudioEngine MA7_ENGINE = new Ma7PlaceholderAudioEngine();
    private static final int DECODE_CACHE_LIMIT = 32;
    private static final int RENDER_CACHE_LIMIT = 32;
    // Some game Destory and recreate the same short action phrases per input event.
    private static final Map<SmafCacheKey, DecodedSmaf> DECODE_CACHE = createLruCache(DECODE_CACHE_LIMIT);
    private static final Map<SmafRenderCacheKey, SmafRenderedAudio> RENDER_CACHE = createLruCache(RENDER_CACHE_LIMIT);
    private static final Map<SmafCacheKey, Future<DecodedSmaf>> DECODE_TASKS = createLruCache(DECODE_CACHE_LIMIT);
    private static final Map<SmafRenderCacheKey, Future<?>> RENDER_WARMUP_TASKS = createLruCache(RENDER_CACHE_LIMIT);
    private static final ExecutorService WARMUP_EXECUTOR = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "remexa-smaf-warmup");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final SmafCacheKey cacheKey;
    private final byte[] source;
    private final Object decodeLock = new Object();
    private final Object openLock = new Object();
    private Sequence sequence;
    private Sequence midiSequence;
    private List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents = Collections.emptyList();
    private List<byte[]> startupPackets = Collections.emptyList();
    private List<byte[]> exclusiveVoices = Collections.emptyList();
    private List<byte[]> pcmClipData = Collections.emptyList();
    private List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers = Collections.emptyList();
    private List<SMAFDecoder.SequenceUserEvent> userEvents = Collections.emptyList();
    private boolean hasPcmPayload;
    private int pcmClipCount;
    private boolean decodedLoaded;

    // Yamaha EXVO custom voices do not translate to the desktop GM synth.
    // Prefer fuller orchestral defaults over the tiny piano/square-lead sounding fallbacks.
    private static final int[] SOFTBANK_EXVO_DEFAULT_PROGRAMS = {48, 60, 40, 46};
    private static final int MIDI_PERCUSSION_CHANNEL = 9;
    private static final int GM_ACOUSTIC_BASS = 32;
    private static final int GM_CELLO = 41;
    private static final int GM_CONTRABASS = 43;
    private static final int GM_PIZZICATO_STRINGS = 45;
    private static final int GM_HARP = 47;
    private static final int GM_STRING_ENSEMBLE_1 = 48;
    private static final int GM_STRING_ENSEMBLE_2 = 49;
    private static final int GM_CHOIR_AAHS = 52;
    private static final int GM_VOICE_OOHS = 51;
    private static final int GM_SYNTH_VOICE = 54;
    private static final int GM_FRENCH_HORN = 61;
    private static final int GM_BASSOON = 70;
    private static final int GM_CLARINET = 71;
    private static final int GM_FLUTE = 73;
    private static final int GM_VIOLIN = 40;
    private static final Object MIDI_LOCK = new Object();
    private static final int[] MELODIC_CHANNEL_POOL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15};
    private static final boolean[] RESERVED_OUTPUT_CHANNELS = new boolean[16];
    private static Synthesizer sharedSynthesizer;
    private static MidiDevice sharedOutputDevice;
    private static Receiver sharedReceiver;
    private static int sharedSynthUsers;

    private Sequencer sequencer;
    private SmafRenderedAudio renderedAudio;
    private YamahaAudioEngine renderedAudioEngine;
    private SmafAudioPlayer audioPlayer;
    private boolean paused;
    private boolean closed;
    private Future<Void> openTask;
    private Exception renderedAudioFailure;
    private String renderedAudioFailureEngineId;
    private int volume = 127;
    private int panpot = 64;
    private PhraseTrackListener listener;
    private Sequence playbackSequence;
    private Map<Integer, Integer> channelRouting = Collections.emptyMap();
    private Set<Integer> outputChannels = Collections.emptySet();

    private SmafPlayback(SmafCacheKey cacheKey, byte[] source) {
        this.cacheKey = cacheKey;
        this.source = source;
    }

    public static SmafPlayback create(byte[] source) throws Exception {
        byte[] sourceCopy = source.clone();
        SmafCacheKey cacheKey = new SmafCacheKey(sourceCopy);
        return new SmafPlayback(cacheKey, sourceCopy);
    }

    public static void prewarm(byte[] source) {
        if (source == null || source.length == 0) {
            return;
        }
        byte[] sourceCopy = source.clone();
        SmafCacheKey cacheKey = new SmafCacheKey(sourceCopy);
        scheduleDecode(cacheKey, sourceCopy, true);
    }

    public void prefetch() throws Exception {
        ensureOpen();
    }

    public void prepareAsync() {
        scheduleOpen(true);
    }

    public int getState() {
        if (audioPlayer != null) {
            return audioPlayer.getState();
        }
        if (paused) {
            return PAUSED;
        }
        if (sequencer == null) {
            return READY;
        }
        return sequencer.isRunning() ? PLAYING : READY;
    }

    public void setListener(PhraseTrackListener listener) {
        this.listener = listener;
        if (audioPlayer != null) {
            audioPlayer.setListener(listener);
        }
    }

    public void setVolume(int value) {
        volume = clamp(value, 0, 127);
        if (audioPlayer != null) {
            audioPlayer.setVolume(volume);
        } else {
            applyMixerState();
        }
    }

    public void setPanpot(int value) {
        panpot = clamp(value, 0, 127);
        if (audioPlayer != null) {
            audioPlayer.setPanpot(panpot);
        } else {
            applyMixerState();
        }
    }

    public void play(int loopCount) {
        play(loopCount, false);
    }

    public void play(int loopCount, boolean completeAtSequenceEnd) {
        try {
            ensureOpen();
            if (audioPlayer != null) {
                Mobile.log(Mobile.LOG_INFO,
                        "Playing SMAF phrase through " + renderedBackendLabel()
                                + " audio backend (loop=" + loopCount + ").");
                audioPlayer.play(loopCount, completeAtSequenceEnd);
                return;
            }
            sequencer.stop();
            sequencer.setMicrosecondPosition(0L);
            sequencer.setLoopStartPoint(0L);
            sequencer.setLoopEndPoint(-1L);
            sequencer.setLoopCount(loopCount == 0 ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, loopCount - 1));
            applyMixerState();
            sequencer.start();
            paused = false;
            Mobile.log(Mobile.LOG_INFO, "Playing SMAF phrase through MIDI backend (loop=" + loopCount + ").");
        } catch (Exception exception) {
            throw new RuntimeException("Failed to play SMAF phrase", exception);
        }
    }

    public void stop() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            return;
        }
        if (sequencer == null) {
            return;
        }
        sequencer.stop();
        silenceOutputChannels();
        sequencer.setMicrosecondPosition(0L);
        paused = false;
    }

    public void pause() {
        if (audioPlayer != null) {
            audioPlayer.pause();
            return;
        }
        if (sequencer == null) {
            return;
        }
        sequencer.stop();
        silenceOutputChannels();
        paused = true;
    }

    public void resume() {
        if (audioPlayer != null) {
            audioPlayer.resume();
            return;
        }
        if (sequencer == null) {
            return;
        }
        try {
            applyMixerState();
            sequencer.start();
            paused = false;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to resume SMAF phrase", exception);
        }
    }

    public boolean hasPcmPayload() {
        ensureDecodedUnchecked("inspect SMAF PCM payload");
        return hasPcmPayload;
    }

    public int pcmClipCount() {
        ensureDecodedUnchecked("inspect SMAF PCM clips");
        return pcmClipCount;
    }

    public byte[] source() {
        return source.clone();
    }

    public List<SMAFDecoder.SequenceUserEvent> userEvents() {
        ensureDecodedUnchecked("inspect SMAF user events");
        return userEvents;
    }

    public SmafRenderedAudio renderedAudio() throws Exception {
        String synthPreference = normalizeSmafSynthPreference(System.getProperty("remexa.smafSynth", "auto"));
        if (SMAF_SYNTH_MIDI.equals(synthPreference)) {
            return null;
        }
        YamahaAudioEngine engine = resolveAudioEngine(synthPreference);
        if (renderedAudio != null && renderedAudioEngine != null && renderedAudioEngine.id().equals(engine.id())) {
            return renderedAudio;
        }
        SmafRenderCacheKey renderCacheKey = new SmafRenderCacheKey(cacheKey, engine.id());
        SmafRenderedAudio cachedRenderedAudio = cachedRenderedAudio(renderCacheKey);
        if (cachedRenderedAudio != null) {
            renderedAudio = cachedRenderedAudio;
            renderedAudioEngine = engine;
            return renderedAudio;
        }
        if (renderedAudioFailure != null && engine.id().equals(renderedAudioFailureEngineId)) {
            throw renderedAudioFailure;
        }
        awaitRenderedWarmup(renderCacheKey);
        SmafRenderedAudio warmed = cachedRenderedAudio(renderCacheKey);
        if (warmed != null) {
            renderedAudio = warmed;
            renderedAudioEngine = engine;
            return renderedAudio;
        }
        try {
            renderedAudio = engine.render(createRenderContext());
            renderedAudioEngine = engine;
            cacheRenderedAudio(renderCacheKey, renderedAudio);
            return renderedAudio;
        } catch (Exception exception) {
            renderedAudioFailure = exception;
            renderedAudioFailureEngineId = engine.id();
            Mobile.log(Mobile.LOG_WARNING,
                    engine.label() + " renderedAudio() failed: " + describeException(exception));
            throw exception;
        }
    }

    public SmafRenderedAudio cachedRenderedAudioIfReady() {
        if (!decodedLoaded) {
            return null;
        }
        String synthPreference = normalizeSmafSynthPreference(System.getProperty("remexa.smafSynth", "auto"));
        if (SMAF_SYNTH_MIDI.equals(synthPreference)) {
            return null;
        }
        YamahaAudioEngine engine = resolveAudioEngine(synthPreference);
        if (renderedAudio != null
                && renderedAudioEngine != null
                && renderedAudioEngine.id().equals(engine.id())) {
            return renderedAudio;
        }
        SmafRenderCacheKey renderCacheKey = new SmafRenderCacheKey(cacheKey, engine.id());
        SmafRenderedAudio cached = cachedRenderedAudio(renderCacheKey);
        if (cached != null) {
            renderedAudio = cached;
            renderedAudioEngine = engine;
        }
        return cached;
    }

    @Override
    public void close() {
        synchronized (openLock) {
            closed = true;
            if (audioPlayer != null) {
                audioPlayer.close();
                audioPlayer = null;
            }
            if (sequencer != null) {
                sequencer.stop();
                silenceOutputChannels();
                sequencer.close();
                sequencer = null;
            }
            releaseSharedMidi();
            releaseChannelRouting();
            playbackSequence = null;
            paused = false;
            openTask = null;
        }
    }

    private void ensureOpen() throws Exception {
        synchronized (openLock) {
            if (closed) {
                throw new IllegalStateException("SMAF playback has been closed");
            }
        }
        Future<Void> task = scheduleOpen(true);
        if (task instanceof FutureTask<Void> futureTask) {
            futureTask.run();
        }
        awaitOpen(task);
    }

    private Future<Void> scheduleOpen(boolean async) {
        FutureTask<Void> newTask = null;
        synchronized (openLock) {
            if (closed || audioPlayer != null || sequencer != null) {
                return completedFuture();
            }
            if (openTask != null) {
                return openTask;
            }
            newTask = new FutureTask<>(() -> {
                openPlayback();
                return null;
            });
            openTask = newTask;
        }
        FutureTask<Void> taskToRun = newTask;
        Runnable runner = () -> {
            try {
                taskToRun.run();
            } finally {
                synchronized (openLock) {
                    if (openTask == taskToRun) {
                        openTask = null;
                    }
                }
            }
        };
        if (async) {
            WARMUP_EXECUTOR.submit(runner);
        } else {
            runner.run();
        }
        return taskToRun;
    }

    private void openPlayback() throws Exception {
        synchronized (openLock) {
            if (closed || audioPlayer != null || sequencer != null) {
                return;
            }
            String synthPreference = normalizeSmafSynthPreference(System.getProperty("remexa.smafSynth", "auto"));
            if (!SMAF_SYNTH_MIDI.equals(synthPreference)) {
                try {
                    openRenderedBackend();
                    return;
                } catch (Exception exception) {
                    Mobile.log(Mobile.LOG_WARNING,
                            "Unable to open SMAF through Yamaha audio backend: " + describeException(exception)
                                    + ". Falling back to host MIDI output.");
                }
            }
            openMidiBackend();
        }
    }

    private void openRenderedBackend() throws Exception {
        ensureDecoded();
        String synthPreference = normalizeSmafSynthPreference(System.getProperty("remexa.smafSynth", "auto"));
        YamahaAudioEngine engine = resolveAudioEngine(synthPreference);
        renderedAudioEngine = engine;
        SmafStreamingSession session = engine.openStream(createRenderContext());
        int sampleRate = session.sampleRate();
        audioPlayer = new SmafStreamingPlayer(session, userEvents);
        audioPlayer.setListener(listener);
        audioPlayer.setVolume(volume);
        audioPlayer.setPanpot(panpot);
        Mobile.log(Mobile.LOG_INFO,
                "SMAF streamed with " + renderedBackendLabel() + " backend at " + sampleRate
                        + (hasPcmPayload ? " with " + pcmClipCount + " PCM clip(s)." : "."));
    }

    private void openMidiBackend() throws Exception {
        ensureDecoded();
        acquireSharedMidi();
        if (playbackSequence == null) {
            channelRouting = allocateChannelRouting(midiSequence);
            playbackSequence = remapSequenceChannels(midiSequence, channelRouting);
            outputChannels = collectOutputChannels(playbackSequence);
        }
        sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(sharedReceiver);
        sequencer.setSequence(playbackSequence);
        sequencer.addMetaEventListener(message -> {
            if (message.getType() == 0x2F && listener != null) {
                dispatchCompletion(listener);
                return;
            }
            if (listener != null) {
                int userEventId = decodeUserEventMeta(message);
                if (userEventId >= 0) {
                    dispatchUserEvent(listener, userEventId);
                }
            }
        });
        if (hasPcmPayload) {
            Mobile.log(Mobile.LOG_WARNING,
                    "Decoded SMAF contains " + pcmClipCount + " PCM clip(s); host MIDI fallback will not render them.");
        }
        applyMixerState();
    }

    private void applyMixerState() {
        Receiver receiver = sharedReceiver;
        if (receiver == null || outputChannels.isEmpty()) {
            return;
        }
        for (int outputChannel : outputChannels) {
            sendShortMessage(receiver, ShortMessage.CONTROL_CHANGE, outputChannel, 7, volume,
                    "apply SMAF volume");
            sendShortMessage(receiver, ShortMessage.CONTROL_CHANGE, outputChannel, 10, panpot,
                    "apply SMAF pan");
        }
    }

    private static void acquireSharedMidi() throws Exception {
        synchronized (MIDI_LOCK) {
            if (sharedReceiver == null) {
                SharedMidiOutput output = openPreferredOutput();
                sharedSynthesizer = output.synthesizer();
                sharedOutputDevice = output.outputDevice();
                sharedReceiver = output.receiver();
                Arrays.fill(RESERVED_OUTPUT_CHANNELS, false);
                RESERVED_OUTPUT_CHANNELS[MIDI_PERCUSSION_CHANNEL] = true;
                Mobile.log(Mobile.LOG_INFO, "SMAF MIDI output: " + output.description());
            }
            sharedSynthUsers++;
        }
    }

    private void releaseSharedMidi() {
        synchronized (MIDI_LOCK) {
            if (sharedSynthUsers > 0) {
                sharedSynthUsers--;
            }
            if (sharedSynthUsers == 0) {
                if (sharedReceiver != null) {
                    sharedReceiver.close();
                    sharedReceiver = null;
                }
                if (sharedOutputDevice != null) {
                    sharedOutputDevice.close();
                    sharedOutputDevice = null;
                }
                if (sharedSynthesizer != null) {
                    sharedSynthesizer.close();
                    sharedSynthesizer = null;
                }
                Arrays.fill(RESERVED_OUTPUT_CHANNELS, false);
            }
        }
    }

    private void silenceOutputChannels() {
        Receiver receiver = sharedReceiver;
        if (receiver == null || outputChannels.isEmpty()) {
            return;
        }
        for (int outputChannel : outputChannels) {
            sendShortMessage(receiver, ShortMessage.CONTROL_CHANGE, outputChannel, 64, 0,
                    "release SMAF sustain");
            sendShortMessage(receiver, ShortMessage.CONTROL_CHANGE, outputChannel, 123, 0,
                    "release SMAF notes");
            sendShortMessage(receiver, ShortMessage.CONTROL_CHANGE, outputChannel, 120, 0,
                    "silence SMAF output");
        }
    }

    private static SharedMidiOutput openPreferredOutput() throws MidiUnavailableException {
        String preference = normalizeMidiDevicePreference(System.getProperty("remexa.midiDevice", "auto"));
        if (!MIDI_DEVICE_AUTO.equals(preference) && !usesInternalSynth(preference)) {
            SharedMidiOutput explicit = tryOpenOutputDevice(preference);
            if (explicit != null) {
                return explicit;
            }
            Mobile.log(Mobile.LOG_WARNING,
                    "Requested MIDI device '" + preference + "' was not available; falling back to automatic selection.");
        }

        if (MIDI_DEVICE_AUTO.equals(preference)) {
            for (String candidate : autoOutputCandidates()) {
                SharedMidiOutput output = tryOpenOutputDevice(candidate);
                if (output != null) {
                    return output;
                }
            }
        }

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        return new SharedMidiOutput(
                synthesizer,
                null,
                synthesizer.getReceiver(),
                synthesizer.getDeviceInfo().getName());
    }

    private static SharedMidiOutput tryOpenOutputDevice(String nameFragment) {
        MidiDevice device = findReceiverDevice(nameFragment);
        if (device == null) {
            return null;
        }
        try {
            device.open();
            return new SharedMidiOutput(null, device, device.getReceiver(), device.getDeviceInfo().getName());
        } catch (MidiUnavailableException exception) {
            if (device.isOpen()) {
                device.close();
            }
            Mobile.log(Mobile.LOG_WARNING,
                    "Unable to open MIDI output '" + device.getDeviceInfo().getName() + "': " + exception.getMessage());
            return null;
        }
    }

    private static MidiDevice findReceiverDevice(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            return null;
        }
        String normalizedNeedle = normalizeMidiDevicePreference(nameFragment);
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device instanceof Sequencer || device.getMaxReceivers() == 0) {
                    continue;
                }
                String name = info.getName();
                if (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(normalizedNeedle)) {
                    return device;
                }
            } catch (MidiUnavailableException ignored) {
                // Ignore devices that cannot be queried while scanning.
            }
        }
        return null;
    }

    private static List<String> autoOutputCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("VirtualMIDISynth");
        if (isWindows()) {
            candidates.add("Microsoft MIDI Mapper");
            candidates.add("Microsoft GS Wavetable Synth");
        }
        return candidates;
    }

    private static boolean usesInternalSynth(String preference) {
        return MIDI_DEVICE_GERVILL.equals(preference)
                || MIDI_DEVICE_DEFAULT.equals(preference)
                || "internal".equals(preference)
                || "synth".equals(preference);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }

    private static String normalizeMidiDevicePreference(String candidate) {
        return candidate == null ? MIDI_DEVICE_AUTO : candidate.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeSmafSynthPreference(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return SMAF_SYNTH_AUTO;
        }
        String normalized = candidate.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case SMAF_SYNTH_AUTO, SMAF_SYNTH_MA3, SMAF_SYNTH_MA5, SMAF_SYNTH_MA7, SMAF_SYNTH_MIDI -> normalized;
            default -> SMAF_SYNTH_AUTO;
        };
    }

    private YamahaAudioEngine resolveAudioEngine(String synthPreference) {
        ensureDecodedUnchecked("resolve SMAF audio engine");
        return resolveAudioEngine(synthPreference, source, startupPackets, exclusiveVoices, sequenceSysExEvents);
    }

    private static YamahaAudioEngine resolveAudioEngine(String synthPreference,
                                                         byte[] source,
                                                         List<byte[]> startupPackets,
                                                         List<byte[]> exclusiveVoices,
                                                         List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        return switch (synthPreference) {
            case SMAF_SYNTH_MA3 -> MA3_ENGINE;
            case SMAF_SYNTH_MA5 -> MA5_ENGINE;
            case SMAF_SYNTH_MA7 -> MA7_ENGINE;
            case SMAF_SYNTH_AUTO -> resolveAutomaticAudioEngine(source, startupPackets, exclusiveVoices, sequenceSysExEvents);
            default -> MA3_ENGINE;
        };
    }

    private static YamahaAudioEngine resolveAutomaticAudioEngine(byte[] source,
                                                                  List<byte[]> startupPackets,
                                                                  List<byte[]> exclusiveVoices,
                                                                  List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        SmafAudioFamily family = SmafAudioDetector.detect(source, startupPackets, exclusiveVoices, sequenceSysExEvents);
        YamahaAudioEngine engine = switch (family) {
            case MA3 -> MA3_ENGINE;
            case MA5 -> MA5_ENGINE;
            case MA7 -> MA7_ENGINE;
            case UNKNOWN -> MA3_ENGINE;
        };
        if (SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
            SmafDebug.info("smaf", "SMAF auto audio family=" + family + " backend=" + engine.label());
        }
        return engine;
    }

    private String renderedBackendLabel() {
        return renderedAudioEngine == null ? "Yamaha" : renderedAudioEngine.label();
    }

    private static void sendShortMessage(Receiver receiver,
                                         int command,
                                         int channel,
                                         int data1,
                                         int data2,
                                         String action) {
        if (receiver == null) {
            return;
        }
        try {
            ShortMessage message = new ShortMessage();
            if (command == ShortMessage.PROGRAM_CHANGE || command == ShortMessage.CHANNEL_PRESSURE) {
                message.setMessage(command, channel, data1, 0);
            } else {
                message.setMessage(command, channel, data1, data2);
            }
            receiver.send(message, -1L);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to " + action + " on MIDI channel " + channel, exception);
        }
    }

    private Map<Integer, Integer> allocateChannelRouting(Sequence sourceSequence) {
        Set<Integer> melodicChannels = collectUsedMelodicChannels(sourceSequence);
        if (melodicChannels.isEmpty()) {
            return Collections.emptyMap();
        }

        synchronized (MIDI_LOCK) {
            Map<Integer, Integer> routing = new LinkedHashMap<>();
            List<Integer> reserved = new ArrayList<>();
            for (int sourceChannel : melodicChannels) {
                int outputChannel = reserveOutputChannel();
                if (outputChannel < 0) {
                    for (int channel : reserved) {
                        RESERVED_OUTPUT_CHANNELS[channel] = false;
                    }
                    return Collections.emptyMap();
                }
                routing.put(sourceChannel, outputChannel);
                reserved.add(outputChannel);
            }
            return routing;
        }
    }

    private void releaseChannelRouting() {
        if (channelRouting.isEmpty()) {
            return;
        }
        synchronized (MIDI_LOCK) {
            for (int outputChannel : channelRouting.values()) {
                if (outputChannel >= 0 && outputChannel < RESERVED_OUTPUT_CHANNELS.length) {
                    RESERVED_OUTPUT_CHANNELS[outputChannel] = false;
                }
            }
        }
        channelRouting = Collections.emptyMap();
        outputChannels = Collections.emptySet();
    }

    private static int reserveOutputChannel() {
        for (int candidate : MELODIC_CHANNEL_POOL) {
            if (!RESERVED_OUTPUT_CHANNELS[candidate]) {
                RESERVED_OUTPUT_CHANNELS[candidate] = true;
                return candidate;
            }
        }
        return -1;
    }

    private static Set<Integer> collectUsedMelodicChannels(Sequence sourceSequence) {
        Set<Integer> channels = new LinkedHashSet<>();
        for (Track track : sourceSequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage message = track.get(i).getMessage();
                if (message instanceof ShortMessage shortMessage) {
                    int command = shortMessage.getCommand();
                    if (isChannelVoiceMessage(command) && shortMessage.getChannel() != MIDI_PERCUSSION_CHANNEL) {
                        channels.add(shortMessage.getChannel());
                    }
                }
            }
        }
        return channels;
    }

    private static Set<Integer> collectOutputChannels(Sequence sourceSequence) {
        Set<Integer> channels = new LinkedHashSet<>();
        for (Track track : sourceSequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage message = track.get(i).getMessage();
                if (message instanceof ShortMessage shortMessage && isChannelVoiceMessage(shortMessage.getCommand())) {
                    channels.add(shortMessage.getChannel());
                }
            }
        }
        return channels;
    }

    private static boolean isChannelVoiceMessage(int command) {
        return command == ShortMessage.NOTE_OFF
                || command == ShortMessage.NOTE_ON
                || command == ShortMessage.POLY_PRESSURE
                || command == ShortMessage.CONTROL_CHANGE
                || command == ShortMessage.PROGRAM_CHANGE
                || command == ShortMessage.CHANNEL_PRESSURE
                || command == ShortMessage.PITCH_BEND;
    }

    private static Sequence remapSequenceChannels(Sequence sourceSequence, Map<Integer, Integer> routing) throws Exception {
        if (routing.isEmpty()) {
            return sourceSequence;
        }
        Sequence remapped = new Sequence(sourceSequence.getDivisionType(), sourceSequence.getResolution());
        for (Track sourceTrack : sourceSequence.getTracks()) {
            Track targetTrack = remapped.createTrack();
            for (int i = 0; i < sourceTrack.size(); i++) {
                MidiEvent event = sourceTrack.get(i);
                MidiMessage remappedMessage = remapMessage(event.getMessage(), routing);
                targetTrack.add(new MidiEvent(remappedMessage, event.getTick()));
            }
        }
        return remapped;
    }

    private static MidiMessage remapMessage(MidiMessage message, Map<Integer, Integer> routing) throws Exception {
        if (message instanceof ShortMessage shortMessage) {
            int command = shortMessage.getCommand();
            int outputChannel = routing.getOrDefault(shortMessage.getChannel(), shortMessage.getChannel());
            ShortMessage copy = new ShortMessage();
            if (command == ShortMessage.PROGRAM_CHANGE || command == ShortMessage.CHANNEL_PRESSURE) {
                copy.setMessage(command, outputChannel, shortMessage.getData1(), 0);
            } else if (isChannelVoiceMessage(command)) {
                copy.setMessage(command, outputChannel, shortMessage.getData1(), shortMessage.getData2());
            } else {
                copy.setMessage(shortMessage.getStatus(), shortMessage.getData1(), shortMessage.getData2());
            }
            return copy;
        }
        if (message instanceof javax.sound.midi.MetaMessage metaMessage) {
            javax.sound.midi.MetaMessage copy = new javax.sound.midi.MetaMessage();
            copy.setMessage(metaMessage.getType(), metaMessage.getData(), metaMessage.getData().length);
            return copy;
        }
        if (message instanceof SysexMessage sysexMessage) {
            SysexMessage copy = new SysexMessage();
            byte[] data = sysexMessage.getData();
            byte[] payload = new byte[data.length + 1];
            payload[0] = (byte) sysexMessage.getStatus();
            System.arraycopy(data, 0, payload, 1, data.length);
            copy.setMessage(payload, payload.length);
            return copy;
        }
        return cloneRawMessage(message);
    }

    private static MidiMessage cloneRawMessage(MidiMessage message) {
        byte[] data = message.getMessage().clone();
        return new MidiMessage(data) {
            @Override
            public Object clone() {
                return cloneRawMessage(this);
            }
        };
    }

    private static DecodedSmaf decode(byte[] source) throws Exception {
        if (SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
            SmafDebug.info("smaf",
                    "Decoding phrase bytes=" + source.length
                            + " exvo=" + containsAsciiTag(source, "EXVO")
                            + " voic=" + containsAsciiTag(source, "VOIC")
                            + " atr=" + containsAsciiTag(source, "ATR"));
        }
        synchronized (SMAFDecoder.class) {
            SMAFDecoder.decodeSMAF(source);
            if (SMAFDecoder.SequenceData == null) {
                throw new IOException("SMAF decoder did not produce sequenced data");
            }
            byte[] midiBytes = readAll(SMAFDecoder.SequenceData);
            Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(midiBytes));
            Sequence renderSequence = cloneSequence(sequence);
            Sequence midiSequence = cloneSequence(sequence);
            List<SMAFDecoder.SequenceUserEvent> userEvents = copySequenceUserEvents(SMAFDecoder.sequenceUserEvents);
            injectUserEvents(renderSequence, userEvents);
            injectUserEvents(midiSequence, userEvents);
            applySoftbankExvoFallback(source,
                    midiSequence,
                    SMAFDecoder.decodedChannelStates(),
                    SMAFDecoder.exclusiveVoices,
                    SMAFDecoder.exclusiveVoices.size(),
                    SMAFDecoder.sequenceSysExEvents.size());
            List<byte[]> startupPackets = copyPacketList(SMAFDecoder.startupPackets);
            List<byte[]> exclusiveVoices = copyExclusiveVoices(SMAFDecoder.exclusiveVoices);
            List<byte[]> pcmClipData = copyPcmClips(SMAFDecoder.pcmData);
            boolean hasPcmPayload = pcmClipData.stream().anyMatch(bytes -> bytes != null && bytes.length > 0);
            int pcmClipCount = (int) pcmClipData.stream().filter(bytes -> bytes != null && bytes.length > 0).count();
            if (SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
                SmafDebug.info("smaf",
                        "Decoded sequence tracks=" + sequence.getTracks().length
                                + " pcmClips=" + pcmClipCount
                                + " pcmTriggers=" + SMAFDecoder.pcmSequenceTriggers.size()
                                + " exclusiveVoices=" + exclusiveVoices.size());
            }
            return new DecodedSmaf(
                    renderSequence,
                    midiSequence,
                    copySequenceSysExEvents(SMAFDecoder.sequenceSysExEvents),
                    startupPackets,
                    exclusiveVoices,
                    pcmClipData,
                    copyPcmSequenceTriggers(SMAFDecoder.pcmSequenceTriggers),
                    userEvents,
                    hasPcmPayload,
                    pcmClipCount);
        }
    }

    private static DecodedSmaf decodeCached(SmafCacheKey cacheKey, byte[] source) throws Exception {
        synchronized (DECODE_CACHE) {
            DecodedSmaf cached = DECODE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        return awaitDecode(scheduleDecode(cacheKey, source, false));
    }

    private static Future<DecodedSmaf> scheduleDecode(SmafCacheKey cacheKey, byte[] source, boolean async) {
        synchronized (DECODE_CACHE) {
            DecodedSmaf cached = DECODE_CACHE.get(cacheKey);
            if (cached != null) {
                return new FutureTask<>(() -> cached) {{
                    run();
                }};
            }
        }
        FutureTask<DecodedSmaf> newTask = null;
        Future<DecodedSmaf> task;
        synchronized (DECODE_TASKS) {
            task = DECODE_TASKS.get(cacheKey);
            if (task == null) {
                newTask = new FutureTask<>(() -> decodeAndCache(cacheKey, source));
                DECODE_TASKS.put(cacheKey, newTask);
                task = newTask;
            }
        }
        if (newTask != null) {
            FutureTask<DecodedSmaf> taskToRun = newTask;
            Runnable runner = () -> {
                try {
                    taskToRun.run();
                } finally {
                    synchronized (DECODE_TASKS) {
                        if (DECODE_TASKS.get(cacheKey) == taskToRun) {
                            DECODE_TASKS.remove(cacheKey);
                        }
                    }
                }
            };
            if (async) {
                WARMUP_EXECUTOR.submit(runner);
            } else {
                runner.run();
            }
        }
        return task;
    }

    private static DecodedSmaf decodeAndCache(SmafCacheKey cacheKey, byte[] source) throws Exception {
        synchronized (DECODE_CACHE) {
            DecodedSmaf cached = DECODE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        DecodedSmaf decoded = decode(source);
        synchronized (DECODE_CACHE) {
            DecodedSmaf cached = DECODE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            DECODE_CACHE.put(cacheKey, decoded);
            return decoded;
        }
    }

    private static DecodedSmaf awaitDecode(Future<DecodedSmaf> task) throws Exception {
        try {
            return task.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while preparing SMAF decode", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Failed to prepare SMAF decode", cause);
        }
    }

    private static void awaitOpen(Future<Void> task) throws Exception {
        try {
            task.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while opening SMAF playback", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Failed to open SMAF playback", cause);
        }
    }

    private static SmafRenderedAudio cachedRenderedAudio(SmafRenderCacheKey cacheKey) {
        synchronized (RENDER_CACHE) {
            return RENDER_CACHE.get(cacheKey);
        }
    }

    private static void cacheRenderedAudio(SmafRenderCacheKey cacheKey, SmafRenderedAudio audio) {
        synchronized (RENDER_CACHE) {
            RENDER_CACHE.put(cacheKey, audio);
        }
    }

    private void awaitRenderedWarmup(SmafRenderCacheKey renderCacheKey) {
        Future<?> task;
        synchronized (RENDER_WARMUP_TASKS) {
            task = RENDER_WARMUP_TASKS.get(renderCacheKey);
        }
        if (task == null) {
            return;
        }
        try {
            task.get();
        } catch (Exception exception) {
            DebugLog.log(
                    LogCategory.AUDIO,
                    SmafPlayback.class.getName(),
                    "Rendered SMAF warmup failed; falling back to synchronous render: "
                            + describeException(exception)
            );
            // Fall back to the synchronous render path below.
        }
    }

    private SmafRenderContext createRenderContext() {
        ensureDecodedUnchecked("create SMAF render context");
        return new SmafRenderContext(
                source,
                sequence,
                sequenceSysExEvents,
                startupPackets,
                exclusiveVoices,
                pcmClipData,
                pcmTriggers);
    }

    private void ensureDecoded() throws Exception {
        if (decodedLoaded) {
            return;
        }
        synchronized (decodeLock) {
            if (decodedLoaded) {
                return;
            }
            applyDecoded(decodeCached(cacheKey, source));
        }
    }

    private void applyDecoded(DecodedSmaf decoded) {
        sequence = decoded.sequence();
        midiSequence = decoded.midiSequence();
        sequenceSysExEvents = decoded.sequenceSysExEvents();
        startupPackets = decoded.startupPackets();
        exclusiveVoices = decoded.exclusiveVoices();
        pcmClipData = decoded.pcmClipData();
        pcmTriggers = decoded.pcmTriggers();
        userEvents = decoded.userEvents();
        hasPcmPayload = decoded.hasPcmPayload();
        pcmClipCount = decoded.pcmClipCount();
        decodedLoaded = true;
    }

    private void ensureDecodedUnchecked(String action) {
        try {
            ensureDecoded();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to " + action, exception);
        }
    }


    private static SmafRenderContext createRenderContext(byte[] source, DecodedSmaf decoded) {
        return new SmafRenderContext(
                source,
                decoded.sequence(),
                decoded.sequenceSysExEvents(),
                decoded.startupPackets(),
                decoded.exclusiveVoices(),
                decoded.pcmClipData(),
                decoded.pcmTriggers());
    }

    private static <K, V> Map<K, V> createLruCache(int limit) {
        return new LinkedHashMap<>(limit, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    private static Future<Void> completedFuture() {
        FutureTask<Void> completed = new FutureTask<>(() -> null);
        completed.run();
        return completed;
    }

    private static Sequence cloneSequence(Sequence sourceSequence) throws Exception {
        Sequence clone = new Sequence(sourceSequence.getDivisionType(), sourceSequence.getResolution());
        for (Track sourceTrack : sourceSequence.getTracks()) {
            Track targetTrack = clone.createTrack();
            for (int i = 0; i < sourceTrack.size(); i++) {
                MidiEvent event = sourceTrack.get(i);
                targetTrack.add(new MidiEvent(remapMessage(event.getMessage(), Collections.emptyMap()), event.getTick()));
            }
        }
        return clone;
    }

    private static List<SMAFDecoder.SequenceSysExEvent> copySequenceSysExEvents(List<SMAFDecoder.SequenceSysExEvent> sourceEvents) {
        List<SMAFDecoder.SequenceSysExEvent> copy = new ArrayList<>(sourceEvents.size());
        for (SMAFDecoder.SequenceSysExEvent event : sourceEvents) {
            copy.add(new SMAFDecoder.SequenceSysExEvent(event.tick(), event.sourceBank(), event.data().clone()));
        }
        return List.copyOf(copy);
    }

    private static List<SMAFDecoder.SequenceUserEvent> copySequenceUserEvents(List<SMAFDecoder.SequenceUserEvent> sourceEvents) {
        return List.copyOf(sourceEvents);
    }

    private static void injectUserEvents(Sequence sequence, List<SMAFDecoder.SequenceUserEvent> userEvents) throws Exception {
        if (userEvents.isEmpty()) {
            return;
        }
        Track[] tracks = sequence.getTracks();
        Track targetTrack = tracks.length == 0 ? sequence.createTrack() : tracks[0];
        for (SMAFDecoder.SequenceUserEvent userEvent : userEvents) {
            MetaMessage metaMessage = new MetaMessage();
            byte[] payload = new byte[]{
                    USER_EVENT_META_PREFIX[0],
                    USER_EVENT_META_PREFIX[1],
                    (byte) (userEvent.eventId() & 0xFF)
            };
            metaMessage.setMessage(USER_EVENT_META_TYPE, payload, payload.length);
            targetTrack.add(new MidiEvent(metaMessage, userEvent.tick()));
        }
    }

    private static int decodeUserEventMeta(MetaMessage message) {
        if (message.getType() != USER_EVENT_META_TYPE) {
            return -1;
        }
        byte[] data = message.getData();
        if (data.length < 3
                || data[0] != USER_EVENT_META_PREFIX[0]
                || data[1] != USER_EVENT_META_PREFIX[1]) {
            return -1;
        }
        return data[2] & 0xFF;
    }

    private static List<SMAFDecoder.PcmSequenceTrigger> copyPcmSequenceTriggers(List<SMAFDecoder.PcmSequenceTrigger> sourceTriggers) {
        return List.copyOf(sourceTriggers);
    }

    private static List<byte[]> copyExclusiveVoices(List<byte[]> sourceVoices) {
        return copyPacketList(sourceVoices);
    }

    private static List<byte[]> copyPacketList(List<byte[]> sourcePackets) {
        if (sourcePackets == null || sourcePackets.isEmpty()) {
            return List.of();
        }
        List<byte[]> copy = new ArrayList<>(sourcePackets.size());
        for (byte[] packet : sourcePackets) {
            copy.add(packet == null ? null : packet.clone());
        }
        return List.copyOf(copy);
    }

    private static List<byte[]> copyPcmClips(List<InputStream> pcmSources) throws IOException {
        if (pcmSources == null || pcmSources.isEmpty()) {
            return List.of();
        }
        List<byte[]> clips = new ArrayList<>(pcmSources.size());
        for (InputStream pcmSource : pcmSources) {
            clips.add(pcmSource == null ? null : readAll(pcmSource));
        }
        return Collections.unmodifiableList(clips);
    }

    private static void applySoftbankExvoFallback(byte[] source,
                                                  Sequence sequence,
                                                  List<SMAFDecoder.DecodedChannelState> decodedChannels,
                                                  List<byte[]> exclusiveVoices,
                                                  int exclusiveVoiceCount,
                                                  int sequenceSysExCount) {
        if (!containsAsciiTag(source, "EXVO")
                || !containsAsciiTag(source, "VOIC")) {
            return;
        }

        boolean hasBankSelect = false;
        Set<Integer> programs = new TreeSet<>();
        Map<Integer, Integer> originalProgramsByChannel = new HashMap<>();
        Map<Integer, Integer> bankMsbByChannel = new HashMap<>();
        Map<Integer, Integer> bankLsbByChannel = new HashMap<>();
        Map<Integer, ChannelProfileBuilder> channelProfiles = new HashMap<>();
        Map<Integer, DecodedChannelProfile> decodedProfilesByMidiChannel = buildDecodedProfiles(decodedChannels);
        Map<Integer, ExclusiveVoiceProfile> exclusiveVoiceProfilesBySlot = buildExclusiveVoiceProfiles(exclusiveVoices);
        Map<Integer, Map<Integer, Long>> activeNotes = new HashMap<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent midiEvent = track.get(i);
                MidiMessage message = midiEvent.getMessage();
                if (!(message instanceof ShortMessage shortMessage)) {
                    continue;
                }

                int channel = shortMessage.getChannel();
                ChannelProfileBuilder profile = channelProfiles.computeIfAbsent(channel, ignored -> new ChannelProfileBuilder());

                if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE
                        && (shortMessage.getData1() == 0 || shortMessage.getData1() == 32)) {
                    hasBankSelect = true;
                    if (shortMessage.getData1() == 0) {
                        bankMsbByChannel.put(channel, shortMessage.getData2());
                    } else {
                        bankLsbByChannel.put(channel, shortMessage.getData2());
                    }
                } else if (shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                    int program = shortMessage.getData1();
                    programs.add(program);
                    originalProgramsByChannel.put(channel, program);
                } else if (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() > 0) {
                    profile.recordNoteOn(shortMessage.getData1());
                    activeNotes.computeIfAbsent(channel, ignored -> new HashMap<>()).put(shortMessage.getData1(), midiEvent.getTick());
                } else if (shortMessage.getCommand() == ShortMessage.NOTE_OFF
                        || (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() == 0)) {
                    Map<Integer, Long> notesByPitch = activeNotes.get(channel);
                    if (notesByPitch == null) {
                        continue;
                    }
                    Long startTick = notesByPitch.remove(shortMessage.getData1());
                    if (startTick != null) {
                        profile.recordDuration(shortMessage.getData1(), midiEvent.getTick() - startTick);
                    }
                }
            }
        }

        boolean unsupportedPrograms = programs.stream().anyMatch(program -> program < 0 || program > 3);
        if (programs.isEmpty() || unsupportedPrograms) {
            if (SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
                SmafDebug.info("smaf",
                        "Skipping host MIDI EXVO fallback: bankSelect=" + hasBankSelect
                                + ", programs=" + programs
                                + ", unsupportedPrograms=" + unsupportedPrograms);
            }
            return;
        }
        if (hasBankSelect && SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
            SmafDebug.info("smaf",
                    "Applying host MIDI EXVO fallback despite bank-select events because the phrase uses custom EXVO voices with program slots "
                            + programs + ".");
        }

        Map<Integer, ChannelProfile> finalizedProfiles = new HashMap<>();
        for (Map.Entry<Integer, ChannelProfileBuilder> entry : channelProfiles.entrySet()) {
            finalizedProfiles.put(entry.getKey(), entry.getValue().build());
        }

        Map<Integer, FallbackVoiceMapping> fallbackProgramsByChannel = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> percussionNoteMapsByChannel = new HashMap<>();
        StringBuilder fallbackSummary = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : originalProgramsByChannel.entrySet()) {
            int channel = entry.getKey();
            int originalProgram = entry.getValue();
            ChannelProfile profile = finalizedProfiles.get(channel);
            DecodedChannelProfile decodedProfile = decodedProfilesByMidiChannel.get(channel);
            ExclusiveVoiceProfile exclusiveVoiceProfile = exclusiveVoiceProfilesBySlot.get(originalProgram);
            int bankMsb = bankMsbByChannel.getOrDefault(channel, 0);
            int bankLsb = bankLsbByChannel.getOrDefault(channel, 0);
            VoiceRoutingContext context = buildVoiceRoutingContext(originalProgram,
                    profile,
                    decodedProfile,
                    exclusiveVoiceProfile,
                    bankMsb,
                    bankLsb);
            FallbackVoiceMapping mapping = chooseFallbackVoiceMapping(context, exclusiveVoiceCount, sequenceSysExCount);
            fallbackProgramsByChannel.put(channel, mapping);

            if (profile != null) {
                if (!fallbackSummary.isEmpty()) {
                    fallbackSummary.append("; ");
                }
                fallbackSummary.append("ch").append(channel)
                        .append(" ").append(originalProgram).append("->").append(mapping.program)
                        .append(mapping.transposeSemitones == 0 ? "" : " transpose=" + mapping.transposeSemitones)
                        .append(exclusiveVoiceProfile == null ? "" : " exvo=" + exclusiveVoiceProfile.signature())
                        .append(" bank=").append(String.format("0x%02X", bankMsb));
                if (bankLsb != 0) {
                    fallbackSummary.append("/").append(String.format("0x%02X", bankLsb));
                }
                fallbackSummary.append(" family=").append(describeReMEXABankFamily(bankMsb))
                        .append(" role=").append(context.role.name().toLowerCase())
                        .append(" notes=").append(profile.noteCount)
                        .append(" range=").append(profile.minNote).append("-").append(profile.maxNote)
                        .append(" unique=").append(profile.uniqueNotes)
                        .append(" avg=").append(String.format("%.1f", profile.averageNote))
                        .append(" avgDur=").append(String.format("%.1f", profile.averageDurationTicks));
                if (decodedProfile != null) {
                    fallbackSummary.append(" meta[type=").append(decodedProfile.channelType)
                            .append(",drum=").append(decodedProfile.usingDrumBank)
                            .append(",oct=").append(decodedProfile.octaveShift)
                            .append("]");
                }
            }

            if (profile != null && looksLikeSoftbankPercussionLane(originalProgram, profile)) {
                percussionNoteMapsByChannel.put(channel, buildPercussionNoteMap(profile));
            } else if (profile != null) {
                Map<Integer, Integer> mixedPercussionMap = buildMixedPercussionNoteMap(originalProgram, profile);
                if (!mixedPercussionMap.isEmpty()) {
                    percussionNoteMapsByChannel.put(channel, mixedPercussionMap);
                }
            }
        }

        int remappedCount = 0;
        int percussionRemappedCount = 0;
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (!(message instanceof ShortMessage shortMessage)) {
                    continue;
                }

                Map<Integer, Integer> percussionMap = percussionNoteMapsByChannel.get(shortMessage.getChannel());
                if (percussionMap != null) {
                    int command = shortMessage.getCommand();
                    if (command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF) {
                        Integer percussionNote = percussionMap.get(shortMessage.getData1());
                        if (percussionNote != null) {
                            try {
                                shortMessage.setMessage(command, MIDI_PERCUSSION_CHANNEL, percussionNote, shortMessage.getData2());
                                percussionRemappedCount++;
                            } catch (Exception exception) {
                                throw new RuntimeException("Failed to reroute SoftBank EXVO percussion notes", exception);
                            }
                            continue;
                        }
                    }
                }

                FallbackVoiceMapping mapping = fallbackProgramsByChannel.get(shortMessage.getChannel());
                if (mapping != null
                        && mapping.transposeSemitones != 0
                        && (shortMessage.getCommand() == ShortMessage.NOTE_ON || shortMessage.getCommand() == ShortMessage.NOTE_OFF)) {
                    int transposedNote = clamp(shortMessage.getData1() + mapping.transposeSemitones, 0, 127);
                    if (transposedNote != shortMessage.getData1()) {
                        try {
                            shortMessage.setMessage(shortMessage.getCommand(),
                                    shortMessage.getChannel(),
                                    transposedNote,
                                    shortMessage.getData2());
                        } catch (Exception exception) {
                            throw new RuntimeException("Failed to transpose SoftBank EXVO fallback notes", exception);
                        }
                    }
                }

                if (shortMessage.getCommand() != ShortMessage.PROGRAM_CHANGE) {
                    continue;
                }

                int originalProgram = shortMessage.getData1();
                int fallbackProgram = mapping == null ? originalProgram : mapping.program;
                if (fallbackProgram == originalProgram) {
                    continue;
                }

                try {
                    shortMessage.setMessage(ShortMessage.PROGRAM_CHANGE, shortMessage.getChannel(), fallbackProgram, 0);
                    remappedCount++;
                } catch (Exception exception) {
                    throw new RuntimeException("Failed to apply SoftBank EXVO fallback program mapping", exception);
                }
            }
        }

        if (remappedCount > 0) {
            Mobile.log(Mobile.LOG_INFO,
                    "Applied SoftBank EXVO host MIDI fallback GM mapping for programs " + programs
                            + " using " + remappedCount + " program-change event(s).");
        }

        if (!fallbackSummary.isEmpty()) {
            Mobile.log(Mobile.LOG_DEBUG, "SoftBank EXVO channel summary: " + fallbackSummary);
        }
        if (SmafDebug.isEnabled("smaf", SmafDebug.Level.INFO)) {
            SmafDebug.info("smaf",
                    "Host MIDI EXVO fallback summary: voices=" + exclusiveVoiceCount
                            + ", sysEx=" + sequenceSysExCount
                            + ", channels=[" + fallbackSummary + "]");
        }

        if (!percussionNoteMapsByChannel.isEmpty()) {
            Mobile.log(Mobile.LOG_INFO,
                    "Rerouted SoftBank EXVO percussion-like lanes " + percussionNoteMapsByChannel.keySet()
                            + " to MIDI percussion channel using " + percussionRemappedCount + " note event(s).");
        }

    }

    private static boolean looksLikeSoftbankPercussionLane(int originalProgram, ChannelProfile profile) {
        return originalProgram == 1
                && profile.noteCount >= 32
                && profile.minNote >= 60
                && profile.maxNote <= 84
                && profile.uniqueNotes <= 4
                && profile.averageDurationTicks <= 100.0;
    }

    private static Map<Integer, Integer> buildPercussionNoteMap(ChannelProfile profile) {
        int[] sortedNotes = profile.sortedNotes;
        Map<Integer, Integer> percussionMap = new HashMap<>();
        if (sortedNotes.length == 0) {
            return percussionMap;
        }
        if (sortedNotes.length == 1) {
            percussionMap.put(sortedNotes[0], 36);
            return percussionMap;
        }
        if (sortedNotes.length == 2) {
            percussionMap.put(sortedNotes[0], 36);
            percussionMap.put(sortedNotes[1], 38);
            return percussionMap;
        }

        int[] gmPercussion = {36, 38, 42, 46};
        for (int i = 0; i < sortedNotes.length && i < gmPercussion.length; i++) {
            percussionMap.put(sortedNotes[i], gmPercussion[i]);
        }
        return percussionMap;
    }

    private static Map<Integer, Integer> buildMixedPercussionNoteMap(int originalProgram, ChannelProfile profile) {
        Map<Integer, Integer> percussionMap = new HashMap<>();
        if (originalProgram != 1 || profile.noteCount < 60 || profile.uniqueNotes < 5 || profile.uniqueNotes > 10) {
            return percussionMap;
        }

        int dominantNote = -1;
        long dominantCount = 0;
        for (Map.Entry<Integer, NoteProfile> entry : profile.noteProfiles.entrySet()) {
            NoteProfile noteProfile = entry.getValue();
            if (noteProfile.count < 24 || noteProfile.averageDurationTicks > 320.0) {
                continue;
            }
            if (noteProfile.count * 100 < profile.noteCount * 35) {
                continue;
            }
            if (noteProfile.count > dominantCount) {
                dominantCount = noteProfile.count;
                dominantNote = entry.getKey();
            }
        }

        if (dominantNote >= 0) {
            percussionMap.put(dominantNote, 42);
        }
        return percussionMap;
    }

    private static Map<Integer, DecodedChannelProfile> buildDecodedProfiles(List<SMAFDecoder.DecodedChannelState> decodedChannels) {
        Map<Integer, DecodedChannelProfile> profiles = new HashMap<>();
        for (SMAFDecoder.DecodedChannelState state : decodedChannels) {
            profiles.merge(state.midiChannel(),
                    new DecodedChannelProfile(state.usingDrumBank(), state.channelType(), state.velocity(), state.octaveShift()),
                    DecodedChannelProfile::merge);
        }
        return profiles;
    }

    private static Map<Integer, ExclusiveVoiceProfile> buildExclusiveVoiceProfiles(List<byte[]> exclusiveVoices) {
        Map<Integer, ExclusiveVoiceProfile> profiles = new HashMap<>();
        for (byte[] voice : exclusiveVoices) {
            if (voice == null || voice.length < 8) {
                continue;
            }
            int slot = voice[7] & 0xFF;
            int selector0 = voice.length > 9 ? voice[9] & 0xFF : -1;
            int selector1 = voice.length > 10 ? voice[10] & 0xFF : -1;
            int selector2 = voice.length > 11 ? voice[11] & 0xFF : -1;
            boolean controlVoice = voice.length >= 16 && (voice[5] & 0xFF) == 0x02;
            profiles.put(slot, new ExclusiveVoiceProfile(
                    slot,
                    classifyExclusiveVoiceSignature(voice),
                    voice.length,
                    selector0,
                    selector1,
                    selector2,
                    controlVoice));
        }
        return profiles;
    }

    private static VoiceRoutingContext buildVoiceRoutingContext(int originalProgram,
                                                                ChannelProfile profile,
                                                                DecodedChannelProfile decodedProfile,
                                                                ExclusiveVoiceProfile exclusiveVoiceProfile,
                                                                int bankMsb,
                                                                int bankLsb) {
        boolean oddLowBank = isOddLowBank(bankMsb);
        boolean arpeggioLayer = looksLikeArpeggioLayer(profile);
        VoiceRole role = inferVoiceRole(originalProgram,
                profile,
                decodedProfile,
                exclusiveVoiceProfile,
                bankMsb,
                bankLsb,
                oddLowBank,
                arpeggioLayer);
        return new VoiceRoutingContext(originalProgram,
                bankMsb,
                bankLsb,
                profile,
                decodedProfile,
                exclusiveVoiceProfile,
                oddLowBank,
                arpeggioLayer,
                role);
    }

    private static VoiceRole inferVoiceRole(int originalProgram,
                                            ChannelProfile profile,
                                            DecodedChannelProfile decodedProfile,
                                            ExclusiveVoiceProfile exclusiveVoiceProfile,
                                            int bankMsb,
                                            int bankLsb,
                                            boolean oddLowBank,
                                            boolean arpeggioLayer) {
        if (profile == null) {
            return VoiceRole.UNKNOWN;
        }
        if (decodedProfile != null && (decodedProfile.usingDrumBank || decodedProfile.channelType == 3)) {
            return VoiceRole.PERCUSSION;
        }
        if (arpeggioLayer) {
            return VoiceRole.ARPEGGIO;
        }
        if (exclusiveVoiceProfile != null) {
            return switch (exclusiveVoiceProfile.signature()) {
                case "exvo-795243" -> VoiceRole.ARPEGGIO;
                case "exvo-794502" -> VoiceRole.CHOIR;
                case "exvo-798302" -> VoiceRole.BASS;
                case "exvo-798503" -> VoiceRole.CHOIR;
                case "exvo-794302", "exvo-798702" ->
                        prefersLowBankChorusTexture(profile, oddLowBank) ? VoiceRole.CHOIR : VoiceRole.STRINGS;
                case "exvo-ctrl-784050" ->
                        prefersLowBankChorusTexture(profile, oddLowBank) ? VoiceRole.CHOIR : VoiceRole.STRINGS;
                case "exvo-798502" -> {
                    if (prefersLowBankChorusTexture(profile, oddLowBank) && profile.averageNote >= 68.0) {
                        yield VoiceRole.HIGH_PAD;
                    }
                    yield profile.averageNote >= 70.0 ? VoiceRole.LEAD : VoiceRole.STRINGS;
                }
                default -> VoiceRole.UNKNOWN;
            };
        }
        if (decodedProfile != null && decodedProfile.octaveShift <= -1) {
            return profile.averageNote <= 52.0 ? VoiceRole.BASS : VoiceRole.LOW_STRINGS;
        }
        if (prefersChoralFallback(profile, bankMsb, bankLsb, originalProgram, decodedProfile)) {
            return VoiceRole.CHOIR;
        }
        if (prefersLowBankChorusTexture(profile, oddLowBank)) {
            return profile.averageNote >= 68.0 ? VoiceRole.HIGH_PAD : VoiceRole.CHOIR;
        }
        if (profile.maxNote <= 52 || profile.averageNote <= 47.5) {
            return VoiceRole.BASS;
        }
        if (profile.minNote >= 72 || profile.averageNote >= 69.0) {
            return profile.averageDurationTicks <= 220.0 ? VoiceRole.LEAD : VoiceRole.HIGH_PAD;
        }
        if (oddLowBank && profile.averageDurationTicks >= 220.0 && profile.averageNote >= 55.0) {
            return VoiceRole.STRINGS;
        }
        if (bankMsb == 0x36 && profile.averageDurationTicks >= 260.0) {
            return VoiceRole.BRASS;
        }
        if (profile.uniqueNotes <= 5 && profile.averageDurationTicks <= 140.0) {
            return VoiceRole.PLUCK;
        }
        if (profile.averageDurationTicks >= 280.0) {
            return VoiceRole.PAD;
        }
        return VoiceRole.STRINGS;
    }

    private static FallbackVoiceMapping chooseFallbackVoiceMapping(VoiceRoutingContext context,
                                                                   int exclusiveVoiceCount,
                                                                   int sequenceSysExCount) {
        ChannelProfile profile = context.profile;
        if (profile == null) {
            return new FallbackVoiceMapping(SOFTBANK_EXVO_DEFAULT_PROGRAMS[context.originalProgram], 0);
        }

        int fallbackProgram = switch (context.role) {
            case PERCUSSION -> SOFTBANK_EXVO_DEFAULT_PROGRAMS[context.originalProgram];
            case ARPEGGIO -> GM_HARP;
            case BASS -> profile.averageDurationTicks >= 200.0 ? GM_CELLO : GM_CONTRABASS;
            case LOW_STRINGS -> profile.averageNote <= 56.0 ? GM_CELLO : GM_STRING_ENSEMBLE_1;
            case CHOIR -> (context.oddLowBank || profile.averageNote >= 62.0) ? GM_VOICE_OOHS : GM_CHOIR_AAHS;
            case LEAD -> profile.averageDurationTicks <= 180.0 ? GM_FLUTE : GM_VIOLIN;
            case HIGH_PAD -> context.oddLowBank ? GM_SYNTH_VOICE : GM_VIOLIN;
            case PAD -> context.oddLowBank
                    ? GM_VOICE_OOHS
                    : (profile.maxNote <= 74 && profile.minNote >= 50 ? GM_FRENCH_HORN : GM_STRING_ENSEMBLE_2);
            case BRASS -> GM_FRENCH_HORN;
            case PLUCK -> profile.minNote >= 68 ? GM_HARP : (profile.maxNote <= 60 ? GM_ACOUSTIC_BASS : GM_PIZZICATO_STRINGS);
            case STRINGS -> prefersLowBankChorusTexture(profile, context.oddLowBank) ? GM_VOICE_OOHS : GM_STRING_ENSEMBLE_2;
            case UNKNOWN -> chooseLegacyFallbackProgram(context, exclusiveVoiceCount, sequenceSysExCount);
        };

        int transposeSemitones = chooseFallbackTransposeSemitones(fallbackProgram, context);
        return new FallbackVoiceMapping(fallbackProgram, transposeSemitones);
    }

    private static int chooseLegacyFallbackProgram(VoiceRoutingContext context,
                                                   int exclusiveVoiceCount,
                                                   int sequenceSysExCount) {
        ChannelProfile profile = context.profile;
        if (context.bankMsb == 0x36) {
            if (profile.averageDurationTicks >= 260.0 && profile.averageNote >= 54.0) {
                return GM_FRENCH_HORN;
            }
            if (profile.averageNote <= 56.0) {
                return GM_CELLO;
            }
        }

        if (context.bankMsb == 0x04 || context.bankMsb == 0x05) {
            if (profile.averageDurationTicks >= 220.0 && profile.averageNote >= 64.0) {
                return GM_VIOLIN;
            }
            if (profile.minNote >= 70) {
                return GM_FLUTE;
            }
        }

        if (prefersLowBankChorusTexture(profile, context.oddLowBank)) {
            return profile.averageNote >= 68.0 ? GM_SYNTH_VOICE : GM_VOICE_OOHS;
        }

        if (profile.averageNote <= 58.0) {
            return profile.averageDurationTicks >= 180.0 ? GM_CELLO : GM_BASSOON;
        }

        return switch (context.originalProgram) {
            case 0 -> GM_STRING_ENSEMBLE_1;
            case 1 -> GM_FRENCH_HORN;
            case 2 -> GM_VIOLIN;
            case 3 -> (sequenceSysExCount > 0 || exclusiveVoiceCount > 1) ? GM_CHOIR_AAHS : GM_CLARINET;
            default -> SOFTBANK_EXVO_DEFAULT_PROGRAMS[Math.max(0, Math.min(SOFTBANK_EXVO_DEFAULT_PROGRAMS.length - 1, context.originalProgram))];
        };
    }

    private static int chooseFallbackTransposeSemitones(int fallbackProgram, VoiceRoutingContext context) {
        ChannelProfile profile = context.profile;
        if (profile == null || context.role == VoiceRole.PERCUSSION || context.role == VoiceRole.ARPEGGIO) {
            return 0;
        }
        if (fallbackProgram == GM_VIOLIN || fallbackProgram == GM_FLUTE || fallbackProgram == GM_HARP) {
            return 0;
        }
        if (context.decodedProfile != null && context.decodedProfile.usingDrumBank) {
            return 0;
        }
        if (context.exclusiveVoiceProfile != null) {
            switch (context.exclusiveVoiceProfile.signature()) {
                case "exvo-798302" -> {
                    return profile.averageNote >= 56.0 ? -12 : 0;
                }
                case "exvo-794502", "exvo-798503", "exvo-794302", "exvo-798702" -> {
                    return profile.averageNote >= 60.0 ? -12 : 0;
                }
                default -> {
                    // fall through to generic rules
                }
            }
        }
        if ((context.role == VoiceRole.BASS || context.role == VoiceRole.LOW_STRINGS)
                && profile.averageNote >= 53.0) {
            return -12;
        }
        if (context.role == VoiceRole.CHOIR) {
            if (context.decodedProfile != null && context.decodedProfile.octaveShift >= 1) {
                return 0;
            }
            return profile.averageNote >= 58.0 ? -12 : 0;
        }
        if (context.role == VoiceRole.HIGH_PAD && fallbackProgram == GM_SYNTH_VOICE) {
            if (context.decodedProfile != null && context.decodedProfile.octaveShift >= 1) {
                return 0;
            }
            return profile.averageNote >= 68.0 ? -12 : 0;
        }
        if ((context.role == VoiceRole.PAD || context.role == VoiceRole.STRINGS || context.role == VoiceRole.BRASS)
                && profile.averageNote >= 64.0
                && profile.averageDurationTicks >= 220.0) {
            return -12;
        }
        if ((fallbackProgram == GM_STRING_ENSEMBLE_1 || fallbackProgram == GM_FRENCH_HORN)
                && profile.uniqueNotes <= 6
                && profile.averageDurationTicks >= 260.0
                && profile.averageNote >= 60.0) {
            return -12;
        }
        return 0;
    }

    private static boolean prefersChoralFallback(ChannelProfile profile,
                                                 int bankMsb,
                                                 int bankLsb,
                                                 int originalProgram,
                                                 DecodedChannelProfile decodedProfile) {
        if (profile == null) {
            return false;
        }
        if (decodedProfile != null && decodedProfile.usingDrumBank) {
            return false;
        }
        if (profile.averageDurationTicks < 260.0
                || profile.averageNote < 58.0
                || profile.uniqueNotes > 6
                || profile.maxNote < 62) {
            return false;
        }
        if (bankMsb == 0x36) {
            return true;
        }
        if (isOddLowBank(bankMsb) && profile.averageDurationTicks >= 280.0) {
            return true;
        }
        if (bankMsb == 0x00 && (originalProgram == 2 || originalProgram == 3)) {
            return true;
        }
        if ((bankMsb == 0x04 || bankMsb == 0x05) && bankLsb == 0 && originalProgram >= 2) {
            return true;
        }
        return false;
    }

    private static String describeReMEXABankFamily(int bankMsb) {
        int unsigned = bankMsb & 0xFF;
        if (isOddLowBank(unsigned)) {
            return "lowbank-page1";
        }
        return switch (unsigned) {
            case 0x00 -> "gm0";
            case 0x04, 0x05 -> "variant";
            case 0x34 -> "family1-special";
            case 0x36 -> "special36";
            default -> unsigned < 0x34 ? "lowbank" : "unknown";
        };
    }

    private static boolean isOddLowBank(int bankMsb) {
        int unsigned = bankMsb & 0xFF;
        return unsigned < 0x34 && (unsigned & 1) != 0;
    }

    private static boolean prefersLowBankChorusTexture(ChannelProfile profile, boolean oddLowBank) {
        return oddLowBank
                && profile != null
                && profile.averageDurationTicks >= 300.0
                && profile.averageNote >= 60.0
                && profile.uniqueNotes <= 10
                && profile.maxNote - profile.minNote <= 20;
    }

    private static boolean looksLikeArpeggioLayer(ChannelProfile profile) {
        return profile != null
                && ((profile.noteCount >= 24
                && profile.averageDurationTicks <= 300.0
                && profile.maxNote - profile.minNote >= 14)
                || (profile.noteCount >= 32 && profile.maxNote - profile.minNote >= 24)
                || (profile.uniqueNotes >= 18
                && profile.averageDurationTicks <= 340.0
                && profile.maxNote - profile.minNote >= 18));
    }

    private static String classifyExclusiveVoiceSignature(byte[] voice) {
        if (voice.length >= 16 && (voice[5] & 0xFF) == 0x02) {
            return String.format("exvo-ctrl-%02X%02X%02X", voice[10] & 0xFF, voice[11] & 0xFF, voice[12] & 0xFF);
        }
        if (voice.length >= 14) {
            return String.format("exvo-%02X%02X%02X", voice[9] & 0xFF, voice[10] & 0xFF, voice[11] & 0xFF);
        }
        return "exvo-unknown";
    }

    private static boolean containsAsciiTag(byte[] source, String tag) {
        return new String(source, StandardCharsets.ISO_8859_1).contains(tag);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toByteArray();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ChannelProfileBuilder {
        private long noteCount;
        private long durationSumTicks;
        private long noteSum;
        private int minNote = Integer.MAX_VALUE;
        private int maxNote = Integer.MIN_VALUE;
        private final Set<Integer> uniques = new TreeSet<>();
        private final Map<Integer, NoteProfileBuilder> notes = new HashMap<>();

        private void recordNoteOn(int note) {
            noteCount++;
            noteSum += note;
            minNote = Math.min(minNote, note);
            maxNote = Math.max(maxNote, note);
            uniques.add(note);
            notes.computeIfAbsent(note, ignored -> new NoteProfileBuilder()).recordNoteOn();
        }

        private void recordDuration(int note, long durationTicks) {
            durationSumTicks += durationTicks;
            NoteProfileBuilder builder = notes.get(note);
            if (builder != null) {
                builder.recordDuration(durationTicks);
            }
        }

        private ChannelProfile build() {
            double averageDurationTicks = noteCount == 0 ? 0.0 : (double) durationSumTicks / noteCount;
            double averageNote = noteCount == 0 ? 0.0 : (double) noteSum / noteCount;
            int[] sortedNotes = uniques.stream().mapToInt(Integer::intValue).toArray();
            Map<Integer, NoteProfile> noteProfiles = new HashMap<>();
            for (Map.Entry<Integer, NoteProfileBuilder> entry : notes.entrySet()) {
                noteProfiles.put(entry.getKey(), entry.getValue().build());
            }
            return new ChannelProfile(noteCount, minNote == Integer.MAX_VALUE ? 0 : minNote,
                    maxNote == Integer.MIN_VALUE ? 0 : maxNote, uniques.size(), averageDurationTicks, averageNote, sortedNotes, noteProfiles);
        }
    }

    private static final class NoteProfileBuilder {
        private long count;
        private long durationSumTicks;

        private void recordNoteOn() {
            count++;
        }

        private void recordDuration(long durationTicks) {
            durationSumTicks += durationTicks;
        }

        private NoteProfile build() {
            double averageDurationTicks = count == 0 ? 0.0 : (double) durationSumTicks / count;
            return new NoteProfile(count, averageDurationTicks);
        }
    }

    private record ChannelProfile(long noteCount, int minNote, int maxNote, int uniqueNotes, double averageDurationTicks,
                                  double averageNote,
                                  int[] sortedNotes, Map<Integer, NoteProfile> noteProfiles) {
    }

    private record NoteProfile(long count, double averageDurationTicks) {
    }

    private record DecodedChannelProfile(boolean usingDrumBank,
                                         int channelType,
                                         int velocity,
                                         int octaveShift) {
        private static DecodedChannelProfile merge(DecodedChannelProfile left, DecodedChannelProfile right) {
            return new DecodedChannelProfile(
                    left.usingDrumBank || right.usingDrumBank,
                    Math.max(left.channelType, right.channelType),
                    Math.max(left.velocity, right.velocity),
                    left.octaveShift != 0 ? left.octaveShift : right.octaveShift);
        }
    }

    private record FallbackVoiceMapping(int program, int transposeSemitones) {
    }

    private record VoiceRoutingContext(int originalProgram,
                                       int bankMsb,
                                       int bankLsb,
                                       ChannelProfile profile,
                                       DecodedChannelProfile decodedProfile,
                                       ExclusiveVoiceProfile exclusiveVoiceProfile,
                                       boolean oddLowBank,
                                       boolean arpeggioLayer,
                                       VoiceRole role) {
    }

    private static void dispatchCompletion(PhraseTrackListener listener) {
        Thread callbackThread = new Thread(() -> listener.eventOccurred(-1), "remexa-smaf-callback");
        callbackThread.setDaemon(true);
        callbackThread.start();
    }

    private static void dispatchUserEvent(PhraseTrackListener listener, int eventId) {
        listener.eventOccurred(eventId);
    }

    private static String describeException(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        StringBuilder description = new StringBuilder(96);
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 4) {
            if (depth > 0) {
                description.append(" <- ");
            }
            description.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                description.append(": ").append(message);
            }
            current = current.getCause();
            depth++;
        }
        return description.toString();
    }

    private enum VoiceRole {
        PERCUSSION,
        ARPEGGIO,
        BASS,
        LOW_STRINGS,
        CHOIR,
        LEAD,
        HIGH_PAD,
        PAD,
        BRASS,
        PLUCK,
        STRINGS,
        UNKNOWN
    }

    private record ExclusiveVoiceProfile(int slot,
                                         String signature,
                                         int length,
                                         int selector0,
                                         int selector1,
                                         int selector2,
                                         boolean controlVoice) {
    }

    private record SharedMidiOutput(Synthesizer synthesizer,
                                    MidiDevice outputDevice,
                                    Receiver receiver,
                                    String description) {
    }

    private record SmafCacheKey(byte[] source, int hash) {
        private SmafCacheKey(byte[] source) {
            this(source, Arrays.hashCode(source));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SmafCacheKey cacheKey
                    && hash == cacheKey.hash
                    && Arrays.equals(source, cacheKey.source);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private record SmafRenderCacheKey(SmafCacheKey sourceKey, String engineId) {
    }

    private record DecodedSmaf(Sequence sequence,
                               Sequence midiSequence,
                               List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents,
                               List<byte[]> startupPackets,
                               List<byte[]> exclusiveVoices,
                               List<byte[]> pcmClipData,
                               List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers,
                               List<SMAFDecoder.SequenceUserEvent> userEvents,
                               boolean hasPcmPayload,
                               int pcmClipCount) {
    }
}
