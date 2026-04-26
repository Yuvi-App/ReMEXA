package com.j_phone.io;

public final class URLEncoder {
    private static final char[] HEX = {
            '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    private URLEncoder() {
    }

    public static java.lang.String encode (java.lang.String in) {
        remexa.probes.SdkStubSupport.log("com.j_phone.io.URLEncoder", "encode", in);
        if (in == null) {
            throw new NullPointerException("URLEncoder.encode: input is null");
        }
        var bytes = in.getBytes(java.nio.charset.Charset.defaultCharset());
        var out = new StringBuilder(bytes.length);
        for (byte raw : bytes) {
            int b = raw & 0xFF;
            if ((b >= 'a' && b <= 'z')
                    || (b >= 'A' && b <= 'Z')
                    || (b >= '0' && b <= '9')
                    || b == '.' || b == '-' || b == '*' || b == '_') {
                out.append((char) b);
            } else if (b == ' ') {
                out.append('+');
            } else {
                out.append('%').append(HEX[(b >>> 4) & 0xF]).append(HEX[b & 0xF]);
            }
        }
        return out.toString();
    }
}
