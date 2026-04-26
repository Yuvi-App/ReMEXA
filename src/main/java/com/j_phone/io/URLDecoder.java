package com.j_phone.io;

public final class URLDecoder {
    private URLDecoder() {
    }

    public static java.lang.String decode (java.lang.String in) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.URLDecoder", "decode", in);
        if (in == null) {
            throw new NullPointerException("URLDecoder.decode: input is null");
        }
        int len = in.length();
        var bytes = new java.io.ByteArrayOutputStream(len);
        int i = 0;
        while (i < len) {
            char c = in.charAt(i);
            if (c == '+') {
                bytes.write(' ');
                i++;
            } else if (c == '%') {
                if (i + 2 >= len) {
                    throw new IllegalArgumentException(
                            "URLDecoder.decode: truncated escape at offset " + i);
                }
                int hi = hexValue(in.charAt(i + 1));
                int lo = hexValue(in.charAt(i + 2));
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException(
                            "URLDecoder.decode: invalid escape at offset " + i);
                }
                bytes.write((hi << 4) | lo);
                i += 3;
            } else {
                // Non-encoded characters are ASCII per the encode() rules; emit as a single byte.
                bytes.write(c & 0xFF);
                i++;
            }
        }
        return new String(bytes.toByteArray(), java.nio.charset.Charset.defaultCharset());
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        return -1;
    }
}
