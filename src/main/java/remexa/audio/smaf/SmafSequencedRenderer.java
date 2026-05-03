package remexa.audio.smaf;

import org.recompile.mobile.Mobile;

import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SmafSequencedRenderer {
    private static final int DEFAULT_OUTPUT_SAMPLE_RATE = 32_000;
    private static final int OUTPUT_CHANNELS = 2;
    private static final float TRAILING_SILENCE_EPSILON = 0.0001f;
    private static final int TRAILING_SILENCE_GRACE_MILLIS = 120;
    private static final int MAX_STREAM_TAIL_MILLIS =
            Integer.getInteger("remexa.smafMaxStreamTailMs", 500);
    private final String rendererName;
    private final SmafSynthProvider synthProvider;
    private final int outputSampleRate;
    private final int framesPerTick;
    private final int tailRenderLimitFrames;

    SmafSequencedRenderer(String rendererName, SmafSynthProvider synthProvider) {
        this(rendererName, synthProvider, DEFAULT_OUTPUT_SAMPLE_RATE);
    }

    SmafSequencedRenderer(String rendererName, SmafSynthProvider synthProvider, int outputSampleRate) {
        this.rendererName = rendererName;
        this.synthProvider = synthProvider;
        this.outputSampleRate = outputSampleRate;
        this.framesPerTick = outputSampleRate / 1_000;
        this.tailRenderLimitFrames = outputSampleRate * 10;
    }

    SmafRenderedAudio render(Sequence sequence,
                             List<SMAFDecoder.SequenceSysExEvent> sysExEvents,
                             List<byte[]> startupPackets,
                             List<byte[]> pcmClipData,
                             List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) throws Exception {
        SmafSynthAdapter sampler = synthProvider.instance(outputSampleRate);
        sampler.reset();

        List<PcmClip> pcmClips = decodePcmClips(pcmClipData);
        List<RenderEvent> events = collectEvents(sequence, sysExEvents, startupPackets, pcmTriggers, pcmClips);
        MidiChannelState[] channelStates = createChannelStates();

        float[] mix = new float[Math.max(outputSampleRate * OUTPUT_CHANNELS, 1)];
        long currentTick = 0L;
        int framePosition = 0;
        int eventIndex = 0;
        while (eventIndex < events.size()) {
            long targetTick = events.get(eventIndex).tick();
            int frames = tickToFrames(targetTick - currentTick);
            if (frames > 0) {
                mix = ensureFrameCapacity(mix, framePosition + frames);
                sampler.render(mix, framePosition * OUTPUT_CHANNELS, frames, 1.0f, 1.0f, true, false);
                framePosition += frames;
                currentTick = targetTick;
            }
            while (eventIndex < events.size() && events.get(eventIndex).tick() == targetTick) {
                applyEvent(sampler, channelStates, events.get(eventIndex));
                eventIndex++;
            }
        }

        int tailFrames = 0;
        while (!sampler.isFinished() && tailFrames < tailRenderLimitFrames) {
            int chunkFrames = 512;
            mix = ensureFrameCapacity(mix, framePosition + chunkFrames);
            sampler.render(mix, framePosition * OUTPUT_CHANNELS, chunkFrames, 1.0f, 1.0f, true, false);
            framePosition += chunkFrames;
            tailFrames += chunkFrames;
        }

        if (SmafDebug.isEnabled("render", SmafDebug.Level.INFO)) {
            SmafDebug.info("render",
                    rendererName + " render events=" + events.size()
                            + " startupPackets=" + startupPackets.size()
                            + " sequenceSysEx=" + sysExEvents.size()
                            + " pcmClips=" + countNonNullClips(pcmClips)
                            + " pcmTriggers=" + pcmTriggers.size());
        }
        int mixedFrameCount = requiredPcmFrameCount(framePosition, pcmClips, pcmTriggers);
        mix = ensureFrameCapacity(mix, mixedFrameCount);
        mixPcmClips(mix, pcmClips, pcmTriggers);
        int finalFrameCount = trimTrailingSilence(mix, mixedFrameCount);
        return new SmafRenderedAudio(outputSampleRate, OUTPUT_CHANNELS, finalFrameCount,
                encodePcm16Le(mix, finalFrameCount));
    }

    SmafStreamingSession openStream(Sequence sequence,
                                    List<SMAFDecoder.SequenceSysExEvent> sysExEvents,
                                    List<byte[]> startupPackets,
                                    List<byte[]> pcmClipData,
                                    List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) throws Exception {
        return new StreamingSession(sequence, sysExEvents, startupPackets, pcmClipData, pcmTriggers);
    }

    private static MidiChannelState[] createChannelStates() {
        MidiChannelState[] states = new MidiChannelState[16];
        for (int i = 0; i < states.length; i++) {
            states[i] = new MidiChannelState();
        }
        return states;
    }

    private static List<RenderEvent> collectEvents(Sequence sequence,
                                                   List<SMAFDecoder.SequenceSysExEvent> sysExEvents,
                                                   List<byte[]> startupPackets,
                                                   List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers,
                                                   List<PcmClip> pcmClips) {
        List<RenderEvent> events = new ArrayList<>();
        int order = 0;
        for (byte[] startupPacket : startupPackets) {
            if (startupPacket == null || startupPacket.length == 0) {
                continue;
            }
            events.add(RenderEvent.sysEx(0L, order++, 0, startupPacket));
        }
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage) {
                    continue;
                }
                if (message instanceof ShortMessage shortMessage) {
                    events.add(RenderEvent.shortMessage(event.getTick(), order++, shortMessage));
                    continue;
                }
                if (message instanceof SysexMessage sysexMessage) {
                    events.add(RenderEvent.sysEx(event.getTick(), order++, -1, sysexMessage.getData()));
                }
            }
        }
        for (SMAFDecoder.SequenceSysExEvent event : sysExEvents) {
            events.add(RenderEvent.sysEx(event.tick(), order++, event.sourceBank(), event.data()));
        }
        events.sort(Comparator
                .comparingLong(RenderEvent::tick)
                .thenComparingInt(RenderEvent::priority)
                .thenComparingInt(RenderEvent::order));
        return suppressPcmCarrierNotes(
                suppressLegacyYamahaCarrierNotes(events),
                pcmTriggers,
                pcmClips);
    }

    private static List<RenderEvent> suppressLegacyYamahaCarrierNotes(List<RenderEvent> events) {
        Set<Long> extendedNoteTicks = new HashSet<>();
        for (RenderEvent event : events) {
            int channel = legacyYamahaExtendedNoteChannel(event);
            if (channel >= 0) {
                extendedNoteTicks.add(tickChannelKey(event.tick, channel));
            }
        }
        if (extendedNoteTicks.isEmpty()) {
            return events;
        }

        List<RenderEvent> filtered = new ArrayList<>(events.size());
        Set<Integer> suppressedNotes = new HashSet<>();
        int suppressed = 0;
        for (RenderEvent event : events) {
            if (event.sysEx == null && event.command == ShortMessage.NOTE_ON && event.data2 > 0
                    && extendedNoteTicks.contains(tickChannelKey(event.tick, event.channel))) {
                suppressedNotes.add(channelNoteKey(event.channel, event.data1));
                suppressed++;
                continue;
            }
            if (event.sysEx == null
                    && (event.command == ShortMessage.NOTE_OFF
                    || (event.command == ShortMessage.NOTE_ON && event.data2 == 0))
                    && suppressedNotes.remove(channelNoteKey(event.channel, event.data1))) {
                suppressed++;
                continue;
            }
            filtered.add(event);
        }

        if (suppressed > 0 && SmafDebug.isEnabled("render", SmafDebug.Level.DEBUG)) {
            SmafDebug.debug("render",
                    "Suppressed " + suppressed + " Yamaha 43 03 90 carrier note event(s)");
        }
        return filtered;
    }

    private static List<RenderEvent> suppressPcmCarrierNotes(List<RenderEvent> events,
                                                             List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers,
                                                             List<PcmClip> pcmClips) {
        if (pcmTriggers.isEmpty() || pcmClips.isEmpty()) {
            return events;
        }

        Set<Long> noteOnKeys = new HashSet<>();
        Set<Long> noteOffKeys = new HashSet<>();
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            if (resolvePcmClip(trigger, pcmClips) == null) {
                continue;
            }
            noteOnKeys.add(tickChannelNoteKey(trigger.startTick(), trigger.midiChannel(), trigger.midiNote()));
            noteOffKeys.add(tickChannelNoteKey(trigger.triggerTick(), trigger.midiChannel(), trigger.midiNote()));
        }
        if (noteOnKeys.isEmpty()) {
            return events;
        }

        List<RenderEvent> filtered = new ArrayList<>(events.size());
        int suppressed = 0;
        for (RenderEvent event : events) {
            if (event.sysEx == null && event.command == ShortMessage.NOTE_ON && event.data2 > 0
                    && noteOnKeys.contains(tickChannelNoteKey(event.tick, event.channel, event.data1))) {
                suppressed++;
                continue;
            }
            if (event.sysEx == null
                    && (event.command == ShortMessage.NOTE_OFF
                    || (event.command == ShortMessage.NOTE_ON && event.data2 == 0))
                    && noteOffKeys.contains(tickChannelNoteKey(event.tick, event.channel, event.data1))) {
                suppressed++;
                continue;
            }
            filtered.add(event);
        }

        if (suppressed > 0 && SmafDebug.isEnabled("render", SmafDebug.Level.DEBUG)) {
            SmafDebug.debug("render",
                    "Suppressed " + suppressed + " PCM carrier note event(s)");
        }
        return filtered;
    }

    private static int legacyYamahaExtendedNoteChannel(RenderEvent event) {
        if (event.sysEx == null || event.sysEx.length < 5
                || (event.sysEx[0] & 0xff) != 0x43
                || (event.sysEx[1] & 0xff) != 0x03
                || (event.sysEx[2] & 0xff) != 0x90) {
            return -1;
        }
        int selector = event.sysEx[3] & 0xf0;
        if (selector != 0xb0 && selector != 0xc0) {
            return -1;
        }
        int sourceBank = Math.max(0, event.sysExSourceBank);
        return (sourceBank << 2) + (event.sysEx[3] & 0x03);
    }

    private static long tickChannelKey(long tick, int channel) {
        return (tick << 5) | (channel & 0x1fL);
    }

    private static int channelNoteKey(int channel, int note) {
        return ((channel & 0x1f) << 8) | (note & 0xff);
    }

    private static long tickChannelNoteKey(long tick, int channel, int note) {
        return (tick << 13) | ((channel & 0x1fL) << 8) | (note & 0xffL);
    }

    private static void applyEvent(SmafSynthAdapter sampler, MidiChannelState[] channelStates, RenderEvent event) {
        if (event.sysEx != null) {
            sampler.sysEx(event.sysExSourceBank, event.sysEx);
            return;
        }

        MidiChannelState channelState = channelStates[event.channel];
        switch (event.command) {
            case ShortMessage.NOTE_OFF -> sampler.keyOff(event.channel, event.data1 - 69);
            case ShortMessage.NOTE_ON -> {
                if (event.data2 == 0) {
                    sampler.keyOff(event.channel, event.data1 - 69);
                } else {
                    sampler.keyOn(event.channel, event.data1 - 69, event.data2 / 127.0f);
                }
            }
            case ShortMessage.PROGRAM_CHANGE -> sampler.programChange(event.channel, event.data1);
            case ShortMessage.PITCH_BEND -> {
                channelState.pitchBendRaw = (event.data2 << 7) | event.data1;
                sampler.pitchBend(event.channel, bendSemitones(channelState));
            }
            case ShortMessage.CONTROL_CHANGE -> applyControlChange(sampler, channelState, event.channel, event.data1, event.data2);
            default -> {
                // Unsupported voice/status messages are ignored.
            }
        }
    }

    private static void applyControlChange(SmafSynthAdapter sampler,
                                           MidiChannelState channelState,
                                           int channel,
                                           int controller,
                                           int value) {
        switch (controller) {
            case 0 -> {
                channelState.bankMsb = value;
                sampler.bankChange(channel, value);
            }
            case 1 -> sampler.modulation(channel, value);
            case 7 -> {
                channelState.channelVolume = value / 127.0f;
                sampler.volume(channel, channelState.channelVolume * channelState.expression);
            }
            case 10 -> sampler.panpot(channel, midiPanToFloat(value));
            case 11 -> {
                channelState.expression = value / 127.0f;
                sampler.volume(channel, channelState.channelVolume * channelState.expression);
            }
            case 32 -> channelState.bankLsb = value;
            case 100 -> channelState.rpnLsb = value;
            case 101 -> channelState.rpnMsb = value;
            case 6 -> {
                if (channelState.rpnMsb == 0 && channelState.rpnLsb == 0) {
                    channelState.pitchBendRangeSemitones = Math.max(0.0f, value);
                    sampler.pitchBendRange(channel, channelState.pitchBendRangeSemitones);
                    sampler.pitchBend(channel, bendSemitones(channelState));
                }
            }
            case 38 -> {
                if (channelState.rpnMsb == 0 && channelState.rpnLsb == 0) {
                    float coarse = (float) Math.floor(channelState.pitchBendRangeSemitones);
                    channelState.pitchBendRangeSemitones = coarse + (value / 100.0f);
                    sampler.pitchBendRange(channel, channelState.pitchBendRangeSemitones);
                    sampler.pitchBend(channel, bendSemitones(channelState));
                }
            }
            case 121 -> {
                channelState.reset();
                sampler.bankChange(channel, 0);
                sampler.volume(channel, 1.0f);
                sampler.panpot(channel, 0.0f);
                sampler.modulation(channel, 0);
                sampler.pitchBendRange(channel, channelState.pitchBendRangeSemitones);
                sampler.pitchBend(channel, 0.0f);
            }
            default -> {
                // Unsupported controllers are ignored by the clean-room SMAF path.
            }
        }
    }

    private static float bendSemitones(MidiChannelState state) {
        float normalized = (state.pitchBendRaw - 8192) / 8192.0f;
        return normalized * state.pitchBendRangeSemitones;
    }

    private static float midiPanToFloat(int value) {
        return Math.max(-1.0f, Math.min(1.0f, (value - 64.0f) / 63.0f));
    }

    private int requiredPcmFrameCount(int framePosition,
                                      List<PcmClip> pcmClips,
                                      List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
        int mixedFrameCount = framePosition;
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            ResolvedPcmClip resolved = resolvePcmClip(trigger, pcmClips);
            if (resolved == null) {
                continue;
            }
            PcmClip clip = resolved.clip();
            mixedFrameCount = Math.max(mixedFrameCount, tickToFrames(trigger.triggerTick()) + clip.frameCount);
        }
        return mixedFrameCount;
    }

    private void mixPcmClips(float[] mix,
                             List<PcmClip> pcmClips,
                             List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
        int loggedTriggers = 0;
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            ResolvedPcmClip resolved = resolvePcmClip(trigger, pcmClips);
            if (resolved == null) {
                continue;
            }
            PcmClip clip = resolved.clip();
            int startFrame = tickToFrames(trigger.triggerTick());
            int clipFrames = clip.frameCount;
            if (clipFrames <= 0) {
                continue;
            }
            float gain = Math.max(0.0f, Math.min(1.0f, trigger.velocity() / 127.0f));
            if (loggedTriggers < 4 && SmafDebug.isEnabled("render", SmafDebug.Level.INFO)) {
                SmafDebug.info("render",
                        "Mixing PCM trigger note=" + trigger.noteValue()
                                + " resolvedClip=" + resolved.index()
                                + " triggerTick=" + trigger.triggerTick()
                                + " startTick=" + trigger.startTick()
                                + " gateTime=" + trigger.gateTime()
                                + " gateTimeMs=" + trigger.gateTimeMs()
                                + " clipFrames=" + clipFrames
                                + " gain=" + gain);
                loggedTriggers++;
            }
            for (int frame = 0; frame < clipFrames; frame++) {
                int outputFrame = startFrame + frame;
                int outputIndex = outputFrame * OUTPUT_CHANNELS;
                int clipIndexBase = frame * OUTPUT_CHANNELS;
                mix[outputIndex] += clip.samples[clipIndexBase] * gain;
                mix[outputIndex + 1] += clip.samples[clipIndexBase + 1] * gain;
            }
        }
    }

    private List<PcmClip> decodePcmClips(List<byte[]> pcmClipData) throws Exception {
        List<PcmClip> clips = new ArrayList<>(pcmClipData.size());
        for (byte[] pcmClip : pcmClipData) {
            clips.add(decodePcmClip(pcmClip));
        }
        return clips;
    }

    private PcmClip decodePcmClip(byte[] clipData) throws Exception {
        if (clipData == null || clipData.length == 0) {
            return null;
        }
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(clipData))) {
            AudioFormat base = source.getFormat();
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false);
            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, source)) {
                byte[] pcmBytes = readAll(pcmStream);
                float[] samples = resampleToOutput(pcmBytes, pcmFormat);
                if (SmafDebug.isEnabled("render", SmafDebug.Level.INFO)) {
                    SmafDebug.info("render",
                            "Decoded PCM clip bytes=" + clipData.length
                                    + " format=" + base.getEncoding()
                                    + "@" + Math.round(base.getSampleRate())
                                    + "Hz"
                                    + " ch=" + base.getChannels()
                                    + " -> frames=" + (samples.length / OUTPUT_CHANNELS)
                                    + " peak=" + peak(samples));
                }
                return new PcmClip(samples, samples.length / OUTPUT_CHANNELS);
            }
        } catch (Exception exception) {
            SmafDebug.info("render", "Unable to decode SMAF PCM clip: " + exception.getMessage());
            Mobile.log(Mobile.LOG_WARNING,
                    "Unable to decode SMAF PCM clip: " + exception.getMessage());
            return null;
        }
    }

    private static ResolvedPcmClip resolvePcmClip(SMAFDecoder.PcmSequenceTrigger trigger,
                                                  List<PcmClip> pcmClips) {
        if (pcmClips.isEmpty()) {
            return null;
        }
        int noteValue = trigger.noteValue();
        ResolvedPcmClip exact = clipAt(pcmClips, noteValue);
        if (exact != null) {
            return exact;
        }
        if (noteValue > 0) {
            ResolvedPcmClip oneBased = clipAt(pcmClips, noteValue - 1);
            if (oneBased != null) {
                return oneBased;
            }
        }
        return soleClip(pcmClips);
    }

    private static ResolvedPcmClip clipAt(List<PcmClip> pcmClips, int index) {
        if (index < 0 || index >= pcmClips.size()) {
            return null;
        }
        PcmClip clip = pcmClips.get(index);
        if (clip == null) {
            return null;
        }
        return new ResolvedPcmClip(index, clip);
    }

    private static ResolvedPcmClip soleClip(List<PcmClip> pcmClips) {
        ResolvedPcmClip resolved = null;
        for (int i = 0; i < pcmClips.size(); i++) {
            PcmClip clip = pcmClips.get(i);
            if (clip == null) {
                continue;
            }
            if (resolved != null) {
                return null;
            }
            resolved = new ResolvedPcmClip(i, clip);
        }
        return resolved;
    }

    private List<ScheduledPcmClip> schedulePcmClips(List<PcmClip> pcmClips,
                                                    List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
        List<ScheduledPcmClip> scheduled = new ArrayList<>();
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            ResolvedPcmClip resolved = resolvePcmClip(trigger, pcmClips);
            if (resolved == null) {
                continue;
            }
            PcmClip clip = resolved.clip();
            if (clip == null || clip.frameCount <= 0) {
                continue;
            }
            int startFrame = tickToFrames(trigger.triggerTick());
            float gain = Math.max(0.0f, Math.min(1.0f, trigger.velocity() / 127.0f));
            scheduled.add(new ScheduledPcmClip(startFrame, clip, gain));
        }
        scheduled.sort(Comparator.comparingInt(ScheduledPcmClip::startFrame));
        return scheduled;
    }

    private int scheduledPcmEndFrame(List<ScheduledPcmClip> scheduled) {
        int endFrame = 0;
        for (ScheduledPcmClip clip : scheduled) {
            endFrame = Math.max(endFrame, clip.endFrame());
        }
        return endFrame;
    }

    private static void mixScheduledPcmClips(float[] output,
                                             int chunkStartFrame,
                                             int frameCount,
                                             List<ScheduledPcmClip> scheduled) {
        int chunkEndFrame = chunkStartFrame + frameCount;
        for (ScheduledPcmClip scheduledClip : scheduled) {
            if (scheduledClip.endFrame() <= chunkStartFrame) {
                continue;
            }
            if (scheduledClip.startFrame() >= chunkEndFrame) {
                break;
            }
            int sourceStartFrame = Math.max(0, chunkStartFrame - scheduledClip.startFrame());
            int outputStartFrame = Math.max(0, scheduledClip.startFrame() - chunkStartFrame);
            int framesToCopy = Math.min(
                    scheduledClip.clip().frameCount - sourceStartFrame,
                    frameCount - outputStartFrame);
            int sourceOffset = sourceStartFrame * OUTPUT_CHANNELS;
            int outputOffset = outputStartFrame * OUTPUT_CHANNELS;
            for (int frame = 0; frame < framesToCopy; frame++) {
                output[outputOffset] += scheduledClip.clip().samples[sourceOffset] * scheduledClip.gain();
                output[outputOffset + 1] += scheduledClip.clip().samples[sourceOffset + 1] * scheduledClip.gain();
                sourceOffset += OUTPUT_CHANNELS;
                outputOffset += OUTPUT_CHANNELS;
            }
        }
    }

    private static int countNonNullClips(List<PcmClip> pcmClips) {
        int count = 0;
        for (PcmClip clip : pcmClips) {
            if (clip != null) {
                count++;
            }
        }
        return count;
    }

    private static float peak(float[] samples) {
        float peak = 0.0f;
        for (float sample : samples) {
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }

    private static float peak(float[] samples, int sampleCount) {
        float peak = 0.0f;
        int length = Math.max(0, Math.min(samples.length, sampleCount));
        for (int i = 0; i < length; i++) {
            peak = Math.max(peak, Math.abs(samples[i]));
        }
        return peak;
    }

    private float[] resampleToOutput(byte[] pcmBytes, AudioFormat pcmFormat) {
        int inputChannels = Math.max(1, pcmFormat.getChannels());
        int inputFrames = pcmBytes.length / (inputChannels * 2);
        if (inputFrames <= 0) {
            return new float[0];
        }
        float[] input = new float[inputFrames * inputChannels];
        int offset = 0;
        for (int i = 0; i < input.length; i++) {
            int low = pcmBytes[offset] & 0xFF;
            int high = pcmBytes[offset + 1];
            short sample = (short) ((high << 8) | low);
            input[i] = sample / 32768.0f;
            offset += 2;
        }
        if (Math.round(pcmFormat.getSampleRate()) == outputSampleRate && inputChannels == OUTPUT_CHANNELS) {
            return input;
        }
        int outputFrames = Math.max(1, Math.round(inputFrames * (outputSampleRate / pcmFormat.getSampleRate())));
        float[] output = new float[outputFrames * OUTPUT_CHANNELS];
        for (int frame = 0; frame < outputFrames; frame++) {
            float sourcePosition = frame * pcmFormat.getSampleRate() / outputSampleRate;
            int leftIndex = Math.min(inputFrames - 1, (int) Math.floor(sourcePosition));
            int rightIndex = Math.min(inputFrames - 1, leftIndex + 1);
            float blend = sourcePosition - leftIndex;
            for (int channel = 0; channel < OUTPUT_CHANNELS; channel++) {
                float start = sampleAt(input, inputChannels, leftIndex, channel);
                float end = sampleAt(input, inputChannels, rightIndex, channel);
                output[frame * OUTPUT_CHANNELS + channel] = start + (end - start) * blend;
            }
        }
        return output;
    }

    private static float sampleAt(float[] input, int inputChannels, int frame, int channel) {
        if (inputChannels == 1) {
            return input[frame];
        }
        int effectiveChannel = Math.min(inputChannels - 1, channel);
        return input[frame * inputChannels + effectiveChannel];
    }

    private static byte[] readAll(AudioInputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static int trimTrailingSilence(float[] mix, int frameCount) {
        int lastAudibleFrame = frameCount;
        while (lastAudibleFrame > 1) {
            int index = (lastAudibleFrame - 1) * OUTPUT_CHANNELS;
            if (Math.abs(mix[index]) > 0.0001f || Math.abs(mix[index + 1]) > 0.0001f) {
                break;
            }
            lastAudibleFrame--;
        }
        return Math.max(1, lastAudibleFrame);
    }

    private static byte[] encodePcm16Le(float[] mix, int frameCount) {
        byte[] output = new byte[Math.max(1, frameCount) * OUTPUT_CHANNELS * 2];
        int offset = 0;
        for (int i = 0; i < frameCount * OUTPUT_CHANNELS; i++) {
            int sample = Math.round(Math.max(-1.0f, Math.min(1.0f, mix[i])) * 32767.0f);
            output[offset++] = (byte) (sample & 0xFF);
            output[offset++] = (byte) ((sample >>> 8) & 0xFF);
        }
        return output;
    }

    private static float[] ensureFrameCapacity(float[] buffer, int frameCount) {
        int requiredSamples = Math.max(1, frameCount * OUTPUT_CHANNELS);
        if (requiredSamples <= buffer.length) {
            return buffer;
        }
        int expanded = buffer.length;
        while (expanded < requiredSamples) {
            expanded *= 2;
        }
        float[] copy = new float[expanded];
        System.arraycopy(buffer, 0, copy, 0, buffer.length);
        return copy;
    }

    private int tickToFrames(long tick) {
        return (int) Math.max(0L, tick * framesPerTick);
    }

    private static final class MidiChannelState {
        private int bankMsb;
        private int bankLsb;
        private float channelVolume = 1.0f;
        private float expression = 1.0f;
        private float pitchBendRangeSemitones = 2.0f;
        private int pitchBendRaw = 8192;
        private int rpnMsb = 127;
        private int rpnLsb = 127;

        private void reset() {
            bankMsb = 0;
            bankLsb = 0;
            channelVolume = 1.0f;
            expression = 1.0f;
            pitchBendRangeSemitones = 2.0f;
            pitchBendRaw = 8192;
            rpnMsb = 127;
            rpnLsb = 127;
        }
    }

    private record PcmClip(float[] samples, int frameCount) {
    }

    private record ScheduledPcmClip(int startFrame, PcmClip clip, float gain) {
        private int endFrame() {
            return startFrame + clip.frameCount;
        }
    }

    private record ResolvedPcmClip(int index, PcmClip clip) {
    }

    private record RenderEvent(long tick,
                               int order,
                               int priority,
                               int command,
                               int channel,
                               int data1,
                               int data2,
                               int sysExSourceBank,
                               byte[] sysEx) {
        private static RenderEvent shortMessage(long tick, int order, ShortMessage message) {
            int command = message.getCommand();
            return new RenderEvent(
                    tick,
                    order,
                    priorityFor(command, message.getData2()),
                    command,
                    message.getChannel(),
                    message.getData1(),
                    message.getData2(),
                    -1,
                    null);
        }

        private static RenderEvent sysEx(long tick, int order, int sourceBank, byte[] sysEx) {
            return new RenderEvent(
                    tick,
                    order,
                    0,
                    0,
                    0,
                    0,
                    0,
                    sourceBank,
                    sysEx == null ? new byte[0] : sysEx.clone());
        }

        private static int priorityFor(int command, int velocity) {
            return switch (command) {
                case ShortMessage.PROGRAM_CHANGE, ShortMessage.CONTROL_CHANGE, ShortMessage.PITCH_BEND -> 1;
                case ShortMessage.NOTE_OFF -> 2;
                case ShortMessage.NOTE_ON -> velocity == 0 ? 2 : 3;
                default -> 4;
            };
        }
    }

    private final class StreamingSession implements SmafStreamingSession {
        private final Sequence sequence;
        private final List<SMAFDecoder.SequenceSysExEvent> sysExEvents;
        private final List<byte[]> startupPackets;
        private final List<PcmClip> pcmClips;
        private final List<ScheduledPcmClip> scheduledPcmClips;
        private final List<RenderEvent> events;
        private final MidiChannelState[] channelStates = createChannelStates();
        private final int pcmEndFrame;
        private final int trailingSilenceGraceFrames;
        private final int maxTailFrames;
        private SmafSynthAdapter sampler;
        private int framePosition;
        private int eventIndex;
        private int trailingSilentFrames;
        private int tailFramesRendered;

        private StreamingSession(Sequence sequence,
                                 List<SMAFDecoder.SequenceSysExEvent> sysExEvents,
                                 List<byte[]> startupPackets,
                                 List<byte[]> pcmClipData,
                                 List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) throws Exception {
            this.sequence = sequence;
            this.sysExEvents = sysExEvents;
            this.startupPackets = startupPackets;
            this.pcmClips = decodePcmClips(pcmClipData);
            this.scheduledPcmClips = schedulePcmClips(this.pcmClips, pcmTriggers);
            this.events = collectEvents(sequence, sysExEvents, startupPackets, pcmTriggers, this.pcmClips);
            this.pcmEndFrame = scheduledPcmEndFrame(scheduledPcmClips);
            this.trailingSilenceGraceFrames = Math.max(1,
                    outputSampleRate * TRAILING_SILENCE_GRACE_MILLIS / 1_000);
            this.maxTailFrames = Math.max(trailingSilenceGraceFrames,
                    outputSampleRate * MAX_STREAM_TAIL_MILLIS / 1_000);
            if (SmafDebug.isEnabled("render", SmafDebug.Level.INFO)) {
                SmafDebug.info("render",
                        rendererName + " stream events=" + events.size()
                                + " startupPackets=" + startupPackets.size()
                                + " sequenceSysEx=" + sysExEvents.size()
                                + " pcmClips=" + countNonNullClips(this.pcmClips)
                                + " pcmTriggers=" + pcmTriggers.size());
            }
            resetSampler();
        }

        @Override
        public int sampleRate() {
            return outputSampleRate;
        }

        @Override
        public int channelCount() {
            return OUTPUT_CHANNELS;
        }

        @Override
        public int render(float[] output, int maxFrames) {
            if (maxFrames <= 0) {
                return 0;
            }
            Arrays.fill(output, 0, Math.min(output.length, maxFrames * OUTPUT_CHANNELS), 0.0f);
            int producedFrames = 0;
            int chunkStartFrame = framePosition;
            while (producedFrames < maxFrames) {
                while (eventIndex < events.size() && tickToFrames(events.get(eventIndex).tick()) <= framePosition) {
                    applyEvent(sampler, channelStates, events.get(eventIndex));
                    eventIndex++;
                }
                if (eventIndex >= events.size() && sampler.isFinished() && framePosition >= pcmEndFrame) {
                    break;
                }
                int framesUntilEvent = maxFrames - producedFrames;
                if (eventIndex < events.size()) {
                    framesUntilEvent = Math.min(framesUntilEvent,
                            Math.max(0, tickToFrames(events.get(eventIndex).tick()) - framePosition));
                }
                boolean tailOnly = eventIndex >= events.size() && framePosition >= pcmEndFrame;
                if (tailOnly) {
                    int remainingTailFrames = maxTailFrames - tailFramesRendered;
                    if (remainingTailFrames <= 0) {
                        break;
                    }
                    framesUntilEvent = Math.min(framesUntilEvent, remainingTailFrames);
                }
                if (framesUntilEvent == 0) {
                    continue;
                }
                sampler.render(output, producedFrames * OUTPUT_CHANNELS, framesUntilEvent, 1.0f, 1.0f, false, false);
                producedFrames += framesUntilEvent;
                framePosition += framesUntilEvent;
                if (tailOnly) {
                    tailFramesRendered += framesUntilEvent;
                }
            }
            if (producedFrames > 0) {
                mixScheduledPcmClips(output, chunkStartFrame, producedFrames, scheduledPcmClips);
                boolean tailOnly = eventIndex >= events.size() && framePosition >= pcmEndFrame;
                if (tailOnly) {
                    float peak = peak(output, producedFrames * OUTPUT_CHANNELS);
                    if (peak <= TRAILING_SILENCE_EPSILON) {
                        trailingSilentFrames += producedFrames;
                        if (trailingSilentFrames >= trailingSilenceGraceFrames) {
                            return 0;
                        }
                    } else {
                        trailingSilentFrames = 0;
                    }
                } else {
                    trailingSilentFrames = 0;
                    tailFramesRendered = 0;
                }
            }
            return producedFrames;
        }

        @Override
        public void rewind() throws Exception {
            framePosition = 0;
            eventIndex = 0;
            trailingSilentFrames = 0;
            tailFramesRendered = 0;
            for (MidiChannelState channelState : channelStates) {
                channelState.reset();
            }
            resetSampler();
        }

        private void resetSampler() throws Exception {
            sampler = synthProvider.instance(outputSampleRate);
            sampler.reset();
        }
    }

}
