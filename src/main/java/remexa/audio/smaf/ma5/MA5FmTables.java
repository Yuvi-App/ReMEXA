package remexa.audio.smaf.ma5;

final class MA5FmTables {
    private MA5FmTables() { }
    static final int[] MULTIPLIER = {0,1,2,3,4,5,6,7,8,9,10,10,12,12,15,15};
    static final int[] FEEDBACK_SHIFT = {0,8,7,6,5,4,3,2};
    static final int[] CARRIERS = {2,3,15,8,8,10,9,13};
    private static final int[] KSL_F = {56,32,24,19,16,13,11,9,8,6,5,4,3,2,1,0};
    private static final int[] DETUNE_KEY = {0,0,0,0,0,0,0,1,2,3,3,3,3,3,3,3};
    static final short[][] WAVES = waves();
    static final int[][] AM = amplitudeModulation();
    static final int[][] PM = pitchModulation();
    private static final int[][] ATTACK = {
        {
            0x0, 0x0, 0x0, 0x0, 0x803a, 0xa048, 0xc057, 0xe065,
            0x10074, 0x14090, 0x180ae, 0x1c0cb, 0x200e7, 0x28122, 0x3015a, 0x38192,
            0x401cb, 0x50244, 0x602bb, 0x70330, 0x803a4, 0xa0472, 0xc0576, 0xe065f,
            0x10070f, 0x14093d, 0x180aec, 0x1c0cbe, 0x200e1f, 0x281119, 0x3013da, 0x38197c,
            0x401c3e, 0x5027ba, 0x602fac, 0x702822, 0x80387b, 0xa04f74, 0xc03f7d, 0xe07ba2,
            0x100385e, 0x1404676, 0x180fe7b, 0x1c0f745, 0x20070bd, 0x2808cec, 0x301fcf6, 0x37f3a28,
            0x400e179, 0x50119d8, 0x5fc09ca, 0x6fe744f, 0xe833158, 0x1223fdae, 0x157ffd3f, 0x193d346c,
            0x1d0662af, 0x2447fb5b, 0x2ca75cbf, 0x305ff9cf, 0x80000000, 0x80000000, 0x80000000, 0x80000000
        },
        {
            0x0, 0x0, 0x0, 0x0, 0x5555, 0x6aab, 0x8000, 0x9555,
            0xaaab, 0xd555, 0x10000, 0x12aab, 0x15555, 0x1aaab, 0x1ffff, 0x25554,
            0x2aaa8, 0x35557, 0x40004, 0x4aaaf, 0x5555a, 0x6aa9f, 0x80007, 0x9555e,
            0xaaa8f, 0xd5579, 0x10000f, 0x12aabc, 0x15551e, 0x1aaa07, 0x1ffeca, 0x255578,
            0x2aaa3b, 0x3557bc, 0x4002e1, 0x4aa3b9, 0x555476, 0x6aaf78, 0x7ff090, 0x95644e,
            0xaa8342, 0xd52413, 0x10035fa, 0x12ac89d, 0x1550685, 0x1aa4826, 0x2006bf3, 0x253c473,
            0x2aa0d0a, 0x354904c, 0x3fb8f88, 0x4a788e5, 0x9a86f42, 0xc128b13, 0xe4eddb8, 0x10cbe2e3,
            0x1350de85, 0x18251626, 0x1db77db9, 0x203172dd, 0x80000000, 0x80000000, 0x80000000, 0x80000000
        },
        {
            0x0, 0x0, 0x0, 0x0, 0x3dec, 0x4d66, 0x5ce1, 0x6c5d,
            0x7bd7, 0x9acc, 0xb9c3, 0xd8b8, 0xf7ae, 0x1359a, 0x17385, 0x1b16f,
            0x1ef5a, 0x26b34, 0x2e70d, 0x362e4, 0x3debb, 0x4d65d, 0x5ce19, 0x6c5c8,
            0x7bd5c, 0x9ace6, 0xb9c32, 0xd8b90, 0xf7ab6, 0x135920, 0x17376e, 0x1b1720,
            0x1ef56e, 0x26b4ec, 0x2e72b5, 0x362904, 0x3deadc, 0x4d69d8, 0x5cd608, 0x6c66f8,
            0x7bba62, 0x9aa8fb, 0xb9e9a2, 0xd8cdf0, 0xf774c4, 0x13551f5, 0x173d344, 0x1b04d88,
            0x1eee989, 0x26aa3eb, 0x2e3d138, 0x3609b10, 0x7020e8e, 0x8c29232, 0xa61dcad, 0xc301950,
            0xe041d1d, 0x11852464, 0x15902cc9, 0x175c3084, 0x80000000, 0x80000000, 0x80000000, 0x80000000
        },
        {
            0x0, 0x0, 0x0, 0x0, 0x38e4, 0x471c, 0x5555, 0x638e,
            0x71c7, 0x8e39, 0xaaab, 0xc71d, 0xe38e, 0x11c72, 0x15555, 0x18e38,
            0x1c71b, 0x238e5, 0x2aaad, 0x31c75, 0x38e3c, 0x471bf, 0x5555a, 0x638e9,
            0x71c5f, 0x8e3a6, 0xaaab4, 0xc71d3, 0xe38be, 0x11c6af, 0x155487, 0x18e3a5,
            0x1c717d, 0x238fd3, 0x2aac96, 0x31c27b, 0x38e2f9, 0x471fa5, 0x554b0b, 0x639834,
            0x71acd7, 0x8e180d, 0xaacea6, 0xc73068, 0xe359ae, 0x11c3019, 0x1559d4d, 0x18d2da2,
            0x1c6b35c, 0x2386033, 0x2a7b505, 0x31a5b44, 0x6704a2c, 0x80c5cb7, 0x989e925, 0xb329742,
            0xce09459, 0x1018b96f, 0x13cfa926, 0x15764c94, 0x80000000, 0x80000000, 0x80000000, 0x80000000
        }
    };
    private static final int[][] DECAY = {
        {
            0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x3fffcd63, 0x3fffc0bc, 0x3fffb414, 0x3fffa76d,
            0x3fff9ac6, 0x3fff8177, 0x3fff6829, 0x3fff4eda, 0x3fff358c, 0x3fff02f0, 0x3ffed053, 0x3ffe9db7,
            0x3ffe6b1b, 0x3ffe05e3, 0x3ffda0ac, 0x3ffd3b75, 0x3ffcd640, 0x3ffc0bd6, 0x3ffb416d, 0x3ffa7709,
            0x3ff9aca7, 0x3ff817e8, 0x3ff68335, 0x3ff4ee8d, 0x3ff359e9, 0x3ff030cb, 0x3fed07e0, 0x3fe9df17,
            0x3fe6b66a, 0x3fe065a5, 0x3fda155f, 0x3fd3c586, 0x3fcd766d, 0x3fc0da46, 0x3fb44135, 0x3fa7aad4,
            0x3f9b1658, 0x3f81f2da, 0x3f68df9f, 0x3f4fcf94, 0x3f36c57f, 0x3f04ddf7, 0x3ed315fe, 0x3ea18432,
            0x3e7003b1, 0x3e0dbc12, 0x3dabb2d2, 0x3d4a87be, 0x3ce9cb34, 0x3c2a092d, 0x3b6d1981, 0x3ab38867,
            0x39f9b30f, 0x3891211e, 0x372de101, 0x35d75eda, 0x3489d115, 0x3489d115, 0x3489d115, 0x3489d115
        },
        {
            0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x3fffde51, 0x3fffd5e5, 0x3fffcd7a, 0x3fffc50e,
            0x3fffbca2, 0x3fffabcb, 0x3fff9af3, 0x3fff8a1c, 0x3fff7945, 0x3fff5796, 0x3fff35e7, 0x3fff1439,
            0x3ffef28a, 0x3ffeaf2e, 0x3ffe6bd1, 0x3ffe2875, 0x3ffde519, 0x3ffd5e63, 0x3ffcd7ac, 0x3ffc50f7,
            0x3ffbca44, 0x3ffabcdf, 0x3ff9af80, 0x3ff8a225, 0x3ff794cb, 0x3ff57a2d, 0x3ff35fa8, 0x3ff14531,
            0x3fef2ac3, 0x3feaf630, 0x3fe6c1ce, 0x3fe28d91, 0x3fde59b0, 0x3fd5f2e0, 0x3fcd8d91, 0x3fc5297e,
            0x3fbcc620, 0x3fac0161, 0x3f9b4545, 0x3f8a8913, 0x3f79cea6, 0x3f5870ff, 0x3f371fa1, 0x3f15e9bf,
            0x3ef4b6ab, 0x3eb2b2b4, 0x3e70b5c0, 0x3e2f2bb0, 0x3dedc99e, 0x3d6bc72c, 0x3ceb26a3, 0x3c6c4be8,
            0x3becbbd0, 0x3af3b08f, 0x39fc4871, 0x390bc594, 0x381f9e91, 0x381f9e91, 0x381f9e91, 0x381f9e91
        },
        {
            0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x3fffe78f, 0x3fffe173, 0x3fffdb56, 0x3fffd53a,
            0x3fffcf1e, 0x3fffc2e6, 0x3fffb6ad, 0x3fffaa75, 0x3fff9e3c, 0x3fff85cb, 0x3fff6d5a, 0x3fff54ea,
            0x3fff3c79, 0x3fff0b97, 0x3ffedab6, 0x3ffea9d5, 0x3ffe78f4, 0x3ffe1733, 0x3ffdb571, 0x3ffd53b1,
            0x3ffcf1f2, 0x3ffc2e73, 0x3ffb6af7, 0x3ffaa77f, 0x3ff9e406, 0x3ff85d20, 0x3ff6d64a, 0x3ff54f79,
            0x3ff3c8ad, 0x3ff0bb3d, 0x3fedade3, 0x3feaa096, 0x3fe7937d, 0x3fe179d1, 0x3fdb6105, 0x3fd548e5,
            0x3fcf3111, 0x3fc30232, 0x3fb6d8b8, 0x3faaae4d, 0x3f9e844a, 0x3f863e83, 0x3f6dfe2c, 0x3f55ce58,
            0x3f3d9d11, 0x3f0d77a6, 0x3edd4971, 0x3ead6147, 0x3e7d888a, 0x3e1e3b87, 0x3dbfbb6a, 0x3d625306,
            0x3d042ec5, 0x3c4bc418, 0x3b93bba6, 0x3ae00552, 0x3a2ec60f, 0x3a2ec60f, 0x3a2ec60f, 0x3a2ec60f
        },
        {
            0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x3fffe98b, 0x3fffe3ee, 0x3fffde51, 0x3fffd8b4,
            0x3fffd317, 0x3fffc7dc, 0x3fffbca2, 0x3fffb168, 0x3fffa62e, 0x3fff8fb9, 0x3fff7945, 0x3fff62d0,
            0x3fff4c5c, 0x3fff1f73, 0x3ffef28a, 0x3ffec5a2, 0x3ffe98ba, 0x3ffe3ee9, 0x3ffde519, 0x3ffd8b4a,
            0x3ffd317b, 0x3ffc7dde, 0x3ffbca43, 0x3ffb16ab, 0x3ffa6313, 0x3ff8fbed, 0x3ff794d4, 0x3ff62dc0,
            0x3ff4c6af, 0x3ff1f8b0, 0x3fef2ac3, 0x3fec5cdf, 0x3fe98f28, 0x3fe3f42d, 0x3fde59f4, 0x3fd8c04f,
            0x3fd326e4, 0x3fc7f4a5, 0x3fbcc72f, 0x3fb198b0, 0x3fa66a63, 0x3f901a5d, 0x3f79cea6, 0x3f63916c,
            0x3f4d522c, 0x3f2109d6, 0x3ef4b6ab, 0x3ec8a133, 0x3e9c9731, 0x3e44d77a, 0x3dedc99e, 0x3d97b2cd,
            0x3d40e411, 0x3c96b7b5, 0x3becbbd0, 0x3b469468, 0x3aa28c12, 0x3aa28c12, 0x3aa28c12, 0x3aa28c12
        }
    };
    private static final int[][] LFO_STEP = {
        {
            0x5a5a8, 0xc7ba9, 0x122151, 0x156646
        },
        {
            0x3e426, 0x89a02, 0xc7e28, 0xebedf
        },
        {
            0x2d2d4, 0x63dd5, 0x910a8, 0xab323
        },
        {
            0x29819, 0x5bc02, 0x8541b, 0x9d495
        }
    };
    private static final int[] DETUNE_Q16 = {
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0, 0xccd, 0xccd, 0xccd, 0xccd,
        0xccd, 0xccd, 0xccd, 0xccd, 0x170a, 0x170a, 0x170a, 0x170a,
        0x170a, 0x23d7, 0x23d7, 0x23d7, 0x2e14, 0x2e14, 0x2e14, 0x3ae1,
        0x3ae1, 0x451f, 0x451f, 0x51eb, 0x5eb8, 0x5eb8, 0x5eb8, 0x5eb8,
        0xccd, 0xccd, 0xccd, 0xccd, 0x170a, 0x170a, 0x170a, 0x170a,
        0x170a, 0x23d7, 0x23d7, 0x23d7, 0x2e14, 0x2e14, 0x2e14, 0x3ae1,
        0x3ae1, 0x451f, 0x451f, 0x51eb, 0x5eb8, 0x5eb8, 0x68f6, 0x75c3,
        0x8000, 0x8ccd, 0x970a, 0xa3d7, 0xbae1, 0xbae1, 0xbae1, 0xbae1,
        0x170a, 0x170a, 0x170a, 0x170a, 0x23d7, 0x23d7, 0x23d7, 0x23d7,
        0x2e14, 0x2e14, 0x2e14, 0x3ae1, 0x3ae1, 0x47ae, 0x47ae, 0x51ec,
        0x5eb8, 0x5eb8, 0x68f6, 0x75c3, 0x8000, 0x8ccd, 0x970a, 0xa3d7,
        0xbae1, 0xc7ae, 0xdeb8, 0xe8f6, 0x10000, 0x10000, 0x10000, 0x10000
    };

