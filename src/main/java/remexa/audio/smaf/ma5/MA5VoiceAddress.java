package remexa.audio.smaf.ma5;

public record MA5VoiceAddress(int bankMsb, int bankLsb, int program, int keyLow, int keyHigh) {
    static MA5VoiceAddress decodeLegacy(byte[] body) {
        if (isNamedVoice(body)) {
            if (body.length < 25) return null;
            return legacyAddress(body[20] & 0xff, body[21] & 0xff,
                    body[22] & 0xff, body[23] & 0xff, 0, false);
        }
        if (body.length < 10) return null;
        return legacyAddress(body[4] & 0xff, body[5] & 0xff,
                body[6] & 0xff, body[7] & 0xff, body[8] & 0xff, true);
    }

    static boolean isNamedVoice(byte[] body) {
        return isNamedMetadata(body) && body[3] == 0;
    }

    static boolean isNamedMetadata(byte[] body) {
        return body != null && body.length >= 4
                && body[0] == 0x43 && body[1] == 2 && body[2] == 0;
    }

    private static MA5VoiceAddress legacyAddress(int msb, int lsb, int program,
                                                int key, int highKey, boolean ranges) {
        if (msb == 0x7a) msb = 0x7c;
        if (msb == 0x7b) msb = 0x7d;
        if (msb == 0x78) msb = 0x7d;
        if (msb == 0x79) msb = 0x7c;
        if (msb == 0x7d) {
            if (lsb != 0 || program > 9) return null;
            if (key > 127) return null;
            int note = key;
            return new MA5VoiceAddress(msb, lsb, program, note, note);
        }
        if (msb != 0x7c || lsb > 9 || program > 127) return null;
        int low = !ranges || (key & 0x80) == 0 ? 0 : key & 0x7f;
        int high = !ranges || (key & 0x80) == 0 ? 127 : highKey & 0x7f;
        return low <= high ? new MA5VoiceAddress(msb, lsb, program, low, high) : null;
    }

    public int programKey() {
        return (bankMsb << 16) | (bankLsb << 8) | program;
    }

    public boolean contains(int midiNote) {
        return midiNote >= keyLow && midiNote <= keyHigh;
    }
}
