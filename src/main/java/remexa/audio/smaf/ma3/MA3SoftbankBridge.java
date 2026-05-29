package remexa.audio.smaf.ma3;

import remexa.audio.smaf.SmafDebug;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Translates SoftBank/Yamaha SMAF packets into the internal MA-3 SysEx format
 * understood by the OpenDoJa sampler.
 */
public final class MA3SoftbankBridge {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int YAMAHA_MA_LEGACY_FAMILY = 0x03;
    private static final int SOFTBANK_EXVO_FAMILY = 0x05;

    private static final int REALTIME_CHANNELS = 16;
    private static final double LOG2 = Math.log(2.0);

    private final Sampler sampler;
    private final Set<String> loggedUnsupportedPackets = new HashSet<>();
    private int implicitLegacyProgramSlot;

    // Per-channel latches for in-sequence FM exclusives (43 03 90 reg val).
    // B0 writes the low pitch byte, C0 the high byte; both must be latched
    // (flags == 3) before a key-on or key-off is issued. Flags persist across
    // events so each subsequent B0/C0 write re-issues, matching the chip
    // (MA3SMWEMU.DLL sub_1000ea70).
    private final int[] realtimeLatchLow = new int[REALTIME_CHANNELS];
    private final int[] realtimeLatchHigh = new int[REALTIME_CHANNELS];
    private final int[] realtimeLatchFlags = new int[REALTIME_CHANNELS];
    private final int[] realtimeActiveKey = new int[REALTIME_CHANNELS];
    private final boolean[] realtimeNoteActive = new boolean[REALTIME_CHANNELS];

    public MA3SoftbankBridge(Sampler sampler) {
        this.sampler = sampler;
    }

    public void reset() {
        implicitLegacyProgramSlot = 0;
        loggedUnsupportedPackets.clear();
        Arrays.fill(realtimeLatchLow, 0);
        Arrays.fill(realtimeLatchHigh, 0);
        Arrays.fill(realtimeLatchFlags, 0);
        Arrays.fill(realtimeActiveKey, 0);
        Arrays.fill(realtimeNoteActive, false);
    }

    public boolean sysEx(int sourceBank, byte[] message) {
        if (message == null || message.length < 2) {
            return false;
        }
        byte[] normalized = unwrapRawVendorEnvelope(message);
        if (normalized != message) {
            return sysEx(sourceBank, normalized);
        }
        if (message.length < 3 || (message[0] & 0xff) != MANUFACTURER_YAMAHA) {
            return false;
        }

        int family = message[1] & 0xff;
        int type = message[2] & 0xff;
        if (family == YAMAHA_MA_LEGACY_FAMILY) {
            return switch (type) {
                case 0x00 -> applyLegacyVoiceProgram(message);
                case 0x90 -> applyRealtimeFm(sourceBank, message);
                default -> unsupported(message);
            };
        }
        if (family == SOFTBANK_EXVO_FAMILY) {
            debugRawPacket("softbank-exvo-pending", message);
            return true;
        }
        return unsupported(message);
    }

    private boolean applyLegacyVoiceProgram(byte[] message) {
        int end = trimF7(message);
        if (end <= 2) {
            return false;
        }

        LegacyVoiceProgram voice = LegacyVoiceProgram.decode(Arrays.copyOfRange(message, 2, end));
        if (voice == null) {
            debugRawPacket("legacy-voice-invalid", message);
            return false;
        }

        int slot = implicitLegacyProgramSlot++ & 0x3f;
        registerFmAlgorithm(voice, 0, slot);

        if (SmafDebug.isEnabled("smaf", SmafDebug.Level.DEBUG)) {
            SmafDebug.debug("smaf", String.format(
                    "[MA3SoftBank] legacy voice slot=%d alg=%d ops=%d",
                    slot,
                    voice.algorithm,
                    voice.operatorCount()));
        }
        return true;
    }

