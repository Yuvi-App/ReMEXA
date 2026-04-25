package com.jblend.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class InflateInputStream extends InputStream {
    private final InputStream delegate;

    protected InflateInputStream() {
        this.delegate = InputStream.nullInputStream();
    }

    public InflateInputStream(InputStream in) throws IOException {
        this.delegate = createInflaterStream(in);
    }

    private static InputStream createInflaterStream(InputStream in) throws IOException {
        PushbackInputStream probe = new PushbackInputStream(new BufferedInputStream(in), 2);
        byte[] header = new byte[2];
        int count = probe.read(header);
        if (count > 0) {
            probe.unread(header, 0, count);
        }

        boolean nowrap = count == 2 && !looksLikeZlibHeader(header[0] & 0xFF, header[1] & 0xFF);
        return new InflaterInputStream(probe, new Inflater(nowrap));
    }

    private static boolean looksLikeZlibHeader(int cmf, int flg) {
        if ((cmf & 0x0F) != 8) {
            return false;
        }
        if (((cmf << 8) | flg) % 31 != 0) {
            return false;
        }
        return (cmf >> 4) <= 7;
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
