package remexa.audio.smaf;

import javax.microedition.media.decoders.SMAFDecoder;
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
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x05)) {
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
