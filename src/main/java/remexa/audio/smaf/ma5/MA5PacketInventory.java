package remexa.audio.smaf.ma5;

import remexa.audio.smaf.SmafDebug;

import javax.microedition.media.decoders.SMAFDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies the Yamaha/SoftBank packets that matter to the MA-5 rebuild.
 */
public final class MA5PacketInventory {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_LEGACY_MA = 0x03;
    private static final int FAMILY_VM5_VOICE = 0x05;
    private static final int FAMILY_MA5_VOICE_ALT = 0x04;

    private final int sourceBytes;
    private final int raw43x03Occurrences;
    private final int raw43x04Occurrences;
    private final int raw43x05Occurrences;
    private final int startupPacketCount;
    private final int exclusiveVoiceCount;
    private final int sequenceSysExCount;
    private final List<PacketRecord> packets;
    private final Map<PacketKind, Integer> counts;

    private MA5PacketInventory(int sourceBytes,
                               int raw43x03Occurrences,
                               int raw43x04Occurrences,
                               int raw43x05Occurrences,
                               int startupPacketCount,
                               int exclusiveVoiceCount,
                               int sequenceSysExCount,
                               List<PacketRecord> packets) {
        this.sourceBytes = sourceBytes;
        this.raw43x03Occurrences = raw43x03Occurrences;
        this.raw43x04Occurrences = raw43x04Occurrences;
        this.raw43x05Occurrences = raw43x05Occurrences;
        this.startupPacketCount = startupPacketCount;
        this.exclusiveVoiceCount = exclusiveVoiceCount;
        this.sequenceSysExCount = sequenceSysExCount;
        this.packets = List.copyOf(packets);
        this.counts = countKinds(packets);
    }

    public static MA5PacketInventory analyze(byte[] source,
                                             List<byte[]> startupPackets,
                                             List<byte[]> exclusiveVoices,
                                             List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        List<PacketRecord> records = new ArrayList<>();
        addPackets(records, PacketOrigin.STARTUP, startupPackets);
        addPacketsDistinct(records, PacketOrigin.EXVO, exclusiveVoices, startupPackets);
        addSequencePackets(records, sequenceSysExEvents);
        return new MA5PacketInventory(
                source == null ? 0 : source.length,
                countYamahaFamilyOccurrences(source, FAMILY_LEGACY_MA),
                countYamahaFamilyOccurrences(source, FAMILY_MA5_VOICE_ALT),
                countYamahaFamilyOccurrences(source, FAMILY_VM5_VOICE),
                startupPackets == null ? 0 : startupPackets.size(),
                exclusiveVoices == null ? 0 : exclusiveVoices.size(),
                sequenceSysExEvents == null ? 0 : sequenceSysExEvents.size(),
                records);
    }

    public boolean hasVm5Content() {
        return count(PacketKind.VM5_FM_VOICE) > 0
                || count(PacketKind.VM35_VOICE) > 0
                || count(PacketKind.MA5_ALT_VOICE) > 0
                || count(PacketKind.VM5_MARKER) > 0
                || raw43x05Occurrences > 0;
    }

    public int count(PacketKind kind) {
        return counts.getOrDefault(kind, 0);
    }

    public List<PacketRecord> packets() {
        return packets;
    }

    public String summary() {
        return "sourceBytes=" + sourceBytes
                + " raw43x03=" + raw43x03Occurrences
                + " raw43x04=" + raw43x04Occurrences
                + " raw43x05=" + raw43x05Occurrences
                + " startupPackets=" + startupPacketCount
                + " exclusiveVoices=" + exclusiveVoiceCount
                + " sequenceSysEx=" + sequenceSysExCount
                + " uniquePackets=" + packets.size()
                + " vm5FmVoices=" + count(PacketKind.VM5_FM_VOICE)
                + " vm35Voices=" + count(PacketKind.VM35_VOICE)
                + " ma5AltVoices=" + count(PacketKind.MA5_ALT_VOICE)
                + " vm5Markers=" + count(PacketKind.VM5_MARKER)
                + " vmaVoices=" + count(PacketKind.VMA_VOICE)
                + " otherYamaha=" + count(PacketKind.OTHER_YAMAHA)
                + " unknown=" + count(PacketKind.UNKNOWN);
    }

