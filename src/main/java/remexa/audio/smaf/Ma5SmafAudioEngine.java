package remexa.audio.smaf;

import remexa.audio.smaf.ma3.MA3SamplerProvider;
import remexa.audio.smaf.ma3.MA3Rom;
import remexa.audio.smaf.ma3.Sampler;
import remexa.audio.smaf.ma5.MA5PacketInventory;
import remexa.audio.smaf.ma5.MA5PcmVoiceProgram;
import remexa.audio.smaf.ma5.MA5PcmVoice;
import remexa.audio.smaf.ma5.MA5MixChannel;
import remexa.audio.smaf.ma5.MA5MixTables;
import remexa.audio.smaf.ma5.MA5PcmPitch;
import remexa.audio.smaf.ma5.MA5FmVoice;
import remexa.audio.smaf.ma5.MA5VoiceProgram;
import remexa.audio.smaf.ma5.MA5SoftbankBridge;
import remexa.audio.smaf.ma5.MA5WaveDataPacket;
import remexa.host.LaunchConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

final class Ma5SmafAudioEngine implements YamahaAudioEngine {
    /**
     * Output sample rate.
     * The chip natively supports {22050, 32000, 44100, 48000};
     */
    private static final int MA5_OUTPUT_SAMPLE_RATE =
            Integer.getInteger("remexa.ma5SampleRate", 48_000);
    private final SmafSequencedRenderer renderer;
    private final SmafSequencedRenderer phraseRenderer;

    Ma5SmafAudioEngine() {
        MA3SamplerProvider provider = new MA3SamplerProvider(
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.WAVE_DRUM_NONE);
        renderer = new SmafSequencedRenderer("MA5", sampleRate -> {
            Sampler sampler = provider.instance(sampleRate);
            return new Ma5Adapter(sampler, sampleRate);
        }, MA5_OUTPUT_SAMPLE_RATE, false, true);
        phraseRenderer = new SmafSequencedRenderer("MA5 phrase", sampleRate ->
                new Ma5Adapter(provider.instance(sampleRate), sampleRate, true), MA5_OUTPUT_SAMPLE_RATE, true);
    }

    @Override
    public String id() {
        return "ma5";
    }

    @Override
    public String label() {
        return "MA5 experimental";
    }

    @Override
    public SmafRenderedAudio render(SmafRenderContext context) throws Exception {
        MA5PacketInventory inventory = MA5PacketInventory.analyze(
                context.source(),
                context.startupPackets(),
                context.exclusiveVoices(),
                context.sequenceSysExEvents());
        inventory.log("ma5");
        return renderer(context).render(
                context.sequence(),
                context.sequenceSysExEvents(),
                YamahaAudioEngine.startupAndExclusivePackets(context),
                context.pcmClipData(),
                context.pcmTriggers());
    }

    @Override
    public SmafStreamingSession openStream(SmafRenderContext context) throws Exception {
        MA5PacketInventory inventory = MA5PacketInventory.analyze(
                context.source(),
                context.startupPackets(),
                context.exclusiveVoices(),
                context.sequenceSysExEvents());
        inventory.log("ma5");
        return renderer(context).openStream(
                context.sequence(),
                context.sequenceSysExEvents(),
                YamahaAudioEngine.startupAndExclusivePackets(context),
                context.pcmClipData(),
                context.pcmTriggers());
    }

