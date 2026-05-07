package remexa.audio.smaf;

import remexa.audio.smaf.ma3.MA3SamplerProvider;
import remexa.audio.smaf.ma3.Sampler;
import remexa.audio.smaf.ma5.MA5PacketInventory;
import remexa.audio.smaf.ma5.MA5PcmVoiceProgram;
import remexa.audio.smaf.ma5.MA5SoftbankBridge;
import remexa.audio.smaf.ma5.MA5WaveDataPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class Ma5SmafAudioEngine implements YamahaAudioEngine {
    /**
     * Output sample rate.
     * The chip natively supports {22050, 32000, 44100, 48000};
     */
    private static final int MA5_OUTPUT_SAMPLE_RATE =
            Integer.getInteger("remexa.ma5SampleRate", 48_000);
    private final SmafSequencedRenderer renderer;

    Ma5SmafAudioEngine() {
        MA3SamplerProvider provider = new MA3SamplerProvider();
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
        private static final int INTERNAL_LEGACY_YAMAHA_PITCH = 0x11;
        private static final int MIDI_PERCUSSION_CHANNEL = 9;
        private static final float DEFAULT_PITCH_BEND_RANGE_SEMITONES = 2.0f;
        private static final float SEMITONES_PER_OCTAVE = 12.0f;
        private static final int MA5_DRUM_KEY_MIDI_BASE = 36;
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
        private static final float PCM_GAIN =
                Float.parseFloat(System.getProperty("remexa.ma5PcmGain", "0.35"));
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
        private static final double PCM_DECAY_DB_PER_SEC_AT_4 = 17.9342 / 2.0;
        private static final double PCM_ATTACK_TIME_SEC_AT_1 = 3.07068;

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
         * factor every 7 output samples in decay/sustain/release stages.
         *
         * <p>Indices 0-3 hold {@code 2.0f} which the chip treats as a no-op
         * because the envelope is clamped at unity; the deepest (index 60+)
         * entries reach ~3.8e-5 for near-instant decay. Rate fields (0..15)
         * map to {@code idx = rate << 2}.</p>
         */
        private static final float[] HW_RATE_TABLE_32K = {
                2.0000000f, 2.0000000f, 2.0000000f, 2.0000000f,
                1.9989512f, 1.9979054f, 1.9968596f, 1.9958138f,
                1.9947681f, 1.9926765f, 1.9905849f, 1.9884934f,
                1.9864018f, 1.9843102f, 1.9801271f, 1.9759438f,
                1.9717607f, 1.9675775f, 1.9591942f, 1.9508115f,
                1.9424285f, 1.9340450f, 1.9172785f, 1.9005120f,
                1.8837459f, 1.8669792f, 1.8334467f, 1.7999142f,
                1.7663815f, 1.7328490f, 1.6657841f, 1.5987189f,
                1.5316539f, 1.4645889f, 1.3304592f, 1.1963297f,
                1.0623415f, 0.9329090f, 0.7975446f, 0.6622045f,
                0.5854902f, 0.4774414f, 0.3496094f, 0.2218018f,
                0.1387939f, 0.0840149f, 0.0524750f, 0.0335999f,
                0.0207825f, 0.0125732f, 0.0080414f, 0.0049019f,
                0.0028534f, 0.0019073f, 0.0009537f, 0.0006104f,
                0.0003815f, 0.0002441f, 0.0001144f, 0.0000610f,
                0.0000381f, 0.0000381f, 0.0000381f, 0.0000381f,
        };

        /**
         * The chip's envelope state machine ticks every 7 output samples at
         * 32 kHz (struct {@code +0x1930}, set by {@code sub_10006a90}). At
         * other output rates we preserve wall-clock tick rate by raising the
         * coefficient to a fractional power.
         */
        private static final int ENV_TICK_INTERVAL_32K = 7;

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
        private final float[] channelPans = new float[CHANNEL_COUNT];
        private final Map<Integer, MA5PcmVoiceProgram> pcmPrograms = new HashMap<>();
        private final Map<Integer, MA5PcmVoiceProgram> pcmDrumPrograms = new HashMap<>();
        private final Map<Integer, int[]> pcmWaves = new HashMap<>();
        private final List<PcmNote> pcmNotes = new ArrayList<>();

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
            pcmWaves.clear();
            pcmNotes.clear();
            Arrays.fill(channelBanks, 0);
            Arrays.fill(channelPrograms, 0);
            Arrays.fill(channelDrumBanks, false);
            Arrays.fill(channelVolumes, 1.0f);
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
            sampler.drumEnable(channel, enable);
        }

        @Override
        public boolean isFinished() {
            return sampler.isFinished() && pcmNotes.isEmpty();
        }

        @Override
        public void keyOff(int channel, int key) {
            boolean releasedPcmNote = false;
            for (PcmNote note : pcmNotes) {
                if (note.channel == channel && note.key == key) {
                    note.releasing = true;
                    releasedPcmNote = true;
                }
            }
            if (releasedPcmNote) {
                return;
            }
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
        public void panpot(int channel, float panpot) {
            if (channel >= 0 && channel < CHANNEL_COUNT) {
                channelPans[channel] = Math.max(-1.0f, Math.min(1.0f, panpot));
            }
            sampler.panpot(channel, panpot);
        }

        @Override
        public void render(float[] samples, int offset, int frames, float left, float right, boolean erase, boolean clamp) {
            sampler.render(samples, offset, frames, left, right, erase, clamp);
            renderPcmOverlay(samples, offset, frames, left, right, clamp);
        }

        @Override
        public void sysEx(byte[] message) {
            sysEx(-1, message);
        }

        @Override
        public void sysEx(int sourceBank, byte[] message) {
            if (applyInternalSoftbankControl(message)) {
                return;
            }
            if (!softbankBridge.sysEx(message)) {
                sampler.sysEx(message);
            }
        }

        private boolean applyInternalSoftbankControl(byte[] message) {
            if (message == null || message.length < 4
                    || (message[0] & 0xff) != INTERNAL_LEGACY_YAMAHA_MESSAGE) {
                return false;
            }
            int command = message[1] & 0xff;
            int logicalChannel = message[2] & 0x0f;
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
                    volume(logicalChannel, normalized);
                    if (logicalChannel >= 0 && logicalChannel < CHANNEL_COUNT && channelDrumBanks[logicalChannel]) {
                        volume(MIDI_PERCUSSION_CHANNEL, normalized);
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

        @Override
        public void onWaveData(MA5WaveDataPacket waveData) {
            pcmWaves.put(waveData.waveId(),
                    decodePcmWave(waveData.encodedData(), 0, waveData.encodedData().length));
        }

        @Override
        public void onPcmVoice(MA5PcmVoiceProgram voice) {
            if (voice.drumVoice()) {
                pcmDrumPrograms.put(programKey(voice.bankLsb(), voice.program()), voice);
            } else {
                pcmPrograms.put(programKey(voice.bankLsb(), voice.program()), voice);
            }
        }

        private MA5PcmVoiceProgram pcmProgram(int channel, int key) {
            if (!PCM_OVERLAY_ENABLED || channel < 0 || channel >= CHANNEL_COUNT) {
                return null;
            }
            if (channelDrumBanks[channel] || channel == MIDI_PERCUSSION_CHANNEL) {
                int midiNote = key + 69;
                int drumKey = midiNote - MA5_DRUM_KEY_MIDI_BASE;
                MA5PcmVoiceProgram drumVoice = pcmDrumPrograms.get(programKey(channelBanks[channel], drumKey));
                if (drumVoice != null) {
                    return drumVoice;
                }
            }
            return pcmPrograms.get(programKey(channelBanks[channel], channelPrograms[channel]));
        }

        private int[] pcmWave(MA5PcmVoiceProgram voice) {
            if (!PCM_OVERLAY_ENABLED || voice == null) {
                return null;
            }
            return pcmWaves.get(voice.waveId());
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
                int end = Math.min(note.voice.endPoint() + 1, wave.length);
                int loop = Math.max(0, Math.min(note.voice.loopPoint(), end));
                if (end <= 0 || note.position >= end) {
                    iterator.remove();
                    continue;
                }

                float pan = note.pan(channelPans[note.channel]);
                // Inlined equal-power pan to avoid allocating a float[2] per
                // PcmNote per render chunk. cos/sin run once per chunk, not
                // per sample.
                double panAngle = (Math.max(-1.0f, Math.min(1.0f, pan)) + 1.0) * Math.PI * 0.25;
                float noteLeft = left * (float) Math.cos(panAngle);
                float noteRight = right * (float) Math.sin(panAngle);
                float gain = channelVolumes[note.channel] * note.velocity * PCM_GAIN;

                for (int frame = 0; frame < frames; frame++) {
                    if (note.position >= end) {
                        if (note.voice.repeatMode() && loop < end) {
                            note.position = loop + (note.position - loop) % (end - loop);
                        } else {
                            note.finished = true;
                            break;
                        }
                    }

                    note.computeLfoSin();
                    float sample = interpolatedWaveSample(wave, note.position)
                            * gain
                            * note.envelope()
                            * note.amScale()
                            * note.releaseGain;
                    int output = offset + frame * 2;
                    samples[output] += sample * noteLeft;
                    samples[output + 1] += sample * noteRight;
                    note.position += note.advance(pitchBendSemitones[note.channel]);
                    note.advanceLfo();

                    if (note.releasing) {
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

        private static int[] decodePcmWave(byte[] adpcm, int offset, int length) {
            return switch (PCM_DECODER) {
                case "aica" -> decodeAica(adpcm, offset, length);
                case "yamaha" -> decodeYamaha(adpcm, offset, length);
                default -> decodeYm2608(adpcm, offset, length);
            };
        }

        private static float interpolatedWaveSample(int[] wave, float position) {
            int source = Math.max(0, Math.min((int) position, wave.length - 1));
            int next = Math.min(source + 1, wave.length - 1);
            float fraction = position - source;
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
            private final float totalLevelGain;
            private final float attackDelta;
            private final float decayCoef;
            private final float sustainCoef;
            private final float releaseCoef;
            private final float sustainLevel;
            private final float lfoPhasePerSample;
            private final float vibratoDepthSemitones;
            private final float amDepth;
            private float position;
            private float releaseGain = 1.0f;
            private boolean releasing;
            private boolean holding;
            private boolean finished;
            private EnvelopeStage envelopeStage = EnvelopeStage.ATTACK;
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
                this.baseAdvance = Math.max(0.001f,
                        voice.frequencySetting() * PCM_FREQ_SCALE / outputSampleRate);
                this.totalLevelGain = PCM_ENVELOPE_ENABLED ? totalLevelGain(voice.totalLevel()) : 1.0f;
                this.attackDelta = attackDelta(voice.attackRate(), outputSampleRate);
                this.decayCoef = decayCoef(voice.decayRate(), outputSampleRate);
                this.sustainCoef = decayCoef(voice.sustainRate(), outputSampleRate);
                this.releaseCoef = decayCoef(voice.releaseRate(), outputSampleRate);
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
                if (!PCM_ENVELOPE_ENABLED || voice.attackRate() <= 0) {
                    this.envelopeLevel = 1.0f;
                    this.envelopeStage = EnvelopeStage.DECAY;
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
                if (voice.drumVoice()) {
                    // Drum-kit voices play at the authored natural rate. The
                    // MIDI note only selects WHICH drum (via the per-note
                    // program lookup), so transposing by key would shift bass
                    // content out of the kick/tom thump range. Pitch bend
                    // still applies as a fine adjustment if the sequence
                    // sweeps a hit.
                    float totalSemitones = bendSemitones + vibrato;
                    if (totalSemitones == 0.0f) {
                        return baseAdvance;
                    }
                    return baseAdvance * hardwarePitchRatio(totalSemitones);
                }
                // SmafSequencedRenderer passes keys relative to MIDI note 69;
                // convert back to absolute MIDI before applying PCM tuning.
                int midiKey = key + 69;
                // Melodic PCM transposes relative to a chip-defined center
                // key. The voice's frequencySetting is calibrated for that
                // key; above it plays faster, below it slower.
                return baseAdvance
                        * hardwarePitchRatio(midiKey - PCM_REFERENCE_KEY + bendSemitones + vibrato);
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
                switch (envelopeStage) {
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

            private static float attackDelta(int attackRate, float outputSampleRate) {
                if (attackRate <= 0) {
                    return 1.0f;
                }
                double seconds = PCM_ATTACK_TIME_SEC_AT_1 / (1 << Math.min(attackRate - 1, 30));
                return (float) (1.0 / Math.max(1.0, seconds * outputSampleRate));
            }

            /**
             * Resolves a 4-bit rate field (0..15) into a per-output-sample
             * envelope multiplier using the chip's 64-entry per-tick table.
             *
             * <p>The chip applies its rate at one tick every 7 samples at
             * 32 kHz. We convert that to a per-sample coefficient so the
             * existing per-sample envelope loop reaches the same wall-clock
             * decay shape: {@code perSample = perTick ^ (32000 / (7 * SR))}.</p>
             *
             * <p>Table indices 0..3 hold {@code 2.0} which on the chip means
             * "no envelope movement" (envelope is clamped at unity); we
             * surface that as a coefficient of {@code 1.0} so {@code env *=
             * coef} is a no-op.</p>
             */
            private static float decayCoef(int rate, float outputSampleRate) {
                if (rate <= 0) {
                    return 1.0f;
                }
                int idx = Math.max(0, Math.min(15, rate)) << 2;
                if (idx >= HW_RATE_TABLE_32K.length) {
                    idx = HW_RATE_TABLE_32K.length - 1;
                }
                float perTick = HW_RATE_TABLE_32K[idx];
                if (perTick >= 1.0f) {
                    return 1.0f;
                }
                double exponent = 32_000.0 / (ENV_TICK_INTERVAL_32K * outputSampleRate);
                return (float) Math.pow(perTick, exponent);
            }

            private static float sustainLevel(int sustainLevel) {
                if (sustainLevel >= 0x0f) {
                    return 0.0f;
                }
                return (float) Math.pow(10.0, -3.0 * sustainLevel / 20.0);
            }

            private static float totalLevelGain(int totalLevel) {
                if (totalLevel >= 63) {
                    return 0.0f;
                }
                return (float) Math.pow(10.0, -0.75 * totalLevel / 20.0);
            }
        }

        private enum EnvelopeStage {
            ATTACK,
            DECAY,
            SUSTAIN,
            RELEASE,
            OFF
        }
    }
}
