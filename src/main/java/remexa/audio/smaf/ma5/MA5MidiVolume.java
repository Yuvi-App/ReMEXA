package remexa.audio.smaf.ma5;

/** Master controls in the native SoftBank SMF player, before channel quantization. */
public final class MA5MidiVolume {
    private int yamaha = 76;
    private int midi = 127;

    public void reset() { yamaha = 76; midi = 127; }

    public int combinedLevel(int volume, int expression, int hostVolume) {
        return MA5MixTables.combinedLevel(volume, expression, hostVolume, yamaha, midi);
    }

    public boolean sysEx(byte[] message) {
        if (message == null || message.length == 0) return false;
        int start = (message[0] & 0xff) == 0xf0 ? 1 : 0;
        int length = message.length - start;
        if (length != 6 && length != 7) return false;
        if (message[start] == 0x7f && message[start + 1] == 0x7f
                && message[start + 2] == 4 && message[start + 3] == 1) {
            midi = message[start + 5] & 0x7f;
            return true;
        }
        if (message[start] == 0x43 && message[start + 1] == 0x79
                && message[start + 2] == 6 && message[start + 3] == 0x7f
                && message[start + 4] == 0) {
            yamaha = message[start + 5] & 0x7f;
            return true;
        }
        return false;
    }
}
