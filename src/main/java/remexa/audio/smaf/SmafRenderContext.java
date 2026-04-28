package remexa.audio.smaf;

import javax.microedition.media.decoders.SMAFDecoder;
import javax.sound.midi.Sequence;
import java.util.List;

record SmafRenderContext(
        byte[] source,
        Sequence sequence,
        List<SMAFDecoder.SequenceSysExEvent> sequenceSysExEvents,
        List<byte[]> startupPackets,
        List<byte[]> pcmClipData,
        List<SMAFDecoder.PcmSequenceTrigger> pcmTriggers) {
}