    public void log(String channel) {
        if (SmafDebug.isEnabled(channel, SmafDebug.Level.INFO)) {
            SmafDebug.info(channel, "[MA5] packet inventory " + summary());
        }
        if (!SmafDebug.isEnabled(channel, SmafDebug.Level.DEBUG)) {
            return;
        }
        for (PacketRecord packet : packets) {
            SmafDebug.debug(channel, "[MA5] " + packet.describe());
        }
    }

    private static void addPackets(List<PacketRecord> records, PacketOrigin origin, List<byte[]> packets) {
        if (packets == null) {
            return;
        }
        for (int i = 0; i < packets.size(); i++) {
            records.add(classify(origin, i, -1, -1, packets.get(i)));
        }
    }

    private static void addPacketsDistinct(List<PacketRecord> records,
                                           PacketOrigin origin,
                                           List<byte[]> packets,
                                           List<byte[]> alreadySeen) {
        if (packets == null) {
            return;
        }
        for (int i = 0; i < packets.size(); i++) {
            byte[] packet = packets.get(i);
            if (containsPacket(alreadySeen, packet)) {
                continue;
            }
            records.add(classify(origin, i, -1, -1, packet));
        }
    }

    private static void addSequencePackets(List<PacketRecord> records,
                                           List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents) {
        if (sequenceSysExEvents == null) {
            return;
        }
        for (int i = 0; i < sequenceSysExEvents.size(); i++) {
            SMAFDecoder.SequenceSysExEvent event = sequenceSysExEvents.get(i);
            records.add(classify(PacketOrigin.SEQUENCE, i, event.tick(), event.sourceBank(), event.data()));
        }
    }

    private static PacketRecord classify(PacketOrigin origin, int index, int tick, int sourceBank, byte[] packet) {
        NormalizedPacket normalized = normalize(packet);
        if (normalized.body.length < 2 || (normalized.body[0] & 0xff) != MANUFACTURER_YAMAHA) {
            return new PacketRecord(origin, index, tick, sourceBank, PacketKind.UNKNOWN,
                    -1, -1, normalized.enveloped, normalized.declaredLength, normalized.body.length,
                    -1, -1, "", hex(packet, 32));
        }

        int family = normalized.body[1] & 0xff;
        int type = normalized.body.length > 2 ? normalized.body[2] & 0xff : -1;
        PacketKind kind = switch (family) {
            case FAMILY_VM5_VOICE -> classifyVm5Family(normalized.body);
            case FAMILY_MA5_VOICE_ALT -> type == 0x01 ? PacketKind.MA5_ALT_VOICE : PacketKind.OTHER_YAMAHA;
            case FAMILY_LEGACY_MA -> type == 0x00 ? PacketKind.VMA_VOICE : PacketKind.OTHER_YAMAHA;
            default -> PacketKind.OTHER_YAMAHA;
        };
        int voiceBank = normalized.body.length > 3 ? normalized.body[3] & 0x7f : -1;
        int voiceProgram = normalized.body.length > 4 ? normalized.body[4] & 0x7f : -1;
        MA5VoiceProgram voiceProgramData = MA5VoiceProgram.decode(messageForPreview(packet, normalized.body));

        return new PacketRecord(origin, index, tick, sourceBank, kind, family, type,
                normalized.enveloped, normalized.declaredLength, normalized.body.length,
                voiceBank, voiceProgram, voiceProgramData == null ? "" : voiceProgramData.summary(), hex(packet, 32));
    }

    private static byte[] messageForPreview(byte[] original, byte[] normalizedBody) {
        return original == null || original.length == 0 ? normalizedBody : original;
    }

    private static PacketKind classifyVm5Family(byte[] body) {
        if (body.length < 3) {
            return PacketKind.OTHER_YAMAHA;
        }
        return switch (body[2] & 0xff) {
            case 0x01 -> body.length > 3 ? PacketKind.VM5_FM_VOICE : PacketKind.VM5_MARKER;
            case 0x02 -> PacketKind.VM35_VOICE;
            default -> PacketKind.OTHER_YAMAHA;
        };
    }