    /**
     * Handles in-sequence FM exclusives of the form {@code 43 03 90 reg val},
     * which SoftBank/J-Phone SFX use for real-time key-on and pitch sweeps
     * directly on chip pitch registers. Without this, such SFX collapse to a
     * single dull SEQU note (e.g. the Argus/Rygar "dying" sound).
     *
     * <p>{@code reg & 0xF0 == 0xB0} latches the low pitch byte, {@code 0xC0}
     * the high byte. Once both are latched the 13-bit pitch word
     * {@code P = ((high & 0x1F) << 8) | low} is decoded. {@code high & 0x20}
     * is the key-on bit: set re-pitches/keys-on the channel's single voice,
     * clear sets the final pitch and then keys it OFF (release). This mirrors
     * the chip's per-channel mono voice: cmd2 (sub_10015d70) attacks a fresh
     * voice only when its slot is idle and otherwise just re-pitches without
     * retriggering, while cmd5 (sub_10015ce0 -> sub_100235d0) sets gate=0.
     * The exact {@code P -> Hz} curve is reverse-engineered from MA3SMWEMU.DLL
     * (sub_10015ce0 / sub_10015d70):</p>
     * <pre>
     * block  = (P >> 10) & 7
     * fnum10 = ((P & 0x3FF) * 0x10911) >> 16   // x 1.0354
     * if (fnum10 > 0x3FF) { fnum10 -= 0x400; if (block < 7) block++; }
     * freqHz = (fnum10 << block) * 48000 / 2^20
     * </pre>
     */
    private boolean applyRealtimeFm(int sourceBank, byte[] message) {
        int end = trimF7(message);
        if (end < 5) {
            debugRawPacket("legacy-note-control-short", message);
            return true;
        }
        int reg = message[3] & 0xff;
        int val = message[4] & 0xff;
        int channel = realtimeChannel(sourceBank, reg);

        int regHigh = reg & 0xf0;
        if (regHigh == 0xB0) {
            realtimeLatchLow[channel] = val;
            realtimeLatchFlags[channel] |= 0x1;
        } else if (regHigh == 0xC0) {
            realtimeLatchHigh[channel] = val;
            realtimeLatchFlags[channel] |= 0x2;
        } else {
            debugRawPacket("legacy-note-control-reg", message);
            return true;
        }

        if ((realtimeLatchFlags[channel] & 0x3) != 0x3) {
            return true; // wait for both B0 and C0 before acting
        }

        int high = realtimeLatchHigh[channel];
        int pitchWord = ((high & 0x1f) << 8) | realtimeLatchLow[channel];
        boolean keyOn = (high & 0x20) != 0;

        int block = (pitchWord >> 10) & 0x7;
        int fnum10 = ((pitchWord & 0x3ff) * 0x10911) >> 16;
        if (fnum10 > 0x3ff) {
            fnum10 -= 0x400;
            if (block < 7) {
                block++;
            }
        }

        double freqHz = ((double) (fnum10 << block)) * 48000.0 / 1048576.0;
        if (freqHz <= 0.0) {
            return true;
        }
        double semisFromA4 = 12.0 * (Math.log(freqHz / 440.0) / LOG2);

        // bendRange = 1/12 makes pitchBend(channel, s) shift exactly s semitones,
        // applying any pitch offset on top of the held note's integer key.
        sampler.pitchBendRange(channel, 1.0f / 12.0f);

        if (keyOn) {
            if (realtimeNoteActive[channel]) {
                // Voice already sounding: glide its pitch, no envelope retrigger
                // (chip sub_10015d70 marker-active path re-pitches only).
                sampler.pitchBend(channel, (float) (semisFromA4 - realtimeActiveKey[channel]));
            } else {
                // Fresh key-on: cut any prior releasing note on this mono channel,
                // then attack a new voice (chip allocates the channel's one voice).
                sampler.keyOff(channel, realtimeActiveKey[channel]);
                int key = clampRealtimeKey((int) Math.round(semisFromA4));
                sampler.pitchBend(channel, (float) (semisFromA4 - key));
                sampler.keyOn(channel, key, 1.0f);
                realtimeActiveKey[channel] = key;
                realtimeNoteActive[channel] = true;
            }
        } else {
            // Key-on bit clear = key-off: set the final pitch first so the release
            // tail decays at the swept pitch, then release the voice
            // (chip sub_10015ce0 -> sub_100235d0 sets gate=0). Further bit-clear
            // writes keep re-pitching the still-releasing note.
            sampler.pitchBend(channel, (float) (semisFromA4 - realtimeActiveKey[channel]));
            sampler.keyOff(channel, realtimeActiveKey[channel]);
            realtimeNoteActive[channel] = false;
        }
        return true;
    }

    private static int realtimeChannel(int sourceBank, int reg) {
        int lane = reg & 0x3;
        if (sourceBank >= 0) {
            return ((sourceBank & 0x3) << 2) | lane;
        }
        return lane;
    }

    private static int clampRealtimeKey(int key) {
        int min = -MA3SamplerProvider.A4;
        int max = 127 - MA3SamplerProvider.A4;
        if (key < min) {
            return min;
        }
        return Math.min(key, max);
    }

