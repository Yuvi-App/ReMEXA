package com.jblend.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class InflateInputStream extends InputStream {
    // JBlend's read boundaries are observable by games. These are not the JDK
    // InflaterInputStream defaults; see the MEXA compatibility regression tests.
    private static final int BUFFER_SIZE = 1024;

    private final InputStream input;
    private final Inflater inflater = new Inflater();
    private final byte[] compressed = new byte[BUFFER_SIZE];
    private final byte[] expanded = new byte[BUFFER_SIZE];
    private int position;
    private int limit;
    private boolean eof;
    private boolean closed;

    protected InflateInputStream() {
        input = InputStream.nullInputStream();
        eof = true;
    }

    public InflateInputStream(InputStream in) throws IOException {
        input = Objects.requireNonNull(in);
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return prepareOutput() ? expanded[position++] & 0xFF : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        ensureOpen();
        if (!prepareOutput()) {
            return -1;
        }
        int count = Math.min(len, limit - position);
        System.arraycopy(expanded, position, b, off, count);
        position += count;
        return count;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        closed = true;
        try {
            input.close();
        } finally {
            inflater.end();
        }
    }

    private boolean prepareOutput() throws IOException {
        if (position < limit) {
            return true;
        }
        if (eof) {
            return false;
        }
        position = 0;
        limit = 0;
        while (limit < expanded.length) {
            try {
                // Drain pending output before requesting more compressed input;
                // needsInput() can be true while a decoded match is unfinished.
                int count = inflater.inflate(expanded, limit, expanded.length - limit);
                limit += count;
                if (inflater.finished()) {
                    eof = true;
                    break;
                }
                if (inflater.needsDictionary()) {
                    throw new IOException("Inflate error: status = 2");
                }
                if (count == 0 && !inflater.needsInput()) {
                    throw new IOException("Inflate error: can not inflate.");
                }
            } catch (DataFormatException exception) {
                limit = 0;
                throw new IOException("Inflate error: status = -3", exception);
            }
            if (limit == expanded.length) {
                break;
            }
            if (inflater.needsInput()) {
                int count = readCompressed();
                if (count == 0) {
                    // JBlend returns a partial final output buffer even when the
                    // zlib trailer is absent. An empty refill is still an error.
                    // Do not require Inflater.finished() as the JDK stream does.
                    eof = true;
                    if (limit == 0) {
                        throw new IOException("Inflate error: can not inflate.");
                    }
                    break;
                }
                inflater.setInput(compressed, 0, count);
            }
        }
        return limit > 0;
    }

    private int readCompressed() throws IOException {
        // The reference JBlend stream fills its compressed buffer with read(),
        // independently of an underlying stream's bulk-read implementation.
        int count = 0;
        while (count < compressed.length) {
            int value = input.read();
            if (value < 0) {
                break;
            }
            compressed[count++] = (byte) value;
        }
        return count;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("stream is closed.");
        }
    }
}
