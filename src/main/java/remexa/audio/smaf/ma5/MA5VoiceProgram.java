package remexa.audio.smaf.ma5;

import java.util.Arrays;

/**
 * Raw Yamaha VM5 FM voice program decoded from SMAF EXVO data.
 *
 * <p>The layout mirrors smaf825's VM35FMVoice reader for packets shaped like
 * {@code 43 05 01 bank program ...}. The payload starts at byte 5 and contains
 * 3 global bytes followed by 2 or 4 seven-byte FM operator records.</p>
 */
public record MA5VoiceProgram(int bankLsb,
                              int program,
                              int drumKey,
                              int panpot,
                              int basicOctave,
                              int lfo,
                              boolean panpotEnable,
                              int algorithm,
                              Operator[] operators,
                              byte[] payload) {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_VM5 = 0x05;
    private static final int VM5_FM_PROGRAM = 0x01;
    private static final int VM5_HEADER_BYTES = 5;
    private static final int GLOBAL_BYTES = 3;
    private static final int OPERATOR_BYTES = 7;

    public MA5VoiceProgram {
        operators = operators.clone();
        payload = payload.clone();
    }

    public static MA5VoiceProgram decode(byte[] packet) {
        byte[] body = normalize(packet);
        if (body.length < VM5_HEADER_BYTES + GLOBAL_BYTES
                || (body[0] & 0xff) != MANUFACTURER_YAMAHA
                || (body[1] & 0xff) != FAMILY_VM5
                || (body[2] & 0xff) != VM5_FM_PROGRAM) {
            return null;
        }

        int bankLsb = body[3] & 0x7f;
        int program = body[4] & 0x7f;
        byte[] payload = Arrays.copyOfRange(body, VM5_HEADER_BYTES, body.length);
        int drumKey = payload[0] & 0xff;
        int panpot = (payload[1] >> 3) & 0x1f;
        int basicOctave = payload[1] & 0x03;
        int lfo = (payload[2] >> 6) & 0x03;
        boolean panpotEnable = (payload[2] & 0x20) != 0;
        int algorithm = payload[2] & 0x07;
        int operatorCount = operatorCount(algorithm);
        int requiredLength = GLOBAL_BYTES + operatorCount * OPERATOR_BYTES;
        if (payload.length != requiredLength) {
            return null;
        }

        Operator[] operators = new Operator[operatorCount];
        int offset = GLOBAL_BYTES;
        for (int i = 0; i < operatorCount; i++) {
            operators[i] = Operator.decode(i, payload, offset);
            offset += OPERATOR_BYTES;
        }
        return new MA5VoiceProgram(bankLsb, program, drumKey, panpot, basicOctave,
                lfo, panpotEnable, algorithm, operators, payload);
    }

    public int operatorCount() {
        return operators.length;
    }

    public String summary() {
        return "bankLsb=" + bankLsb
                + " program=" + program
                + " alg=" + algorithm
                + " ops=" + operatorCount()
                + " panpot=" + panpot
                + " bo=" + basicOctave
                + " lfo=" + lfo
                + " drumKey=" + drumKey;
    }

    public byte[] toMa3FmAlgorithmMessage() {
        int type = operatorCount() == 2 ? 1 : 2;
        int entrySize = type == 1 ? 20 : 34;
        byte[] message = new byte[4 + entrySize];
        message[0] = 0x11;
        message[1] = 0x01;
        message[2] = (byte) 0xf0;
        message[3] = 0x04;

        int offset = 4;
        message[offset] = (byte) type;
        message[offset + 1] = (byte) (bankLsb & 0x7f);
        message[offset + 2] = (byte) (program & 0x7f);
        message[offset + 3] = 0x00;
        message[offset + 4] = (byte) (((panpot & 0x1f) << 3) | (basicOctave & 0x03));
        message[offset + 5] = (byte) (((lfo & 0x03) << 6)
                | (panpotEnable ? 0x20 : 0x00)
                | (algorithm & 0x07));
        System.arraycopy(payload, GLOBAL_BYTES, message, offset + 6, operatorCount() * OPERATOR_BYTES);
        return message;
    }

    private static int operatorCount(int algorithm) {
        return algorithm < 2 ? 2 : 4;
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

    public record Operator(int index,
                           int multi,
                           int detune,
                           int attackRate,
                           int decayRate,
                           int sustainRate,
                           int releaseRate,
                           int sustainLevel,
                           int totalLevel,
                           int keyScaleLevel,
                           int amplitudeModDepth,
                           int vibratoDepth,
                           int feedback,
                           int waveShape,
                           boolean ignoreKeyOff,
                           boolean sustain,
                           boolean keyScaleRate,
                           boolean amplitudeModEnabled,
                           boolean vibratoEnabled) {
        private static Operator decode(int index, byte[] data, int offset) {
            int b0 = data[offset] & 0xff;
            int b1 = data[offset + 1] & 0xff;
            int b2 = data[offset + 2] & 0xff;
            int b3 = data[offset + 3] & 0xff;
            int b4 = data[offset + 4] & 0xff;
            int b5 = data[offset + 5] & 0xff;
            int b6 = data[offset + 6] & 0xff;
            return new Operator(
                    index,
                    (b5 >> 4) & 0x0f,
                    b5 & 0x07,
                    (b2 >> 4) & 0x0f,
                    b1 & 0x0f,
                    (b0 >> 4) & 0x0f,
                    (b1 >> 4) & 0x0f,
                    b2 & 0x0f,
                    (b3 >> 2) & 0x3f,
                    b3 & 0x03,
                    (b4 >> 5) & 0x03,
                    (b4 >> 1) & 0x03,
                    b6 & 0x07,
                    (b6 >> 3) & 0x1f,
                    (b0 & 0x08) != 0,
                    (b0 & 0x02) != 0,
                    (b0 & 0x01) != 0,
                    (b4 & 0x10) != 0,
                    (b4 & 0x01) != 0);
        }
    }
}