    private static int ratePage(int rate) {
        return switch(rate) { case 22050 -> 0; case 44100 -> 2; case 48000 -> 3; default -> 1; };
    }

    static long attack(int index, int sampleRate) {
        long value = Integer.toUnsignedLong(ATTACK[ratePage(sampleRate)][index]);
        if (value == 0x80000000L || sampleRate == 22050 || sampleRate == 32000 || sampleRate == 44100 || sampleRate == 48000) return value;
        return Math.round(value * 32000.0 / sampleRate);
    }

    static long decay(int index, int sampleRate) {
        long value = DECAY[ratePage(sampleRate)][index];
        if (sampleRate == 22050 || sampleRate == 32000 || sampleRate == 44100 || sampleRate == 48000) return value;
        return Math.round(Math.pow(value / 1073741824.0, 32000.0 / sampleRate) * 1073741824.0);
    }

    static int lfoStep(int rate, int lfo) {
        int value = LFO_STEP[ratePage(rate)][lfo];
        return rate == 22050 || rate == 32000 || rate == 44100 || rate == 48000 ? value : (int)Math.round(value * 32000.0 / rate);
    }

    static int totalLevel(int level) {
        return (int)Math.min(32767, Math.round(32768 * Math.pow(10, -.75 * level / 20)));
    }

