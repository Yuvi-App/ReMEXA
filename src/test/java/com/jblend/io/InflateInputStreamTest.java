package com.jblend.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import org.junit.Test;

import static org.junit.Assert.*;

public class InflateInputStreamTest {
    @Test
    public void decodesZlibWithDifferentCallerBufferSizes() throws Exception {
        for (int size : new int[]{0, 515, 1024, 1025, 5000}) {
            byte[] expected = payload(size);
            for (int readSize : new int[]{1, 73, 1024, 4096}) {
                assertArrayEquals(expected, decode(compress(expected, false), readSize));
            }
        }
    }

    @Test
    public void returnsPartialFinalBufferWithoutAdlerTrailer() throws Exception {
        for (int size : new int[]{515, 1025, 5000}) {
            byte[] expected = payload(size);
            byte[] encoded = compress(expected, false);
            for (int readSize : new int[]{1, 73, 1024, 4096}) {
                assertArrayEquals(expected, decode(Arrays.copyOf(encoded, encoded.length - 4), readSize));
            }
        }
    }

    @Test
    public void returnsDecodedPrefixWhenInputEndsInsideDeflateData() throws Exception {
        // MEXA returns the available partial output even before DEFLATE's end
        // marker; it does not restrict this behavior to a missing checksum.
        byte[] encoded = compress(payload(1024), false);
        assertArrayEquals(payload(515), decode(Arrays.copyOf(encoded, encoded.length - 8), 4096));
    }

    @Test
    public void emptyRefillWithoutTrailerStillThrows() throws Exception {
        byte[] expected = payload(1024);
        byte[] encoded = compress(expected, false);
        for (int missing : new int[]{2, 4}) {
            try (var stream = stream(Arrays.copyOf(encoded, encoded.length - missing))) {
                byte[] actual = new byte[1024];
                assertEquals(1024, stream.read(actual));
                assertArrayEquals(expected, actual);
                assertThrows(IOException.class, stream::read);
                assertEquals(-1, stream.read());
            }
        }
    }

    @Test
    public void readsOnlyAvailableOutputBuffer() throws Exception {
        try (var stream = stream(compress(payload(5000), false))) {
            byte[] buffer = new byte[4096];
            assertEquals(1024, stream.read(buffer));
            assertEquals(1024, stream.read(buffer));
            assertEquals(1024, stream.read(buffer));
            assertEquals(1024, stream.read(buffer));
            assertEquals(904, stream.read(buffer));
            assertEquals(-1, stream.read(buffer));
        }
    }

    @Test
    public void corruptChecksumIsNotTreatedAsEof() throws Exception {
        byte[] encoded = compress(payload(1024), false);
        encoded[encoded.length - 1] ^= 1;
        try (var stream = stream(encoded)) {
            assertThrows(IOException.class, stream::read);
        }
    }

    @Test
    public void rejectsRawDeflateAndEmptyInput() throws Exception {
        for (byte[] encoded : new byte[][]{compress(payload(1024), true), new byte[0], {0x78}}) {
            try (var stream = stream(encoded)) {
                assertThrows(IOException.class, stream::read);
            }
        }
    }

