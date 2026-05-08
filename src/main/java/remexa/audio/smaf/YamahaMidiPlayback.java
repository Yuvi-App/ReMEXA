package remexa.audio.smaf;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import remexa.audio.pcm.RenderedPcmAudio;
import remexa.audio.pcm.RenderedPcmPlayer;

public final class YamahaMidiPlayback implements AutoCloseable {
    public static final String SYNTH_MA3 = "ma3";
    public static final String SYNTH_MA5 = "ma5";
    public static final int READY = RenderedPcmPlayer.READY;
    public static final int PLAYING = RenderedPcmPlayer.PLAYING;
    public static final int PAUSED = RenderedPcmPlayer.PAUSED;

    private static final int MIDI_TICKS_PER_QUARTER = 1_000;
    private static final int DEFAULT_TEMPO_MICROS_PER_QUARTER = 500_000;
    private static final int TEMPO_META_TYPE = 0x51;
    private static final int MIDI_PERCUSSION_CHANNEL = 9;
    private static final Ma3SmafAudioEngine MA3_ENGINE = new Ma3SmafAudioEngine();
    private static final Ma5SmafAudioEngine MA5_ENGINE = new Ma5SmafAudioEngine();

    private final SmafRenderedAudio audio;
    private final RenderedPcmPlayer player;
    private final long durationMillis;

    private long startedAtMillis;
    private long pausedAtMillis;

    private YamahaMidiPlayback(SmafRenderedAudio audio) {
        this.audio = audio;
        this.player = new RenderedPcmPlayer(new RenderedPcmAudio(
                audio.sampleRate(),
                audio.channelCount(),
                audio.frameCount(),
                audio.pcm16Le()
        ));
        this.durationMillis = Math.max(0L, audio.frameCount() * 1_000L / Math.max(1, audio.sampleRate()));
    }

    public static YamahaMidiPlayback create(byte[] source, String synthType) throws Exception {
        var sourceSequence = MidiSystem.getSequence(new ByteArrayInputStream(source));
        var renderSequence = normalizeMidiTiming(sourceSequence);
        var engine = resolveEngine(synthType);
        var rendered = engine.render(new SmafRenderContext(
                source.clone(),
                renderSequence,
                Collections.emptyList(),
                drumStartupPackets(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        return new YamahaMidiPlayback(rendered);
    }

    private static List<byte[]> drumStartupPackets() {
        return List.of(new byte[]{0x72, 0x0c, MIDI_PERCUSSION_CHANNEL, (byte) 0x80});
    }

    public int getState() {
        return player.getState();
    }

    public void setCompletionListener(Runnable listener) {
        player.setCompletionListener(listener);
    }

    public void setVolume(int value) {
        player.setVolume(value);
    }

    public void play(int loopCount) {
        startedAtMillis = System.currentTimeMillis();
        pausedAtMillis = 0L;
        player.play(loopCount);
    }

    public void stop() {
        startedAtMillis = 0L;
        pausedAtMillis = 0L;
        player.stop();
    }

    public void pause() {
        if (player.getState() == PLAYING) {
            pausedAtMillis = mediaTimeMillis();
        }
        player.pause();
    }

    public void resume() {
        if (pausedAtMillis > 0L) {
            startedAtMillis = System.currentTimeMillis() - pausedAtMillis;
        }
        pausedAtMillis = 0L;
        player.resume();
    }

    public long setMediaTime(long millis) {
        if (millis > 0L) {
            return mediaTimeMillis();
        }
        stop();
        return 0L;
    }

    public long mediaTimeMillis() {
        if (pausedAtMillis > 0L) {
            return pausedAtMillis;
        }
        if (startedAtMillis <= 0L) {
            return 0L;
        }
        return Math.min(durationMillis, Math.max(0L, System.currentTimeMillis() - startedAtMillis));
    }

    public long durationMillis() {
        return durationMillis;
    }

    @Override
    public void close() {
        player.close();
    }

    private static YamahaAudioEngine resolveEngine(String synthType) {
        if (SYNTH_MA5.equalsIgnoreCase(synthType)) {
            return MA5_ENGINE;
        }
        return MA3_ENGINE;
    }

    private static Sequence normalizeMidiTiming(Sequence source) throws InvalidMidiDataException {
        var target = new Sequence(Sequence.PPQ, MIDI_TICKS_PER_QUARTER);
        var targetTrack = target.createTrack();
        var events = collectTimedEvents(source);
        for (var event : events) {
            targetTrack.add(new MidiEvent(cloneMessage(event.message()), event.tickMillis()));
        }
        return target;
    }

    private static List<TimedMidiEvent> collectTimedEvents(Sequence source) {
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

        var events = new ArrayList<TimedMidiEvent>(rawEvents.size());
        long previousTick = 0L;
        double currentMillis = 0.0;
        int tempoMicrosPerQuarter = DEFAULT_TEMPO_MICROS_PER_QUARTER;
        for (var event : rawEvents) {
            long deltaTicks = Math.max(0L, event.tick() - previousTick);
            currentMillis += ticksToMillis(source, deltaTicks, tempoMicrosPerQuarter);
            previousTick = event.tick();
            events.add(new TimedMidiEvent(Math.max(0L, Math.round(currentMillis)), event.message()));
            Integer tempo = tempoFrom(event.message());
            if (tempo != null) {
                tempoMicrosPerQuarter = tempo;
            }
        }
        return events;
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
}
