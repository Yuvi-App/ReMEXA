package remexa.audio.smaf;

import java.util.ArrayList;
import java.util.List;

interface YamahaAudioEngine {
    String id();

    String label();

    SmafRenderedAudio render(SmafRenderContext context) throws Exception;

    default SmafStreamingSession openStream(SmafRenderContext context) throws Exception {
        return new BufferedSmafStreamingSession(render(context));
    }

    static List<byte[]> startupAndExclusivePackets(SmafRenderContext context) {
        List<byte[]> startupPackets = context.startupPackets();
        List<byte[]> exclusiveVoices = context.exclusiveVoices();
        if (exclusiveVoices == null || exclusiveVoices.isEmpty()) {
            return startupPackets;
        }

        List<byte[]> packets = new ArrayList<>(
                (startupPackets == null ? 0 : startupPackets.size()) + exclusiveVoices.size());
        if (startupPackets != null) {
            packets.addAll(startupPackets);
        }
        packets.addAll(exclusiveVoices);
        return packets;
    }
}