    private SmafSequencedRenderer renderer(SmafRenderContext context) {
        byte[] source = context.source();
        if (source != null) for (int offset = 8; offset + 8 <= source.length;) {
            if (source[offset] == 'M' && source[offset + 1] == 'M'
                    && source[offset + 2] == 'M' && source[offset + 3] == 'G') return phraseRenderer;
            long length = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(source, offset + 4, 4).getInt());
            long next = offset + 8L + length;
            if (next > source.length) break;
            offset = (int)next;
        }
        return renderer;
    }

    private static final class Ma5Adapter implements SmafSynthAdapter, MA5SoftbankBridge.PcmVoiceSink {
        private static final int CHANNEL_COUNT = 16;
        private static final int INTERNAL_LEGACY_YAMAHA_MESSAGE = 0x72;
        private static final int INTERNAL_LEGACY_YAMAHA_SELECTOR = 0x06;
        private static final int INTERNAL_LEGACY_YAMAHA_MODULATION = 0x07;
        private static final int INTERNAL_LEGACY_YAMAHA_VOLUME = 0x08;
        private static final int INTERNAL_LEGACY_YAMAHA_PAN = 0x09;
        private static final int INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME = 0x0a;
        private static final int INTERNAL_LEGACY_YAMAHA_PROGRAM = 0x0b;
        private static final int INTERNAL_LEGACY_YAMAHA_BANK = 0x0c;
        private static final int INTERNAL_LEGACY_YAMAHA_EXPRESSION = 0x0d;
        private static final int INTERNAL_LEGACY_YAMAHA_PITCH = 0x11;
        private static final int MIDI_PERCUSSION_CHANNEL = 9;
        private static final float DEFAULT_PITCH_BEND_RANGE_SEMITONES = 2.0f;
        private static final float SEMITONES_PER_OCTAVE = 12.0f;
        private static final int MA5_DRUM_KEY_MIDI_BASE = 36;
        private static final int LEGACY_MELODIC_BANK_MSB = 0x7c;
        private static final int LEGACY_DRUM_BANK_MSB = 0x7d;
        private static final int[] AICA_STEPS = {230, 230, 230, 230, 307, 409, 512, 614};
        private static final int[] YM2608_STEPS = {57, 57, 57, 57, 77, 102, 128, 153};
        private static final boolean PCM_OVERLAY_ENABLED =
                Boolean.parseBoolean(System.getProperty("remexa.ma5PcmOverlay", "true"));
        private static final String PCM_DECODER =
                System.getProperty("remexa.ma5PcmDecoder", "yamaha").trim().toLowerCase();
        private static final boolean PCM_HIGH_NIBBLE_FIRST =
                Boolean.parseBoolean(System.getProperty("remexa.ma5PcmHighNibbleFirst", "false"));
        /**
         * Multiplier on the authored PCM sample rate.
         *
         * <p>MGS Mobile's one-shot MA-5 PCM voices use round values such as
         * 4000 and 6000, matching low-rate ADPCM sample rates rather than a
         * 16.16 phase increment. Tunable via
         * {@code -Dremexa.ma5PcmFreqScale=N} for future hardware calibration.</p>
         */
        private static final float PCM_FREQ_SCALE =
                Float.parseFloat(System.getProperty("remexa.ma5PcmFreqScale", "1.0"));
        /**
         * MIDI key at which a melodic PCM voice's frequencySetting represents
         * unity (natural-rate) playback. Above this key the voice plays
         * faster, below it slower. 60 (middle C) is the common Yamaha
         * convention; tunable via {@code remexa.ma5PcmReferenceKey} if a
         * particular bank was authored against a different center.
         */
        private static final int PCM_REFERENCE_KEY =
                Integer.parseInt(System.getProperty("remexa.ma5PcmReferenceKey", "60"));

        private final Sampler sampler;
        private final boolean phraseChannels;
        private final boolean[] phraseKeysDown = new boolean[CHANNEL_COUNT];
        private final int[] phraseVoiceTypes = new int[CHANNEL_COUNT];
        private final float sampleRate;
        private final MA5SoftbankBridge softbankBridge;
        private final float[] pitchBendSemitones = new float[CHANNEL_COUNT];
        private final float[] pitchBendRanges = new float[CHANNEL_COUNT];
        private final int[] channelBanks = new int[CHANNEL_COUNT];
        private final int[] channelPrograms = new int[CHANNEL_COUNT];
        private final boolean[] channelDrumBanks = new boolean[CHANNEL_COUNT];
        private final float[] channelVolumes = new float[CHANNEL_COUNT];
        private final float[] internalChannelVolumes = new float[CHANNEL_COUNT];
        private final float[] internalChannelExpressions = new float[CHANNEL_COUNT];
        private final float[] channelPans = new float[CHANNEL_COUNT];
        private final Map<Integer, List<MA5PcmVoiceProgram>> pcmPrograms = new HashMap<>();
        private final Map<Integer, MA5PcmVoiceProgram> pcmDrumPrograms = new HashMap<>();
        private final Set<Integer> fmDrumPrograms = new HashSet<>();
        private final Map<Integer, MA5VoiceProgram> fmPrograms = new HashMap<>();
        private final Map<Integer, MA5VoiceProgram> fmDrumVoices = new HashMap<>();
        private final Map<Integer, int[]> pcmWaves = new HashMap<>();
        private final List<PcmNote> pcmNotes = new ArrayList<>();
        private final List<FmNote> fmNotes = new ArrayList<>();
        private final int[] channelModulation = new int[CHANNEL_COUNT];
        private final MA5MixChannel[] mixChannels = new MA5MixChannel[CHANNEL_COUNT];
        private int phraseVolume = 127, phrasePanpot = 64;
        private long mixFrame, mixServiceTick, mixServiceEnd, nextMixControlFrame;


        private Ma5Adapter(Sampler sampler, float sampleRate) {
            this(sampler, sampleRate, false);
        }

        private Ma5Adapter(Sampler sampler, float sampleRate, boolean phraseChannels) {
            this.sampler = sampler;
            this.phraseChannels = phraseChannels;
            this.sampleRate = sampleRate;
            for (int i=0;i<CHANNEL_COUNT;i++) mixChannels[i]=new MA5MixChannel(Math.round(sampleRate));
            this.softbankBridge = new MA5SoftbankBridge(sampler, this, phraseChannels ? 1 : 0);
        }

        @Override
        public void reset() {
            sampler.reset();
            softbankBridge.reset();
            pcmPrograms.clear();
            pcmDrumPrograms.clear();
            fmDrumPrograms.clear();
            fmPrograms.clear();
            fmDrumVoices.clear();
            pcmWaves.clear();
            pcmNotes.clear();
            fmNotes.clear();
            Arrays.fill(phraseVoiceTypes, 0);
            mixFrame=mixServiceTick=mixServiceEnd=nextMixControlFrame=0;
            resetChannels(true);
        }

        @Override public void repeat() {
            if (!phraseChannels) { reset(); return; }
            // 100dea20 -> 100dc840/100deed0 issues All Sound Off and
            // resets the driver controls, without clearing physical LFOs.
            for (FmNote note : fmNotes) note.core.stop();
            for (PcmNote note : pcmNotes) note.core.stop();
            if (sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3)
                for (int channel = 0; channel < CHANNEL_COUNT; channel++) ma3.stopChannel(channel);
            resetChannels(false);
        }

        private void resetChannels(boolean initial) {
            Arrays.fill(phraseKeysDown, false);
            for (FmNote note : fmNotes) note.core.setModulation(0);
            for (PcmNote note : pcmNotes) { note.core.setModulation(0); note.core.hold(false); }
            Arrays.fill(channelModulation, 0);
            Arrays.fill(channelBanks, phraseChannels ? 1 : 0);
            Arrays.fill(channelPrograms, 0);
            Arrays.fill(channelDrumBanks, false);
            Arrays.fill(channelVolumes, 1.0f);
            Arrays.fill(internalChannelVolumes, phraseChannels ? 100/127f : 1f);
            Arrays.fill(internalChannelExpressions, 1.0f);
            Arrays.fill(channelPans, midiPanToFloat(phrasePanpot));
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                pitchBendSemitones[channel] = 0.0f;
                pitchBendRanges[channel] = DEFAULT_PITCH_BEND_RANGE_SEMITONES;
                sampler.drumEnable(channel, false);
                sampler.bankChange(channel, channelBanks[channel]);
                sampler.programChange(channel, 0);
                int value=phraseChannels ? 100 : 127;
                mixChannels[channel].reset(value,initial && phraseChannels ? 100 : phraseVolume,64);
                mixChannels[channel].volume(value,phraseVolume);
                mixChannels[channel].pan(phrasePanpot);
                sampler.volume(channel,MA5MixTables.channelLevel(value,phraseVolume)/32768f);
                sampler.panpot(channel,channelPans[channel]);
                sampler.modulation(channel, 0);
                sampler.pitchBendRange(channel, normalizePitchBendRange(DEFAULT_PITCH_BEND_RANGE_SEMITONES));
                sampler.pitchBend(channel, 0.0f);
            }
        }

        @Override
        public void drumEnable(int channel, boolean enable) {
            if (channel >= 0 && channel < CHANNEL_COUNT) channelDrumBanks[channel] = enable;
            sampler.drumEnable(channel, enable);
        }

        @Override
        public boolean isFinished() {
            return sampler.isFinished() && pcmNotes.stream().allMatch(n -> n.core.isFinished())
                    && fmNotes.stream().allMatch(n -> n.core.isFinished());
        }

        @Override
        public void keyOff(int channel, int key) {
            if (phraseChannels && channel >= 0 && channel < CHANNEL_COUNT) {
                phraseKeysDown[channel] = false;
                if (phraseVoiceTypes[channel] == 1)
                    for (FmNote note : fmNotes) if (note.channel == channel) note.core.keyOff();
                if (phraseVoiceTypes[channel] == 2)
                    for (PcmNote note : pcmNotes) if (note.channel == channel) note.beginRelease();
                if (phraseVoiceTypes[channel] == 3 && sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3)
                    ma3.keyOffPhrase(channel);
                return;
            }
            if (channel >= 0 && channel < CHANNEL_COUNT) phraseKeysDown[channel] = false;
            for (PcmNote note : pcmNotes) {
                if (note.channel == channel && note.key == key) {
                    note.beginRelease();
                }
            }
            for (FmNote note : fmNotes) if (note.channel == channel && note.key == key) note.core.keyOff();
            sampler.keyOff(channel, key);
        }

        @Override
        public void keyOn(int channel, int key, float velocity) {
            if (channel < 0 || channel >= CHANNEL_COUNT) return;
            boolean legato = phraseChannels && phraseKeysDown[channel] && !channelDrumBanks[channel];
            if (phraseChannels) {
                phraseKeysDown[channel] = true;
                if (legato) {
                    boolean pcmTarget = pcmProgram(channel, key) != null;
                    for (FmNote note : fmNotes) if (!pcmTarget && note.channel == channel) {
                        note.key = key; note.velocity = velocity;
                        note.frequency(pitchBendSemitones[channel]);
                        return;
                    }
                    for (PcmNote note : pcmNotes) if (pcmTarget && note.channel == channel) {
                        note.key = key; note.velocity = velocity;
                        note.frequency(pitchBendSemitones[channel]);
                        return;
                    }
                    if (!pcmTarget && phraseVoiceTypes[channel] == 3 && sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3) {
                        ma3.keyOnPhrase(channel, key, velocity, true);
                    }
                    return;
                }
                MA5VoiceProgram fm = fmProgram(channel, key);
                for (FmNote note : fmNotes) if (note.channel == channel
                        && !channelDrumBanks[channel] && note.program.equals(fm)) {
                    note.key = key; note.velocity = velocity;
                    note.frequency(pitchBendSemitones[channel]); note.core.keyOn();
                    phraseVoiceTypes[channel] = 1;
                    return;
                }
                MA5PcmVoiceProgram pcm = pcmProgram(channel, key);
                for (PcmNote note : pcmNotes) if (note.channel == channel
                        && !channelDrumBanks[channel] && note.voice.equals(pcm)) {
                    note.key = key; note.velocity = velocity;
                    note.frequency(pitchBendSemitones[channel]); note.core.keyOn();
                    phraseVoiceTypes[channel] = 2;
                    return;
                }
                if (channelDrumBanks[channel]) {
                    if (phraseVoiceTypes[channel] == 1)
                        for (FmNote note : fmNotes) if (note.channel == channel) note.core.stop();
                    if (phraseVoiceTypes[channel] == 2)
                        for (PcmNote note : pcmNotes) if (note.channel == channel) note.core.stop();
                    if (sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3) ma3.stopChannel(channel);
                }
            }
            MA5PcmVoiceProgram pcmVoice = pcmProgram(channel, key);
            int[] pcmWave = pcmWave(pcmVoice);
            if (PCM_OVERLAY_ENABLED && pcmVoice != null && pcmWave != null && pcmWave.length > 0) {
                PcmNote previous = null;
                if (phraseChannels) {
                    previous = pcmNotes.stream().filter(n -> n.channel == channel).findFirst().orElse(null);
                    pcmNotes.removeIf(note -> note.channel == channel);
                    phraseVoiceTypes[channel] = 2;
                }
                PcmNote note = new PcmNote(channel, key, velocity, pcmVoice, pcmWave, sampleRate);
                if (previous != null) note.core.reuseSlot(previous.core);
                note.frequency(pitchBendSemitones[channel]);
                note.core.setModulation(channelModulation[channel]);
                pcmNotes.add(note);
                return;
            }
            if (PCM_OVERLAY_ENABLED && pcmVoice != null) return; // Missing wave is not an FM voice.
            MA5VoiceProgram fmVoice = fmProgram(channel, key);
            if (fmVoice != null && MA5FmVoice.supports(fmVoice)) {
                FmNote previous = null;
                if (phraseChannels) {
                    previous = fmNotes.stream().filter(n -> n.channel == channel).findFirst().orElse(null);
                    fmNotes.removeIf(note -> note.channel == channel);
                    if (sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3) ma3.stopChannel(channel);
                    phraseVoiceTypes[channel] = 1;
                }
                for (FmNote note : fmNotes) if (note.channel == channel && note.key == key) note.core.keyOff();
                sampler.keyOff(channel, key);
                FmNote note = new FmNote(channel, key, velocity, fmVoice, Math.round(sampleRate));
                if (previous != null) note.core.reuseSlot(previous.core);
                note.frequency(pitchBendSemitones[channel]);
                note.core.setModulation(channelModulation[channel]);
                fmNotes.add(note);
                return;
            }
            if (channel >= 0 && channel < CHANNEL_COUNT && (channelDrumBanks[channel] || (!phraseChannels && channel == MIDI_PERCUSSION_CHANNEL))) {
                int drumKey = key + 69 - MA5_DRUM_KEY_MIDI_BASE;
                if (channelBanks[channel] != 0
                        && !fmDrumPrograms.contains(programKey(channelBanks[channel], drumKey))) return;
            }
            if (phraseChannels) fmNotes.removeIf(note -> note.channel == channel);
            if (phraseChannels && sampler instanceof remexa.audio.smaf.ma3.MA3Sampler ma3)
                ma3.keyOnPhrase(channel, key, velocity, legato && phraseVoiceTypes[channel] == 3);
            else sampler.keyOn(channel, key, velocity);
            if (phraseChannels) phraseVoiceTypes[channel] = 3;
        }

        @Override
        public void bankChange(int channel, int bank) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelBanks[channel] = bank & 0x7f;
            }
            sampler.bankChange(channel, bank);
        }

        @Override
        public void programChange(int channel, int program) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelPrograms[channel] = program & 0x7f;
            }
            sampler.programChange(channel, program);
        }

        @Override
        public void pitchBend(int channel, float semitones) {
            if (channel < 0 || channel >= CHANNEL_COUNT) {
                return;
            }
            pitchBendSemitones[channel] = semitones;
            for (FmNote note : fmNotes) if (note.channel == channel) note.frequency(semitones);
            for (PcmNote note : pcmNotes) if (note.channel == channel) note.frequency(semitones);
            sampler.pitchBend(channel, normalizePitchBend(semitones, pitchBendRanges[channel]));
        }

        @Override
        public void pitchBendRange(int channel, float range) {
            if (channel < 0 || channel >= CHANNEL_COUNT) {
                return;
            }
            float clampedRange = Math.max(0.0f, range);
            pitchBendRanges[channel] = clampedRange;
            sampler.pitchBendRange(channel, normalizePitchBendRange(clampedRange));
            sampler.pitchBend(channel, normalizePitchBend(pitchBendSemitones[channel], clampedRange));
        }

        @Override
        public void volume(int channel, float volume) { channelLevels(channel,volume,1); }

        @Override public void channelLevels(int channel, float volume, float expression) {
            if (channel < 0 || channel >= CHANNEL_COUNT) return;
            internalChannelVolumes[channel]=clampUnit(volume);
            internalChannelExpressions[channel]=clampUnit(expression);
            applyInternalEffectiveVolume(channel);
        }

        @Override public void phraseVolume(int value) {
            phraseVolume=Math.clamp(value,0,127);
            for (int ch=0;ch<CHANNEL_COUNT;ch++) applyInternalEffectiveVolume(ch);
        }

        @Override public void phrasePanpot(int value) {
            phrasePanpot=Math.clamp(value,0,127);
            for (int ch=0;ch<CHANNEL_COUNT;ch++) panpot(ch,midiPanToFloat(phrasePanpot));
        }

        @Override
        public void modulation(int channel, int value) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelModulation[channel] = Math.clamp(value, 0, 127);
                for (FmNote note : fmNotes) if (note.channel == channel) note.core.setModulation(value);
                for (PcmNote note : pcmNotes) if (note.channel == channel) note.core.setModulation(value);
            }
            sampler.modulation(channel, value);
        }

        @Override
        public void panpot(int channel, float panpot) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelPans[channel] = Math.max(-1.0f, Math.min(1.0f, panpot));
                mixChannels[channel].pan(Math.clamp(Math.round(64+panpot*63),0,127));
            }
            sampler.panpot(channel, panpot);
        }

        @Override
        public void render(float[] samples, int offset, int frames, float left, float right, boolean erase, boolean clamp) {
            int remaining=frames, target=offset;
            while (remaining > 0) {
                boolean latch=mixFrame >= nextMixControlFrame;
                if (latch) {
                    if (mixFrame >= mixServiceEnd) mixServiceEnd=++mixServiceTick*Math.round(sampleRate)/1000;
                    nextMixControlFrame=Math.min(mixServiceEnd,mixFrame+32);
                    for (MA5MixChannel channel : mixChannels) channel.latch();
                }
                int count=(int)Math.min(remaining,nextMixControlFrame-mixFrame);
                sampler.render(samples,target,count,left,right,erase,false);
                renderFmVoices(samples,target,count,left,right);
                renderPcmOverlay(samples,target,count,left,right,false);
                for (MA5MixChannel channel : mixChannels) channel.advance(count-(latch?1:0));
                mixFrame+=count; remaining-=count; target+=count*2;
            }
            if (clamp) for (int i = offset, end = offset + frames * 2; i < end; i++) {
                samples[i] = Math.clamp(samples[i], -1.0f, 1.0f);
            }
        }

        @Override
        public void sysEx(byte[] message) {
            sysEx(-1, message);
        }

        @Override
        public void sysEx(int sourceBank, byte[] message) {
            if (applyInternalSoftbankControl(sourceBank, message)) {
                return;
            }
            if (!softbankBridge.sysEx(message)) {
                sampler.sysEx(message);
            }
        }

        private boolean applyInternalSoftbankControl(int sourceBank, byte[] message) {
            if (message == null || message.length < 4
                    || (message[0] & 0xff) != INTERNAL_LEGACY_YAMAHA_MESSAGE) {
                return false;
            }
            int command = message[1] & 0xff;
            int logicalChannel = logicalChannel(sourceBank, message[2] & 0xff);
            int rawValue = message[3] & 0xff;
            int value = rawValue & 0x7f;
            switch (command) {
                case 0x0e -> {
                    if (phraseChannels) {
                        phraseKeysDown[logicalChannel] = false;
                        for (FmNote note : fmNotes) if (note.channel == logicalChannel) note.core.keyOff();
                        for (PcmNote note : pcmNotes) if (note.channel == logicalChannel) note.core.keyOff();
                        for (int key = -69; key < 59; key++) sampler.keyOff(logicalChannel, key);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_SELECTOR, INTERNAL_LEGACY_YAMAHA_PROGRAM -> {
                    programChange(logicalChannel, value);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        programChange(MIDI_PERCUSSION_CHANNEL, value);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_BANK -> {
                    boolean drumBank = (rawValue & 0x80) != 0;
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT) {
                        channelDrumBanks[logicalChannel] = drumBank;
                    }
                    sampler.drumEnable(logicalChannel, drumBank);
                    bankChange(logicalChannel, value);
                    if (!phraseChannels && logicalChannel != MIDI_PERCUSSION_CHANNEL && drumBank) {
                        channelDrumBanks[MIDI_PERCUSSION_CHANNEL] = true;
                        sampler.drumEnable(MIDI_PERCUSSION_CHANNEL, true);
                        bankChange(MIDI_PERCUSSION_CHANNEL, value);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_MODULATION -> {
                    modulation(logicalChannel, value);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        modulation(MIDI_PERCUSSION_CHANNEL, value);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_VOLUME, INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME -> {
                    float normalized = value / 127.0f;
                    internalVolume(logicalChannel, normalized);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        internalVolume(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_EXPRESSION -> {
                    float normalized = value / 127.0f;
                    internalExpression(logicalChannel, normalized);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        internalExpression(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_PAN -> {
                    float normalized = midiPanToFloat(value);
                    panpot(logicalChannel, normalized);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        panpot(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_PITCH -> {
                    float semitones = centeredLegacyPitchBend(value, logicalChannel);
                    pitchBend(logicalChannel, semitones);
                    if (!phraseChannels && logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        pitchBend(MIDI_PERCUSSION_CHANNEL, semitones);
                    }
                }
                default -> {
                    return true;
                }
            }
            return true;
        }

        private static int logicalChannel(int sourceBank, int channelByte) {
            int channel = channelByte & 0x0f;
            if (sourceBank >= 0) {
                return ((sourceBank & 0x03) << 2) | (channel & 0x03);
            }
            return channel;
        }

        private void internalVolume(int channel, float volume) {
            if (channel < 0 || channel >= CHANNEL_COUNT) {
                return;
            }
            internalChannelVolumes[channel] = clampUnit(volume);
            applyInternalEffectiveVolume(channel);
        }

        private void internalExpression(int channel, float expression) {
            if (channel < 0 || channel >= CHANNEL_COUNT) {
                return;
            }
            internalChannelExpressions[channel] = clampUnit(expression);
            applyInternalEffectiveVolume(channel);
        }

        private void applyInternalEffectiveVolume(int channel) {
            int volume=Math.clamp(Math.round(internalChannelVolumes[channel]*127),0,127);
            int expression=MA5MixTables.combinedLevel(Math.round(internalChannelExpressions[channel]*127),phraseVolume);
            mixChannels[channel].volume(volume,expression);
            channelVolumes[channel]=MA5MixTables.channelLevel(volume,expression)/32768f;
            sampler.volume(channel,channelVolumes[channel]);
        }

        private static float clampUnit(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }

        @Override
        public void onWaveData(MA5WaveDataPacket waveData) {
            pcmWaves.put(waveData.waveId(),
                    decodePcmWave(waveData.encodedData(), 0, waveData.encodedData().length));
        }

        @Override
        public void onPcmVoice(MA5PcmVoiceProgram voice) {
            if (voice.drumVoice()) {
                fmDrumPrograms.remove(programKey(voice.bankLsb(), voice.program()));
                fmDrumVoices.remove(programKey(voice.bankLsb(), voice.program()));
                pcmDrumPrograms.put(programKey(voice.bankLsb(), voice.program()), voice);
            } else {
                List<MA5PcmVoiceProgram> programs = pcmPrograms.computeIfAbsent(
                        programKey(voice.bankLsb(), voice.program()), ignored -> new ArrayList<>());
                programs.removeIf(previous -> Arrays.equals(previous.payload(), voice.payload()));
                programs.addFirst(voice);
            }
        }

        @Override
        public void onFmVoice(MA5VoiceProgram voice) {
            int key = programKey(voice.bankLsb(), voice.program());
            if (voice.drumVoice()) {
                pcmDrumPrograms.remove(key);
                fmDrumPrograms.add(key);
                fmDrumVoices.put(key, voice);
            } else {
                pcmPrograms.remove(key);
                fmPrograms.put(key, voice);
            }
        }

        private MA5VoiceProgram fmProgram(int channel, int key) {
            if (channel < 0 || channel >= CHANNEL_COUNT) return null;
            return channelDrumBanks[channel] || (!phraseChannels && channel == MIDI_PERCUSSION_CHANNEL)
                    ? fmDrumVoices.get(programKey(channelBanks[channel], key + 69 - MA5_DRUM_KEY_MIDI_BASE))
                    : fmPrograms.get(programKey(channelBanks[channel], channelPrograms[channel]));
        }

        private void renderFmVoices(float[] samples, int offset, int frames, float left, float right) {
            for (Iterator<FmNote> it = fmNotes.iterator(); it.hasNext();) {
                FmNote note = it.next();
                MA5MixChannel channel=mixChannels[note.channel];
                int level=MA5MixTables.noteLevel(Math.round(note.velocity*127),false);
                int gainLeft=MA5MixTables.gain(level,channel.volumeLevel(),channel.panLevel(false),
                        note.program.panpot(),note.program.panpotEnable(),false,false);
                int gainRight=MA5MixTables.gain(level,channel.volumeLevel(),channel.panLevel(true),
                        note.program.panpot(),note.program.panpotEnable(),false,true);
                for (int i = 0; i < frames; i++) {
                    int value = note.core.nextSample();
                    samples[offset + i * 2] += ((long)value*gainLeft >> 15)*(left/32768f);
                    samples[offset + i * 2 + 1] += ((long)value*gainRight >> 15)*(right/32768f);
                    if (!phraseChannels && note.core.isFinished()) break;
                }
                if (!phraseChannels && note.core.isFinished()) it.remove();
            }
        }

        private static final class FmNote {
            private final int channel;
            private int key;
            private float velocity;
            private final MA5VoiceProgram program;
            private final MA5FmVoice core;

            private FmNote(int channel, int key, float velocity, MA5VoiceProgram program, int sampleRate) {
                this.channel = channel;
                this.key = key;
                this.velocity = velocity;
                this.program = program;
                this.core = new MA5FmVoice(program, sampleRate);
            }

            private void frequency(float bend) {
                double midi = program.drumVoice() ? program.drumKey() : key + 69 + bend;
                core.setFrequency(440 * Math.pow(2, (midi - 69) / 12));
            }
        }

        private MA5PcmVoiceProgram pcmProgram(int channel, int key) {
            if (!PCM_OVERLAY_ENABLED || channel < 0 || channel >= CHANNEL_COUNT) {
                return null;
            }
            int midiNote = key + 69;
            if (channelDrumBanks[channel] || (!phraseChannels && channel == MIDI_PERCUSSION_CHANNEL)) {
                int drumKey = midiNote - MA5_DRUM_KEY_MIDI_BASE;
                if (fmDrumPrograms.contains(programKey(channelBanks[channel], drumKey))) return null;
                MA5PcmVoiceProgram drumVoice = pcmDrumPrograms.get(programKey(channelBanks[channel], drumKey));
                if (drumVoice != null) {
                    return drumVoice;
                }
                drumVoice = pcmDrumPrograms.get(programKey(LEGACY_DRUM_BANK_MSB, drumKey));
                if (drumVoice != null) {
                    return drumVoice;
                }
                // Missing percussion must never select a melodic PCM program.
                return channelBanks[channel] == 0 ? MA5PcmVoiceProgram.presetDrum(midiNote) : null;
            }
            MA5PcmVoiceProgram voice = pcmProgramForKey(channelBanks[channel], channelPrograms[channel], midiNote);
            if (voice != null) {
                return voice;
            }
            voice = pcmProgramForKey(LEGACY_MELODIC_BANK_MSB, channelPrograms[channel], midiNote);
            if (voice != null) {
                return voice;
            }
            return pcmProgramForKey(0, channelPrograms[channel], midiNote);
        }

        private MA5PcmVoiceProgram pcmProgramForKey(int bank, int program, int midiNote) {
            List<MA5PcmVoiceProgram> voices = pcmPrograms.get(programKey(bank, program));
            if (voices == null || voices.isEmpty()) {
                return null;
            }
            for (MA5PcmVoiceProgram voice : voices) {
                if (midiNote >= voice.keyLow() && midiNote <= voice.keyHigh()) {
                    return voice;
                }
            }
            return null;
        }

        private int[] pcmWave(MA5PcmVoiceProgram voice) {
            if (!PCM_OVERLAY_ENABLED || voice == null) {
                return null;
            }
            if (!voice.romWave()) return pcmWaves.get(voice.waveId());
            return voice.waveId() < PcmRom.WAVES.length ? PcmRom.WAVES[voice.waveId()] : null;
        }

        private static final class PcmRom {
            private static final int[][] WAVES = load();

            private static int[][] load() {
                int[][] waves = new int[7][];
                for (int i = 0; i < waves.length; i++) {
                    byte[] data = MA3Rom.MA3_WAVEROM.bytes(i);
                    // Encoded bytes match the MA5 DLL. Decode without MA3
                    // predictor leakage, using native integer bitwise deltas.
                    waves[i] = decodeYamaha(data, 0, data.length);
                }
                return waves;
            }
        }

        private void renderPcmOverlay(float[] samples, int offset, int frames,
                                      float left, float right, boolean clamp) {
            for (Iterator<PcmNote> it = pcmNotes.iterator(); it.hasNext();) {
                PcmNote note = it.next();
                MA5MixChannel channel=mixChannels[note.channel];
                int level=MA5MixTables.noteLevel(Math.round(note.velocity*127),false);
                int gainLeft=MA5MixTables.gain(level,channel.volumeLevel(),channel.panLevel(false),
                        note.voice.panpot(),note.voice.panpotEnable(),false,false);
                int gainRight=MA5MixTables.gain(level,channel.volumeLevel(),channel.panLevel(true),
                        note.voice.panpot(),note.voice.panpotEnable(),false,true);
                float trim=pcmGain();
                for (int i = 0; i < frames; i++) {
                    int value = note.core.nextSample();
                    samples[offset + i * 2] += (((value*gainLeft) >> 15)<<2)*(left*trim/32768f);
                    samples[offset + i * 2 + 1] += (((value*gainRight) >> 15)<<2)*(right*trim/32768f);
                    if (!phraseChannels && note.core.isFinished()) break;
                }
                if (!phraseChannels && note.core.isFinished()) it.remove();
            }
        }

        private static int programKey(int bank, int program) {
            return (bank & 0x7f) << 8 | (program & 0x7f);
        }

        private static float pcmGain() {
            return LaunchConfig.resolveConfiguredPcmMixGain(LaunchConfig.MA5_PCM_GAIN_PROPERTY);
        }

        private static int[] decodePcmWave(byte[] adpcm, int offset, int length) {
            return switch (PCM_DECODER) {
                case "aica" -> decodeAica(adpcm, offset, length);
                case "yamaha" -> decodeYamaha(adpcm, offset, length);
                default -> decodeYm2608(adpcm, offset, length);
            };
        }

        private static long nativePcmRegister(float frequency) {
            double ideal = Math.max(0, frequency) * 65536.0 / 48000.0;
            int shift = Math.max(0, Math.getExponent(ideal) - 10);
            long quantum = 1L << Math.min(shift, 30);
            return (long) (ideal / quantum) * quantum;
        }

        private static int[] decodeAica(byte[] adpcm, int offset, int length) {
            int[] decoded = new int[length * 2];
            int step = 127;
            int predictor = 0;
            for (int src = offset, dest = 0; src < offset + length; src++) {
                for (int nibble = 0; nibble < 2; nibble++, dest++) {
                    int code = nibble(adpcm[src], nibble);
                    int magnitude = Math.min(Math.max((((code & 7) << 1) | 1) * step >> 3, 0), 32767);
                    int sign = 1 - ((code & 8) >> 2);
                    predictor = Math.min(Math.max(sign * magnitude + predictor * 254 / 255, -32768), 32767);
                    decoded[dest] = predictor;
                    step = Math.min(Math.max(AICA_STEPS[code & 7] * step >> 8, 127), 24576);
                }
            }
            return decoded;
        }

        private static int[] decodeYm2608(byte[] adpcm, int offset, int length) {
            int[] decoded = new int[length * 2];
            long step = 127;
            long predictor = 0;
            int count = 0;
            for (int src = offset, dest = 0; src < offset + length; src++) {
                for (int index = 0; index < 2; index++, dest++, count++) {
                    int code = nibble(adpcm[src], index);
                    long delta = ((code & 7) * 2L + 1L) * step / 8L;
                    predictor += (code & 8) != 0 ? -delta : delta;
                    predictor = Math.max(-32768, Math.min(32767, predictor));
                    decoded[dest] = (int) predictor;
                    step = step * YM2608_STEPS[code & 7] / 64L;
                    step = Math.max(127, Math.min(24576, step));
                    if ((count + 1) % 1024 == 0) {
                        step = 127;
                        predictor = 0;
                    }
                }
            }
            return decoded;
        }

        private static int[] decodeYamaha(byte[] adpcm, int offset, int length) {
            int[] decoded = new int[length * 2];
            int step = 127;
            int predictor = 0;
            for (int src = offset, dest = 0; src < offset + length; src++) {
                for (int index = 0; index < 2; index++, dest++) {
                    int code = nibble(adpcm[src], index);
                    int diff = step / 8;
                    if ((code & 0x01) != 0) {
                        diff += step / 4;
                    }
                    if ((code & 0x02) != 0) {
                        diff += step / 2;
                    }
                    if ((code & 0x04) != 0) {
                        diff += step;
                    }
                    predictor += (code & 0x08) != 0 ? -diff : diff;
                    predictor = Math.max(-32768, Math.min(32767, predictor));
                    decoded[dest] = predictor;
                    step = adjustYamahaStep(code, step);
                }
            }
            return decoded;
        }

        private static int adjustYamahaStep(int code, int step) {
            // Matches the MA-5 hardware ADPCM path in M5_EmuHw sub_10013350.
            step = switch (code & 0x07) {
                case 0, 1, 2, 3 -> step * 115 / 128;
                case 4 -> step * 307 / 256;
                case 5 -> step * 409 / 256;
                case 6 -> step * 2;
                default -> step * 307 / 128;
            };
            return Math.max(127, Math.min(24576, step));
        }

        private static int nibble(byte value, int index) {
            int bits = value & 0xff;
            if (PCM_HIGH_NIBBLE_FIRST) {
                return index == 0 ? (bits >> 4) & 0x0f : bits & 0x0f;
            }
            return index == 0 ? bits & 0x0f : (bits >> 4) & 0x0f;
        }

        private static float midiPanToFloat(int value) {
            return Math.max(-1.0f, Math.min(1.0f, (value - 64.0f) / 63.0f));
        }

        private static float normalizePitchBend(float semitones, float rangeSemitones) {
            if (rangeSemitones <= 0.0f) {
                return 0.0f;
            }
            return semitones / rangeSemitones;
        }

        private float centeredLegacyPitchBend(int value, int channel) {
            int clampedChannel = channel >= 0 && channel < CHANNEL_COUNT ? channel : 0;
            float normalized = value >= 127 ? 1.0f : (value - 64.0f) / 64.0f;
            return normalized * pitchBendRanges[clampedChannel];
        }

        private static float normalizePitchBendRange(float rangeSemitones) {
            return rangeSemitones / SEMITONES_PER_OCTAVE;
        }

        private static final class PcmNote {
            private final int channel;
            private int key;
            private float velocity;
            private final MA5PcmVoiceProgram voice;
            private final MA5PcmVoice core;
            private final float sourceFrequency;

            private PcmNote(int channel, int key, float velocity, MA5PcmVoiceProgram voice,
                            int[] wave, float outputSampleRate) {
                this.channel = channel;
                this.key = key;
                this.velocity = velocity;
                this.voice = voice;
                sourceFrequency = voice.frequencySetting() * PCM_FREQ_SCALE;
                core = new MA5PcmVoice(voice, wave, Math.round(outputSampleRate));
            }

            private void frequency(float bend) {
                if (voice.drumVoice()) core.setFrequencyRegister((int)nativePcmRegister(sourceFrequency));
                else core.setFrequencyRegister(MA5PcmPitch.register(Math.round(sourceFrequency),voice.program(),key+69+60-PCM_REFERENCE_KEY+bend));
            }

            private void beginRelease() { core.keyOff(); }

        }
    }
}
