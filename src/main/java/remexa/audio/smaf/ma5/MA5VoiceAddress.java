package remexa.audio.smaf.ma5;

public record MA5VoiceAddress(int bankMsb, int bankLsb, int program, int keyLow, int keyHigh) {
    static MA5VoiceAddress decodeLegacy(byte[] body) {
        if (body.length < 10) return null;
        int msb = body[4] & 0xff;
        int lsb = body[5] & 0xff;
        int program = body[6] & 0xff;
        if (msb == 0x78) msb = 0x7d;
        if (msb == 0x79) msb = 0x7c;
        if (msb == 0x7d) {
            if (lsb != 0 || program > 9) return null;
            int note = body[7] & 0x7f;
            return new MA5VoiceAddress(msb, lsb, program, note, note);
        }
        if (msb != 0x7c || lsb > 9 || program > 127) return null;
        int low = (body[7] & 0x80) == 0 ? 0 : body[7] & 0x7f;
        int high = (body[7] & 0x80) == 0 ? 127 : body[8] & 0x7f;
        return low <= high ? new MA5VoiceAddress(msb, lsb, program, low, high) : null;
    }

    public int programKey() {
        return (bankMsb << 16) | (bankLsb << 8) | program;
    }

    public boolean contains(int midiNote) {
        return midiNote >= keyLow && midiNote <= keyHigh;
    }
}
