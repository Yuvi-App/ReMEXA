package remexa.audio.smaf.fuetrek;

import java.util.Arrays;

/**
 * Parses SoftBank/Yamaha EXVO packets carried as raw SMAF SysEx data.
 */
final class SoftbankExvoPacket {
    enum Kind {
        WAVE_UPLOAD,
        VOICE_PROGRAM,
        CONTROL_TEMPLATE,
        CONTROL_UPLOAD_81,
        CONTROL_PITCH_BEND_RANGE,
        CONTROL_UNKNOWN,
        UNKNOWN
    }

    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int YAMAHA_MA_EXVO_FAMILY = 0x04;
    private static final int SOFTBANK_EXVO_FAMILY = 0x05;

    private final byte[] data;
    private final int end;
    private final Kind kind;
    private final int family;
    private final int type;
    private final int subtype;
    private final int channel;
    private final int groupId;
    private final int objectRaw;
    private final int objectIndex;
    private final int payloadOffset;

    private SoftbankExvoPacket(
            byte[] data,
            int end,
            Kind kind,
            int family,
            int type,
            int subtype,
            int channel,
            int groupId,
            int objectRaw,
            int objectIndex,
            int payloadOffset) {
        this.data = data;
        this.end = end;
        this.kind = kind;
        this.family = family;
        this.type = type;
        this.subtype = subtype;
        this.channel = channel;
        this.groupId = groupId;
        this.objectRaw = objectRaw;
        this.objectIndex = objectIndex;
        this.payloadOffset = payloadOffset;
    }

    static SoftbankExvoPacket parse(byte[] message) {
        if (message == null || message.length < 3
                || (message[0] & 0xff) != MANUFACTURER_YAMAHA
                || !isKnownExvoFamily(message[1] & 0xff)) {
            return null;
        }
        int end = message.length;
        if (end > 0 && (message[end - 1] & 0xff) == 0xf7) {
            end--;
        }
        int family = message[1] & 0xff;
        int type = message[2] & 0xff;
        if (family == YAMAHA_MA_EXVO_FAMILY) {
            return parseYamahaMaPacket(message, end, family, type);
        }
        return switch (type) {
            case 0x00 -> parseWaveUpload(message, end, family, type);
            case 0x01 -> parseVoiceProgram(message, end, family, type);
            case 0x02 -> parseControl(message, end, family, type);
            default -> new SoftbankExvoPacket(message, end, Kind.UNKNOWN, family, type, -1, -1, -1, -1, -1, end);
        };
    }

    private static boolean isKnownExvoFamily(int family) {
        return family == YAMAHA_MA_EXVO_FAMILY || family == SOFTBANK_EXVO_FAMILY;
    }

    private static SoftbankExvoPacket parseYamahaMaPacket(byte[] message, int end, int family, int type) {
        if (type == 0x01 && end > 5) {
            // Older MA/VM35 EXVO chunks use `43 04 01 gg oo ...`, where gg is
            // the FueTrek ROM group and oo is the object selector. The program
            // slot is implicit: chunks are applied in VOIC order.
            int groupId = message[3] & 0xff;
            int objectRaw = message[4] & 0xff;
            return new SoftbankExvoPacket(message, end, Kind.VOICE_PROGRAM, family, type,
                    -1, -1, groupId, objectRaw, decodeObjectIndex(objectRaw), 5);
        }
        if (type == 0x02) {
            if (end > 4) {
                int channel = message[3] & 0xff;
                return new SoftbankExvoPacket(message, end, Kind.CONTROL_PITCH_BEND_RANGE, family, type,
                        -1, channel, -1, -1, -1, 4);
            }
            return new SoftbankExvoPacket(message, end, Kind.CONTROL_UNKNOWN, family, type,
                    -1, -1, -1, -1, -1, Math.min(end, 4));
        }
        return new SoftbankExvoPacket(message, end, Kind.UNKNOWN, family, type,
                -1, -1, -1, -1, -1, end);
    }

    private static SoftbankExvoPacket parseWaveUpload(byte[] message, int end, int family, int type) {
        int waveId = end > 3 ? message[3] & 0x7f : -1;
        return new SoftbankExvoPacket(message, end, Kind.WAVE_UPLOAD, family, type,
                -1, -1, waveId, -1, -1, 4);
    }

