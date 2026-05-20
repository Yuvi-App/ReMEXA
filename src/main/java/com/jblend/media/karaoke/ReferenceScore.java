package com.jblend.media.karaoke;

public class ReferenceScore {
    private static final int TAG_COUNT = 16;
    private final int referenceEventChannelMask;
    private final com.jblend.media.karaoke.ReferenceEvent[] events;
    private final byte[] optionalData;
    private final int[] tagStart;
    private final int[] tagEnd;

    public ReferenceScore() {
        this(0, new com.jblend.media.karaoke.ReferenceEvent[0], new byte[0], defaultTags(), defaultTags());
    }

    public ReferenceScore(
            int referenceEventChannelMask,
            com.jblend.media.karaoke.ReferenceEvent[] events,
            byte[] optionalData,
            int[] tagStart,
            int[] tagEnd
    ) {
        this.referenceEventChannelMask = referenceEventChannelMask;
        this.events = events == null ? new com.jblend.media.karaoke.ReferenceEvent[0] : events.clone();
        this.optionalData = optionalData == null ? new byte[0] : optionalData.clone();
        this.tagStart = normalizeTags(tagStart);
        this.tagEnd = normalizeTags(tagEnd);
    }

    public static com.jblend.media.karaoke.ReferenceScore empty() {
        return new com.jblend.media.karaoke.ReferenceScore();
    }

    public int getReferenceEventCh () {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceScore", "getReferenceEventCh");
        return referenceEventChannelMask;
    }

    public com.jblend.media.karaoke.ReferenceEvent getReferenceEvents (int ch, int size) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceScore", "getReferenceEvents", ch, size);
        if (ch < 0 || ch >= events.length || events[ch] == null) {
            return com.jblend.media.karaoke.ReferenceEvent.empty();
        }
        return events[ch];
    }

    public byte[] getOptionalData (int size) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceScore", "getOptionalData", size);
        if (size < 0) {
            throw new IllegalArgumentException("ReferenceScore.getOptionalData: size is negative.");
        }
        return java.util.Arrays.copyOf(optionalData, Math.min(size, optionalData.length));
    }

    public int getTagStart (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceScore", "getTagStart", tag);
        return tag >= 0 && tag < tagStart.length ? tagStart[tag] : -1;
    }

    public int getTagEnd (int tag) {
        remexa.probes.SdkStubSupport.log("com.jblend.media.karaoke.ReferenceScore", "getTagEnd", tag);
        return tag >= 0 && tag < tagEnd.length ? tagEnd[tag] : -1;
    }

    private static int[] defaultTags() {
        int[] tags = new int[TAG_COUNT];
        java.util.Arrays.fill(tags, -1);
        return tags;
    }

    private static int[] normalizeTags(int[] tags) {
        int[] normalized = defaultTags();
        if (tags != null) {
            System.arraycopy(tags, 0, normalized, 0, Math.min(tags.length, normalized.length));
        }
        return normalized;
    }
}
