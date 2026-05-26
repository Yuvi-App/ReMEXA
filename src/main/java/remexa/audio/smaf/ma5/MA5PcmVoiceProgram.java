package remexa.audio.smaf.ma5;

import java.util.Arrays;

/**
 * Raw Yamaha VM35/VM5 PCM voice metadata decoded from {@code 43 05 02 ...}
 * and legacy SoftBank ATS-MA5 {@code 43 02 02 08 7C/7D ...} packets.
 *
 * <p>The inline EXVO form observed in MA-5 phrases mirrors the FM packet's
 * bank/program header: {@code 43 05 02 bank program}, followed by the 16 PCM
 * voice bytes.</p>
 */
public record MA5PcmVoiceProgram(int bankLsb,
                                 int program,
                                 boolean drumVoice,
                                 int frequencySetting,
                                 int panpot,
                                 boolean panpotEnable,
                                 int lfo,
                                 int attackRate,
                                 int decayRate,
                                 int sustainRate,
                                 int releaseRate,
                                 int sustainLevel,
                                 int totalLevel,
                                 int amplitudeModDepth,
                                 int vibratoDepth,
                                 boolean ignoreKeyOff,
                                 boolean sustain,
                                 boolean amplitudeModEnabled,
                                 boolean vibratoEnabled,
                                 int loopPoint,
                                 int endPoint,
                                 boolean repeatMode,
                                 int waveId,
                                 int keyLow,
                                 int keyHigh,
                                 byte[] payload) {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_LEGACY_SOFTBANK = 0x02;
    private static final int FAMILY_VM5 = 0x05;
    private static final int LEGACY_SOFTBANK_SUBFAMILY = 0x02;
    private static final int LEGACY_SOFTBANK_VOICE = 0x08;
    private static final int LEGACY_MELODIC_BANK_MSB = 0x7c;
    private static final int LEGACY_DRUM_BANK_MSB = 0x7d;
    private static final int LEGACY_FIXED_FIELD_OFFSET = 10;
    private static final int LEGACY_MIN_PCM_BYTES = 27;
    private static final int LEGACY_MIDI_DRUM_BASE = 36;
    private static final int VM5_PCM_PROGRAM = 0x02;
    private static final int VM5_PCM_HEADER_BYTES = 5;
    private static final int VM5_PCM_PAYLOAD_BYTES = 16;

    public MA5PcmVoiceProgram {
        keyLow = clampMidiKey(keyLow);
        keyHigh = clampMidiKey(keyHigh);
        if (keyHigh < keyLow) {
            int swap = keyLow;
            keyLow = keyHigh;
            keyHigh = swap;
        }
        payload = payload.clone();
    }

    public MA5PcmVoiceProgram(int bankLsb,
                              int program,
                              boolean drumVoice,
                              int frequencySetting,
                              int panpot,
                              boolean panpotEnable,
                              int lfo,
                              int attackRate,
                              int decayRate,
                              int sustainRate,
                              int releaseRate,
                              int sustainLevel,
                              int totalLevel,
                              int amplitudeModDepth,
                              int vibratoDepth,
                              boolean ignoreKeyOff,
                              boolean sustain,
                              boolean amplitudeModEnabled,
                              boolean vibratoEnabled,
                              int loopPoint,
                              int endPoint,
                              boolean repeatMode,
                              int waveId,
                              byte[] payload) {
        this(bankLsb,
                program,
                drumVoice,
                frequencySetting,
                panpot,
                panpotEnable,
                lfo,
                attackRate,
                decayRate,
                sustainRate,
                releaseRate,
                sustainLevel,
                totalLevel,
                amplitudeModDepth,
                vibratoDepth,
                ignoreKeyOff,
                sustain,
                amplitudeModEnabled,
                vibratoEnabled,
                loopPoint,
                endPoint,
                repeatMode,
                waveId,
                0,
                127,
                payload);
    }

    public static MA5PcmVoiceProgram decode(byte[] packet) {
        byte[] body = normalize(packet);
        MA5PcmVoiceProgram legacyVoice = decodeLegacySoftbankVoice(body);
        if (legacyVoice != null) {
            return legacyVoice;
        }

        if (body.length != VM5_PCM_HEADER_BYTES + VM5_PCM_PAYLOAD_BYTES
                || (body[0] & 0xff) != MANUFACTURER_YAMAHA
                || (body[1] & 0xff) != FAMILY_VM5
                || (body[2] & 0xff) != VM5_PCM_PROGRAM) {
            return null;
        }

        int bankByte = body[3] & 0xff;
        int bankLsb = bankByte & 0x7f;
        int program = body[4] & 0x7f;
        boolean drumVoice = (bankByte & 0x80) != 0;
        byte[] payload = Arrays.copyOfRange(body, VM5_PCM_HEADER_BYTES, body.length);
        return fromPayload(bankLsb, program, drumVoice, 0, 127, payload);
    }

    public String summary() {
        return "pcmVoice bankLsb=" + bankLsb
                + " program=" + program
                + " drumVoice=" + drumVoice
                + " fs=" + frequencySetting
                + " waveId=" + waveId
                + " keys=" + keyLow + "-" + keyHigh
                + " loop=" + loopPoint
                + " end=" + endPoint
                + " repeat=" + repeatMode
                + " panpot=" + panpot
                + " pe=" + panpotEnable
                + " lfo=" + lfo
                + " ar=" + attackRate
                + " dr=" + decayRate
                + " sr=" + sustainRate
                + " rr=" + releaseRate
                + " sl=" + sustainLevel
                + " tl=" + totalLevel
                + " dam=" + amplitudeModDepth
                + " dvb=" + vibratoDepth;
    }

    private static MA5PcmVoiceProgram decodeLegacySoftbankVoice(byte[] body) {
        if (body.length < LEGACY_MIN_PCM_BYTES
                || (body[0] & 0xff) != MANUFACTURER_YAMAHA
                || (body[1] & 0xff) != FAMILY_LEGACY_SOFTBANK
                || (body[2] & 0xff) != LEGACY_SOFTBANK_SUBFAMILY
                || (body[3] & 0xff) != LEGACY_SOFTBANK_VOICE) {
            return null;
        }

        int bankMsb = body[4] & 0xff;
        if (bankMsb == LEGACY_MELODIC_BANK_MSB) {
            int splitCount = body[9] & 0xff;
            if (splitCount == 0) {
                return null;
            }
            int program = body[6] & 0x7f;
            int keyLow = body[7] & 0x7f;
            int keyHigh = body[8] & 0x7f;
            byte[] payload = legacyPayload(body);
            if (u16(payload, 13) <= 0) {
                return null;
            }
            return fromPayload(
                    LEGACY_MELODIC_BANK_MSB,
                    program,
                    false,
                    keyLow,
                    keyHigh,
                    payload);
        }
        if (bankMsb == LEGACY_DRUM_BANK_MSB) {
            int midiNote = body[7] & 0x7f;
            int drumKey = Math.max(0, midiNote - LEGACY_MIDI_DRUM_BASE);
            byte[] payload = legacyPayload(body);
            if (u16(payload, 13) <= 0) {
                return null;
            }
            return fromPayload(
                    LEGACY_DRUM_BANK_MSB,
                    drumKey,
                    true,
                    0,
                    127,
                    payload);
        }
        return null;
    }

    private static MA5PcmVoiceProgram fromPayload(int bankLsb,
                                                  int program,
                                                  boolean drumVoice,
                                                  int keyLow,
                                                  int keyHigh,
                                                  byte[] payload) {
        int b2 = payload[2] & 0xff;
        int b3 = payload[3] & 0xff;
        int b4 = payload[4] & 0xff;
        int b5 = payload[5] & 0xff;
        int b6 = payload[6] & 0xff;
        int b7 = payload[7] & 0xff;
        int b8 = payload[8] & 0xff;
        int b15 = payload[15] & 0xff;

        return new MA5PcmVoiceProgram(
                bankLsb,
                program,
                drumVoice,
                u16(payload, 0),
                (b2 >> 3) & 0x1f,
                (b2 & 0x01) != 0,
                (b3 >> 6) & 0x03,
                (b6 >> 4) & 0x0f,
                b5 & 0x0f,
                (b4 >> 4) & 0x0f,
                (b5 >> 4) & 0x0f,
                b6 & 0x0f,
                (b7 >> 2) & 0x3f,
                (b8 >> 5) & 0x03,
                (b8 >> 1) & 0x03,
                (b4 & 0x08) != 0,
                (b4 & 0x02) != 0,
                (b8 & 0x10) != 0,
                (b8 & 0x01) != 0,
                u16(payload, 11),
                u16(payload, 13),
                (b15 & 0x80) != 0,
                b15 & 0x7f,
                keyLow,
                keyHigh,
                payload);
    }

    private static byte[] legacyPayload(byte[] body) {
        byte[] payload = new byte[VM5_PCM_PAYLOAD_BYTES];
        int fixedBytes = Math.min(9, Math.max(0, body.length - LEGACY_FIXED_FIELD_OFFSET));
        System.arraycopy(body, LEGACY_FIXED_FIELD_OFFSET, payload, 0, fixedBytes);
        if (body.length >= LEGACY_MIN_PCM_BYTES) {
            payload[9] = body[19];
            payload[10] = body[20];
            payload[11] = body[22];
            payload[12] = body[23];
            payload[13] = body[24];
            payload[14] = body[25];
            payload[15] = body[body.length - 1];
        }
        return payload;
    }

    private static int u16(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }

    private static int clampMidiKey(int key) {
        return Math.max(0, Math.min(127, key));
    }

    private static byte[] normalize(byte[] packet) {
        if (packet == null || packet.length == 0) {
            return new byte[0];
        }
        int start = 0;
        int end = trimF7(packet, packet.length);
        if (packet.length >= 4 && (packet[0] & 0xff) == 0xff && (packet[1] & 0xff) == 0xf0) {
            start = 3;
            end = trimF7(packet, Math.min(packet.length, start + (packet[2] & 0xff)));
        } else if (packet.length >= 4 && (packet[0] & 0xff) == 0xff && (packet[1] & 0xff) == 0xf1) {
            start = 4;
            int bodyLength = (packet[2] & 0xff) | ((packet[3] & 0xff) << 8);
            end = trimF7(packet, Math.min(packet.length, start + bodyLength));
        } else if ((packet[0] & 0xff) == 0xf0) {
            start = 1;
            end = trimF7(packet, packet.length);
        }
        if (end < start) {
            end = start;
        }
        return Arrays.copyOfRange(packet, start, end);
    }

    private static int trimF7(byte[] packet, int end) {
        int clampedEnd = Math.max(0, Math.min(packet.length, end));
        if (clampedEnd > 0 && (packet[clampedEnd - 1] & 0xff) == 0xf7) {
            return clampedEnd - 1;
        }
        return clampedEnd;
    }
}
