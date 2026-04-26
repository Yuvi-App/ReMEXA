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
        CONTROL_UNKNOWN,
        UNKNOWN
    }

    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int SOFTBANK_EXVO_FAMILY = 0x05;

    private final byte[] data;
    private final int end;
    private final Kind kind;
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
                || (message[1] & 0xff) != SOFTBANK_EXVO_FAMILY) {
            return null;
        }
        int end = message.length;
        if (end > 0 && (message[end - 1] & 0xff) == 0xf7) {
            end--;
        }
        int type = message[2] & 0xff;
        return switch (type) {
            case 0x00 -> parseWaveUpload(message, end, type);
            case 0x01 -> parseVoiceProgram(message, end, type);
            case 0x02 -> parseControl(message, end, type);
            default -> new SoftbankExvoPacket(message, end, Kind.UNKNOWN, type, -1, -1, -1, -1, -1, end);
        };
    }

    private static SoftbankExvoPacket parseWaveUpload(byte[] message, int end, int type) {
        int waveId = end > 3 ? message[3] & 0x7f : -1;
        return new SoftbankExvoPacket(message, end, Kind.WAVE_UPLOAD, type, -1, -1, waveId, -1, -1, 4);
    }

    private static SoftbankExvoPacket parseVoiceProgram(byte[] message, int end, int type) {
        // `43 05 01 bb pp ...` is a VM3/VM5 FM voice program.  The bytes after
        // the bank/program header are the VM35 voice body; keep the legacy
        // channel/object interpretation as a ROM fallback, but expose the real
        // voice body as the payload.
        int channel = end > 4 ? message[4] & 0xff : -1;
        int groupId = end > 6 ? message[6] & 0xff : -1;
        int objectRaw = end > 7 ? message[7] & 0xff : -1;
        return new SoftbankExvoPacket(message, end, Kind.VOICE_PROGRAM, type, -1, channel,
                groupId, objectRaw, decodeObjectIndex(objectRaw), 5);
    }

    private static SoftbankExvoPacket parseControl(byte[] message, int end, int type) {
        int subtype = end > 3 ? message[3] & 0xff : -1;
        if (subtype == 0x81) {
            return new SoftbankExvoPacket(message, end, Kind.CONTROL_UPLOAD_81, type, subtype,
                    -1, -1, -1, -1, 4);
        }
        if (subtype == 0x01) {
            int channel = end > 4 ? message[4] & 0xff : -1;
            int groupId = end > 7 ? message[7] & 0xff : -1;
            int objectRaw = end > 8 ? message[8] & 0xff : -1;
            return new SoftbankExvoPacket(message, end, Kind.CONTROL_TEMPLATE, type, subtype,
                    channel, groupId, objectRaw, decodeObjectIndex(objectRaw), 9);
        }
        return new SoftbankExvoPacket(message, end, Kind.CONTROL_UNKNOWN, type, subtype,
                -1, -1, -1, -1, Math.min(end, 4));
    }

    Kind kind() {
        return kind;
    }

    int type() {
        return type;
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
