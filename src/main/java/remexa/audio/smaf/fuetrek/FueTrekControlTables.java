package remexa.audio.smaf.fuetrek;

import java.io.IOException;
import java.io.InputStream;

/**
 * Native lookup tables used by the FueTrek control/template packet decoders.
 *
 * <p>The names intentionally track the template offsets they feed in
 * {@code MFiSynth_ft.dll} because the higher-level semantics are still inferred
 * from call flow rather than official symbols.
 */
final class FueTrekControlTables {
    static final int CURVE_16 = 0;
    static final int CURVE_18 = 1;
    static final int CURVE_1A = 2;
    static final int CURVE_1E = 3;
    static final int CURVE_20 = 4;
    static final int CURVE_24 = 5;
    static final int CURVE_28 = 6;
    static final int CURVE_2A = 7;
    static final int CURVE_2C = 8;
    static final int CURVE_32 = 9;
    static final int CURVE_34 = 10;
    static final int CURVE_36 = 11;
    private static final String RESOURCE_NAME = "fuetrek-control-tables.bin";
    private static final int MAGIC = 0x4c425446; // FTBL
    private static final int VERSION = 1;
    private static final int[][] CURVES = loadCurves();

    private FueTrekControlTables() {
    }

    static int quantize(int curveId, int value) {
        int[] table = CURVES[curveId];
        int first = signedWord(table[0]);
        if (value <= first) {
            return first;
        }
        int last = signedWord(table[table.length - 1]);
        if (value >= last) {
            return last;
        }
        for (int i = table.length - 2; i >= 0; i--) {
            if (signedWord(table[i]) < value) {
                return signedWord(table[i + 1]);
            }
        }
        return first;
    }

    private static int signedWord(int value) {
        return (short) value;
    }

    private static int[][] loadCurves() {
        byte[] data = readResourceBytes(RESOURCE_NAME);
        Reader reader = new Reader(data);
        int magic = reader.u32();
        if (magic != MAGIC) {
            throw new IllegalStateException("Invalid FueTrek control table magic: 0x" + Integer.toHexString(magic));
        }
        int version = reader.u16();
        if (version != VERSION) {
            throw new IllegalStateException("Unsupported FueTrek control table version: " + version);
        }
        int curveCount = reader.u16();
        int[] lengths = new int[curveCount];
        for (int i = 0; i < curveCount; i++) {
            lengths[i] = reader.u16();
        }
        int[][] curves = new int[curveCount][];
        for (int i = 0; i < curveCount; i++) {
            curves[i] = new int[lengths[i]];
            for (int j = 0; j < lengths[i]; j++) {
                curves[i][j] = reader.u16();
            }
        }
        reader.expectFullyConsumed();
        if (curveCount != 12) {
            throw new IllegalStateException("Unexpected FueTrek control curve count: " + curveCount);
        }
        return curves;
    }

    private static byte[] readResourceBytes(String resourceName) {
        try (InputStream input = FueTrekControlTables.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Missing FueTrek resource " + resourceName);
            }
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load FueTrek resource " + resourceName, exception);
        }
    }

    private static final class Reader {
        private final byte[] data;
        private int offset;

        private Reader(byte[] data) {
            this.data = data;
        }

        private int u16() {
            int value = (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
            offset += 2;
            return value;
        }

        private int u32() {
            int value = u16();
            return value | (u16() << 16);
        }

        private void expectFullyConsumed() {
            if (offset != data.length) {
                throw new IllegalStateException(
                        "FueTrek control table resource has trailing data: " + (data.length - offset));
            }
        }
    }
}
