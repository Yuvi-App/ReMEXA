package remexa.audio.smaf;

import remexa.audio.spatial.Audio3DSource;
import remexa.host.runtime.MidletRuntime;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

public final class YamahaMidiPlayback implements AutoCloseable {
    public static final String SYNTH_AUTO = "auto";
    public static final String SYNTH_MA3 = "ma3";
    public static final String SYNTH_MA5 = "ma5";
    public static final int READY = SmafPlayback.READY;
    public static final int PLAYING = SmafPlayback.PLAYING;
    public static final int PAUSED = SmafPlayback.PAUSED;

    private static final int MIDI_TICKS_PER_QUARTER = 1_000;
    private static final int DEFAULT_TEMPO_MICROS_PER_QUARTER = 500_000;
    private static final int TEMPO_META_TYPE = 0x51;
    private static final int SEQUENCER_SPECIFIC_META_TYPE = 0x7f;
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_LEGACY_SOFTBANK = 0x02;
    private static final int FAMILY_MA5_COMPACT = 0x04;
    private static final int FAMILY_MA5 = 0x05;
    private static final int LEGACY_SOFTBANK_SUBFAMILY = 0x02;
    private static final int LEGACY_SOFTBANK_VOICE = 0x08;
    private static final int LEGACY_SOFTBANK_WAVE = 0x0a;
    private static final int MIDI_PERCUSSION_CHANNEL = 9;
    private static final Ma3SmafAudioEngine MA3_ENGINE = new Ma3SmafAudioEngine();
    private static final Ma5SmafAudioEngine MA5_ENGINE = new Ma5SmafAudioEngine();

    private final SmafStreamingPlayer player;
    private final long durationMicros;

    private YamahaMidiPlayback(SmafStreamingSession session, long durationMicros,
                               Audio3DSource spatialSource) {
        this.player = new SmafStreamingPlayer(session, Collections.emptyList(),
                MidletRuntime.currentAppClassLoader(), spatialSource);
        this.durationMicros = Math.max(0L, durationMicros);
    }

    public static YamahaMidiPlayback create(byte[] source, String synthType) throws Exception {
        return create(source, synthType, null);
    }

