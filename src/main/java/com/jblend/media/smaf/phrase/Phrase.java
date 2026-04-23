package com.jblend.media.smaf.phrase;

import java.io.IOException;

public final class Phrase extends PhraseBase {
    public Phrase(byte[] data) {
        super(data);
    }

    public Phrase(String resource) throws IOException {
        super(resource);
    }
}
