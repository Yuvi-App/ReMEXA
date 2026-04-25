package com.jblend.media.smaf.phrase;

import java.io.IOException;

public final class Phrase extends PhraseBase {
    public Phrase(byte[] data) {
        super(data, SmafDataType.PHRASE);
    }

    public static Phrase unchecked(byte[] data) {
        return new Phrase(data, true);
    }

    private Phrase(byte[] data, boolean skipValidation) {
        super(data, SmafDataType.PHRASE, skipValidation);
    }

    public Phrase(String resource) throws IOException {
        super(resource, SmafDataType.PHRASE);
    }
}
