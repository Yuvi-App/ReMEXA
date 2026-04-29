package remexa.audio.smaf.ma5;

import remexa.audio.smaf.SmafDebug;
import remexa.audio.smaf.ma3.Sampler;

import java.util.HashSet;
import java.util.Set;

/**
 * Loads MA-5/VM5 voice packets into the current temporary FM renderer.
 */
public final class MA5SoftbankBridge {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_VM5 = 0x05;
    private static final int VM5_WAVE_DATA = 0x00;
    private static final int VM5_FM_PROGRAM = 0x01;
    private static final int VM5_PCM_PROGRAM = 0x02;

    private final Sampler sampler;
    private final PcmVoiceSink pcmVoiceSink;
    private final Set<String> loggedPackets = new HashSet<>();

    public MA5SoftbankBridge(Sampler sampler) {
        this(sampler, PcmVoiceSink.NOOP);
    }

    public MA5SoftbankBridge(Sampler sampler, PcmVoiceSink pcmVoiceSink) {
        this.sampler = sampler;
        this.pcmVoiceSink = pcmVoiceSink == null ? PcmVoiceSink.NOOP : pcmVoiceSink;
    }

    public void reset() {
        loggedPackets.clear();
    }

    public boolean sysEx(byte[] message) {
        byte[] body = normalize(message);
        if (body.length < 3 || (body[0] & 0xff) != MANUFACTURER_YAMAHA) {
            return false;
        }
        int family = body[1] & 0xff;
        int type = body[2] & 0xff;
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
    }
}
