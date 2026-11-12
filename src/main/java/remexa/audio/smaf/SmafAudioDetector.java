package remexa.audio.smaf;

import javax.microedition.media.decoders.SMAFDecoder;
import remexa.audio.smaf.ma5.MA5VoiceProgram;
import java.util.ArrayList;
import java.util.List;

final class SmafAudioDetector {
    private SmafAudioDetector() {
    }

    static SmafAudioFamily detect(byte[] source,
                                  List<byte[]> startupPackets,
                                  List<byte[]> exclusiveVoices,
                                  List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        if (containsYamahaFamily(startupPackets, 0x07)
                || containsYamahaFamily(exclusiveVoices, 0x07)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x07)) {
            return SmafAudioFamily.MA7;
        }
        if (containsYamahaFamily(startupPackets, 0x05)
                || containsYamahaFamily(exclusiveVoices, 0x05)
                || containsYamahaFamily(startupPackets, 0x04)
                || containsYamahaFamily(exclusiveVoices, 0x04)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x04)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x05)
                || containsLegacySoftbankMa5Packet(startupPackets)
                || containsLegacySoftbankMa5Packet(exclusiveVoices)
                || containsLegacySoftbankMa5Events(sequenceSysExEvents)
                || supportsVmaPhrase(startupPackets, exclusiveVoices, sequenceSysExEvents)) {
            return SmafAudioFamily.MA5;
        }
        if (containsYamahaFamily(startupPackets, 0x03)
                || containsYamahaFamily(exclusiveVoices, 0x03)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x03)
                || containsMa3Packet(startupPackets)
                || containsMa3Packet(exclusiveVoices)
                || containsMa3Events(sequenceSysExEvents)) {
            return SmafAudioFamily.MA3;
        }
        return SmafAudioFamily.UNKNOWN;
    }

    private static boolean supportsVmaPhrase(List<byte[]> startupPackets,
                                             List<byte[]> exclusiveVoices,
                                             List<SMAFDecoder.SequenceSysExEvent> events) {
        boolean phrase = false, voice = false;
        var packets = new ArrayList<byte[]>();
        if (startupPackets != null) packets.addAll(startupPackets);
        if (exclusiveVoices != null) packets.addAll(exclusiveVoices);
        if (events != null) for (var event : events) {
            byte[] data = event.data();
            if (data.length >= 2 && data[0] == 0x72 && data[1] == 0x0e) phrase = true;
            packets.add(data);
        }
        for (byte[] packet : packets) {
            if (packet == null || packet.length == 0) continue;
            byte[] body = normalizeVendorBody(packet);
            if (body.length == 0 || body[0] != 0x43) continue;
            // MA5 supports the voice records, but not MA3's 43 03 90 pitch-register events.
            if (body.length < 3 || body[1] != 3 || body[2] != 0
                    || MA5VoiceProgram.decode(packet) == null) return false;
            voice = true;
        }
        return phrase && voice;
    }

    private static boolean containsYamahaFamily(List<byte[]> packets, int family) {
        if (packets == null) {
            return false;
        }
        for (byte[] packet : packets) {
            if (containsYamahaFamily(packet, family)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsYamahaFamilyEvents(List<SMAFDecoder.SequenceSysExEvent> events, int family) {
        if (events == null) {
            return false;
        }
        for (SMAFDecoder.SequenceSysExEvent event : events) {
            if (containsYamahaFamily(event.data(), family)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMa3Packet(List<byte[]> packets) {
        if (packets == null) {
            return false;
        }
        for (byte[] packet : packets) {
            if (looksLikeMa3SamplerPacket(packet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMa3Events(List<SMAFDecoder.SequenceSysExEvent> events) {
        if (events == null) {
            return false;
        }
        for (SMAFDecoder.SequenceSysExEvent event : events) {
            if (looksLikeMa3SamplerPacket(event.data())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLegacySoftbankMa5Packet(List<byte[]> packets) {
        if (packets == null) {
            return false;
        }
        for (byte[] packet : packets) {
            if (looksLikeLegacySoftbankMa5Packet(packet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLegacySoftbankMa5Events(List<SMAFDecoder.SequenceSysExEvent> events) {
        if (events == null) {
            return false;
        }
        for (SMAFDecoder.SequenceSysExEvent event : events) {
            if (looksLikeLegacySoftbankMa5Packet(event.data())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsYamahaFamily(byte[] data, int family) {
        if (data == null || data.length < 2) {
            return false;
        }
        byte[] body = normalizeVendorBody(data);
        if (body.length >= 2
                && (body[0] & 0xFF) == 0x43
                && (body[1] & 0xFF) == family) {
            return true;
        }
        if (body.length >= 3
                && (body[0] & 0xFF) == 0x43
                && (body[1] & 0xFF) == 0x79) {
            int version = body[2] & 0xFF;
            return (family == 0x05 && version == 0x07)
                    || (family == 0x03 && version == 0x06);
        }
        return false;
    }

    private static boolean looksLikeLegacySoftbankMa5Packet(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        byte[] body = normalizeVendorBody(data);
        if (body.length < 4
                || (body[0] & 0xff) != 0x43
                || (body[1] & 0xff) != 0x02
                || (body[2] & 0xff) != 0x02) {
            return false;
        }
        int type = body[3] & 0xff;
        return type == 0x08 || type == 0x0a;
    }

    private static byte[] normalizeVendorBody(byte[] data) {
        int start = 0;
        int end = trimF7(data, data.length);
        if (data.length >= 4 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xF0) {
            start = 3;
            end = trimF7(data, Math.min(data.length, start + (data[2] & 0xFF)));
        } else if (data.length >= 4 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xF1) {
            start = 4;
            int bodyLength = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
            end = trimF7(data, Math.min(data.length, start + bodyLength));
        } else if ((data[0] & 0xFF) == 0xF0) {
            start = 1;
        }
        if (end < start) {
            end = start;
        }
        byte[] body = new byte[end - start];
        System.arraycopy(data, start, body, 0, body.length);
        return body;
    }

    private static int trimF7(byte[] data, int end) {
        int clampedEnd = Math.max(0, Math.min(data.length, end));
        if (clampedEnd > 0 && (data[clampedEnd - 1] & 0xFF) == 0xF7) {
            return clampedEnd - 1;
        }
        return clampedEnd;
    }

    private static boolean looksLikeMa3SamplerPacket(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        for (int i = 0; i <= data.length - 4; i++) {
            if ((data[i] & 0xFF) == 0x11
                    && (data[i + 1] & 0xFF) == 0x01
                    && (data[i + 2] & 0xF0) == 0xF0
                    && (data[i + 3] & 0xFF) <= 0x06) {
                return true;
            }
        }
        return false;
    }
}
