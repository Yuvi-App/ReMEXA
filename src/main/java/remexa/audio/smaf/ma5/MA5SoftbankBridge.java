package remexa.audio.smaf.ma5;

import remexa.audio.smaf.SmafDebug;
import remexa.audio.smaf.ma3.Sampler;

import java.util.HashSet;
import java.util.Set;

/**
 * Loads VM5, VMA, and legacy SoftBank SMF instruments into the synthesizer.
 */
public final class MA5SoftbankBridge {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_LEGACY_SOFTBANK = 0x02;
    private static final int FAMILY_VMA = 0x03;
    private static final int FAMILY_VM5 = 0x05;
    private static final int FAMILY_VM5_COMPACT_4OP = 0x04;
    private static final int LEGACY_SOFTBANK_SUBFAMILY = 0x02;
    private static final int LEGACY_SOFTBANK_VOICE = 0x08;
    private static final int LEGACY_SOFTBANK_WAVE_DATA = 0x0a;
    private static final int VM5_WAVE_DATA = 0x00;
    private static final int VM5_FM_PROGRAM = 0x01;
    private static final int VM5_PCM_PROGRAM = 0x02;

    private final Sampler sampler;
    private final PcmVoiceSink pcmVoiceSink;
    private final int implicitCompactVoiceBank;
    private final Set<String> loggedPackets = new HashSet<>();
    private int implicitCompactVoiceSlot;

    public MA5SoftbankBridge(Sampler sampler) {
        this(sampler, PcmVoiceSink.NOOP);
    }

    public MA5SoftbankBridge(Sampler sampler, PcmVoiceSink pcmVoiceSink) {
        this(sampler, pcmVoiceSink, 0);
    }

    public MA5SoftbankBridge(Sampler sampler, PcmVoiceSink pcmVoiceSink, int implicitCompactVoiceBank) {
        this.sampler = sampler;
        this.pcmVoiceSink = pcmVoiceSink == null ? PcmVoiceSink.NOOP : pcmVoiceSink;
        this.implicitCompactVoiceBank = implicitCompactVoiceBank & 0x7f;
    }

    public void reset() {
        loggedPackets.clear();
        implicitCompactVoiceSlot = 0;
    }

    public boolean sysEx(byte[] message) {
        byte[] body = normalize(message);
        if (body.length < 3 || (body[0] & 0xff) != MANUFACTURER_YAMAHA) {
            return false;
        }
        int family = body[1] & 0xff;
        int type = body[2] & 0xff;
        if ((family == FAMILY_VM5_COMPACT_4OP && type == VM5_FM_PROGRAM)
                || (family == FAMILY_VMA && type == 0)) {
            MA5VoiceProgram voice = MA5VoiceProgram.decode(message);
            if (voice == null) {
                debugOnce("implicit-fm-invalid", message);
                return true;
            }
            int slot = implicitCompactVoiceSlot++ & 0x3f;
            // VMA and compact voices share EXVO-order slots (10033e00/10033c90).
            // MMMG EXVO slots belong to authored bank 1 (100df0a0).
            // They carry no bank byte of their own; generic MIDI retains bank 0.
            voice = voice.withBankProgram(implicitCompactVoiceBank, slot);
            sampler.sysEx(voice.toMa3FmAlgorithmMessage());
            pcmVoiceSink.onFmVoice(voice);
            if (SmafDebug.isEnabled("ma5", SmafDebug.Level.DEBUG)) {
                SmafDebug.debug("ma5", "[MA5] loaded implicit FM voice slot=" + slot + " " + voice.summary());
            }
            return true;
        }
        if (family == FAMILY_LEGACY_SOFTBANK && type == LEGACY_SOFTBANK_SUBFAMILY) {
            return legacySoftbankSysEx(message, body);
        }
        if (family == FAMILY_LEGACY_SOFTBANK && type == 0) {
            return namedSoftbankSysEx(message, body);
        }
        if (family != FAMILY_VM5) {
            return false;
        }
        if (type == VM5_WAVE_DATA) {
            MA5WaveDataPacket waveData = MA5WaveDataPacket.decode(message);
            if (waveData == null) {
                debugOnce("vm5-wave-invalid", message);
                return true;
            }
            pcmVoiceSink.onWaveData(waveData);
            debugOnce("vm5-wave-pending " + waveData.summary(), message);
            return true;
        }
        if (type == VM5_FM_PROGRAM) {
            MA5VoiceProgram voice = MA5VoiceProgram.decode(message);
            if (voice == null) {
                debugOnce("vm5-fm-invalid", message);
                return true;
            }
            sampler.sysEx(voice.toMa3FmAlgorithmMessage());
            pcmVoiceSink.onFmVoice(voice);
            if (SmafDebug.isEnabled("ma5", SmafDebug.Level.DEBUG)) {
                SmafDebug.debug("ma5", "[MA5] loaded VM5 FM voice " + voice.summary());
            }
            return true;
        }
        if (type == VM5_PCM_PROGRAM) {
            MA5PcmVoiceProgram pcmVoice = MA5PcmVoiceProgram.decode(message);
            if (pcmVoice == null) {
                debugOnce("vm5-pcm-invalid", message);
                return true;
            }
            pcmVoiceSink.onPcmVoice(pcmVoice);
            debugOnce("vm5-pcm-pending " + pcmVoice.summary(), message);
            return true;
        }
        debugOnce("vm5-unsupported", message);
        return true;
    }