    public static YamahaMidiPlayback create(byte[] source, String synthType,
                                           Audio3DSource spatialSource) throws Exception {
        var sourceSequence = MidiSystem.getSequence(new ByteArrayInputStream(source));
        var timing = collectMidiTiming(sourceSequence);
        var timedEvents = timing.events();
        var renderSequence = normalizeMidiTiming(timedEvents);
        var sequenceSysExEvents = extractSequencerSpecificYamahaEvents(timedEvents);
        var engine = resolveEngine(synthType, timedEvents, sequenceSysExEvents);
        var session = engine.openStream(new SmafRenderContext(
                source.clone(),
                renderSequence,
                sequenceSysExEvents,
                drumStartupPackets(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        return new YamahaMidiPlayback(session, timing.durationMicros(), spatialSource);
    }

    private static List<byte[]> drumStartupPackets() {
        return List.of(new byte[]{0x72, 0x0c, MIDI_PERCUSSION_CHANNEL, (byte) 0x80});
    }

    public int getState() {
        return player.getState();
    }

    public void setCompletionListener(Runnable listener) {
        player.setListener(eventId -> {
            if (eventId == -1 && listener != null) {
                listener.run();
            }
        });
    }

    public void setVolume(int value) {
        player.setVolume(value);
    }

    public void play(int loopCount) {
        if (mediaTimeMicros() >= durationMicros) {
            player.seek(0L);
        }
        player.playFromCurrentPosition(loopCount);
    }

    public void stop() {
        player.stop();
    }

    public void pause() {
        player.pause();
    }

    public void resume() {
        player.resume();
    }

    public long setMediaTime(long micros) {
        return Math.min(durationMicros, player.seek(Math.clamp(micros, 0L, durationMicros)));
    }

    public long mediaTimeMicros() {
        return Math.min(durationMicros, player.mediaTimeMicros());
    }

    public long durationMicros() {
        return durationMicros;
    }

    @Override
    public void close() {
        player.close();
    }

    private static YamahaAudioEngine resolveEngine(String synthType,
                                                  List<TimedMidiEvent> timedEvents,
                                                  List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        if (SYNTH_MA5.equalsIgnoreCase(synthType)) {
            return MA5_ENGINE;
        }
        if (SYNTH_AUTO.equalsIgnoreCase(synthType)
                && containsMa5MidiData(timedEvents, sequenceSysExEvents)) {
            return MA5_ENGINE;
        }
        return MA3_ENGINE;
    }

    private static Sequence normalizeMidiTiming(List<TimedMidiEvent> events) throws InvalidMidiDataException {
        var target = new Sequence(Sequence.PPQ, MIDI_TICKS_PER_QUARTER);
        var targetTrack = target.createTrack();
        for (var event : events) {
            targetTrack.add(new MidiEvent(cloneMessage(event.message()), event.tickMillis()));
        }
        return target;
    }

    private static List<SMAFDecoder.SequenceSysExEvent> extractSequencerSpecificYamahaEvents(
            List<TimedMidiEvent> events) {
        var sysExEvents = new ArrayList<SMAFDecoder.SequenceSysExEvent>();
        for (var event : events) {
            if (!(event.message() instanceof MetaMessage metaMessage)
                    || metaMessage.getType() != SEQUENCER_SPECIFIC_META_TYPE) {
                continue;
            }
            byte[] data = metaMessage.getData();
            if (data.length >= 2 && (data[0] & 0xff) == MANUFACTURER_YAMAHA) {
                sysExEvents.add(new SMAFDecoder.SequenceSysExEvent(
                        clampedTick(event.tickMillis()),
                        -1,
                        data.clone()));
            }
        }
        return sysExEvents;
    }

    private static int clampedTick(long tick) {
        return tick > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) tick);
    }

    private static boolean containsMa5MidiData(List<TimedMidiEvent> timedEvents,
                                               List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        for (SMAFDecoder.SequenceSysExEvent event : sequenceSysExEvents) {
            if (looksLikeMa5Packet(event.data())) {
                return true;
            }
        }
        for (TimedMidiEvent event : timedEvents) {
            if (event.message() instanceof SysexMessage sysexMessage
                    && looksLikeMa5Packet(sysexMessage.getData())) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeMa5Packet(byte[] packet) {
        byte[] body = normalizeVendorBody(packet);
        if (body.length < 3 || (body[0] & 0xff) != MANUFACTURER_YAMAHA) {
            return false;
        }
        int family = body[1] & 0xff;
        if (family == FAMILY_MA5 || family == FAMILY_MA5_COMPACT) {
            return true;
        }
        if (family != FAMILY_LEGACY_SOFTBANK || body.length < 4) {
            return false;
        }
        int subfamily = body[2] & 0xff;
        int packetType = body[3] & 0xff;
        return subfamily == LEGACY_SOFTBANK_SUBFAMILY
                && (packetType == LEGACY_SOFTBANK_VOICE || packetType == LEGACY_SOFTBANK_WAVE);
    }

    private static byte[] normalizeVendorBody(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        int start = 0;
        int end = trimF7(data, data.length);
        if (data.length >= 4 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xf0) {
            start = 3;
            end = trimF7(data, Math.min(data.length, start + (data[2] & 0xff)));
        } else if (data.length >= 4 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xf1) {
            start = 4;
            int bodyLength = (data[2] & 0xff) | ((data[3] & 0xff) << 8);
            end = trimF7(data, Math.min(data.length, start + bodyLength));
        } else if ((data[0] & 0xff) == 0xf0) {
            start = 1;
            end = trimF7(data, data.length);
        }
        if (end < start) {
            end = start;
        }
        byte[] body = new byte[end - start];
        System.arraycopy(data, start, body, 0, body.length);
        return body;
    }

    private static int trimF7(byte[] data, int end) {
        int clampedEnd = Math.max(0, Math.min(data.length, end));
        if (clampedEnd > 0 && (data[clampedEnd - 1] & 0xff) == 0xf7) {
            return clampedEnd - 1;
        }
        return clampedEnd;
    }

    private static List<TimedMidiEvent> collectTimedEvents(Sequence source) {
        return collectMidiTiming(source).events();
    }

    private static MidiTiming collectMidiTiming(Sequence source) {
        var rawEvents = new ArrayList<RawMidiEvent>();
        for (var track : source.getTracks()) {
            for (int index = 0; index < track.size(); index++) {
                var event = track.get(index);
                rawEvents.add(new RawMidiEvent(event.getTick(), rawEvents.size(), event.getMessage()));
            }
        }
        rawEvents.sort(Comparator
                .comparingLong(RawMidiEvent::tick)
                .thenComparingInt(RawMidiEvent::order));

        long setupMicros = hasLegacySetupBar(source, rawEvents) ? 250_000L : 0L;
        var events = new ArrayList<TimedMidiEvent>(rawEvents.size());
        long previousTick = 0L;
        double currentMillis = 0.0;
        int tempoMicrosPerQuarter = DEFAULT_TEMPO_MICROS_PER_QUARTER;
        for (var event : rawEvents) {
            long deltaTicks = Math.max(0L, event.tick() - previousTick);
            currentMillis += ticksToMillis(source, deltaTicks, tempoMicrosPerQuarter);
            previousTick = event.tick();
            events.add(new TimedMidiEvent(
                    Math.max(0L, Math.round(currentMillis - setupMicros / 1_000.0)), event.message()));
            Integer tempo = tempoFrom(event.message());
            if (tempo != null) {
                tempoMicrosPerQuarter = tempo;
            }
        }
        return new MidiTiming(events, Math.max(0L, source.getMicrosecondLength() - setupMicros));
    }

    /**
     * The SoftBank SMF loader (M7_EmuSmw7 10049550) recognizes a setup bar
     * using GM reset, 1/4 meter and 250000 us/qn at tick zero, followed by a
     * tempo event at one quarter note. It processes that bar without charging
     * its 250 ms to playback. Keep setup events in order at time zero, rather
     * than trimming to the first note, so authored rests remain intact.
     */
    private static boolean hasLegacySetupBar(Sequence source, List<RawMidiEvent> events) {
        if (source.getDivisionType() != Sequence.PPQ) return false;
        boolean reset = false, meter = false, tempo = false, legacy = false, boundary = false;
        for (var event : events) {
            if (event.tick() > source.getResolution()) break;
            MidiMessage message = event.message();
            if (event.tick() < source.getResolution()) {
                // Do not consume authored audio or a tempo-changing intro.
                if (message instanceof ShortMessage shortMessage
                        && shortMessage.getCommand() == ShortMessage.NOTE_ON
                        && shortMessage.getData2() > 0) return false;
                Integer value = tempoFrom(message);
                if (value != null && value != 250_000) return false;
            }
            if (event.tick() == source.getResolution() && tempoFrom(message) != null) {
                boundary = true;
            }
            if (event.tick() != 0L) continue;
            if (message instanceof SysexMessage sysex) {
                reset |= Arrays.equals(sysex.getData(), new byte[]{0x7e, 0x7f, 9, 1, (byte) 0xf7});
            } else if (message instanceof MetaMessage meta) {
                byte[] data = meta.getData();
                if (meta.getType() == 0x58 && data.length >= 2) {
                    meter |= data[0] == 1 && data[1] == 2;
                } else if (meta.getType() == TEMPO_META_TYPE) {
                    tempo |= Integer.valueOf(250_000).equals(tempoFrom(meta));
                } else if (meta.getType() == SEQUENCER_SPECIFIC_META_TYPE && data.length >= 4) {
                    legacy |= (data[0] & 0xff) == MANUFACTURER_YAMAHA
                            && data[1] == FAMILY_LEGACY_SOFTBANK
                            && data[2] == LEGACY_SOFTBANK_SUBFAMILY;
                }
            }
        }
        return reset && meter && tempo && legacy && boundary;
    }

    private static double ticksToMillis(Sequence sequence, long ticks, int tempoMicrosPerQuarter) {
        if (sequence.getDivisionType() == Sequence.PPQ) {
            return ticks * tempoMicrosPerQuarter / (sequence.getResolution() * 1_000.0);
        }
        return ticks * 1_000.0 / (sequence.getDivisionType() * sequence.getResolution());
    }

    private static Integer tempoFrom(MidiMessage message) {
        if (!(message instanceof MetaMessage metaMessage) || metaMessage.getType() != TEMPO_META_TYPE) {
            return null;
        }
        byte[] data = metaMessage.getData();
        if (data.length < 3) {
            return null;
        }
        return ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
    }

    private static MidiMessage cloneMessage(MidiMessage message) throws InvalidMidiDataException {
        if (message instanceof ShortMessage shortMessage) {
            var copy = new ShortMessage();
            copy.setMessage(
                    shortMessage.getCommand(),
                    shortMessage.getChannel(),
                    shortMessage.getData1(),
                    shortMessage.getData2()
            );
            return copy;
        }
        return (MidiMessage) message.clone();
    }

    private record RawMidiEvent(long tick, int order, MidiMessage message) {
    }

    private record TimedMidiEvent(long tickMillis, MidiMessage message) {
    }

    private record MidiTiming(List<TimedMidiEvent> events, long durationMicros) {
    }
}
