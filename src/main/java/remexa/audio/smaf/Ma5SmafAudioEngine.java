package remexa.audio.smaf;

import remexa.audio.smaf.ma3.MA3SamplerProvider;
import remexa.audio.smaf.ma3.MA3Rom;
import remexa.audio.smaf.ma3.Sampler;
import remexa.audio.smaf.ma5.MA5PacketInventory;
import remexa.audio.smaf.ma5.MA5PcmVoiceProgram;
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

    Ma5SmafAudioEngine() {
        MA3SamplerProvider provider = new MA3SamplerProvider(
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.WAVE_DRUM_NONE);
        renderer = new SmafSequencedRenderer("MA5", sampleRate -> {
            Sampler sampler = provider.instance(sampleRate);
            return new Ma5Adapter(sampler, sampleRate);
        }, MA5_OUTPUT_SAMPLE_RATE);
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
        return renderer.render(
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
        return renderer.openStream(
                context.sequence(),
                context.sequenceSysExEvents(),
                YamahaAudioEngine.startupAndExclusivePackets(context),
                context.pcmClipData(),
                context.pcmTriggers());
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
        private static final boolean PCM_ENVELOPE_ENABLED =
                Boolean.parseBoolean(System.getProperty("remexa.ma5PcmEnvelope", "true"));
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
        private static final int[] PCM_SEMITONE_Q15 = {
                0x8000, 0x78D7, 0x7215, 0x6BB3, 0x65AD, 0x5FFD,
                0x5A9E, 0x558C, 0x50C3, 0x4C3F, 0x47FB, 0x43F4, 0x4027
        };
        private static final double PCM_EPSILON = 1.0 / 32768.0;

        private static final int[] PCM_TOTAL_LEVEL_Q15 = {
                0x7fff, 0x7569, 0x6bb3, 0x62ca, 0x5a9e, 0x531f, 0x4c3f, 0x45f0,
                0x4027, 0x3ad8, 0x35fa, 0x3183, 0x2d6b, 0x29a9, 0x2637, 0x230d,
                0x2027, 0x1d7e, 0x1b0d, 0x18d1, 0x16c3, 0x14e1, 0x1327, 0x1191,
                0x101d, 0xec8, 0xd8f, 0xc70, 0xb68, 0xa77, 0x999, 0x8ce,
                0x814, 0x768, 0x6cc, 0x63c, 0x5b8, 0x53f, 0x4d0, 0x46a,
                0x40c, 0x3b6, 0x368, 0x320, 0x2de, 0x2a1, 0x269, 0x236,
                0x207, 0x1dc, 0x1b5, 0x191, 0x170, 0x151, 0x135, 0x11c,
                0x104, 0xef, 0xdb, 0xc9, 0xb8, 0xa9, 0x9b, 0x8e
        };
        private static final long[] PCM_SUSTAIN_LEVEL_Q31 = {
                0x80000000L, 0x5a9df7acL, 0x4026e73dL, 0x2d6a866fL,
                0x2026f310L, 0x16c310e3L, 0x101d3f2eL, 0xb68737aL,
                0x8138562L, 0x5b7b15bL, 0x40c3714L, 0x2dd958aL,
                0x207567aL, 0x16fa9bbL, 0x1044915L, 0xbbccL
        };

        /**
         * MIDI key at which a melodic PCM voice's frequencySetting represents
         * unity (natural-rate) playback. Above this key the voice plays
         * faster, below it slower. 60 (middle C) is the common Yamaha
         * convention; tunable via {@code remexa.ma5PcmReferenceKey} if a
         * particular bank was authored against a different center.
         */
        private static final int PCM_REFERENCE_KEY =
                Integer.parseInt(System.getProperty("remexa.ma5PcmReferenceKey", "60"));

        /**
         * Per-tick envelope rate coefficients at 32 kHz, transcribed verbatim
         * from {@code M5_EmuHw.dll} at {@code 0x100292E0} (the table base
         * stored at struct {@code +0x1934} by {@code sub_10006a90} when
         * sample rate is 32000). The chip's PCM voice generator
         * ({@code sub_100129a0}) multiplies the Q30 envelope value by this
         * factor once per output sample in decay/sustain/release stages.
         *
         * <p>The SDK stores these as Q30 coefficients; {@code 0x40000000}
         * means unity. A previous transcription used the decompiler's IEEE
         * float view of those same bit patterns, which made low and mid PCM
         * rates behave like no-ops. Rate fields (0..15) map to
         * {@code idx = rate == 0 ? 0 : (rate << 2) + (keycode >> 2)}.</p>
         */
        private static final float[] HW_RATE_TABLE_32K = {
                1.000000000f, 1.000000000f, 1.000000000f, 1.000000000f,
                0.999991969f, 0.999989961f, 0.999987954f, 0.999985946f,
                0.999983938f, 0.999979923f, 0.999975908f, 0.999971893f,
                0.999967878f, 0.999959847f, 0.999951816f, 0.999943786f,
                0.999935756f, 0.999919696f, 0.999903635f, 0.999887575f,
                0.999871516f, 0.999839398f, 0.999807280f, 0.999775163f,
                0.999743048f, 0.999678819f, 0.999614596f, 0.999550377f,
                0.999486159f, 0.999357742f, 0.999229349f, 0.999100969f,
                0.998972598f, 0.998715922f, 0.998459293f, 0.998202697f,
                0.997946188f, 0.997433394f, 0.996920959f, 0.996408818f,
                0.995896846f, 0.994873376f, 0.993851964f, 0.992830533f,
                0.991809523f, 0.989773034f, 0.987739475f, 0.985712468f,
                0.983686130f, 0.979656864f, 0.975629270f, 0.971629068f,
                0.967638401f, 0.959703248f, 0.951852473f, 0.944109894f,
                0.936324075f, 0.921123638f, 0.906023131f, 0.891343493f,
                0.876929895f, 0.876929895f, 0.876929895f, 0.876929895f,
        };

        /**
         * The 32 kHz table is applied once per PCM generator sample by
         * {@code sub_100129a0}. At other output rates we preserve wall-clock
         * decay by raising the coefficient to a fractional power.
         */
        private static final int HW_RATE_TABLE_SAMPLE_RATE = 32_000;

        // Q31 attack increments read from the native PCM engine's +0x1019dc
        // table at 32 kHz. AR=15 is instantaneous at every output rate.
        private static final long[] HW_ATTACK_TABLE_32K = {
                0, 0, 0, 0, 0x5555, 0x6aab, 0x8000, 0x9555,
                0xaaab, 0xd555, 0x10000, 0x12aab, 0x15555, 0x1aaab, 0x1ffff, 0x25554,
                0x2aaa8, 0x35557, 0x40004, 0x4aaaf, 0x5555a, 0x6aa9f, 0x80007, 0x9555e,
                0xaaa8f, 0xd5579, 0x10000f, 0x12aabc, 0x15551e, 0x1aaa07, 0x1ffeca, 0x255578,
                0x2aaa3b, 0x3557bc, 0x4002e1, 0x4aa3b9, 0x555476, 0x6aaf78, 0x7ff090, 0x95644e,
                0xaa8342, 0xd52413, 0x10035fa, 0x12ac89d, 0x1550685, 0x1aa4826, 0x2006bf3, 0x253c473,
                0x2aa0d0a, 0x354904c, 0x3fb8f88, 0x4a788e5, 0x9a86f42, 0xc128b13, 0xe4eddb8, 0x10cbe2e3,
                0x1350de85, 0x18251626, 0x1db77db9, 0x203172dd, 0x80000000L, 0x80000000L, 0x80000000L, 0x80000000L
        };

        /**
         * LFO frequency in Hz for the 2-bit {@code lfo} index, decoded
         * directly from the chip's per-rate Q32 phase-increment tables in
         * {@code M5_EmuHw.dll}: 48 kHz at {@code 0x100fa8ac}, 44.1 kHz at
         * {@code 0x100fa8bc}, 32 kHz at {@code 0x100fa8cc}, 22.05 kHz at
         * {@code 0x100fa8dc}. All four tables encode the same wall-clock
         * frequencies (the chip compensates the Q32 step value for each
         * output rate), so these Hz values apply unchanged at any output
         * rate.
         *
         * <p>At 48 kHz, the raw table values
         * {@code 0x00029819 0x0005BC02 0x0008541B 0x0009D495}
         * decode to {@code 1.894, 4.205, 6.099, 7.199 Hz}.</p>
         *
         * <p>Used when {@code vibratoEnabled} or {@code amplitudeModEnabled}
         * is set on the PCM voice.</p>
         */
        private static final float[] LFO_FREQ_HZ = {1.90f, 4.20f, 6.10f, 7.20f};

        /**
         * 4096-entry sine LUT covering [0, 2π). Used by the per-sample LFO
         * lookup in PCM rendering to avoid {@code Math.sin} on the audio
         * thread; with vibrato + AM both modulating from the same {@code
         * lfoPhase} every output sample, two {@code Math.sin} calls per
         * sample per active PCM note pushed past the chunk budget at higher
         * output rates. ~0.0015 rad precision is well within audible LFO
         * resolution.
         */
        private static final int SIN_LUT_SIZE = 4096;
        private static final int SIN_LUT_MASK = SIN_LUT_SIZE - 1;
        private static final float SIN_LUT_SCALE = (float) (SIN_LUT_SIZE / (2.0 * Math.PI));
        private static final float[] SIN_LUT = buildSinLut();

        private static float[] buildSinLut() {
            float[] lut = new float[SIN_LUT_SIZE];
            for (int i = 0; i < SIN_LUT_SIZE; i++) {
                lut[i] = (float) Math.sin(2.0 * Math.PI * i / SIN_LUT_SIZE);
            }
            return lut;
        }

        private static float fastSin(float radians) {
            int idx = ((int) (radians * SIN_LUT_SCALE)) & SIN_LUT_MASK;
            return SIN_LUT[idx];
        }

        private final Sampler sampler;
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

        private Ma5Adapter(Sampler sampler, float sampleRate) {
            this.sampler = sampler;
            this.sampleRate = sampleRate;
            this.softbankBridge = new MA5SoftbankBridge(sampler, this);
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
            Arrays.fill(channelModulation, 0);
            Arrays.fill(channelBanks, 0);
            Arrays.fill(channelPrograms, 0);
            Arrays.fill(channelDrumBanks, false);
            Arrays.fill(channelVolumes, 1.0f);
            Arrays.fill(internalChannelVolumes, 1.0f);
            Arrays.fill(internalChannelExpressions, 1.0f);
            Arrays.fill(channelPans, 0.0f);
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                pitchBendSemitones[channel] = 0.0f;
                pitchBendRanges[channel] = DEFAULT_PITCH_BEND_RANGE_SEMITONES;
                sampler.drumEnable(channel, false);
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
            return sampler.isFinished() && pcmNotes.isEmpty() && fmNotes.isEmpty();
        }

        @Override
        public void keyOff(int channel, int key) {
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
            MA5PcmVoiceProgram pcmVoice = pcmProgram(channel, key);
            int[] pcmWave = pcmWave(pcmVoice);
            if (PCM_OVERLAY_ENABLED && pcmVoice != null && pcmWave != null && pcmWave.length > 0) {
                pcmNotes.add(new PcmNote(channel, key, velocity, pcmVoice, pcmWave, sampleRate));
                return;
            }
            if (PCM_OVERLAY_ENABLED && pcmVoice != null) return; // Missing wave is not an FM voice.
            MA5VoiceProgram fmVoice = fmProgram(channel, key);
            if (fmVoice != null && MA5FmVoice.supports(fmVoice)) {
                for (FmNote note : fmNotes) if (note.channel == channel && note.key == key) note.core.keyOff();
                sampler.keyOff(channel, key);
                FmNote note = new FmNote(channel, key, velocity, fmVoice, Math.round(sampleRate));
                note.frequency(pitchBendSemitones[channel]);
                note.core.setModulation(channelModulation[channel]);
                fmNotes.add(note);
                return;
            }
            if (channel >= 0 && channel < CHANNEL_COUNT && (channelDrumBanks[channel] || channel == MIDI_PERCUSSION_CHANNEL)) {
                int drumKey = key + 69 - MA5_DRUM_KEY_MIDI_BASE;
                if (channelBanks[channel] != 0
                        && !fmDrumPrograms.contains(programKey(channelBanks[channel], drumKey))) return;
            }
            sampler.keyOn(channel, key, velocity);
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
        public void volume(int channel, float volume) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelVolumes[channel] = Math.max(0.0f, volume);
            }
            sampler.volume(channel, volume);
        }

        @Override
        public void modulation(int channel, int value) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelModulation[channel] = Math.clamp(value, 0, 127);
                for (FmNote note : fmNotes) if (note.channel == channel) note.core.setModulation(value);
            }
            sampler.modulation(channel, value);
        }

        @Override
        public void panpot(int channel, float panpot) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelPans[channel] = Math.max(-1.0f, Math.min(1.0f, panpot));
            }
            sampler.panpot(channel, panpot);
        }

        @Override
        public void render(float[] samples, int offset, int frames, float left, float right, boolean erase, boolean clamp) {
            sampler.render(samples, offset, frames, left, right, erase, false);
            renderFmVoices(samples, offset, frames, left, right);
            renderPcmOverlay(samples, offset, frames, left, right, false);
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
                case INTERNAL_LEGACY_YAMAHA_SELECTOR, INTERNAL_LEGACY_YAMAHA_PROGRAM -> {
                    programChange(logicalChannel, value);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
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
                    if (logicalChannel != MIDI_PERCUSSION_CHANNEL && drumBank) {
                        channelDrumBanks[MIDI_PERCUSSION_CHANNEL] = true;
                        sampler.drumEnable(MIDI_PERCUSSION_CHANNEL, true);
                        bankChange(MIDI_PERCUSSION_CHANNEL, value);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_MODULATION -> {
                    modulation(logicalChannel, value);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        modulation(MIDI_PERCUSSION_CHANNEL, value);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_VOLUME, INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME -> {
                    float normalized = value / 127.0f;
                    internalVolume(logicalChannel, normalized);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        internalVolume(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_EXPRESSION -> {
                    float normalized = value / 127.0f;
                    internalExpression(logicalChannel, normalized);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        internalExpression(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_PAN -> {
                    float normalized = midiPanToFloat(value);
                    panpot(logicalChannel, normalized);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        panpot(MIDI_PERCUSSION_CHANNEL, normalized);
                    }
                }
                case INTERNAL_LEGACY_YAMAHA_PITCH -> {
                    float semitones = centeredLegacyPitchBend(value, logicalChannel);
                    pitchBend(logicalChannel, semitones);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
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
            volume(channel, internalChannelVolumes[channel] * internalChannelExpressions[channel]);
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
                pcmPrograms.computeIfAbsent(programKey(voice.bankLsb(), voice.program()), ignored -> new ArrayList<>())
                        .addFirst(voice);
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
            return channelDrumBanks[channel] || channel == MIDI_PERCUSSION_CHANNEL
                    ? fmDrumVoices.get(programKey(channelBanks[channel], key + 69 - MA5_DRUM_KEY_MIDI_BASE))
                    : fmPrograms.get(programKey(channelBanks[channel], channelPrograms[channel]));
        }

        private void renderFmVoices(float[] samples, int offset, int frames, float left, float right) {
            for (Iterator<FmNote> it = fmNotes.iterator(); it.hasNext();) {
                FmNote note = it.next();
                // Retain the existing host FM pan/gain staging. Native output
                // filtering and final mixed gain are calibrated separately.
                float voiceRight = note.program.panpot() / (note.program.panpot() <= 15 ? 30.0f : 31.0f);
                float channelRight = (channelPans[note.channel] + 1) * .5f;
                float gain = channelVolumes[note.channel] * note.velocity / 65536.0f;
                float gainLeft = left * (1 - voiceRight) * (1 - channelRight) * gain;
                float gainRight = right * voiceRight * channelRight * gain;
                for (int i = 0; i < frames; i++) {
                    int value = note.core.nextSample();
                    samples[offset + i * 2] += value * gainLeft;
                    samples[offset + i * 2 + 1] += value * gainRight;
                    if (note.core.isFinished()) break;
                }
                if (note.core.isFinished()) it.remove();
            }
        }

        private static final class FmNote {
            private final int channel;
            private final int key;
            private final float velocity;
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
            if (channelDrumBanks[channel] || channel == MIDI_PERCUSSION_CHANNEL) {
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

        private void renderPcmOverlay(float[] samples,
                                      int offset,
                                      int frames,
                                      float left,
                                      float right,
                                      boolean clamp) {
            if (!PCM_OVERLAY_ENABLED || pcmNotes.isEmpty()) {
                return;
            }
            Iterator<PcmNote> iterator = pcmNotes.iterator();
            while (iterator.hasNext()) {
                PcmNote note = iterator.next();
                int[] wave = note.wave;
                int end = Math.min(note.voice.endPoint(), wave.length);
                int loop = Math.max(0, Math.min(note.voice.loopPoint(), end));
                boolean repeat = note.voice.repeatMode()
                        && note.voice.loopPoint() < note.voice.endPoint()
                        && loop < end;
                if (end <= 0) {
                    iterator.remove();
                    continue;
                }
                if (note.position >= end) {
                    if (repeat) {
                        note.position = loop + (note.position - loop) % (end - loop);
                    } else {
                        iterator.remove();
                        continue;
                    }
                }

                float pan = note.pan(channelPans[note.channel]);
                // Inlined equal-power pan to avoid allocating a float[2] per
                // PcmNote per render chunk. cos/sin run once per chunk, not
                // per sample.
                double panAngle = (Math.max(-1.0f, Math.min(1.0f, pan)) + 1.0) * Math.PI * 0.25;
                float noteLeft = left * (float) Math.cos(panAngle);
                float noteRight = right * (float) Math.sin(panAngle);
                float gain = channelVolumes[note.channel] * note.velocity * pcmGain();

                for (int frame = 0; frame < frames; frame++) {
                    if (note.position >= end) {
                        if (repeat) {
                            note.position = loop + (note.position - loop) % (end - loop);
                        } else {
                            note.finished = true;
                            break;
                        }
                    }

                    note.computeLfoSin();
                    float sample = interpolatedWaveSample(wave, note.position, repeat ? loop : end, end)
                            * gain
                            * note.envelope()
                            * note.amScale()
                            * note.releaseGain;
                    int output = offset + frame * 2;
                    samples[output] += sample * noteLeft;
                    samples[output + 1] += sample * noteRight;
                    note.position += note.advance(pitchBendSemitones[note.channel]);
                    note.advanceLfo();

                    if (!PCM_ENVELOPE_ENABLED && note.releasing) {
                        note.release();
                        if (note.finished) {
                            note.finished = true;
                            break;
                        }
                    }
                }
                if (note.finished) {
                    iterator.remove();
                }
            }
            if (clamp) {
                int end = offset + frames * 2;
                for (int i = offset; i < end; i++) {
                    samples[i] = Math.max(-1.0f, Math.min(1.0f, samples[i]));
                }
            }
        }

        private static int programKey(int bank, int program) {
            return (bank & 0x7f) << 8 | (program & 0x7f);
        }

        private static float[] equalPowerPan(float pan) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, pan));
            double angle = (clamped + 1.0) * Math.PI * 0.25;
            return new float[] {(float) Math.cos(angle), (float) Math.sin(angle)};
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

        private static float nativePcmAdvance(float frequency, float outputSampleRate) {
            long register = nativePcmRegister(frequency);
            long q16 = ((0x5dc00000L / (int) outputSampleRate) * register) >> 15;
            return q16 / 65536.0f;
        }

        private static long nativePcmRegister(float frequency) {
            double ideal = Math.max(0, frequency) * 65536.0 / 48000.0;
            int shift = Math.max(0, Math.getExponent(ideal) - 10);
            long quantum = 1L << Math.min(shift, 30);
            return (long) (ideal / quantum) * quantum;
        }

        private static float interpolatedWaveSample(int[] wave, double position, int loop, int end) {
            int source = Math.max(0, Math.min((int) position, end - 1));
            int next = source + 1 < end ? source + 1 : loop < end ? loop : source;
            float fraction = (float) (position - source);
            float sample = wave[source] + (wave[next] - wave[source]) * fraction;
            return sample / 32768.0f;
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
            private final int key;
            private final float velocity;
            private final MA5PcmVoiceProgram voice;
            private final int[] wave;
            private final float baseAdvance;
            private final float sourceFrequency;
            private final float outputSampleRate;
            private final float noteSemitones;
            private float cachedBend = Float.NaN;
            private float cachedAdvance;
            private final float totalLevelGain;
            private final float attackDelta;
            private final float decayCoef;
            private final float sustainCoef;
            private final float releaseCoef;
            private final float sustainLevel;
            private final float lfoPhasePerSample;
            private final float vibratoDepthSemitones;
            private final float amDepth;
            private double position;
            private float releaseGain = 1.0f;
            private boolean releasing;
            private boolean holding;
            private boolean finished;
            private EnvelopeStage envelopeStage = EnvelopeStage.RESET;
            private float envelopeLevel = 0.0f;
            private float lfoPhase;

            private PcmNote(int channel, int key, float velocity, MA5PcmVoiceProgram voice, int[] wave, float outputSampleRate) {
                this.channel = channel;
                this.key = key;
                this.velocity = Math.max(0.0f, velocity);
                this.voice = voice;
                this.wave = wave;
                // The VM35 frequency field stores the wave's authored sample
                // rate for the one-shot MA-5 PCM effects seen in MGS Mobile.
                this.sourceFrequency = voice.frequencySetting() * PCM_FREQ_SCALE;
                this.outputSampleRate = outputSampleRate;
                this.noteSemitones = voice.drumVoice() ? 0 : key + 69 - PCM_REFERENCE_KEY;
                float frequency = sourceFrequency * hardwarePitchRatio(noteSemitones);
                this.baseAdvance = nativePcmAdvance(frequency, outputSampleRate);
                long register = nativePcmRegister(frequency);
                int block = Math.clamp(Math.getExponent((double) register) - 11, 0, 7);
                int fnum = (int) (register >> (block + 1)) - 1024;
                int keyRateOffset = ((block << 1) | ((fnum >> 9) & 1)) >> 2;
                this.totalLevelGain = PCM_ENVELOPE_ENABLED ? totalLevelGain(voice.totalLevel()) : 1.0f;
                this.attackDelta = attackDelta(voice.attackRate(), keyRateOffset, outputSampleRate);
                this.decayCoef = decayCoef(voice.decayRate(), keyRateOffset, outputSampleRate);
                this.sustainCoef = decayCoef(voice.sustainRate(), keyRateOffset, outputSampleRate);
                this.releaseCoef = decayCoef(voice.releaseRate(), keyRateOffset, outputSampleRate);
                this.sustainLevel = sustainLevel(voice.sustainLevel());
                int lfoIdx = Math.max(0, Math.min(LFO_FREQ_HZ.length - 1, voice.lfo()));
                this.lfoPhasePerSample = (float) (2.0 * Math.PI * LFO_FREQ_HZ[lfoIdx] / outputSampleRate);
                // Voice depth fields are 2 bits (0..3). Map vibrato to ~1
                // semitone peak swing at depth 3 (musically conservative;
                // chip exact mapping requires the SMW driver tables not yet
                // located). AM is one-sided attenuating per chip behavior:
                // depth 3 trims to ~50% amplitude at the LFO trough.
                this.vibratoDepthSemitones =
                        voice.vibratoEnabled() ? voice.vibratoDepth() / 3.0f : 0.0f;
                this.amDepth =
                        voice.amplitudeModEnabled() ? voice.amplitudeModDepth() / 6.0f : 0.0f;
                if (!PCM_ENVELOPE_ENABLED) {
                    this.envelopeLevel = 1.0f;
                    this.envelopeStage = EnvelopeStage.DECAY;
                } else if (voice.attackRate() == 0) {
                    this.finished = true; // Native AR=0 has a zero attack increment.
                }
            }

            private float pan(float channelPan) {
                if (!voice.panpotEnable()) {
                    return channelPan;
                }
                return Math.max(-1.0f, Math.min(1.0f, (voice.panpot() - 15.0f) / 15.0f));
            }

            private float advance(float bendSemitones) {
                float vibrato = vibratoSemitones();
                if (bendSemitones == 0 && vibrato == 0) return baseAdvance;
                if (vibrato == 0 && cachedBend == bendSemitones) return cachedAdvance;
                float advance = nativePcmAdvance(sourceFrequency
                        * hardwarePitchRatio(noteSemitones + bendSemitones + vibrato), outputSampleRate);
                if (vibrato == 0) { cachedBend = bendSemitones; cachedAdvance = advance; }
                return advance;
            }

            private boolean oneShotDrum() {
                return voice.drumVoice() && voice.ignoreKeyOff();
            }

            /**
             * Cached {@code sin(lfoPhase)} for the current sample frame.
             * Computed once per frame in {@link #computeLfoSin} and consumed
             * by {@link #vibratoSemitones} and {@link #amScale} so the
             * relatively expensive sine evaluation runs once per active
             * PCM note per output sample instead of twice.
             */
            private float lfoSinCached;

            private void computeLfoSin() {
                lfoSinCached = (vibratoDepthSemitones != 0.0f || amDepth != 0.0f)
                        ? fastSin(lfoPhase)
                        : 0.0f;
            }

            private float vibratoSemitones() {
                if (vibratoDepthSemitones == 0.0f) {
                    return 0.0f;
                }
                return vibratoDepthSemitones * lfoSinCached;
            }

            /**
             * Returns the amplitude scale for this sample frame from the
             * voice's AM LFO. Chip AM is one-sided attenuating: it trims the
             * carrier toward zero at the LFO peak, never boosts above unity.
             * Returns {@code 1.0f} when AM is disabled or depth is zero.
             */
            private float amScale() {
                if (amDepth == 0.0f) {
                    return 1.0f;
                }
                float lfo01 = (1.0f + lfoSinCached) * 0.5f;
                return Math.max(0.0f, 1.0f - amDepth * lfo01);
            }

            private void advanceLfo() {
                if (lfoPhasePerSample == 0.0f) {
                    return;
                }
                lfoPhase += lfoPhasePerSample;
                if (lfoPhase > (float) (2.0 * Math.PI)) {
                    lfoPhase -= (float) (2.0 * Math.PI);
                }
            }

            private static float hardwarePitchRatio(float semitones) {
                int lower = (int) Math.floor(semitones);
                int upper = lower + 1;
                float fraction = semitones - lower;
                float lowerRatio = hardwarePitchRatioWhole(lower);
                float upperRatio = hardwarePitchRatioWhole(upper);
                return lowerRatio + (upperRatio - lowerRatio) * fraction;
            }

            private static float hardwarePitchRatioWhole(int semitones) {
                if (semitones == 0) {
                    return 1.0f;
                }
                int magnitude = Math.abs(semitones);
                int octaves = magnitude / 12;
                int withinOctave = magnitude % 12;
                float downwardRatio = PCM_SEMITONE_Q15[withinOctave] / 32768.0f;
                if (withinOctave == 0) {
                    downwardRatio = 1.0f;
                }
                float octaveRatio = (float) Math.scalb(1.0f, octaves);
                if (semitones > 0) {
                    return octaveRatio / downwardRatio;
                }
                return downwardRatio / octaveRatio;
            }

            private float envelope() {
                if (!PCM_ENVELOPE_ENABLED) {
                    return 1.0f;
                }
                if (releasing
                        && envelopeStage != EnvelopeStage.RELEASE
                        && envelopeStage != EnvelopeStage.OFF) {
                    envelopeStage = EnvelopeStage.RELEASE;
                }
                switch (envelopeStage) {
                    case RESET -> envelopeStage = EnvelopeStage.ATTACK;
                    case ATTACK -> {
                        envelopeLevel += attackDelta;
                        if (envelopeLevel >= 1.0f) {
                            envelopeLevel = 1.0f;
                            envelopeStage = EnvelopeStage.DECAY;
                        }
                    }
                    case DECAY -> {
                        if (envelopeLevel > sustainLevel) {
                            envelopeLevel *= decayCoef;
                        } else {
                            envelopeStage = EnvelopeStage.SUSTAIN;
                        }
                    }
                    case SUSTAIN -> {
                        if (envelopeLevel > PCM_EPSILON) {
                            envelopeLevel *= sustainCoef;
                        } else {
                            envelopeLevel = 0.0f;
                            envelopeStage = EnvelopeStage.OFF;
                            finished = true;
                        }
                    }
                    case RELEASE -> release();
                    case OFF -> finished = true;
                }
                return envelopeLevel * totalLevelGain;
            }

            private void beginRelease() {
                if (voice.ignoreKeyOff() && !finished) {
                    return;
                }
                releasing = true;
                if (PCM_ENVELOPE_ENABLED && envelopeStage != EnvelopeStage.OFF) {
                    envelopeStage = EnvelopeStage.RELEASE;
                }
            }

            private void release() {
                if (!PCM_ENVELOPE_ENABLED) {
                    releaseGain *= 0.985f;
                    if (releaseGain < 0.001f) {
                        finished = true;
                    }
                    return;
                }
                if (voice.ignoreKeyOff() && !finished) {
                    return;
                }
                envelopeStage = EnvelopeStage.RELEASE;
                if (envelopeLevel > PCM_EPSILON) {
                    envelopeLevel *= releaseCoef;
                } else {
                    envelopeLevel = 0.0f;
                    envelopeStage = EnvelopeStage.OFF;
                    finished = true;
                }
            }

            private static float attackDelta(int attackRate, int keyRateOffset, float outputSampleRate) {
                int index = attackRate == 0 ? 0 : Math.min(63, (attackRate << 2) + keyRateOffset);
                long delta = HW_ATTACK_TABLE_32K[index];
                if (delta == 0x80000000L) return 1.0f;
                return (float) (delta / 2147483648.0 * HW_RATE_TABLE_SAMPLE_RATE / outputSampleRate);
            }

            /**
             * Resolves a 4-bit rate field (0..15) into a per-output-sample
             * envelope multiplier using the chip's 64-entry per-tick table.
             *
             * <p>The SDK applies its rate once per generated PCM sample at
             * 32 kHz. We convert that to a per-sample coefficient so the
             * existing per-sample envelope loop reaches the same wall-clock
             * decay shape: {@code perSample = perTick ^ (32000 / SR)}.</p>
             *
             * <p>Table indices 0..3 hold unity, so rate 0 remains a no-op.
             * Higher rates are just below unity and decay over the same
             * wall-clock time as the SDK.</p>
             */
            private static float decayCoef(int rate, int keyRateOffset, float outputSampleRate) {
                if (rate <= 0) {
                    return 1.0f;
                }
                int idx = (Math.max(0, Math.min(15, rate)) << 2) + keyRateOffset;
                if (idx >= HW_RATE_TABLE_32K.length) {
                    idx = HW_RATE_TABLE_32K.length - 1;
                }
                float perTick = HW_RATE_TABLE_32K[idx];
                if (perTick >= 1.0f) {
                    return 1.0f;
                }
                double exponent = HW_RATE_TABLE_SAMPLE_RATE / outputSampleRate;
                return (float) Math.pow(perTick, exponent);
            }

            private static float sustainLevel(int sustainLevel) {
                return (float) (PCM_SUSTAIN_LEVEL_Q31[Math.clamp(sustainLevel, 0, 15)] / 2147483648.0);
            }

            private static float totalLevelGain(int totalLevel) {
                return PCM_TOTAL_LEVEL_Q15[Math.clamp(totalLevel, 0, 63)] / 32768.0f;
            }
        }

        private enum EnvelopeStage {
            RESET,
            ATTACK,
            DECAY,
            SUSTAIN,
            RELEASE,
            OFF
        }
    }
}