    static long sustainLevel(int level) {
        return level == 15 ? 0xbbcc : Math.round(2147483648.0 * Math.pow(10, -3.0 * level / 20));
    }

    static int keyScale(int scale, int block, int fnum) {
        if (scale == 0) return 32768;
        int key = (fnum >> 6) & 15;
        // The native second KSL table has a different threshold in this cell.
        int threshold = scale == 1 && key == 3 ? 21 : KSL_F[key];
        double depth = scale == 1 ? .375 : scale == 2 ? .1875 : .75;
        return (int)Math.min(32512, Math.round(32768 * Math.pow(10, -Math.max(0, block * 8 - threshold) * depth / 20)));
    }

    static int phaseAdvance(int base, int multiplier, int detune, int block, int fnum, int sampleRate) {
        long phase = Integer.toUnsignedLong(base);
        if (detune != 0) {
            int key = (block << 2) + DETUNE_KEY[(fnum >> 6) & 15];
            long frequency = Math.max(65536, Integer.toUnsignedLong(sampleRate * (int)(phase >> 16)));
            int delta = DETUNE_Q16[(detune & 3) * 32 + key];
            if ((detune & 4) != 0) delta = -delta;
            phase = (frequency + delta) * phase / frequency;
        }
        int multiple = MULTIPLIER[multiplier];
        return multiple == 0 ? (int)(phase >>> 1) : (int)(phase * multiple);
    }