    private boolean namedSoftbankSysEx(byte[] message, byte[] body) {
        if (body.length < 4) return true;
        if (body[3] == 1) {
            MA5WaveDataPacket wave = MA5WaveDataPacket.decode(message);
            if (wave != null) {
                pcmVoiceSink.onWaveData(wave);
                pcmVoiceSink.onLegacyPresetFallback();
            }
            else debugOnce("named-wave-invalid", message);
        } else if (body[3] == 0) {
            MA5VoiceAddress address = MA5VoiceAddress.decodeLegacy(body);
            if (address == null) { debugOnce("named-address-invalid", message); return true; }
            MA5VoiceProgram fm = MA5VoiceProgram.decode(message);
            MA5PcmVoiceProgram pcm = MA5PcmVoiceProgram.decode(message);
            if (fm != null) pcmVoiceSink.onLegacyFmVoice(address, fm);
            else if (pcm != null) pcmVoiceSink.onLegacyPcmVoice(address, pcm);
            else { debugOnce("named-voice-invalid", message); return true; }
            pcmVoiceSink.onLegacyPresetFallback();
        } else debugOnce("named-control-pending", message);
        return true;
    }

    private boolean legacySoftbankSysEx(byte[] message, byte[] body) {
        if (body.length < 4) {
            debugOnce("legacy-invalid", message);
            return true;
        }
        int packetType = body[3] & 0xff;
        if (packetType == LEGACY_SOFTBANK_WAVE_DATA) {
            MA5WaveDataPacket waveData = MA5WaveDataPacket.decode(message);
            if (waveData == null) {
                debugOnce("legacy-wave-invalid", message);
                return true;
            }
            pcmVoiceSink.onWaveData(waveData);
            debugOnce("legacy-wave " + waveData.summary(), message);
            return true;
        }
        if (packetType == LEGACY_SOFTBANK_VOICE && body.length >= 5) {
            MA5VoiceAddress address = MA5VoiceAddress.decodeLegacy(body);
            if (address != null) {
                MA5VoiceProgram fmVoice = MA5VoiceProgram.decode(message);
                if (fmVoice != null) {
                    pcmVoiceSink.onLegacyFmVoice(address, fmVoice);
                    debugOnce("legacy-fm " + address + " " + fmVoice.summary(), message);
                    return true;
                }
                MA5PcmVoiceProgram pcmVoice = MA5PcmVoiceProgram.decode(message);
                if (pcmVoice != null) {
                    pcmVoiceSink.onLegacyPcmVoice(address, pcmVoice);
                    debugOnce("legacy-pcm " + address + " " + pcmVoice.summary(), message);
                    return true;
                }
                debugOnce("legacy-fm-or-unsupported", message);
                return true;
            }
        }
        debugOnce("legacy-unsupported", message);
        return true;
    }

    private void debugOnce(String label, byte[] message) {
        if (!SmafDebug.isEnabled("ma5", SmafDebug.Level.DEBUG)) {
            return;
        }
        String key = label + ":" + hex(message, 24);
        if (loggedPackets.add(key)) {
            SmafDebug.debug("ma5", "[MA5] " + label + " " + hex(message, 32));
        }
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
        byte[] body = new byte[end - start];
        System.arraycopy(packet, start, body, 0, body.length);
        return body;
    }

    private static int trimF7(byte[] packet, int end) {
        int clampedEnd = Math.max(0, Math.min(packet.length, end));
        if (clampedEnd > 0 && (packet[clampedEnd - 1] & 0xff) == 0xf7) {
            return clampedEnd - 1;
        }
        return clampedEnd;
    }

    private static String hex(byte[] data, int maxBytes) {
        if (data == null) {
            return "";
        }
        int limit = Math.min(data.length, maxBytes);
        StringBuilder builder = new StringBuilder(limit * 3);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", data[i] & 0xff));
        }
        if (data.length > maxBytes) {
            builder.append(" ...");
        }
        return builder.toString();
    }

    public interface PcmVoiceSink {
        /** The older named dialect permits native ROM fallback for missing melodic voices. */
        default void onLegacyPresetFallback() { }
        PcmVoiceSink NOOP = new PcmVoiceSink() {
            @Override
            public void onWaveData(MA5WaveDataPacket waveData) {
            }

            @Override
            public void onPcmVoice(MA5PcmVoiceProgram voice) {
            }
        };

        void onWaveData(MA5WaveDataPacket waveData);

        void onPcmVoice(MA5PcmVoiceProgram voice);

        default void onFmVoice(MA5VoiceProgram voice) { }

        default void onLegacyFmVoice(MA5VoiceAddress address, MA5VoiceProgram voice) {
            onFmVoice(voice);
        }

        default void onLegacyPcmVoice(MA5VoiceAddress address, MA5PcmVoiceProgram voice) {
            onPcmVoice(voice);
        }
    }
}