    @Test
    public void handlesMultipleCompressedInputBuffersAndShortSourceReads() throws Exception {
        byte[] expected = new byte[20000];
        new Random(4).nextBytes(expected);
        byte[] encoded = compress(expected, false);
        assertArrayEquals(expected, decode(encoded, 4096));
        try (var stream = new InflateInputStream(new ByteArrayInputStream(encoded) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 7));
            }
        })) {
            assertArrayEquals(expected, drain(stream, 4096));
        }
    }

    @Test
    public void decodesAllCompressionModesAroundBufferAndWindowBoundaries() throws Exception {
        int[] sizes = {0, 1, 255, 256, 257, 1013, 1023, 1024, 1025,
                2037, 2047, 2048, 2049, 32767, 32768, 32769, 65537};
        for (int size : sizes) {
            for (boolean random : new boolean[]{false, true}) {
                byte[] expected = payload(size);
                if (random) {
                    new Random(size).nextBytes(expected);
                }
                for (int level : new int[]{0, 1, 6, 9}) {
                    for (int strategy : new int[]{Deflater.DEFAULT_STRATEGY,
                            Deflater.FILTERED, Deflater.HUFFMAN_ONLY}) {
                        byte[] encoded = compress(expected, false, level, strategy);
                        assertArrayEquals("size=" + size + ", random=" + random
                                        + ", level=" + level + ", strategy=" + strategy,
                                expected, decode(encoded, 4096));
                    }
                }
            }
        }
    }

    @Test
    public void stopsAtFirstZlibStreamAndSupportsSkippingBufferedBytes() throws Exception {
        byte[] expected = payload(5000);
        byte[] encoded = compress(expected, false);
        byte[] concatenated = new byte[encoded.length * 2];
        System.arraycopy(encoded, 0, concatenated, 0, encoded.length);
        System.arraycopy(encoded, 0, concatenated, encoded.length, encoded.length);
        assertArrayEquals(expected, decode(concatenated, 4096));
        try (var stream = stream(encoded)) {
            assertEquals(0, stream.available());
            assertFalse(stream.markSupported());
            assertThrows(IOException.class, stream::reset);
            assertEquals(0, stream.skip(-1));
            assertEquals(1023, stream.skip(1023));
            assertEquals(expected[1023] & 0xFF, stream.read());
            assertEquals(1025, stream.skip(1025));
            assertArrayEquals(Arrays.copyOfRange(expected, 2049, expected.length), drain(stream, 73));
            assertEquals(0, stream.skip(1));
        }
    }

    @Test
    public void singleByteReadsAreUnsignedAndShareTheBufferedPosition() throws Exception {
        byte[] expected = payload(1025);
        byte[] encoded = compress(expected, false);
        try (var stream = stream(Arrays.copyOf(encoded, encoded.length - 4))) {
            for (int i = 0; i < 257; i++) {
                assertEquals(expected[i] & 0xFF, stream.read());
            }
            byte[] tail = new byte[768];
            assertEquals(767, stream.read(tail, 0, 768));
            assertEquals(expected[1024] & 0xFF, stream.read());
            assertArrayEquals(Arrays.copyOfRange(expected, 257, 1024), Arrays.copyOf(tail, 767));
            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void propagatesSourceErrorsAndClosesUnderlyingStream() throws Exception {
        IOException failure = new IOException("source failed");
        boolean[] closed = {false};
        var stream = new InflateInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        });
        assertSame(failure, assertThrows(IOException.class, stream::read));
        stream.close();
        assertThrows(IOException.class, stream::close);
        assertTrue(closed[0]);
        assertThrows(IOException.class, stream::read);
    }

    @Test
    public void fillsCompressedBufferWithSingleByteSourceReads() throws Exception {
        byte[] encoded = compress(payload(1024), false);
        int[] reads = {0};
        try (var stream = new InflateInputStream(new ByteArrayInputStream(encoded) {
            @Override
            public synchronized int read() {
                reads[0]++;
                return super.read();
            }

            @Override
            public synchronized int read(byte[] b, int off, int len) {
                fail("JBlend must not call the source's bulk-read method");
                return -1;
            }
        })) {
            assertEquals(0, reads[0]);
            assertEquals(0, stream.read(new byte[1], 0, 0));
            assertEquals(encoded.length + 1, reads[0]);
            assertArrayEquals(payload(1024), drain(stream, 4096));
        }
    }

    @Test
    public void rejectsPresetDictionaryWithoutHanging() throws Exception {
        var deflater = new Deflater();
        byte[] encoded = new byte[100];
        int length;
        try {
            byte[] dictionary = payload(50);
            deflater.setDictionary(dictionary);
            deflater.setInput(dictionary);
            deflater.finish();
            length = deflater.deflate(encoded);
            assertTrue(deflater.finished());
        } finally {
            deflater.end();
        }
        try (var stream = stream(Arrays.copyOf(encoded, length))) {
            assertEquals("Inflate error: status = 2",
                    assertThrows(IOException.class, stream::read).getMessage());
        }
    }

    @Test
    public void validatesArgumentsAndChecksEofBeforeZeroLengthReads() throws Exception {
        assertThrows(NullPointerException.class, () -> new InflateInputStream(null));
        try (var stream = stream(compress(payload(515), false))) {
            assertThrows(NullPointerException.class, () -> stream.read(null, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[3], -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[3], 0, 4));
            assertEquals(0, stream.read(new byte[3], 0, 0));
            assertEquals(0, stream.read());
            assertEquals(514, drain(stream, 4096).length);
            assertEquals(-1, stream.read(new byte[3], 0, 0));
        }
    }

    private static InflateInputStream stream(byte[] encoded) throws IOException {
        return new InflateInputStream(new ByteArrayInputStream(encoded));
    }

    private static byte[] decode(byte[] encoded, int readSize) throws IOException {
        try (var stream = stream(encoded)) {
            return drain(stream, readSize);
        }
    }

    private static byte[] drain(InputStream stream, int readSize) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[readSize];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static byte[] payload(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private static byte[] compress(byte[] bytes, boolean raw) {
        return compress(bytes, raw, Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    private static byte[] compress(byte[] bytes, boolean raw, int level, int strategy) {
        var deflater = new Deflater(level, raw);
        try {
            deflater.setStrategy(strategy);
            deflater.setInput(bytes);
            deflater.finish();
            var output = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }
}
