package remexa.audio.smaf;

import remexa.audio.smaf.fuetrek.FueTrekSamplerProvider;
import remexa.audio.smaf.fuetrek.Sampler;
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
import java.util.Comparator;
import java.util.List;

final class SmafFueTrekRenderer {
    private static final int OUTPUT_SAMPLE_RATE = 32_000;
    private static final int OUTPUT_CHANNELS = 2;
    private static final int FRAMES_PER_TICK = OUTPUT_SAMPLE_RATE / 1_000;
    private static final int TAIL_RENDER_LIMIT_FRAMES = OUTPUT_SAMPLE_RATE * 10;
    private static final FueTrekSamplerProvider FUETREK = new FueTrekSamplerProvider();

    SmafRenderedAudio render(Sequence sequence,
                             List<SMAFDecoder.SequenceSysExEvent> sysExEvents,
                             List<byte[]> exclusiveVoices,
                             List<byte[]> pcmClipData,
                             List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) throws Exception {
        Sampler sampler = FUETREK.instance(OUTPUT_SAMPLE_RATE);
        sampler.reset();
        sampler.drumEnable(9, true);

        List<RenderEvent> events = collectEvents(sequence, sysExEvents, exclusiveVoices);
        MidiChannelState[] channelStates = createChannelStates();

        float[] mix = new float[Math.max(OUTPUT_SAMPLE_RATE * OUTPUT_CHANNELS, 1)];
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
        while (!sampler.isFinished() && tailFrames < TAIL_RENDER_LIMIT_FRAMES) {
            int chunkFrames = 512;
            mix = ensureFrameCapacity(mix, framePosition + chunkFrames);
            sampler.render(mix, framePosition * OUTPUT_CHANNELS, chunkFrames, 1.0f, 1.0f, true, false);
            framePosition += chunkFrames;
            tailFrames += chunkFrames;
        }

        List<PcmClip> pcmClips = decodePcmClips(pcmClipData);
        int mixedFrameCount = requiredPcmFrameCount(framePosition, pcmClips, pcmTriggers);
        mix = ensureFrameCapacity(mix, mixedFrameCount);
        mixPcmClips(mix, pcmClips, pcmTriggers);
        int finalFrameCount = trimTrailingSilence(mix, mixedFrameCount);
        return new SmafRenderedAudio(OUTPUT_SAMPLE_RATE, OUTPUT_CHANNELS, finalFrameCount,
                encodePcm16Le(mix, finalFrameCount));
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
                                                   List<byte[]> exclusiveVoices) {
        List<RenderEvent> events = new ArrayList<>();
        int order = 0;
        for (byte[] exclusiveVoice : exclusiveVoices) {
            if (exclusiveVoice == null || exclusiveVoice.length == 0) {
                continue;
            }
            events.add(RenderEvent.sysEx(0L, order++, exclusiveVoice));
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
                    events.add(RenderEvent.sysEx(event.getTick(), order++, sysexMessage.getData()));
                }
            }
        }
        for (SMAFDecoder.SequenceSysExEvent event : sysExEvents) {
            events.add(RenderEvent.sysEx(event.tick(), order++, event.data()));
        }
        events.sort(Comparator
                .comparingLong(RenderEvent::tick)
                .thenComparingInt(RenderEvent::priority)
                .thenComparingInt(RenderEvent::order));
        return events;
    }

    private static void applyEvent(Sampler sampler, MidiChannelState[] channelStates, RenderEvent event) {
        if (event.sysEx != null) {
            sampler.sysEx(event.sysEx);
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

    private static void applyControlChange(Sampler sampler,
                                           MidiChannelState channelState,
                                           int channel,
                                           int controller,
                                           int value) {
        switch (controller) {
            case 0 -> {
                channelState.bankMsb = value;
                sampler.bankChange(channel, value);
            }
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
                sampler.pitchBendRange(channel, channelState.pitchBendRangeSemitones);
                sampler.pitchBend(channel, 0.0f);
            }
            default -> {
                // Controllers such as modulation are currently ignored by the
                // clean-room SMAF path because the Fuetrek sampler does not expose
                // a direct MIDI controller surface for them.
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

    private static int requiredPcmFrameCount(int framePosition,
                                             List<PcmClip> pcmClips,
                                             List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
        int mixedFrameCount = framePosition;
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            int clipIndex = trigger.noteValue();
            if (clipIndex <= 0 || clipIndex >= pcmClips.size()) {
                continue;
            }
            PcmClip clip = pcmClips.get(clipIndex);
            if (clip == null) {
                continue;
            }
            int clipFrames = clip.frameCount;
            if (trigger.gateTime() > 0) {
                clipFrames = Math.min(clipFrames, tickToFrames(trigger.gateTime()));
            }
            mixedFrameCount = Math.max(mixedFrameCount, tickToFrames(trigger.startTick()) + clipFrames);
        }
        return mixedFrameCount;
    }

    private static void mixPcmClips(float[] mix,
                                    List<PcmClip> pcmClips,
                                    List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
        for (SMAFDecoder.PcmSequenceTrigger trigger : pcmTriggers) {
            int clipIndex = trigger.noteValue();
            if (clipIndex <= 0 || clipIndex >= pcmClips.size()) {
                continue;
            }
            PcmClip clip = pcmClips.get(clipIndex);
            if (clip == null) {
                continue;
            }
            int startFrame = tickToFrames(trigger.startTick());
            int clipFrames = clip.frameCount;
            if (trigger.gateTime() > 0) {
                clipFrames = Math.min(clipFrames, tickToFrames(trigger.gateTime()));
            }
            if (clipFrames <= 0) {
                continue;
            }
            float gain = Math.max(0.0f, Math.min(1.0f, trigger.velocity() / 127.0f));
            for (int frame = 0; frame < clipFrames; frame++) {
                int outputFrame = startFrame + frame;
                int outputIndex = outputFrame * OUTPUT_CHANNELS;
                int clipIndexBase = frame * OUTPUT_CHANNELS;
                mix[outputIndex] += clip.samples[clipIndexBase] * gain;
                mix[outputIndex + 1] += clip.samples[clipIndexBase + 1] * gain;
            }
        }
    }

    private static List<PcmClip> decodePcmClips(List<byte[]> pcmClipData) throws Exception {
        List<PcmClip> clips = new ArrayList<>(pcmClipData.size());
        for (byte[] pcmClip : pcmClipData) {
            clips.add(decodePcmClip(pcmClip));
        }
        return clips;
    }

    private static PcmClip decodePcmClip(byte[] clipData) throws Exception {
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
                return new PcmClip(samples, samples.length / OUTPUT_CHANNELS);
            }
        } catch (Exception exception) {
            Mobile.log(Mobile.LOG_WARNING,
                    "Unable to decode SMAF PCM clip: " + exception.getMessage());
            return null;
        }
    }

    private static float[] resampleToOutput(byte[] pcmBytes, AudioFormat pcmFormat) {
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
        if (Math.round(pcmFormat.getSampleRate()) == OUTPUT_SAMPLE_RATE && inputChannels == OUTPUT_CHANNELS) {
            return input;
        }
        int outputFrames = Math.max(1, Math.round(inputFrames * (OUTPUT_SAMPLE_RATE / pcmFormat.getSampleRate())));
        float[] output = new float[outputFrames * OUTPUT_CHANNELS];
        for (int frame = 0; frame < outputFrames; frame++) {
            float sourcePosition = frame * pcmFormat.getSampleRate() / OUTPUT_SAMPLE_RATE;
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

    private static int tickToFrames(long tick) {
        return (int) Math.max(0L, tick * FRAMES_PER_TICK);
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

    private record RenderEvent(long tick,
                               int order,
                               int priority,
                               int command,
                               int channel,
                               int data1,
                               int data2,
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
                    null);
        }

        private static RenderEvent sysEx(long tick, int order, byte[] sysEx) {
            return new RenderEvent(tick, order, 0, 0, 0, 0, 0, sysEx == null ? new byte[0] : sysEx.clone());
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
}