    private static NormalizedPacket normalize(byte[] packet) {
        if (packet == null || packet.length == 0) {
            return new NormalizedPacket(new byte[0], false, -1);
        }

        int start = 0;
        int end = trimF7(packet, packet.length);
        int declaredLength = -1;
        boolean enveloped = false;

        if (packet.length >= 4 && (packet[0] & 0xff) == 0xff && (packet[1] & 0xff) == 0xf0) {
            enveloped = true;
            declaredLength = packet[2] & 0xff;
            start = 3;
            int declaredEnd = Math.min(packet.length, start + declaredLength);
            end = trimF7(packet, declaredEnd);
        } else if (packet.length >= 2 && (packet[0] & 0xff) == 0xf0) {
            enveloped = true;
            start = 1;
            end = trimF7(packet, packet.length);
        }

        if (end < start) {
            end = start;
        }
        byte[] body = new byte[end - start];
        System.arraycopy(packet, start, body, 0, body.length);
        return new NormalizedPacket(body, enveloped, declaredLength);
    }

    private static int trimF7(byte[] packet, int end) {
        int clampedEnd = Math.max(0, Math.min(packet.length, end));
        if (clampedEnd > 0 && (packet[clampedEnd - 1] & 0xff) == 0xf7) {
            return clampedEnd - 1;
        }
        return clampedEnd;
    }

    private static Map<PacketKind, Integer> countKinds(List<PacketRecord> packets) {
        Map<PacketKind, Integer> counts = new EnumMap<>(PacketKind.class);
        for (PacketRecord packet : packets) {
            counts.merge(packet.kind, 1, Integer::sum);
        }
        return counts;
    }

    private static int countYamahaFamilyOccurrences(byte[] source, int family) {
        if (source == null || source.length < 2) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < source.length - 1; i++) {
            if ((source[i] & 0xff) == MANUFACTURER_YAMAHA && (source[i + 1] & 0xff) == family) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsPacket(List<byte[]> packets, byte[] candidate) {
        if (packets == null) {
            return false;
        }
        for (byte[] packet : packets) {
            if (Arrays.equals(packet, candidate)) {
                return true;
            }
        }
        return false;
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

    public enum PacketKind {
        VM5_MARKER,
        VM5_FM_VOICE,
        VM35_VOICE,
        MA5_ALT_VOICE,
        VMA_VOICE,
        OTHER_YAMAHA,
        UNKNOWN
    }

    public enum PacketOrigin {
        STARTUP,
        EXVO,
        SEQUENCE
    }

    public record PacketRecord(PacketOrigin origin,
                               int index,
                               int tick,
                               int sourceBank,
                               PacketKind kind,
                               int family,
                               int type,
                               boolean enveloped,
                               int declaredLength,
                               int bodyLength,
                               int voiceBank,
                               int voiceProgram,
                               String decodedVoiceSummary,
                               String preview) {
        private String describe() {
            return "origin=" + origin
                    + "[" + index + "]"
                    + (tick >= 0 ? " tick=" + tick : "")
                    + (sourceBank >= 0 ? " bank=" + sourceBank : "")
                    + " kind=" + kind
                    + (family >= 0 ? String.format(" family=0x%02X", family) : "")
                    + (type >= 0 ? String.format(" type=0x%02X", type) : "")
                    + " enveloped=" + enveloped
                    + (declaredLength >= 0 ? " declaredLen=" + declaredLength : "")
                    + " bodyLen=" + bodyLength
                    + (voiceBank >= 0 ? " voiceBank=" + voiceBank : "")
                    + (voiceProgram >= 0 ? " voiceProgram=" + voiceProgram : "")
                    + (decodedVoiceSummary == null || decodedVoiceSummary.isBlank() ? "" : " " + decodedVoiceSummary)
                    + " data=" + preview;
        }
    }

    private record NormalizedPacket(byte[] body, boolean enveloped, int declaredLength) {
    }
}