    static boolean supported(int shape) { return shape != 15 && shape != 23 && shape != 31; }

    private static int rounded(double value) { return Math.clamp((int)(value + .5), -32767, 32767); }

    private static short[][] waves() {
        short[][] waves = new short[32][1024];
        for (int i = 0; i < 1024; i++) {
            double sine = 32768 * Math.sin(i * Math.PI / 512);
            waves[0][i] = (short)rounded(sine);
            waves[8][i] = (short)rounded(sine * Math.sqrt(2));
            waves[16][i] = (short)rounded(32768 * (i < 256 ? i / 256.0 : i < 768 ? 2 - i / 256.0 : i / 256.0 - 4));
            waves[24][i] = (short)rounded(32768 * (i < 512 ? i / 512.0 : i / 512.0 - 2));
            // WS7's negative half has the native half-step exponent offset.
            waves[7][i] = (short)rounded(i < 512 ? 32768 * Math.pow(2, -i / 32.0) : -32768 * Math.pow(2, (i - 1023.5) / 32));
        }
        for (int i = 0; i < 1024; i++) {
            for (int base : new int[]{0,8,16,24}) {
                short[] wave = waves[base];
                waves[base+1][i] = i < 512 ? wave[i] : 0;
                waves[base+2][i] = wave[i & 511];
                waves[base+3][i] = (i & 511) < 256 ? wave[i & 255] : 0;
                waves[base+4][i] = i < 512 ? wave[i << 1] : 0;
                waves[base+5][i] = i < 512 ? wave[(i << 1) & 511] : 0;
            }
            waves[6][i] = (short)(i < 512 ? 32767 : -32767);
            waves[14][i] = (short)(i < 512 ? 32767 : 0);
            waves[22][i] = (short)((i & 511) < 256 ? 32767 : 0);
            waves[27][i] = i < 128 ? waves[24][i] : i >= 512 && i < 768 ? waves[24][(i - 512) << 1] : 0;
            waves[30][i] = (short)(i < 256 ? 32767 : 0);
        }
        return waves;
    }

    private static int[][] amplitudeModulation() {
        int[][] table = new int[4][4096];
        double[] depths = {1.3,2.8,5.8,11.8};
        for (int d = 0; d < 4; d++) for (int i = 0; i < 4096; i++) {
            double phase = (i < 2048 ? i : 4096 - i) / 2048.0;
            table[d][i] = (int)Math.round(32768 * Math.pow(10, -depths[d] * phase / 20));
        }
        return table;
    }

    private static int[][] pitchModulation() {
        int[][] table = new int[4][4096];
        double[] cents = {3.4,6.7,13.5,26.8};
        for (int d = 0; d < 4; d++) for (int i = 0; i < 4096; i++) {
            // Preserve the two-index offset in the native second quadrant.
            double phase = (i < 1024 ? i : i < 2048 ? 2046 - i : i < 3072 ? 2048 - i : i - 4096) / 1024.0;
            table[d][i] = (int)Math.round(1048576 * Math.pow(2, cents[d] * phase / 1200));
        }
        return table;
    }
}