    private static SoftbankExvoPacket parseVoiceProgram(byte[] message, int end, int family, int type) {
        // `43 05 01 bb pp ...` is a VM3/VM5 FM voice program.  The bytes after
        // the bank/program header are the VM35 voice body; keep the legacy
        // channel/object interpretation as a ROM fallback, but expose the real
        // voice body as the payload.
        int channel = end > 4 ? message[4] & 0xff : -1;
        int groupId = end > 6 ? message[6] & 0xff : -1;
        int objectRaw = end > 7 ? message[7] & 0xff : -1;
        return new SoftbankExvoPacket(message, end, Kind.VOICE_PROGRAM, family, type, -1, channel,
                groupId, objectRaw, decodeObjectIndex(objectRaw), 5);
    }

    private static SoftbankExvoPacket parseControl(byte[] message, int end, int family, int type) {
        int subtype = end > 3 ? message[3] & 0xff : -1;
        if (subtype == 0x81) {
            return new SoftbankExvoPacket(message, end, Kind.CONTROL_UPLOAD_81, family, type, subtype,
                    -1, -1, -1, -1, 4);
        }
        if (subtype == 0x01) {
            int channel = end > 4 ? message[4] & 0xff : -1;
            int groupId = end > 7 ? message[7] & 0xff : -1;
            int objectRaw = end > 8 ? message[8] & 0xff : -1;
            return new SoftbankExvoPacket(message, end, Kind.CONTROL_TEMPLATE, family, type, subtype,
                    channel, groupId, objectRaw, decodeObjectIndex(objectRaw), 9);
        }
        return new SoftbankExvoPacket(message, end, Kind.CONTROL_UNKNOWN, family, type, subtype,
                -1, -1, -1, -1, Math.min(end, 4));
    }

    Kind kind() {
        return kind;
    }

    int type() {
        return type;
    }

    int family() {
        return family;
    }

    int subtype() {
        return subtype;
    }

    int channel() {
        return channel;
    }

    int groupId() {
        return groupId;
    }

    int objectRaw() {
        return objectRaw;
    }

    int objectIndex() {
        return objectIndex;
    }

    int payloadOffset() {
        return payloadOffset;
    }

    int payloadLength() {
        return Math.max(0, end - Math.min(payloadOffset, end));
    }

    byte[] payload() {
        if (payloadOffset >= end) {
            return new byte[0];
        }
        return Arrays.copyOfRange(data, payloadOffset, end);
    }

    boolean hasTrailingF7() {
        return end < data.length;
    }

    String payloadShape() {
        int length = payloadLength();
        if (kind == Kind.VOICE_PROGRAM && length >= 17) {
            return "vm35-fm-" + length;
        }
        if (kind == Kind.VOICE_PROGRAM) {
            return "vm35-fm-short-" + length;
        }
        if (kind == Kind.CONTROL_TEMPLATE) {
            return "template-tail-" + length;
        }
        return "payload-" + length;
    }

    String summary() {
        StringBuilder sb = new StringBuilder(96);
        sb.append("kind=").append(kind)
                .append(" family=0x").append(hex2(family))
                .append(" type=0x").append(hex2(type));
        if (subtype >= 0) {
            sb.append(" subtype=0x").append(hex2(subtype));
        }
        if (channel >= 0) {
            sb.append(" ch=").append(channel);
        }
        if (groupId >= 0) {
            sb.append(" group=0x").append(hex2(groupId));
        }
        if (objectRaw >= 0) {
            sb.append(" objectRaw=0x").append(hex2(objectRaw))
                    .append(" object=").append(objectIndex);
        }
        sb.append(" ").append(payloadShape());
        if (hasTrailingF7()) {
            sb.append(" f7");
        }
        return sb.toString();
    }

    static int decodeObjectIndex(int rawValue) {
        if (rawValue < 0) {
            return -1;
        }
        int index = rawValue & 0x3f;
        if ((rawValue & 0xc0) != 0) {
            index |= 0x40;
        }
        return index;
    }

    private static String hex2(int value) {
        return String.format("%02x", value & 0xff);
    }
}
