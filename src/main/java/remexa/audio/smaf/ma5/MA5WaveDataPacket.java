package remexa.audio.smaf.ma5;

import java.util.Arrays;

/**
 * Raw MA-5 user-wave payload decoded from an MMMG {@code EXWV} subchunk.
 */
public record MA5WaveDataPacket(int waveId,
                                int encodedBytes,
                                byte[] encodedData) {
    private static final int MANUFACTURER_YAMAHA = 0x43;
    private static final int FAMILY_VM5 = 0x05;
    private static final int VM5_WAVE_DATA = 0x00;
    private static final int HEADER_BYTES = 4;

    public MA5WaveDataPacket {
        encodedData = encodedData.clone();
    }

    public static MA5WaveDataPacket decode(byte[] packet) {
        byte[] body = normalize(packet);
        if (body.length <= HEADER_BYTES
                || (body[0] & 0xff) != MANUFACTURER_YAMAHA
                || (body[1] & 0xff) != FAMILY_VM5
                || (body[2] & 0xff) != VM5_WAVE_DATA) {
            return null;
        }

        int waveId = body[3] & 0xff;
        byte[] encodedData = Arrays.copyOfRange(body, HEADER_BYTES, body.length);
        return new MA5WaveDataPacket(waveId, encodedData.length, encodedData);
    }

    public String summary() {
        return "waveData id=" + waveId
                + " encodedBytes=" + encodedBytes
                + " head=" + hex(encodedData, 12);
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
            end = Math.min(packet.length, start + bodyLength);
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

    private static String hex(byte[] data, int maxBytes) {
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
}
