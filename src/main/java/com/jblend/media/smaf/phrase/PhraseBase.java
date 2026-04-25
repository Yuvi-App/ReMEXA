/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package com.jblend.media.smaf.phrase;

import org.recompile.mobile.Mobile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class PhraseBase {
    protected enum SmafDataType {
        PHRASE,
        AUDIO
    }

    private final byte[] data;

    protected PhraseBase(byte[] data, SmafDataType expectedType) {
        this(data, expectedType, false);
    }

    protected PhraseBase(byte[] data, SmafDataType expectedType, boolean skipValidation) {
        this.data = Objects.requireNonNull(data, "data").clone();
        if (!skipValidation) {
            validateSmafType(this.data, expectedType);
        }
    }

    protected PhraseBase(String url, SmafDataType expectedType) throws IOException {
        this(Mobile.getMIDletResourceAsByteArray(url), expectedType);
    }

    public int getSize() {
        return data.length;
    }

    public byte[] getData() {
        return data.clone();
    }

    public int getUseTracks() {
        return 1;
    }

    private static void validateSmafType(byte[] data, SmafDataType expectedType) {
        if (data.length == 0) {
            throw new IllegalArgumentException("SMAF data is empty");
        }
        if (!startsWithAscii(data, "MMMD")) {
            throw new IllegalArgumentException("SMAF data must start with MMMD");
        }

        boolean mmmgLike = containsAnyAscii(data,
                "MMMG",
                "SEQU",
                "VOIC",
                "EXVO",
                "DEVO");
        boolean mtrLike = containsAnyAscii(data,
                "MTR",
                "Mtsu",
                "Mtsq",
                "MspI",
                "Mtsp",
                "Mwa");
        boolean atrLike = containsAnyAscii(data,
                "ATR",
                "AspI",
                "Atsu",
                "Atsq",
                "Awa");

        switch (expectedType) {
            case PHRASE -> {
                if (!mmmgLike) {
                    throw new IllegalArgumentException("SMAF data is not phrase data");
                }
            }
            case AUDIO -> {
                if (!mtrLike && !atrLike) {
                    throw new IllegalArgumentException("SMAF data is not audio data");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported SMAF data type");
        }
    }

    private static boolean startsWithAscii(byte[] data, String marker) {
        if (data.length < marker.length()) {
            return false;
        }
        byte[] bytes = marker.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            if (data[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAnyAscii(byte[] data, String... markers) {
        String ascii = new String(data, StandardCharsets.ISO_8859_1);
        for (String marker : markers) {
            if (ascii.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
