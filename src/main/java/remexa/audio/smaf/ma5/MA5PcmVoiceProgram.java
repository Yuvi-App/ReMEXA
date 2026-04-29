package remexa.audio.smaf.ma5;

import java.util.Arrays;

/**
 * Raw Yamaha VM35/VM5 PCM voice metadata decoded from {@code 43 05 02 ...}.
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
                                 byte[] payload) {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_VM5 = 0x05;
    private static final int VM5_PCM_PROGRAM = 0x02;
    private static final int VM5_PCM_HEADER_BYTES = 5;
    private static final int VM5_PCM_PAYLOAD_BYTES = 16;

    public MA5PcmVoiceProgram {
        payload = payload.clone();
    }

    public static MA5PcmVoiceProgram decode(byte[] packet) {
        byte[] body = normalize(packet);
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
                payload);
    }

    public String summary() {
        return "pcmVoice bankLsb=" + bankLsb
                + " program=" + program
                + " drumVoice=" + drumVoice
                + " fs=" + frequencySetting
                + " waveId=" + waveId
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

    private static int u16(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
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
