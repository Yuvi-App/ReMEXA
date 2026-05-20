package remexa.audio.smaf;

import remexa.audio.smaf.ma3.MA3SamplerProvider;
import remexa.audio.smaf.ma3.MA3SoftbankBridge;
import remexa.audio.smaf.ma3.Sampler;

final class Ma3SmafAudioEngine implements YamahaAudioEngine {
    private final SmafSequencedRenderer renderer;

    Ma3SmafAudioEngine() {
        MA3SamplerProvider provider = new MA3SamplerProvider();
        renderer = new SmafSequencedRenderer("MA3", sampleRate -> {
            Sampler sampler = provider.instance(sampleRate);
            return new Ma3Adapter(sampler, new MA3SoftbankBridge(sampler));
        });
    }

    @Override
    public String id() {
        return "ma3";
    }

    @Override
    public String label() {
        return "MA3";
    }

    @Override
    public SmafRenderedAudio render(SmafRenderContext context) throws Exception {
        return renderer.render(
                context.sequence(),
                context.sequenceSysExEvents(),
                YamahaAudioEngine.startupAndExclusivePackets(context),
                context.pcmClipData(),
                context.pcmTriggers());
    }

    @Override
    public SmafStreamingSession openStream(SmafRenderContext context) throws Exception {
        return renderer.openStream(
                context.sequence(),
                context.sequenceSysExEvents(),
                YamahaAudioEngine.startupAndExclusivePackets(context),
                context.pcmClipData(),
                context.pcmTriggers());
    }

    private static final class Ma3Adapter implements SmafSynthAdapter {
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
        private static final float DEFAULT_PITCH_BEND_RANGE_SEMITONES = 2.0f;
        private static final float SEMITONES_PER_OCTAVE = 12.0f;

        private final Sampler sampler;
        private final MA3SoftbankBridge softbankBridge;
        private final float[] pitchBendSemitones = new float[CHANNEL_COUNT];
        private final float[] pitchBendRanges = new float[CHANNEL_COUNT];

        private Ma3Adapter(Sampler sampler, MA3SoftbankBridge softbankBridge) {
            this.sampler = sampler;
            this.softbankBridge = softbankBridge;
        }

        @Override
        public void reset() {
            sampler.reset();
            softbankBridge.reset();
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
            return sampler.isFinished();
        }

        @Override
        public void keyOff(int channel, int key) {
            sampler.keyOff(channel, key);
        }

        @Override
        public void keyOn(int channel, int key, float velocity) {
            sampler.keyOn(channel, key, velocity);
        }

        @Override
        public void bankChange(int channel, int bank) {
            sampler.bankChange(channel, bank);
        }

        @Override
        public void programChange(int channel, int program) {
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
            sampler.volume(channel, volume);
        }

        @Override
        public void modulation(int channel, int value) {
            sampler.modulation(channel, value);
        }

        @Override
        public void panpot(int channel, float panpot) {
            sampler.panpot(channel, panpot);
        }

        @Override
        public void render(float[] samples, int offset, int frames, float left, float right, boolean erase, boolean clamp) {
            sampler.render(samples, offset, frames, left, right, erase, clamp);
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
            if (!softbankBridge.sysEx(sourceBank, message)) {
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
                case INTERNAL_LEGACY_YAMAHA_SELECTOR, INTERNAL_LEGACY_YAMAHA_PROGRAM ->
                        sampler.programChange(logicalChannel, value);
                case INTERNAL_LEGACY_YAMAHA_BANK -> {
                    boolean drumBank = (rawValue & 0x80) != 0;
                    sampler.drumEnable(logicalChannel, drumBank);
                    sampler.bankChange(logicalChannel, value);
                }
                case INTERNAL_LEGACY_YAMAHA_MODULATION -> {
                    modulation(logicalChannel, value);
                }
                case INTERNAL_LEGACY_YAMAHA_VOLUME, INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME, INTERNAL_LEGACY_YAMAHA_EXPRESSION ->
                        sampler.volume(logicalChannel, value / 127.0f);
                case INTERNAL_LEGACY_YAMAHA_PAN ->
                        sampler.panpot(logicalChannel, midiPanToFloat(value));
                case INTERNAL_LEGACY_YAMAHA_PITCH ->
                        pitchBend(logicalChannel, centeredLegacyPitchBend(value, logicalChannel));
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
    }
}