    private void registerFmAlgorithm(LegacyVoiceProgram voice, int bank, int program) {
        byte[] translated = voice.toMa3FmAlgorithmMessage(bank & 0x7f, program & 0x7f);
        sampler.sysEx(translated);
    }

    private boolean unsupported(byte[] message) {
        int family = message.length > 1 ? message[1] & 0xff : -1;
        if (family > 0x05) {
            return false;
        }
        String key = hex(message, Math.min(message.length, 12));
        if (loggedUnsupportedPackets.add(key)) {
            debugRawPacket("unsupported", message);
        }
        return true;
    }

    private static byte[] unwrapRawVendorEnvelope(byte[] message) {
        if (message.length < 5) {
            return message;
        }
        if ((message[0] & 0xff) != 0xff || (message[1] & 0xff) != 0xf0) {
            return message;
        }
        int end = trimF7(message);
        if (end <= 3) {
            return message;
        }
        return Arrays.copyOfRange(message, 3, end);
    }

    private static int trimF7(byte[] message) {
        return message.length > 0 && (message[message.length - 1] & 0xff) == 0xf7
                ? message.length - 1
                : message.length;
    }

    private static void debugRawPacket(String source, byte[] message) {
        if (!SmafDebug.isEnabled("smaf", SmafDebug.Level.DEBUG)) {
            return;
        }
        SmafDebug.debug("smaf", "[MA3SoftBank] " + source + " " + hex(message, Math.min(message.length, 24)));
    }

