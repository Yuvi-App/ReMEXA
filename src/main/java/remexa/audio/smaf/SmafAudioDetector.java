package remexa.audio.smaf;

import javax.microedition.media.decoders.SMAFDecoder;
import java.util.List;

final class SmafAudioDetector {
    private SmafAudioDetector() {
    }

    static SmafAudioFamily detect(byte[] source,
                                  List<byte[]> startupPackets,
                                  List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        if (containsYamahaFamily(source, 0x07)
                || containsYamahaFamily(startupPackets, 0x07)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x07)) {
            return SmafAudioFamily.MA7;
        }
        if (containsYamahaFamily(source, 0x05)
                || containsYamahaFamily(startupPackets, 0x05)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x05)) {
            return SmafAudioFamily.MA5;
        }
        if (containsYamahaFamily(source, 0x03)
                || containsYamahaFamily(startupPackets, 0x03)
                || containsYamahaFamilyEvents(sequenceSysExEvents, 0x03)
                || containsMa3Packet(startupPackets)
                || containsMa3Events(sequenceSysExEvents)) {
            return SmafAudioFamily.MA3;
        }
        return SmafAudioFamily.UNKNOWN;
    }

    private static boolean containsYamahaFamily(List<byte[]> packets, int family) {
        for (byte[] packet : packets) {
            if (containsYamahaFamily(packet, family)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsYamahaFamilyEvents(List<SMAFDecoder.SequenceSysExEvent> events, int family) {
        for (SMAFDecoder.SequenceSysExEvent event : events) {
            if (containsYamahaFamily(event.data(), family)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMa3Packet(List<byte[]> packets) {
        for (byte[] packet : packets) {
            if (looksLikeMa3SamplerPacket(packet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMa3Events(List<SMAFDecoder.SequenceSysExEvent> events) {
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
        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == 0x43 && (data[i + 1] & 0xFF) == family) {
                return true;
            }
        }
        return false;
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