    private static String hex(byte[] data, int maxBytes) {
        StringBuilder sb = new StringBuilder(maxBytes * 3);
        int limit = Math.min(data.length, maxBytes);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i] & 0xff));
        }
        if (data.length > maxBytes) {
            sb.append(" ...");
        }
        return sb.toString();
    }

    private static final class LegacyVoiceProgram {
        /**
         * MA-3 chip-internal panpot byte for "center". The synthesized SysEx
         * emitted by {@link #toMa3FmAlgorithmMessage} stores
         * {@code (panpot << 3) | basicOctave} in this slot; {@code panpot=15}
         * decodes via {@link MA3Algorithm#initVolume()} to
         * {@code volLeft = volRight = 0.5}.
         *
         * <p>VMA family-0x03 voices have no per-voice panpot field — the
         * decompiled MA3SMWEMU's {@code (edi[4] & 3) | 0x80} write was a
         * chip-side register-select marker, not a per-voice pan setting. Per
         * the smaf825 canonical reference ({@code vmafm.go:206}), payload
         * byte 1 is a constant {@code 0x01} marker. Channel-level pan
         * arrives separately via MIDI CC10.</p>
         *
         * <p>Low two bits encode {@code basicOctave = 1} so the legacy path
         * leaves pitch unshifted; {@code basicOctave = 0} would transpose the
         * voice up by an octave</p>
         */
        private static final int CENTER_PANPOT_BYTE = (15 << 3) | 0x01;

        private final int lfo;
        private final int algorithm;
        private final byte[] operatorBytes;

        private LegacyVoiceProgram(int lfo, int algorithm, byte[] operatorBytes) {
            this.lfo = lfo;
            this.algorithm = algorithm;
            this.operatorBytes = operatorBytes;
        }

        static LegacyVoiceProgram decode(byte[] data) {
            if (data == null || data.length < 10 || (data[0] & 0xff) != 0x00) {
                return null;
            }
            int header0 = data[3] & 0xff;
            int algorithm = header0 & 0x7;
            int operatorCount = operatorCount(algorithm);
            int requiredLength = 5 + (operatorCount * 5);
            if (data.length < requiredLength) {
                return null;
            }

            byte[] operators = new byte[operatorCount * 7];
            int sharedBits = (header0 >> 3) & 0x7;
            for (int i = 0; i < operatorCount; i++) {
                byte[] decoded = decodeLegacyOperator(data, 5 + (i * 5), i == 0 ? sharedBits : 0);
                if (decoded == null) {
                    return null;
                }
                System.arraycopy(decoded, 0, operators, i * 7, decoded.length);
            }
            return new LegacyVoiceProgram((header0 >> 6) & 0x3, algorithm, operators);
        }

        byte[] toMa3FmAlgorithmMessage(int bank, int program) {
            int type = operatorCount() == 2 ? 1 : 2;
            int entrySize = type == 1 ? 20 : 34;
            byte[] message = new byte[4 + entrySize];
            message[0] = 0x11;
            message[1] = 0x01;
            message[2] = (byte) 0xf0;
            message[3] = 0x04;

            int offset = 4;
            message[offset] = (byte) type;
            message[offset + 1] = (byte) bank;
            message[offset + 2] = (byte) program;
            message[offset + 3] = 0x00;
            message[offset + 4] = (byte) CENTER_PANPOT_BYTE;
            message[offset + 5] = (byte) (((lfo & 0x3) << 6) | (algorithm & 0x7));
            System.arraycopy(operatorBytes, 0, message, offset + 6, operatorBytes.length);
            return message;
        }

        int operatorCount() {
            return operatorBytes.length / 7;
        }

        private static int operatorCount(int algorithm) {
            return algorithm < 2 ? 2 : 4;
        }

        /**
         * Translates a 5-byte VMA family-0x03 FM operator into the 7-byte
         * MA-3 chip-internal operator layout consumed by
         * {@link MA3Operator#MA3Operator(byte[], int, boolean)}.
         *
         * <p>VMA input layout (smaf825 reference {@code vmafm.go}):</p>
         * <pre>
         * +0 | MULT[7:4] | VIB | EGT | SUS | KSR |
         * +1 | RR[7:4]   | DR[3:0]               |
         * +2 | AR[7:4]   | SL[3:0]               |
         * +3 | TL[7:2]   | KSL[1:0]              |
         * +4 | DVB[7:6]  | DAM[5:4] | AM[3] | WS[2:0] |
         * </pre>
         *
         * <p>MA-3 chip-internal output layout (matches {@code VM35FMOperator}
         * in smaf825):</p>
         * <pre>
         * +0 | SR[7:4] | XOF | - | SUS | KSR |
         * +1 | RR[7:4] | DR[3:0]              |
         * +2 | AR[7:4] | SL[3:0]              |
         * +3 | TL[7:2] | KSL[1:0]             |
         * +4 | - | DAM | EAM | - | DVB[2:1] | EVB |
         * +5 | MULT[7:4] | - | DT[2:0]            |
         * +6 | WS[5:3] | FB[2:0]                  |
         * </pre>
         *
         * <p>Per VMA→VM35 mapping in {@code vmafm.go ToVM35()}:
         * {@code SR = EGT ? 0 : RR} (EGT means infinite sustain). XOF/DT
         * have no VMA equivalent; emitted as zero. {@code FB} comes from the
         * voice's shared bits and is only applied to operator 0.</p>
         */
        private static byte[] decodeLegacyOperator(byte[] data, int offset, int sharedBits) {
            if (offset < 0 || offset + 5 > data.length) {
                return null;
            }
            int b0 = data[offset] & 0xff;     // MULT|VIB|EGT|SUS|KSR
            int b1 = data[offset + 1] & 0xff; // RR|DR
            int b2 = data[offset + 2] & 0xff; // AR|SL
            int b3 = data[offset + 3] & 0xff; // TL|KSL
            int b4 = data[offset + 4] & 0xff; // DVB|DAM|AM|WS

            int mult = (b0 >> 4) & 0xf;
            int vib  = (b0 >> 3) & 0x1;
            int egt  = (b0 >> 2) & 0x1;
            int sus  = (b0 >> 1) & 0x1;
            int ksr  =  b0       & 0x1;

            int rr = (b1 >> 4) & 0xf;
            int sr = (egt != 0) ? 0 : rr; // EGT → infinite sustain (SR=0)

            int dam = (b4 >> 4) & 0x3;
            int eam = (b4 >> 3) & 0x1;
            int dvb = (b4 >> 6) & 0x3;
            int ws  =  b4       & 0x07;   // VMA WS is 3 bits

            return new byte[]{
                    (byte) ((sr << 4) | (sus << 1) | ksr),                  // +0 SR|XOF=0|-|SUS|KSR
                    (byte) ((rr << 4) | (b1 & 0xf)),                        // +1 RR|DR
                    (byte) b2,                                              // +2 AR|SL
                    (byte) b3,                                              // +3 TL|KSL passthrough
                    (byte) ((dam << 5) | (eam << 4) | (dvb << 1) | vib),    // +4 -|DAM|EAM|-|DVB|EVB
                    (byte) (mult << 4),                                     // +5 MULT|DT=0
                    (byte) ((ws << 3) | (sharedBits & 0x7))                 // +6 WS|FB
            };
        }
    }
}
