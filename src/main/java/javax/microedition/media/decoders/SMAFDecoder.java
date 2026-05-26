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
package javax.microedition.media.decoders;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import remexa.audio.smaf.SmafDebug;

public final class SMAFDecoder
{
    private static final int SMAF_LOG_DEBUG = 0;
    private static final int SMAF_LOG_INFO = 1;
    private static final int SMAF_LOG_WARNING = 2;
    private static final int SMAF_LOG_ERROR = 3;
    private static final String SMAF_LOG_CHANNEL = "decoder";
    private static final String SMAF_LOG_SOURCE =
            SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": ";

    private static final byte CRCSize = 2; // SMAF has 2 bytes for CRC at the end of the file chunk, which is the main data chunk

    private static ChannelData[] channelData;

    private static int decodePos = 0;
    private static byte formatType = 0;
    private static byte sequenceType = 0;
    private static byte handyChannelIdx = 0;
    private static int startPoint = 0, stopPoint = 0;

    private static byte TimeBase_D;
    private static byte TimeBase_G;

    // Create a new sequence and track for the converted SMAF file
    private static Sequence sequence;
    private static Track track;

    private static byte[] input;

    private static final byte[] shortModValues = new byte[] 
    {
        0x00, // RESERVED
        0x00, // Short type 0x1
        0x08, // Short type 0x2
        0x10, // Short type 0x3
        0x18, // Short type 0x4
        0x20, // Short type 0x5
        0x28, // Short type 0x6
        0x30, // Short type 0x7
        0x38, // Short type 0x8
        0x40, // Short type 0x9
        0x48, // Short type 0xA
        0x50, // Short type 0xB
        0x60, // Short type 0xC
        0x70, // Short type 0xD
        0x7F  // Short type 0xE
    };

    private static final byte[] shortPitchBendValues = new byte[] 
    {
        0x00, // RESERVED
        0x08, // Short type 0x1
        0x10, // Short type 0x2
        0x18, // Short type 0x3
        0x20, // Short type 0x4
        0x28, // Short type 0x5
        0x30, // Short type 0x6
        0x38, // Short type 0x7
        0x40, // Short type 0x8
        0x48, // Short type 0x9
        0x50, // Short type 0xA
        0x58, // Short type 0xB
        0x60, // Short type 0xC
        0x68, // Short type 0xD
        0x70  // Short type 0xE
    };

    private static final byte[] shortExpressionValues = new byte[] 
    {
        0x00, // RESERVED
        0x00, // Short type 0x1
        0x1F, // Short type 0x2
        0x27, // Short type 0x3
        0x2F, // Short type 0x4
        0x37, // Short type 0x5
        0x3F, // Short type 0x6
        0x47, // Short type 0x7
        0x4F, // Short type 0x8
        0x57, // Short type 0x9
        0x5F, // Short type 0xA
        0x67, // Short type 0xB
        0x6F, // Short type 0xC
        0x77, // Short type 0xD
        0x7F  // Short type 0xE
    };

    // These are used only for debugging
    private static final String[] formatTypes = {"Handy Phone Standard (MA-1/2)", "Mobile Standard (MA-3/5) - Compressed", "Mobile Standard (MA-3/5) - Not Compressed", "Yamaha MA-7 (Unsupported)", "Extended Voice/Softbank"};
    private static final String[] sequenceTypes = {"Stream Sequence", "Sub-Sequence (UNTESTED)"};
    private static final String[] channelTypes = {"No Care", "Melody", "No Melody", "Rhythm"};
    private static final String[] noteTypes = {"Invalid", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "Invalid", "Invalid", "Invalid"};
    
    @SuppressWarnings("unchecked")
    private static final List<SimpleEntry<Byte, String>> timebases = Arrays.asList(
        new SimpleEntry<Byte, String>((byte) 0, "1msec"),
        new SimpleEntry<Byte, String>((byte) 1, "2msec"),
        new SimpleEntry<Byte, String>((byte) 2, "4msec"),
        new SimpleEntry<Byte, String>((byte) 3, "5msec"),
        new SimpleEntry<Byte, String>((byte) 16, "10msec"),
        new SimpleEntry<Byte, String>((byte) 17, "20msec"),
        new SimpleEntry<Byte, String>((byte) 18, "40msec"),
        new SimpleEntry<Byte, String>((byte) 19, "50msec")
    );

    // PCM-Specific variables
    private static byte ATRFormatType = 0, ATRChannelType = 0, ATRDataFormat = 0, ATRSamplingFreq = 0, ATRBaseBit = 0;
    private static byte pcmSequenceType = 0;
    public static boolean isPCM = false; // Used by PlatformPlayer to decide whether it'll load the decoded data into a WAVPlayer or MIDIPlayer
    private static final String[] pcmDataFormats = {"2's complement PCM", "Offset Binary PCM", "YAMAHA ADPCM"};
    private static final String[] ATRPcmDataFormats = {"2's complement PCM", "YAMAHA ADPCM", "TwinVQ", "MP3", "Reserved", "Reserved", "Reserved", "Reserved"};
    private static final String[] pcmBaseBits = {"4 bits", "8 bits", "12 bits", "16 bits", "Reserved", "Reserved", "Reserved", "Reserved"};
    private static final String[] pcmSamplingFreqs = {"4000", "8000", "11025", "22050", "44100", "Reserved", "Reserved", "Reserved"};

    // Structures that hold decoded SMAF data

    public static List<InputStream> pcmData = null;
    public static InputStream SequenceData = null;
    public static Map<Integer, Integer> pcmDataPositions = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> pcmDataVelocities = new HashMap<Integer, Integer>();
    public static List<PcmSequenceTrigger> pcmSequenceTriggers = new ArrayList<PcmSequenceTrigger>();
    public static List<byte[]> startupPackets = new ArrayList<byte[]>();
    public static List<byte[]> exclusiveVoices = new ArrayList<byte[]>();
    public static List<SequenceSysExEvent> sequenceSysExEvents = new ArrayList<SequenceSysExEvent>();
    public static List<SequenceUserEvent> sequenceUserEvents = new ArrayList<SequenceUserEvent>();
    private static final int INTERNAL_LEGACY_YAMAHA_SELECTOR = 0x06;
    private static final int INTERNAL_LEGACY_YAMAHA_MODULATION = 0x07;
    private static final int INTERNAL_LEGACY_YAMAHA_VOLUME = 0x08;
    private static final int INTERNAL_LEGACY_YAMAHA_PAN = 0x09;
    private static final int INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME = 0x0A;
    private static final int INTERNAL_LEGACY_YAMAHA_PROGRAM = 0x0B;
    private static final int INTERNAL_LEGACY_YAMAHA_BANK = 0x0C;
    private static final int INTERNAL_LEGACY_YAMAHA_EXPRESSION = 0x0D;
    private static final int INTERNAL_LEGACY_YAMAHA_PITCH = 0x11;

    private static boolean smafLogEnabled(int level)
    {
        return level == SMAF_LOG_DEBUG
                ? SmafDebug.isEnabled(SMAF_LOG_CHANNEL, SmafDebug.Level.DEBUG)
                : SmafDebug.isEnabled(SMAF_LOG_CHANNEL, SmafDebug.Level.INFO);
    }

    private static void smafLog(int level, String message)
    {
        if (!smafLogEnabled(level))
        {
            return;
        }
        String normalized = message == null ? "" : message;
        if (normalized.startsWith(SMAF_LOG_SOURCE))
        {
            normalized = normalized.substring(SMAF_LOG_SOURCE.length());
        }
        normalized = switch (level)
        {
            case SMAF_LOG_DEBUG -> "[smaf:debug] " + normalized;
            case SMAF_LOG_INFO -> "[smaf:info] " + normalized;
            case SMAF_LOG_WARNING -> "[smaf:warn] " + normalized;
            case SMAF_LOG_ERROR -> "[smaf:error] " + normalized;
            default -> "[smaf] " + normalized;
        };
        if (level == SMAF_LOG_DEBUG)
        {
            SmafDebug.debug(SMAF_LOG_CHANNEL, normalized);
        }
        else
        {
            SmafDebug.info(SMAF_LOG_CHANNEL, normalized);
        }
    }

    private static void smafLogException(Throwable throwable)
    {
        if (throwable == null)
        {
            return;
        }
        smafLog(SMAF_LOG_ERROR, throwable.getClass().getName() + ": " + throwable.getMessage());
    }

    public static synchronized void decodeSMAF(byte[] data)
	{
        // Reset any and all static variables to their defaults
        isPCM = false;
        handyChannelIdx = 0;
        decodePos = 0;
        startPoint = 0;
        stopPoint = 0;
        formatType = 0;
        channelData = new ChannelData[16];
        for (int j = 0; j < channelData.length; j++) { channelData[j] = new ChannelData(); }
        sequence = null; // Clear the previous MIDI sequence

        // Clear previous decoded data objects
        SequenceData = null;
        pcmData = new ArrayList<InputStream>();
        pcmDataPositions.clear();
        pcmDataVelocities.clear();
        pcmSequenceTriggers.clear();
        startupPackets.clear();
        exclusiveVoices.clear();
        sequenceSysExEvents.clear();
        sequenceUserEvents.clear();

        input = data;

        // Start parsing the file.
        decodeHeader(); // MMMD (file chunk header)

        while(decodePos < input.length-CRCSize) // -2, because CRC at the end uses 2 bytes
        {
            if(decodePos >= input.length-4) { break; } // We can't read the chunkID below
            
            String chunkID = "" + (char) input[decodePos] + (char) input[decodePos+1] + (char) input[decodePos+2] + (char) input[decodePos+3];

            if(chunkID.startsWith("MTR")) { decodeScoreTrackChunk(); }
            else if(chunkID.startsWith("MspI") || chunkID.startsWith("AspI")) { decodeSeekAndPhraseChunk(); }
            else if(chunkID.startsWith("Mtsu") || chunkID.startsWith("Atsu")) { decodeSetupDataChunk(); }
            else if(chunkID.startsWith("Mtsq") || chunkID.startsWith("Atsq") || chunkID.startsWith("SEQU"))
            {
                scoreTrackSequenceData();
                // Handy Phone format states that more than 4 channels can be used by having multiple Score Track Sequences (each track has 4). Softbank also needs this
                if(formatType == (byte) 0x00 || formatType == (byte) 0x04) { handyChannelIdx += 4; }
            }
            else if(chunkID.startsWith("Mtsp")) { scoreTrackPCMData(); } // Unfinished as far as spec compliance goes, but works more often than not
            else if(chunkID.startsWith("Mwa"))  { scoreTrackWaveData(); } // Same as above (in fact, it pretty much ties with the above)
            else if(chunkID.startsWith("MMMG")) { decodeMMMGChunk(); }
            else if(chunkID.startsWith("VOIC")) { decodeVoiceChunk(); }
            else if(chunkID.startsWith("EXVO")) { decodeExclusiveVoiceChunk(); }
            else if(chunkID.startsWith("DEVO")) { decodeDEVoiceChunk(); }
            else if(chunkID.startsWith("ATR"))  { decodePCMScoreTrackChunk(); }
            else if(chunkID.startsWith("Awa"))  { decodeAudioTrackWaveDataChunk(); }
            else { decodePos++; }

            // TODO: Decode more of SMAF
            /* TODOs 
                case "GTR ":
                case "Gtsu":
                case "Gsq ":
                case "Gftd":
                case "Gimd":
                case "MTSR":
                case "Mssq":
                
            */
        }

        // Check the file's CRC for good measure. If that's fine, any and all inaccuracies with the decoding lie in the decoder itself.
        if(SmafCRC.verifySmafCRC(input)) 
        {
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " SMAF file passed CRC check! ");
        }
        else 
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " SMAF file did not pass CRC check. There might be corruption or incorrect data on the decoded file as a result. ");
        }

        // Convert the resulting sequence to byte array and send to the player.
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MidiSystem.write(sequence, 0, output);
            SequenceData = new ByteArrayInputStream(output.toByteArray());

            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " SMAF parsing and conversion finished, Sequence data size:" + output.size() + " | number of PCM streams:" + pcmData.size());
        }
        catch (IOException e) 
        { 
            smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " couldn't write converted SMAF Data:" + e.getMessage()); 
            smafLogException(e);
            SequenceData = null;
            pcmData = null;
        }
	}

    public static void decodeHeader() 
    {
        if (input == null || input.length < 16) {
            throw new IllegalArgumentException("SMAF input is too short to contain an MMMD header");
        }

        String fileChunkID = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "MMMD"
        int fileChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        String cnti = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int cntichunksize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        int cntiChunkEnd = Math.min(input.length, decodePos + cntichunksize);
        byte contentClass = 0;
        byte contentType = 0;
        byte contentCodeType = 0;
        byte copyStatus = 0;
        byte copyCounts = 0;

        if (decodePos < cntiChunkEnd) { contentClass = (byte) (input[decodePos++] & 0xFF); }
        if (decodePos < cntiChunkEnd) { contentType = (byte) (input[decodePos++] & 0xFF); }
        if (decodePos < cntiChunkEnd) { contentCodeType = (byte) (input[decodePos++] & 0xFF); }
        if (decodePos < cntiChunkEnd) { copyStatus = (byte) (input[decodePos++] & 0xFF); }
        if (decodePos < cntiChunkEnd) { copyCounts = (byte) (input[decodePos++] & 0xFF); }
        String[] data = {""};
        StringBuilder sb = new StringBuilder();

        // Used just to make the content type print below a bit easier to understand
        String typeString = "", codeString = "UTF-8", copyStatString = "";

        if(smafLogEnabled(SMAF_LOG_DEBUG)) 
        {
            if((0x00 <= contentType && contentType <= 0x0F) || (0x30 <= contentType && contentType <= 0x33)) 
            {
                typeString = "Ringtone";
            }
            else if((0x10 <= contentType && contentType <= 0x1F) || (0x40 <= contentType && contentType <= 0x42)) 
            {
                typeString = "Karaoke";
            }
            else if((0x20 <= contentType && contentType <= 0x2F) || (0x50 <= contentType && contentType <= 0x53)) 
            {
                typeString = "Commercial";
            }
            else { typeString = "Reserved"; }

            codeString = getContentCodeType(contentCodeType);

            if((copyStatus & 0x04) == 1) { copyStatString = copyStatString + " NO Edit"; }
            else { copyStatString = copyStatString + " Edit OK"; }

            if((copyStatus & 0x02) == 1) { copyStatString = copyStatString + " NO Save"; } 
            else { copyStatString = copyStatString + " Save OK"; }

            if((copyStatus & 0x01) == 1) { copyStatString = copyStatString + " NO Trans"; }
            else { copyStatString = copyStatString + " Trans OK"; }
        }
        


        // -------------------------- Back to parsing
        
        decodePos = cntiChunkEnd;
        
        if(contentClass >= 0x00 && contentClass <= 0xFF)
        {
            if(decodePos + 3 < input.length &&
               (char) input[decodePos] == 'O' && (char) input[decodePos+1] == 'P' && (char) input[decodePos+2] == 'D' && (char) input[decodePos+3] == 'A') 
            {
                decodePos += 4;
                int opdaLength = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 

                for(int i = 0; i < opdaLength; i++) 
                {
                    if((char) input[decodePos] == 'D' && (char) input[decodePos+1] == 'c' && (char) input[decodePos+2] == 'h') 
                    {
                        decodePos += 3;
                        contentCodeType = (byte) (input[decodePos++] & 0xFF);
                        int dchLength = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
                        i += 8;
                    }
                    else 
                    {
                        if((char) input[decodePos] == 'V' && (char) input[decodePos+1] == 'N') { sb.append("Vendor name: "); }
                        else if((char) input[decodePos] == 'C' && (char) input[decodePos+1] == 'N') { sb.append("Carrier name: "); }
                        else if((char) input[decodePos] == 'C' && (char) input[decodePos+1] == 'A') { sb.append("Category name: "); }
                        else if((char) input[decodePos] == 'S' && (char) input[decodePos+1] == 'T') { sb.append("Song title: "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == 'N') { sb.append("Artist name: "); }
                        else if((char) input[decodePos] == 'W' && (char) input[decodePos+1] == 'W') { sb.append("Lyricist: "); }
                        else if((char) input[decodePos] == 'S' && (char) input[decodePos+1] == 'W') { sb.append("Composer: "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == 'W') { sb.append("Arranger: "); }
                        else if((char) input[decodePos] == 'C' && (char) input[decodePos+1] == 'R') { sb.append("Copyright: "); }
                        else if((char) input[decodePos] == 'G' && (char) input[decodePos+1] == 'R') { sb.append("Manage Group: "); }
                        else if((char) input[decodePos] == 'M' && (char) input[decodePos+1] == 'I') { sb.append("Management Info: "); }
                        else if((char) input[decodePos] == 'C' && (char) input[decodePos+1] == 'D') { sb.append("Creation Date: "); }
                        else if((char) input[decodePos] == 'U' && (char) input[decodePos+1] == 'D') { sb.append("Revision Date: "); }
                        else if((char) input[decodePos] == 'E' && (char) input[decodePos+1] == 'S') { sb.append("Edit Status: "); }
                        else if((char) input[decodePos] == 'V' && (char) input[decodePos+1] == 'C') { sb.append("V-Card: "); }
                        else if((char) input[decodePos] == 'I' && (char) input[decodePos+1] == 'C') { sb.append("Image Creator: "); }
                        else if((char) input[decodePos] == 'I' && (char) input[decodePos+1] == 'R') { sb.append("Image Copyright Owner: "); }
                        else if((char) input[decodePos] == 'I' && (char) input[decodePos+1] == 'P') { sb.append("Image Editor: "); }
                        else if((char) input[decodePos] == 'T' && (char) input[decodePos+1] == 'R') { sb.append("Text Copyright Owner: "); }
                        else if((char) input[decodePos] == 'T' && (char) input[decodePos+1] == 'P') { sb.append("Text Editor: "); }
                        else if((char) input[decodePos] == 'G' && (char) input[decodePos+1] == 'P') { sb.append("Content Copyright Owner: "); }
                        else if((char) input[decodePos] == 'V' && (char) input[decodePos+1] == 'E') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'C' && (char) input[decodePos+1] == 'E') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'U' && (char) input[decodePos+1] == 'I') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'O' && (char) input[decodePos+1] == 'W') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == '0') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == '1') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == '2') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'A' && (char) input[decodePos+1] == 'S') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'R' && (char) input[decodePos+1] == 'F') { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }
                        else if((char) input[decodePos] == 'P' && (char) input[decodePos+1] == 'r') 
                        { 
                            // 'Pro' field is undocumented, but seems to contain 4 chars (4th defines some kind of type) then length
                            sb.append((char) input[decodePos]); 
                            sb.append((char) input[decodePos+1]);
                            sb.append((char) input[decodePos+2]); 
                            sb.append((char) input[decodePos+3]); 
                            sb.append(": "); 
                            decodePos += 2; // Will add to the +2 below
                        }
                        else { sb.append((char) input[decodePos]); sb.append((char) input[decodePos+1]); sb.append(": "); }

                        decodePos += 2;

                        int fieldLength = (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); 
                        for(int j = 0; j < fieldLength; j++) { sb.append((char) (input[decodePos++] & 0xFF)); }
                        sb.append(',');
                        i += 4 + fieldLength;
                    }
                }
            }
            else 
            {
                while(decodePos + 3 < input.length &&
                !((char) input[decodePos] == 'M' && (char) input[decodePos+1] == 'T' && (char) input[decodePos+2] == 'R') &&
                !((char) input[decodePos] == 'A' && (char) input[decodePos+1] == 'T' && (char) input[decodePos+2] == 'R') &&
                !((char) input[decodePos] == 'M' && (char) input[decodePos+1] == 'M' && (char) input[decodePos+2] == 'M' && (char) input[decodePos+3] == 'G')) 
                {
                    sb.append((char) (input[decodePos++] & 0xFF));
                }
                
            }
            
            try 
            {
                byte[] bytes = sb.toString().getBytes(codeString);
                String encodedString = new String(bytes, codeString);
                // "," or 0x2C is used as a delimiter for identifier data in cases where the OPDA chunk isn't present, but here, it's also used in OPDA as "," is used as a separator
                data = encodedString.split(",");
            }
            catch(UnsupportedEncodingException e) { smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Java does not support the requested encoding: " + codeString); }
        }

        while(decodePos + 3 < input.length &&
            !((char) input[decodePos] == 'M' && (char) input[decodePos+1] == 'T' && (char) input[decodePos+2] == 'R') && 
            !((char) input[decodePos] == 'A' && (char) input[decodePos+1] == 'T' && (char) input[decodePos+2] == 'R') &&
            !((char) input[decodePos] == 'M' && (char) input[decodePos+1] == 'M' && (char) input[decodePos+2] == 'M' && (char) input[decodePos+3] == 'G')) // assume garbage data is present in files that don't use that "options" field
        {
            decodePos++;
        }

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- SMAF CONTENT HEADER --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"fileChunkID: " + fileChunkID);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"fileChunkSize: " + fileChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"contentsInfoChunk: " + cnti);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"contentsInfoChunkSize: " + cntichunksize); // This seems to always match with the real CNTI chunk size minus this data, so it's likely always present
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"contentClass: " + (contentClass == 0x00 ? "YAMAHA" : "OTHER " + contentClass));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"contentType: " + (typeString + " " + (contentType & 0xFF)));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"contentCodeType: " + codeString);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"copyStatus: " + copyStatString);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"copyCounts: " + copyCounts);
        
        if(contentClass >= 0x00 && contentClass <= 0xFF)
        {
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Format has 'Option' field!");
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t additional data: ");
            for (int i = 0; i < data.length; i++) { smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t " + data[i]); }
        }
        // OK, we're at the start of the "MTR" (Score) Track, which means the content info chunk (content header) has been left behind
    }

    public static void decodeMMMGChunk() 
    {
        String MMMG = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "MMMG"
        int MMMGChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); // Size of remaining data minus the next 2 bytes
        int sequenceHeader = input[decodePos++] & 0xFF;
        int timeBaseHint = input[decodePos++] & 0xFF;
        handyChannelIdx = 0;

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- " + MMMG + " HEADER --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"MMMGChunkSize: " + MMMGChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"MMMGSequenceHeader: 0x" + String.format("%02X", sequenceHeader));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"MMMGTimeBaseHint: 0x" + String.format("%02X", timeBaseHint));
        if ((sequenceHeader & 0x01) == 0)
        {
            smafLog(SMAF_LOG_WARNING,
                    SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName()
                            + ": " + "Unexpected MMMG sequence header 0x"
                            + String.format("%02X", sequenceHeader)
                            + " (bit 0 should be set for valid Phrase Class 2 data).");
        }

        // SoftBank MMMG phrases carry a short 2-byte header ahead of VOIC/SEQU.
        // The first byte appears to be a version/format marker. The second byte
        // behaves like a literal time-base value in milliseconds (for Ys files
        // this is 0x14 == 20 ms), not the normal SMAF encoded time-base index.
        formatType = (byte) 0x04; // SMAF with MMMG chunk uses a different format for notes and events
        TimeBase_D = encodeLiteralMmmgTimeBase(timeBaseHint);
        TimeBase_G = TimeBase_D;

        // According to some Sharp SMAFs, this should be followed by the VOIC chunk
        // TODO: We don't really have sufficient data to decode the PCM sequences and set up the MIDI sequence, but... let's try anyway
        if(sequence == null) 
        { 
            try 
            {
                sequence = new Sequence(Sequence.PPQ, 500); // TODO: Maybe revise this? We shouldn't rely on a fixed PPQ value, i think. (though there are separate timebases for duration and gateTime, so who knows, maybe it's correct)
                track = sequence.createTrack();
            } catch(InvalidMidiDataException ie) { smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " couldn't create MIDI Sequence to convert:" + ie.getMessage()); }
        }
    }

    public static void decodeVoiceChunk() 
    {
        String voic = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "VOIC"
        int voicChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        int voicChunkEnd = Math.min(input.length, decodePos + voicChunkSize);
    
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- " + voic + " HEADER --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"VOICChunkSize: " + voicChunkSize);

        while (decodePos + 8 <= voicChunkEnd)
        {
            String chunkID = readChunkId(input, decodePos);
            int chunkSize = readInt32(input, decodePos + 4);
            decodePos += 8;

            int chunkEnd = Math.min(voicChunkEnd, decodePos + chunkSize);
            if (chunkEnd < decodePos)
            {
                break;
            }

            byte[] chunkData = Arrays.copyOfRange(input, decodePos, chunkEnd);
            decodePos = chunkEnd;

            if (chunkID.startsWith("EXWV"))
            {
                byte[] startupPacket = normalizeVoiceWavePacket(chunkData);
                if (startupPacket != null && startupPacket.length > 0)
                {
                    startupPackets.add(startupPacket);
                }
            }
            else if (chunkID.startsWith("EXVO"))
            {
                recordExclusiveVoice(chunkData);
            }
        }

        decodePos = voicChunkEnd;
    }


    public static void decodeExclusiveVoiceChunk() 
    {
        String exvo = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "EXVO"
        int exvoChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        byte[] exclusiveVoice = new byte[exvoChunkSize];

        for(int i = 0; i < exvoChunkSize; i++) 
        {
            exclusiveVoice[i] = (byte) (input[decodePos++] & 0xFF);
        }

        recordExclusiveVoice(exclusiveVoice);
    
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- " + exvo + " HEADER --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"EXVOChunkSize: " + exvoChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"data: " + Arrays.toString(exclusiveVoice));
    }

    public static void decodeDEVoiceChunk() 
    {
        String devo = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "EXVO"
        int devoChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        byte[] DEVoice = new byte[devoChunkSize];

        for(int i = 0; i < devoChunkSize; i++) 
        {
            DEVoice[i] = (byte) (input[decodePos++] & 0xFF);
        }
    
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- " + devo + " HEADER --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"DEVOChunkSize: " + devoChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"data: " + Arrays.toString(DEVoice));
    }
    

    public static void decodeScoreTrackChunk()
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        String mtr = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "MTR" actually uses 4 chars, the last one appears to always be 0x01
        int mtrChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        formatType = (byte) (input[decodePos++]); // 0x00 = Handy Phone, 0x01 = Standard Mobile (compress), 0x02 = Standard Mobile (no comp.)
        sequenceType = input[decodePos++]; // for formatType 0x00, 0x00 and 0x01 are allowed, 0x02 is reserved
        TimeBase_D = input[decodePos++]; // 0x00 = 1msec, 0x01 = 2msec, 0x02 = 4msec, 0x03 = 5msec. 0x10 = 10msec, 0x11 = 20msec, 0x12 = 40msec, 0x13 = 50msec
        TimeBase_G = input[decodePos++]; // same value range as above

        // These will depend on formatType;
        byte channelStatus1 = 0x00, channelStatus2 = 0x00; // used for formatType 0x00
        
        int formatDataChunk; // sequenceDataChunk, or also StreamPCMDataChunk when formatType = "Mobile Standard (0x01 or 0x02)"

        if(formatType == (byte) 0x00) // Handy Phone
        {
            channelStatus1 = (byte) (input[decodePos++] & 0xFF);
            channelStatus2 = (byte) (input[decodePos++] & 0xFF);

            for(int i = 0; i < 4; i++)
            {
                byte dataToInsert = (i < 2) ? channelStatus1 : channelStatus2;

                if (i % 2 == 0) // Channels 1 and 2
                {
                    channelData[i].keyControlBasic = (dataToInsert & 0x80) != 0; // KCS
                    channelData[i].vibStatus = (dataToInsert & 0x40) != 0; // VS
                    channelData[i].channelType = (byte) ((dataToInsert >> 4) & 0x03); // Channel Type
                }
                else // Channels 2 and 3
                {
                    channelData[i].keyControlBasic = (dataToInsert & 0x08) != 0; // KCS
                    channelData[i].vibStatus = (dataToInsert & 0x04) != 0; // VS
                    channelData[i].channelType = (byte) (dataToInsert & 0x03); // Channel Type
                }
            }
        }
        else // The only difference between the Standard Mobile formats is compression, so they're identical here
        {
            int i = 0;
            while(!((char) input[decodePos] == 'M' && ((char) input[decodePos + 1] == 's' || (char) input[decodePos + 1] == 't'))) // Before we reach the "MspI" (Seek and Phrase) or "Mtsu" (Setup Data Chunk) sections, keep adding channels
            {
                if(i < 16) 
                {
                    channelData[i].keyControl = (byte) (input[decodePos] & 0xC0);         // 0b11000000
                    channelData[i].vibStatus = (input[decodePos] & 0x20) != 0;            // 0b00100000
                    channelData[i].led = (input[decodePos] & 0x10) != 0;                  // 0b00010000
                    channelData[i].channelType = (byte) ((input[decodePos] & 0x0C) >> 2); // 0b00001100
                    i++;
                }
                else { smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"channel limit reached, ignoring further channel data..."); }
                decodePos++;
            }
        }

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- '" + mtr +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"mtrChunkSize: " + mtrChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"formatType: " + formatTypes[formatType]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"sequenceType: " + sequenceTypes[sequenceType]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"TimeBase_Duration: " + getTimeBaseDescription(TimeBase_D));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"TimeBase_GateTime: " + getTimeBaseDescription(TimeBase_G));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"channelStatus: ");
        for(int i = 0; i < ((formatType == (byte) 0x00 || formatType == (byte) 0x04) ? 4 : channelData.length); i++) // Format 0x00 and 0x04 have 4 channels, 0x01 and 0x02 have 16
        {
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\tChannel" + (i+1) + ": ");
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t\tKeyControlStatus:" + channelData[i].keyControl);
            if(formatType != (byte) 0x00) { smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t\tLED:" + (channelData[i].led ? "Enabled" : "Disabled")); }
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t\tVibration:" + (channelData[i].vibStatus ? "Enabled" : "Disabled"));
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"\t\tChannelType:" + channelTypes[channelData[i].channelType]);
        }
        
        // We now have sufficient data to create a proper MIDI sequence.
        if(sequence == null) 
        { 
            try 
            {
                sequence = new Sequence(Sequence.PPQ, 500); // TODO: Maybe revise this? We shouldn't rely on a fixed PPQ value, i think. (though there are separate timebases for duration and gateTime, so who knows, maybe it's correct)
                track = sequence.createTrack();
            } catch(InvalidMidiDataException ie) { smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " couldn't create MIDI Sequence to convert:" + ie.getMessage()); }
        }
    }

    public static void decodePCMScoreTrackChunk()
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        String atr = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++]; // "ATR" actually uses 4 chars, the last one appears to always be 0x01
        int atrChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        ATRFormatType = (byte) input[decodePos++]; // 0x00 = Handy Phone, 0x01~0xFF = Reserved
        pcmSequenceType = (byte) input[decodePos++]; // 0x00 = Stream Sequence, 0x01 = Sub-Sequence, 0x02~0xFF = Reserved
        byte pcmWaveTypeMSB = (byte) input[decodePos++];
        byte pcmWaveTypeLSB = (byte) input[decodePos++];
        TimeBase_D = (byte) input[decodePos++];
        TimeBase_G = (byte) input[decodePos++];
        ATRChannelType = (byte) ((pcmWaveTypeMSB >> 7) & 0x01);
        ATRDataFormat = (byte) ((pcmWaveTypeMSB >> 4) & 0x07);
        ATRSamplingFreq = (byte) (pcmWaveTypeMSB & 0x0F);
        ATRBaseBit = (byte) ((pcmWaveTypeLSB >> 4) & 0x0F);

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "-------------------------- '" + atr +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "atrChunkSize: " + atrChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "formatType: " + (ATRFormatType == (byte) 0x00 ? "Handy Phone Standard" : "Reserved"));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "PCMSequenceType: " + sequenceTypes[pcmSequenceType]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "channelType: " + (ATRChannelType == (byte) 0x00 ? "Mono" : "Stereo"));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "dataFormat: " + ATRPcmDataFormats[ATRDataFormat]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "baseBit: " + pcmBaseBits[ATRBaseBit]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "samplingFrequency: " + pcmSamplingFreqs[ATRSamplingFreq]);
        
        // We now have sufficient data to decode the PCM sequences and set up the MIDI sequence based on the 'Atsq' chunk.
        if(sequence == null) 
        { 
            try 
            {
                sequence = new Sequence(Sequence.PPQ, 500); // TODO: Maybe revise this? We shouldn't rely on a fixed PPQ value, i think. (though there are separate timebases for duration and gateTime, so who knows, maybe it's correct)
                track = sequence.createTrack();
            } catch(InvalidMidiDataException ie) { smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + " couldn't create MIDI Sequence to convert:" + ie.getMessage()); }
        }
    }

    public static void decodeAudioTrackWaveDataChunk() 
    {
        String awa = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        byte chunkNumber = (byte) (input[decodePos++]);
        int awaChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "-------------------------- '" + awa +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "chunkNumber: " + chunkNumber);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "mwaChunkSize: " + awaChunkSize);

        pcmData.add(null); // Awa Chunk number starts from 1, so we need to add a null position.
        byte[] waveData = new byte[awaChunkSize];

        for(int i = 0; i < awaChunkSize; i++) { waveData[i] = input[decodePos++]; }

        if(ATRDataFormat == (byte) 0x00) // 2's complement PCM WAV
        {
            if(ATRBaseBit == (byte) 0x00) { pcmData.add(new ByteArrayInputStream(WAVTools.convert4BitWav(waveData, ATRChannelType+1, Integer.parseInt(pcmSamplingFreqs[ATRSamplingFreq]), true)));}
            if(ATRBaseBit == (byte) 0x01) { pcmData.add(new ByteArrayInputStream(WAVTools.convert8BitWav(waveData, ATRChannelType+1, Integer.parseInt(pcmSamplingFreqs[ATRSamplingFreq]), true)));}
            if(ATRBaseBit == (byte) 0x02) { pcmData.add(new ByteArrayInputStream(WAVTools.convert12BitWav(waveData, ATRChannelType+1, Integer.parseInt(pcmSamplingFreqs[ATRSamplingFreq]), true)));}
            if(ATRBaseBit == (byte) 0x03) { pcmData.add(new ByteArrayInputStream(WAVTools.convert16BitWav(waveData, ATRChannelType+1, Integer.parseInt(pcmSamplingFreqs[ATRSamplingFreq]), true)));}
        }
        else if(ATRDataFormat == (byte) 0x01) // YAMAHA ADPCM
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"PCM data uses YAMAHA ADPCM. Decoding for this is not fully tested!");
            pcmData.add(new ByteArrayInputStream(WAVYamahaADPCMDecoder.ADPCMBDecode(waveData, Integer.parseInt(pcmSamplingFreqs[ATRSamplingFreq]), ATRChannelType+1)));
        } 
        else if(ATRDataFormat == (byte) 0x02) // TODO: TwinVQ
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"PCM Data is of TwinVQ format! Decoding not supported!"); 
            pcmData.add(new ByteArrayInputStream(null));
        }
        else if(ATRDataFormat == (byte) 0x03) // TODO: MP3
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"PCM Data is of MP3 format! Decoding not supported!"); 
            pcmData.add(new ByteArrayInputStream(null));
        }
        else 
        { 
            smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Invalid PCM data format!"); 
            pcmData.add(new ByteArrayInputStream(null));
        }
    }

    public static void decodeSeekAndPhraseChunk() 
    {
        String seekAndPhrase = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int mspiChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        int curPos = 0;

        if(((char) input[decodePos] == 's' && (char) input[decodePos+1] == 't' && (char) input[decodePos+2] == ':')) 
        {
            decodePos+=3; // Skip to the integer data
            curPos+=3;
            if(sequenceType == 0) { startPoint = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); curPos+=4; }
            else { startPoint = (input[decodePos++] & 0xFF); curPos++; }
            decodePos++; // Skip ',' delimiter char
            curPos++;
        }
        if(((char) input[decodePos] == 's' && (char) input[decodePos+1] == 'p' && (char) input[decodePos+2] == ':')) 
        {
            decodePos+=3; // Skip to the integer data
            curPos+=3;
            if(sequenceType == 0) { stopPoint = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF); curPos+=4; }
            else { stopPoint = (input[decodePos++] & 0xFF); curPos++; }
            decodePos++; // Skip ',' delimiter char
            curPos++;
        }

        if(curPos < mspiChunkSize) 
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Seek and Phrase section has Phrase List (Unsupported)");
            decodePos += (mspiChunkSize - curPos);
        }

        if(seekAndPhrase != "")
        {
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- '" + seekAndPhrase +"' SECTION --------------------------");
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Start Point: " + startPoint);
            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Stop Point: " + stopPoint);
        }
    }

    public static void decodeSetupDataChunk() 
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        String matsu = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int matsuChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        List<Byte> exclusiveMessage = new ArrayList<Byte>(); // Seems to relate to SysEx messages, maybe we don't need these?
        int mtsuPos = 0;

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- '" + matsu +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + matsu + "ChunkSize: " + matsuChunkSize);

        // FormatType doesn't seem to really matter for us here
        while(mtsuPos < matsuChunkSize) 
        {
            exclusiveMessage.add(input[decodePos++]);
            mtsuPos++;
        }
        
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Exclusive Message(SysEx) length: " + exclusiveMessage.size());
        // Now we should be entering the Score Track Sequence Data (Mtsq) section, which contains actual notes
    }

    public static void scoreTrackSequenceData() 
    {
        // We're at the Score Track Chunk, so let's decode the info about the audio data
        String matsq = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int matsqChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        int curPos = 0;

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- '" + matsq +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + matsq + "ChunkSize: " + matsqChunkSize);

        byte[] seqBytes = new byte[matsqChunkSize];
        // TODO: Decode these properly
        if(formatType == (byte) 0x00 || formatType == (byte) 0x02 || formatType == (byte) 0x04) // These formats are uncompressed, and use the same steps until event decoding
        {
            // Collect sequence data
            while (curPos < matsqChunkSize)
            {
                seqBytes[curPos] = input[decodePos++];
                curPos++;
            }

            try { convertSequenceEvents(seqBytes); }
            catch(Exception e) { smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Couldn't decode sequence events:" + e.getMessage()); smafLogException(e); }
        } 
        else if(formatType == (byte) 0x01) // Standard Mobile, Compressed (Huffman table must be applied here, COMPLETELY UNTESTED)
        {
            smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Huffman decoding is untested. Expect issues. ");
            
            // Step 1: We start by getting the decoded size (saved prior to the huffman-compressed data)
            int decodedSize = ((input[decodePos++] & 0xFF) << 24) |
                            ((input[decodePos++] & 0xFF) << 16) |
                            ((input[decodePos++] & 0xFF) << 8) |
                            (input[decodePos++] & 0xFF);

            while (curPos < matsqChunkSize - 4) // We already read the size above, so we need to decrease the actual data to be read
            {
                seqBytes[curPos] = input[decodePos++];
                curPos++;
            }

            smafLog(SMAF_LOG_INFO, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"orig len:" + seqBytes.length);
            smafLog(SMAF_LOG_INFO, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"dec len:" + decodedSize);

            // Decode the data
            byte[] decodedData = HuffmanDecoder.huffmanDecode(decodedSize, seqBytes);

            try { convertSequenceEvents(decodedData); }
            catch(Exception e) { smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Couldn't decode sequence events:" + e.getMessage()); smafLogException(e); }
        }
        else 
        {
            smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Unknown format type: " + formatType);
        }
    }

    public static void scoreTrackPCMData() 
    {
        String mtsp = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        int mtspChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"-------------------------- '" + mtsp +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"mtspChunkSize: " + mtspChunkSize);

        isPCM = true;
    }

    public static void scoreTrackWaveData() 
    {
        String mwa = "" + (char) input[decodePos++] + (char) input[decodePos++] + (char) input[decodePos++];
        byte chunkNumber = (byte) input[decodePos++];
        int mwaChunkSize = (input[decodePos++] & 0xFF) << 24 | (input[decodePos++] & 0xFF) << 16 | (input[decodePos++] & 0xFF) << 8 | (input[decodePos++] & 0xFF);
        
        byte waveType = (byte) (input[decodePos++] & 0xFF);
        byte channelType = (byte) ((waveType >> 7) & 0x01);
        byte dataFormat = (byte) ((waveType >> 4) & 0x07);
        byte baseBit = (byte) (waveType & 0x0F);
        byte samplingFreqMSB = (byte) (input[decodePos++] & 0xFF);
        byte samplingFreqLSB = (byte) (input[decodePos++] & 0xFF);
        short samplingFrequency = (short) (((samplingFreqMSB & 0xFF) << 8) | (samplingFreqLSB & 0xFF)); // I really doubt this will ever go over 48000Hz (hell, even 22050Hz since it's J2ME), so a short should suffice

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "-------------------------- '" + mwa +"' SECTION --------------------------");
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "chunkNumber: " + (chunkNumber+1));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "mwaChunkSize: " + mwaChunkSize);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "waveNumber: " + (waveType > (byte) 0x00 && waveType < (byte) 0x3F ? "Wave ID" : "PROHIBITED"));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "channelType: " + (channelType == (byte) 0x00 ? "Mono" : "Stereo"));
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "dataFormat: " + pcmDataFormats[dataFormat]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "baseBit: " + pcmBaseBits[baseBit]);
        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "samplingFrequency: " + samplingFrequency);

        // We have no PCM Audio Track Chunk, prepare to parse and, if needed, decode the PCM data directly
        byte[] waveData = new byte[mwaChunkSize];
        // "mwaChunkSize-3" because we have alread read 3 bytes after mwaChunkSize
        for(int i = 0; i < mwaChunkSize-3; i++) { waveData[i] = input[decodePos++]; }

        if(dataFormat == (byte) 0x00) // 2's complement PCM WAV
        {
            if(baseBit == (byte) 0x00) { pcmData.add(new ByteArrayInputStream(WAVTools.convert4BitWav(waveData, channelType+1, samplingFrequency, true)));}
            if(baseBit == (byte) 0x01) { pcmData.add(new ByteArrayInputStream(WAVTools.convert8BitWav(waveData, channelType+1, samplingFrequency, true)));}
            if(baseBit == (byte) 0x02) { pcmData.add(new ByteArrayInputStream(WAVTools.convert12BitWav(waveData, channelType+1, samplingFrequency, true)));}
            if(baseBit == (byte) 0x03) { pcmData.add(new ByteArrayInputStream(WAVTools.convert16BitWav(waveData, channelType+1, samplingFrequency, true)));}
        }
        else if(dataFormat == (byte) 0x01) // Binary Offset PCM WAV
        {
            if(baseBit == (byte) 0x00) { pcmData.add(new ByteArrayInputStream(WAVTools.convert4BitWav(waveData, channelType+1, samplingFrequency, false)));}
            if(baseBit == (byte) 0x01) { pcmData.add(new ByteArrayInputStream(WAVTools.convert8BitWav(waveData, channelType+1, samplingFrequency, false)));}
            if(baseBit == (byte) 0x02) { pcmData.add(new ByteArrayInputStream(WAVTools.convert12BitWav(waveData, channelType+1, samplingFrequency, false)));}
            if(baseBit == (byte) 0x03) { pcmData.add(new ByteArrayInputStream(WAVTools.convert16BitWav(waveData, channelType+1, samplingFrequency, false)));}
        } 
        else if(dataFormat == (byte) 0x02) // YAMAHA ADPCM (TODO: SMAF seems to only use ADPCM-B?)
        {
            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"PCM data uses YAMAHA ADPCM. Decoding for this is not fully tested!");
            pcmData.add(new ByteArrayInputStream(WAVYamahaADPCMDecoder.ADPCMBDecode(waveData, samplingFrequency, channelType+1)));
        }
        else 
        { 
            smafLog(SMAF_LOG_ERROR, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Invalid PCM data format!"); 
            pcmData.add(new ByteArrayInputStream(null));
        }
    }

    /* -------------------------------------------------------------------------------------- */
    /*         Lower level decoding functions (huffman, sequence event, duration, etc)        */
    /* -------------------------------------------------------------------------------------- */

    public static void convertSequenceEvents(byte[] data) throws InvalidMidiDataException 
    {
        int offset = 0;
        int totalDuration = 0;
        byte firstDurByte = 0;
        int duration = 0;
        byte channel = 0;
        int gateTime = 0;
        byte firstGateByte = 0;
        byte noteNumber = 0;
        MidiEvent midiEvent = null;

        // Handy Phone and SoftBank MMMG no-inline-velocity notes use the same
        // loud default as the reference Yamaha path; authored volume and
        // expression events still shape the channel level afterward.
        if(formatType == (byte) 0x00 || formatType == (byte) 0x04)
        {
            for(int i = 0; i < channelData.length; i++) { channelData[i].velocity = 127; }
        }
    
        while (offset < data.length) 
        {

            if(formatType == (byte) 0x00) // Handy Phone format
            {
                firstDurByte = (byte) (data[offset++] & 0xFF);
                
                // Check firt byte's MSB for duration size in bytes
                if ((firstDurByte & (byte) 0x80) == 0) // Single-byte duration
                {
                    duration = firstDurByte;
                } 
                else // Dual-byte duration
                {
                    byte secondDurByte = (byte) (data[offset++] & 0xFF);
                    duration = ((firstDurByte & 0x3F) << 7) | (secondDurByte & 0x7F);
                    duration += 128; // Add 128 to Duration as per the SMAF documentation
                }

                totalDuration += (duration * timeBasetoMs(TimeBase_D)); // Update total duration

                // Now read the following event bytes.
                byte eventType = (byte) (data[offset++] & 0xFF);

                if (eventType == 0x00) // Event Type 0x00 indicates a message (bank change, program change, pitch bend, etc)
                {
                    byte eventData = (byte) (data[offset++] & 0xFF); // Read the next byte to determine the event type

                    channel = (byte) ((eventData >> 6) & 0x03); // Extract channel value from bits 6-7
                    channel += handyChannelIdx;
                    // bits 4 and 5 are additional event classification bits (used to discern between long type and short type events)
                    int b5 = (eventData >> 5) & 0x01;
                    int b4 = (eventData >> 4) & 0x01;

                    byte shortEventValue = (byte) (eventData & 0x0F);

                    if(eventData == (byte) 0x00) // Might signal the start of an End-Of-Sequence byte arrangement
                    {
                        byte endOfSequenceEventCheck = (byte) (data[offset++] & 0xFF);

                        if (endOfSequenceEventCheck == 0x00) { smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Reached end of Handy Phone Sequence Chunk."); } 
                        else 
                        {
                            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Invalid Handy Phone End Of Sequence value: " + endOfSequenceEventCheck);
                            return;
                        }
                    }
                    else if (b5 == 1 && b4 == 1) // Long type event
                    {
                        byte eventCategory = (byte) (eventData & 0x0F); // Get bits 4-7 for event category
                        byte valueField = (byte) (data[offset++] & 0xFF);
                        ShortMessage event = new ShortMessage();

                        switch (eventCategory) 
                        {
                            case 0x0: // Program Change
                                channelData[channel].program = (byte) (valueField & 0x7F);
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding program change value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                if (!usesMidiPercussionChannel(channel))
                                {
                                    setChannelMessage(event, ShortMessage.PROGRAM_CHANGE, channel, valueField, 0);
                                    midiEvent = new MidiEvent(event, totalDuration);
                                    track.add(midiEvent);
                                }
                                break;

                            case 0x1: // Bank Select
                                byte bankType = (byte) ((valueField & 0x80) >> 7); // Check if it's normal or drum bank
                                byte bankNumber = (byte) (valueField & 0x7F); // Use the lower 7 bits for the bank number
                    
                                // Log the bank type and number
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding bank change value 0x" + String.format("%02X", bankNumber) + "(" + bankNumber + ") of Type: (" + (bankType == 0 ? "Normal" : "Drum") + ") to channel " + channel);
                                channelData[channel].usingDrumBank = bankType == 1;
                                if (bankType == 1) 
                                {
                                    smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Channel Drum Bank requested. Routing channel " + channel + " through MIDI percussion channel 10 until a non-drum bank is requested");
                                }
                                // Handy Phone bank select is used to switch
                                // melodic/drum interpretation, not to emit a
                                // literal GM bank message. The active routing
                                // state is tracked in channelData and later
                                // note events are emitted on the resolved
                                // output channel.
                                break;

                            case 0x2: // Octave Shift (NOTE: MIDI doesn't have anything analogous to this, we have to implement it manually by shifting all notes in the channel after this event)                                
                                byte octaveShift = 0;

                                if (valueField >= (byte) 0x00 && valueField <= (byte) 0x04) { octaveShift = valueField; } /* Octave shifts 0 to +4 */
                                else if (valueField >= (byte) 0x81 && valueField <= (byte) 0x84) 
                                {
                                    octaveShift = (byte) (valueField - (byte) 0x80); // Map to -1 to -4
                                    octaveShift = (byte) -octaveShift; // Convert to negative value
                                } 
                                else 
                                {
                                    smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Reserved octave shift value: 0x" + String.format("%02X", valueField));
                                    continue;
                                }
                                
                                channelData[channel].octaveShift = octaveShift;
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding Octave Shift value 0x" + String.format("%02X", valueField) + "(" + channelData[channel].octaveShift + " octave) to channel " + channel);
                                break;

                            case 0x3: // Modulation (Long Type)
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding modulation value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 1, valueField);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                break;

                            case 0x4: // Pitch Bend (Long Type)
                                // SMAF long-form pitch bend uses the same 7-bit
                                // center as the short-form table below. Treating
                                // it as an 8-bit value centered at 0x80 turns
                                // small bends like 0x42 into almost a full
                                // semitone detune, which matches the FF4 menu
                                // pitch glitch we hear in practice.
                                int pitchBendValue = valueField & 0x7F;
                                int pitchBend = encodeCentered7PitchBend(pitchBendValue);
                                byte pitchBendLSB = (byte) (pitchBend & 0x7F);
                                byte pitchBendMSB = (byte) ((pitchBend >> 7) & 0x7F);

                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding pitch bend value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                setChannelMessage(event, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                break;

                            case 0x7: // Volume Change
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding volume value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 7, valueField);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                break;

                            case 0xA: // Panning Change
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding panning value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 10, valueField);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                break;

                            case 0xB: // Expression Change (Long Type)
                                smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding expression value 0x" + String.format("%02X", valueField) + "(" + valueField + ") to channel " + channel);
                                setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 11, valueField);
                                midiEvent = new MidiEvent(event, totalDuration);
                                track.add(midiEvent);
                                break;

                            default:
                                smafLog(SMAF_LOG_ERROR, "Unknown long event category: " + eventCategory);
                                break;
                        }
                    } 
                    else // Short type event (bits 4 and/or 5 are not zero, any order)
                    {
                        ShortMessage event = new ShortMessage();
                        // NOTE: Short values for mod, pitch, expr always go from 0x1 to 0xE, so this is why we access value-1 in the constant arrays
                        if (b5 == 1 && b4 == 0) // Modulation (Short Type)
                        {
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding modulation value 0x" + String.format("%02X", shortModValues[shortEventValue]) + "(" + shortModValues[shortEventValue] + ") to channel " + channel);
                            setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 1, shortModValues[shortEventValue]);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                        }

                        if (b5 == 0 && b4 == 1) // Pitch Bend (Short Type)
                        {
                            int pitchBend = encodeCentered7PitchBend(shortPitchBendValues[shortEventValue]);
                            int pitchBendLSB = pitchBend & 0x7F;
                            int pitchBendMSB = (pitchBend >> 7) & 0x7F;

                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding pitch bend value 0x" + String.format("%02X", shortPitchBendValues[shortEventValue]) + "(" + shortPitchBendValues[shortEventValue] + ") to channel " + channel);
                            setChannelMessage(event, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                        }

                        if (b5 == 0 && b4 == 0) // Expression (Short Type)
                        {
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding expression value 0x" + String.format("%02X", shortExpressionValues[shortEventValue]) + "(" + shortExpressionValues[shortEventValue] + ") to channel " + channel);
                            setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 11, shortExpressionValues[shortEventValue]);
                            midiEvent = new MidiEvent(event, totalDuration);
                            track.add(midiEvent);
                        }
                    }
                }
                else if (eventType == (byte) 0xFF) // 0xFF denotes this event is a sysEx message or NOP (No Operation)
                {
                    byte SysEx = (byte) (data[offset++] & 0xFF);
                    if(SysEx == (byte) 0x00) 
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "NOP event received");
                    }
                    else 
                    {
                        List<Byte> exclusiveMessage = new ArrayList<Byte>(); // Seems to relate to SysEx messages, maybe we don't need these?
                        while(data[offset] != (byte) 0xF7) // 0xF7 as the value marks the end of the SysEx message
                        {
                            exclusiveMessage.add(data[offset++]);   
                        }
                        offset++; // Move out of offset containing 0xF7, as the next one is the next status byte

                        sequenceSysExEvents.add(new SequenceSysExEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                toByteArray(exclusiveMessage)));
                        
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "SysEx event received");
                        // TODO: Maybe use this SysEx for something?
                    }
                }
                else // Any other value for event type is a note message (which contains channel, octave and note in the eventType byte itself)
                {
                    // Extract channel, octave, and note number from the eventType
                    channel = (byte) ((eventType >> 6) & 0x03); // Bits 6-7 for channel
                    channel += handyChannelIdx;
                    byte octave = (byte) ((eventType >> 4) & 0x03); // Bits 4-5 for octave
                    noteNumber = (byte) (eventType & 0x0F); // Bits 0-3 for note number
                
                    // Read gate time
                    firstGateByte = (byte) (data[offset++] & 0xFF); // First byte for gate time
            
                    // gateTime works pretty much like duration in how it's read
                    if ((firstGateByte & (byte) 0x80) == 0) // Single-byte gate time
                    { 
                        gateTime = firstGateByte;
                    } 
                    else // Dual-byte gate time
                    { 
                        byte secondGateByte = (byte) (data[offset++] & 0xFF);
                        gateTime = ((firstGateByte & 0x3F) << 7) | (secondGateByte & 0x7F);
                        gateTime += 128; // Add 128 to gate time as per the SMAF documentation
                    }
            
                    // Some real-world phrases encode zero gate lengths. Skip the
                    // broken note, but keep decoding the remainder of the track.
                    if (gateTime <= 0) 
                    {
                        smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping note with zero gateTime on channel " + channel + ".");
                        continue;
                    }

                    int midiNoteNumber;
                    if (usesMidiPercussionChannel(channel))
                    {
                        // Handy Phone percussion selects the drum sound with
                        // the current program number rather than the packed
                        // note byte. This mirrors the reference SMAF routing
                        // model more closely and avoids pitched instruments
                        // showing up as the wrong percussion sound.
                        midiNoteNumber = channelData[channel].program & 0x7F;
                    }
                    else
                    {
                        midiNoteNumber = 0;
                        switch (octave) 
                        {
                            case 0x00: midiNoteNumber = (byte) (36 + noteNumber); break; // Low
                            case 0x01: midiNoteNumber = (byte) (48 + noteNumber); break; // Mid Low
                            case 0x02: midiNoteNumber = (byte) (60 + noteNumber); break; // Mid High
                            case 0x03: midiNoteNumber = (byte) (72 + noteNumber); break; // High
                        }
                        midiNoteNumber += 12 * channelData[channel].octaveShift;
                    }

                    // Create MIDI note event
                    smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding note event " + noteTypes[noteNumber] + (4+octave+channelData[channel].octaveShift) + "(" + noteNumber + ")" + " to channel " + channel);
                    recordPcmSequenceTrigger(totalDuration,
                            gateTime,
                            noteNumber,
                            channelData[channel].velocity,
                            channel,
                            resolveMidiChannel(channel),
                            midiNoteNumber);
                    
                    ShortMessage noteOn = new ShortMessage();
                    setChannelMessage(noteOn, ShortMessage.NOTE_ON, channel, midiNoteNumber, channelData[channel].velocity);
                    midiEvent = new MidiEvent(noteOn, totalDuration);
                    track.add(midiEvent); // Add the note event to the corresponding channel

                    ShortMessage noteOff = new ShortMessage();
                    setChannelMessage(noteOff, ShortMessage.NOTE_OFF, channel, midiNoteNumber, 0);
                    midiEvent = new MidiEvent(noteOff, totalDuration+(gateTime * timeBasetoMs(TimeBase_G)));
                    track.add(midiEvent);
                }
            }
            else if (formatType == (byte) 0x01 || formatType == (byte) 0x02) // Mobile Standard formats (with or without compression, as this method will receive uncompressed data either way)
            {
                // In Mobile Standard, both duration and gateTime use the variable length notation, up to 4 bytes
                // TODO: Values longer than 2 and 3 bytes are untested
                firstDurByte = (byte) (data[offset++] & 0xFF);
                if ((firstDurByte & (byte) 0x80) == 0) { duration = firstDurByte & 0x7F; } // Single-byte duration
                else // Multi-byte duration
                {
                    duration = (firstDurByte & 0x3F) << 7;

                    for (int i = 1; i < 4; i++) 
                    {
                        byte nextDurByte = (byte) (data[offset++] & 0xFF);
                        duration |= (nextDurByte & 0x7F) << (7 * (i - 1));

                        // Break if the MSB of the next byte is 0 (indicating the end)
                        if ((nextDurByte & (byte) 0x80) == 0) { break; }
                    }
                }

                totalDuration += (duration * timeBasetoMs(TimeBase_D)); // Update total duration
                // Read status byte
                int status = data[offset++] & 0xFF; // Read status byte
           
                if(status == 0xF0) // SysEx messages, maybe we don't need these?
                {
                    List<Byte> exclusiveMessage = new ArrayList<Byte>();

                    while(offset < data.length && data[offset] != (byte) 0xF7) // Like with formatType 0x00, 0xF7 as the value marks the end of the SysEx message
                    {
                        exclusiveMessage.add(data[offset++]);   
                    }
                    offset++; // Move out of offset containing 0xF7, as the next one is the next status byte
                    
                    smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "SysEx Message parsed. Size:" + exclusiveMessage.size());
                    // TODO: Maybe use this SysEx for something?
                }
                else if(status >= 0xF1 && status <= 0xFE) // Reserved Sequence status bytes
                {
                    smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Found a reserved sequence status byte: " + status);
                }
                else if(status == 0xFF) // End of Sequence (EOS) or NOP
                {
                    if (data[offset] == (byte) 0x2F) // EOS
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "End of Sequence reached. " + status);
                        return;
                    }
                    if (data[offset] == (byte) 0x00) // NOP
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "NOP parsed");
                        offset += 1; // Skip 0xFF and 0x00
                    }
                }
                else // Audio events
                {
                    ShortMessage noteOn = new ShortMessage();
                    ShortMessage noteOff = new ShortMessage();
                    switch (status & 0xF0) 
                    {
                        case 0x80: // Note without velocity
                            channel = (byte) (status & 0x0F);
                            noteNumber = (byte) (data[offset++] & 0x7F); // Note Number
                            // Read gate time
                            firstGateByte = (byte) (data[offset++] & 0xFF);
                            if ((firstGateByte & (byte) 0x80) == 0) { gateTime = firstGateByte & 0x7F; } // Single-byte gateTime
                            else // Multi-byte gateTime
                            {
                                gateTime = (firstGateByte & 0x3F) << 7;

                                for (int i = 1; i < 4; i++) 
                                {
                                    byte nextGateByte = (byte) (data[offset++] & 0xFF);
                                    gateTime |= (nextGateByte & 0x7F) << (7 * (i - 1));

                                    // Break if the MSB of the next byte is 0 (indicating the end)
                                    if ((nextGateByte & (byte) 0x80) == 0) { break; }
                                }
                            }
                    
                            // Some carrier-authored files use zero gate times for
                            // malformed note placeholders. Ignore the note instead
                            // of aborting the whole phrase.
                            if (gateTime <= 0) 
                            {
                                smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping note with zero gateTime on channel " + channel + ".");
                                break;
                            }
                            
                            // TODO: This might be incorrect as the SMAF documentation doesn't detail how the chip differentiates between PCM data and Sequence Data to play, for now notes are played alongside the PCM index
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding note value " + noteNumber + " to channel " + channel);
                            recordPcmSequenceTrigger(totalDuration,
                                    gateTime,
                                    noteNumber,
                                    channelData[channel].velocity,
                                    channel,
                                    resolveMidiChannel(channel),
                                    noteNumber & 0x7F);
                            
                            // We still add the notes no matter, just so that the sequencer can actually reach the PCM request time
                            setChannelMessage(noteOn, ShortMessage.NOTE_ON, channel, noteNumber, channelData[channel].velocity);
                            midiEvent = new MidiEvent(noteOn, totalDuration);
                            track.add(midiEvent); // Add the note event to the corresponding channel

                            setChannelMessage(noteOff, ShortMessage.NOTE_OFF, channel, noteNumber, 0);
                            midiEvent = new MidiEvent(noteOff, totalDuration+(gateTime * timeBasetoMs(TimeBase_G)));
                            track.add(midiEvent);
                            break;
            
                        case 0x90: // Note with velocity
                            channel = (byte) (status & 0x0F);
                            noteNumber = (byte) (data[offset++] & 0x7F); // Note Number
                            channelData[channel].velocity = (byte) (data[offset++] & 0x7F); // Key Velocity
                            // Read gate time
                            firstGateByte = (byte) (data[offset++] & 0xFF);
                            if ((firstGateByte & (byte) 0x80) == 0) { gateTime = firstGateByte & 0x7F; } // Single-byte gateTime
                            else // Multi-byte gateTime
                            {
                                gateTime = (firstGateByte & 0x3F) << 7;

                                for (int i = 1; i < 4; i++) 
                                {
                                    byte nextGateByte = (byte) (data[offset++] & 0xFF);
                                    gateTime |= (nextGateByte & 0x7F) << (7 * (i - 1));

                                    // Break if the MSB of the next byte is 0 (indicating the end)
                                    if ((nextGateByte & (byte) 0x80) == 0) { break; }
                                }
                            }
                    
                            // Some carrier-authored files use zero gate times for
                            // malformed note placeholders. Ignore the note instead
                            // of aborting the whole phrase.
                            if (gateTime <= 0) 
                            {
                                smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping note with zero gateTime on channel " + channel + ".");
                                break;
                            }
                            
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding note value " + noteNumber + " with new velocity to channel " + channel);
                            recordPcmSequenceTrigger(totalDuration,
                                    gateTime,
                                    noteNumber,
                                    channelData[channel].velocity,
                                    channel,
                                    resolveMidiChannel(channel),
                                    noteNumber & 0x7F);
                            
                            setChannelMessage(noteOn, ShortMessage.NOTE_ON, channel, noteNumber, channelData[channel].velocity);
                            midiEvent = new MidiEvent(noteOn, totalDuration);
                            track.add(midiEvent);

                            setChannelMessage(noteOff, ShortMessage.NOTE_OFF, channel, noteNumber, 0);
                            midiEvent = new MidiEvent(noteOff, totalDuration+(gateTime * timeBasetoMs(TimeBase_G)));
                            track.add(midiEvent);
                            break;
            
                        case 0xA0: // Reserved (3 bytes)
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping 3 reserved bytes");
                            offset += 3; // Skip 3 bytes
                            break;
            
                        case 0xB0: // Control Change (0xB0 to 0xBF)
                            channel = (byte) (status & 0x0F);
                            int controlNumber = data[offset++] & 0x7F; // Control Number
                            int controlValue = data[offset++] & 0x7F; // Control Value
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding control change number " + controlNumber + " with value " + controlValue + " to channel " + channel);
                            
                            ShortMessage ctrlChange = new ShortMessage();
                            setChannelMessage(ctrlChange, ShortMessage.CONTROL_CHANGE, channel, controlNumber, controlValue);
                            midiEvent = new MidiEvent(ctrlChange, totalDuration);
                            track.add(midiEvent);
                            break;
            
                        case 0xC0: // Program Change (0xC0 to 0xCF)
                            channel = (byte) (status & 0x0F);
                            int programNumber = data[offset++] & 0x7F; // Program Number
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding program change number " + programNumber + " to channel " + channel);
                            
                            ShortMessage prgChange = new ShortMessage();
                            setChannelMessage(prgChange, ShortMessage.PROGRAM_CHANGE, channel, programNumber, 0);
                            midiEvent = new MidiEvent(prgChange, totalDuration);
                            track.add(midiEvent);
                            break;
            
                        case 0xD0: // Reserved (2 bytes)
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping 2 reserved bytes");
                            offset += 2; // Skip 2 bytes
                            break;
            
                        case 0xE0: // Pitch Bend (0xE0 to 0xEF)
                            channel = (byte) (status & 0x0F);
                            byte pitchBendLSB = (byte) (data[offset++] & 0x7F); // Pitch Bend Change LSB
                            byte pitchBendMSB = (byte) (data[offset++] & 0x7F); // Pitch Bend Change MSB
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding pitch bend MSB " + pitchBendMSB + " LSB " + pitchBendLSB + " to channel " + channel);
                            
                            ShortMessage pitchBend = new ShortMessage();
                            setChannelMessage(pitchBend, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                            midiEvent = new MidiEvent(pitchBend, totalDuration);
                            track.add(midiEvent);
                            break;

                        default:
                            // Unknown status
                            smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Unknown status byte: " + status);
                            break;
                    }
                }
            }
            else if(formatType == (byte) 0x04) // This one is weird, it works similar to DoJa's iMelody in parts, Mobile Standard in some, and Handy Phone in others...
            {
                /*
                 * Based on: 
                 * https://github.com/umjammer/vavi-sound/blob/master/src/main/java/vavi/sound/smaf/chunk/SequenceDataChunk.java 
                 * https://github.com/but80/smaf825/blob/v1/smaf/event/event.go
                 */

                // MMMG/SEQU phrases use the native Yamaha 1-byte / 2-byte
                // length encoding rather than a generic MIDI VLQ:
                // 0xxxxxxx -> 0..127
                // 1xxxxxxx yyyyyyyy -> (((x & 0x7f) + 1) << 7) + y
                int[] offsetRef = new int[]{offset};
                duration = readSmafPhraseVariableLength(data, offsetRef);
                offset = offsetRef[0];

                totalDuration += (duration * timeBasetoMs(TimeBase_D)); // Update total duration
                byte status = (byte) (data[offset++] & 0xFF); // Read status byte

                if(status == 0x00) // Control event
                {
                    ShortMessage event = new ShortMessage();

                    byte controlEvent = (byte) (data[offset++] & 0xFF);
                    channel = (byte) ((controlEvent >> 6) & 0x03);
                    channel += handyChannelIdx;
                    byte eventType = (byte) (controlEvent & 0x3f);

                    if(eventType == (byte) 0x00) // Fine tune event (fine pitch bend)
                    {
                        int eventValue = data[offset++] & 0x7F;
                        int pitchBend = encodeCentered7PitchBend(eventValue);
                        byte pitchBendLSB = (byte) (pitchBend & 0x7F);
                        byte pitchBendMSB = (byte) ((pitchBend >> 7) & 0x7F);
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding fine tune value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                    }
                    else if (eventType >= (byte) 0x01 && eventType <= (byte) 0x0E) // Short Expression event
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding expression value 0x" + String.format("%02X", shortExpressionValues[eventType]) + "(" + shortExpressionValues[eventType] + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 11, shortExpressionValues[eventType]);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_EXPRESSION,
                                shortExpressionValues[eventType] & 0x7f);
                    }
                    else if (eventType >= (byte) 0x11 && eventType <= (byte) 0x1E) // Short Pitch Bend event
                    {
                        int pitchBend = encodeCentered7PitchBend(shortPitchBendValues[eventType-0x10]);
                        byte pitchBendLSB = (byte) (pitchBend & 0x7F);
                        byte pitchBendMSB = (byte) ((pitchBend >> 7) & 0x7F);

                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding pitch bend value 0x" + String.format("%02X", shortPitchBendValues[eventType-0x10]) + "(" + shortPitchBendValues[eventType-0x10] + ") to channel " + channel);
                        
                        setChannelMessage(event, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                    }
                    else if (eventType >= (byte) 0x21 && eventType <= (byte) 0x2E) // Short Modulation event
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "(short) Adding modulation value 0x" + String.format("%02X", shortModValues[eventType-0x20]) + "(" + shortModValues[eventType-0x20] + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 1, shortModValues[eventType-0x20]);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_MODULATION,
                                shortModValues[eventType - 0x20] & 0x7f);
                    }
                    else if (eventType == (byte) 0x30) // Program Change event
                    {
                        byte eventValue = (byte) (data[offset++] & 0x7F);
                        
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding program change number " + eventValue + " to channel " + channel);
                        setChannelMessage(event, ShortMessage.PROGRAM_CHANGE, channel, eventValue, 0);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_SELECTOR,
                                eventValue & 0x03);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_PROGRAM,
                                eventValue & 0x7f);
                    }
                    else if (eventType == (byte) 0x31) // Bank Select event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);
                        int bankType = (eventValue & 0x80) >> 7; // Check if it's normal or drum bank
                        int bankNumber = eventValue & 0x7F; // Use the lower 7 bits for the bank number
            
                        // Log the bank type and number
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding bank change value 0x" + String.format("%02X", bankNumber) + "(" + bankNumber + ") of Type: (" + (bankType == 0 ? "Normal" : "Drum") + ") to channel " + channel);
                        channelData[channel].usingDrumBank = bankType == 1;
                        if (bankType == 1) 
                        {
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Channel Drum Bank requested. Routing channel " + channel + " through MIDI percussion channel 10 until a non-drum bank is requested");
                        }
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 0, bankNumber);
                        // Send the bank select message
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_BANK,
                                eventValue & 0xff);
                    }
                    else if (eventType == (byte) 0x32) // Octave Shift event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);
                        byte octaveShift = 0;

                        if (eventValue >= (byte) 0x00 && eventValue <= (byte) 0x04) { octaveShift = eventValue; } /* Octave shifts 0 to +4 */
                        else if (eventValue >= (byte) 0x81 && eventValue <= (byte) 0x84) 
                        {
                            octaveShift = (byte) (eventValue - (byte) 0x80); // Map to -1 to -4
                            octaveShift = (byte) -octaveShift; // Convert to negative value
                        } 
                        else 
                        {
                            smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Reserved octave shift value: 0x" + String.format("%02X", eventValue));
                            continue;
                        }
                        
                        channelData[channel].octaveShift = octaveShift;
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding Octave Shift value 0x" + String.format("%02X", eventValue) + "(" + channelData[channel].octaveShift + " octave) to channel " + channel);
                    }
                    else if (eventType == (byte) 0x33) // Modulation event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);
                        
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding modulation value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 1, eventValue);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_MODULATION,
                                eventValue & 0x7f);
                    }
                    else if (eventType == (byte) 0x34) // Pitch Bend event
                    {
                        int eventValue = data[offset++] & 0xFF;
                        // Real-world SoftBank SEQU phrases sound closer to the
                        // official MA3 emulator when 0x34 is interpreted on
                        // the centered-7 lane rather than the full 8-bit lane.
                        int pitchBend = encodeCentered7PitchBend(eventValue & 0x7F);

                        byte pitchBendLSB = (byte) (pitchBend & 0x7F);
                        byte pitchBendMSB = (byte) ((pitchBend >> 7) & 0x7F);
                        
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding pitch bend value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.PITCH_BEND, channel, pitchBendLSB, pitchBendMSB);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_PITCH,
                                eventValue & 0x7f);
                    }
                    else if (eventType == (byte) 0x35) // Reserved one-byte mix/control event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Ignoring reserved control event 0x35 with value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") on channel " + channel);
                    }
                    // TODO: Maybe something's missing? This gap doesn't seem normal
                    else if (eventType == (byte) 0x36) // Expression event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);

                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding expression value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 11, eventValue);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_EXPRESSION,
                                eventValue & 0x7f);
                    }
                    else if (eventType == (byte) 0x37) // Volume event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);

                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding volume value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 7, eventValue);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME,
                                eventValue & 0x7f);
                    }
                    // TODO: Same as above, and this gap is even bigger
                    else if (eventType == (byte) 0x3A) // Panpot event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);

                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding panning value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 10, eventValue);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_PAN,
                                eventValue & 0x7f);
                    }
                    else if (eventType == (byte) 0x3B) // Phrase volume/fade event
                    {
                        byte eventValue = (byte) (data[offset++] & 0xFF);

                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding phrase volume value 0x" + String.format("%02X", eventValue) + "(" + eventValue + ") to channel " + channel);
                        setChannelMessage(event, ShortMessage.CONTROL_CHANGE, channel, 7, eventValue);
                        midiEvent = new MidiEvent(event, totalDuration);
                        track.add(midiEvent);
                        recordInternalLegacyYamahaStateEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                (controlEvent >> 6) & 0x03,
                                INTERNAL_LEGACY_YAMAHA_PHRASE_VOLUME,
                                eventValue & 0x7f);
                    }
                    else 
                    {
                        smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "unknown control event received:" + String.format("%02X", eventType));
                        offset++; // Consume the common one-byte control payload so later events stay aligned.
                    }
                }
                else if(status == (byte) 0xFF) // SysEx Message (works similarly to the Handy Phone format)
                {
                    byte SysEx = (byte) (data[offset++] & 0xFF);

                    if(SysEx == (byte) 0x00) 
                    {
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "NOP event received");
                    }
                    else if(SysEx == (byte) 0xF0)
                    {
                        byte size = (byte) (data[offset++] & 0xFF);
                        List<Byte> exclusiveMessage = new ArrayList<Byte>(); // Seems to relate to SysEx messages, maybe we don't need these?
                        while(data[offset] != (byte) 0xF7 && size > 1) // 0xF7 as the value marks the end of the SysEx message
                        {
                            exclusiveMessage.add(data[offset++]);
                            size--;
                        }
                        offset++; // Move out of offset containing 0xF7 or the last byte according to size (in case there's no 0xF7), as the next one is the next status byte

                        sequenceSysExEvents.add(new SequenceSysExEvent(
                                totalDuration,
                                handyChannelIdx >> 2,
                                toByteArray(exclusiveMessage)));
                        
                        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "SysEx event received");
                        // TODO: Maybe use this SysEx for something?
                    }
                    else if (SysEx >= (byte) 0x10 && SysEx <= (byte) 0x1F)
                    {
                        int userEventId = SysEx - 0x10;
                        sequenceUserEvents.add(new SequenceUserEvent(totalDuration, userEventId));
                        smafLog(SMAF_LOG_DEBUG,
                                SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName()
                                        + ": " + "User event received:" + userEventId + " at tick " + totalDuration);
                    }
                    else 
                    {
                        smafLog(SMAF_LOG_WARNING,
                                SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName()
                                        + ": " + "Unknown event received:" + String.format("%02X", SysEx));
                    }
                }
                else // Note event
                {
                    channel = (byte) ((status >> 6) & 0x03);
                    channel += handyChannelIdx;
                    byte noteValue = clampMidiNote((status & 15) + ((status >> 4 & 3) + 3) * 12
                            + (12 * channelData[channel].octaveShift));

                    offsetRef[0] = offset;
                    gateTime = readSmafPhraseVariableLength(data, offsetRef);
                    offset = offsetRef[0];

                    // Some real-world SEQU phrases encode zero gate lengths.
                    // Skip the note and continue decoding the phrase.
                    if (gateTime <= 0) 
                    {
                        smafLog(SMAF_LOG_WARNING, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Skipping note with zero gateTime on channel " + channel + ".");
                        continue;
                    }
                    
                    smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " + "Adding note value " + noteValue + " to channel " + channel);
                    recordPcmSequenceTrigger(totalDuration,
                            gateTime,
                            noteValue,
                            channelData[channel].velocity,
                            channel,
                            resolveMidiChannel(channel),
                            noteValue & 0x7F);
                    
                    ShortMessage noteOn = new ShortMessage();
                    setChannelMessage(noteOn, ShortMessage.NOTE_ON, channel, noteValue, channelData[channel].velocity); // This will always use the default velocity
                    midiEvent = new MidiEvent(noteOn, totalDuration);
                    track.add(midiEvent);

                    ShortMessage noteOff = new ShortMessage();
                    setChannelMessage(noteOff, ShortMessage.NOTE_OFF, channel, noteValue, 0);
                    midiEvent = new MidiEvent(noteOff, totalDuration+(gateTime * timeBasetoMs(TimeBase_G)));
                    track.add(midiEvent);
                }
            }
        }

        smafLog(SMAF_LOG_DEBUG, SMAFDecoder.class.getPackage().getName() + "." + SMAFDecoder.class.getSimpleName() + ": " +"Sequence data end reached. Returning.");
    }

    private static String getContentCodeType(int codeType) 
    {
        switch (codeType) 
        {
            case 0x00: return "Shift-JIS";
            case 0x01: return "ISO-8859-1"; // Latin-1
            case 0x02: return "EUC_KR";
            case 0x03: return "HZ-GB-2312";
            case 0x04: return "Big5";
            case 0x05: return "KOI8-R";
            case 0x20: return "UTF-16";
            case 0x21: return "UTF-32";
            case 0x22: return "UTF-7";
            case 0x23: return "UTF-8";
            case 0x24: return "UTF-16";
            case 0x25: return "UTF-32";
            default: return "UTF-8";
        }
    }

    private static int readMidiVariableLength(byte[] data, int[] offsetRef)
    {
        int offset = offsetRef[0];
        int value = 0;
        int count = 0;
        while (offset < data.length)
        {
            int next = data[offset++] & 0xFF;
            value = (value << 7) | (next & 0x7F);
            count++;
            if ((next & 0x80) == 0 || count == 4)
            {
                offsetRef[0] = offset;
                return value;
            }
        }
        offsetRef[0] = offset;
        return value;
    }

    private static int readSmafPhraseVariableLength(byte[] data, int[] offsetRef)
    {
        int offset = offsetRef[0];
        if (offset >= data.length)
        {
            return 0;
        }

        int first = data[offset++] & 0xFF;
        if ((first & 0x80) == 0)
        {
            offsetRef[0] = offset;
            return first;
        }

        if (offset >= data.length)
        {
            offsetRef[0] = offset;
            return first & 0x7F;
        }

        int second = data[offset++] & 0xFF;
        offsetRef[0] = offset;
        return (((first & 0x7F) + 1) << 7) + second;
    }

    private static int timeBasetoMs(byte timeBase) 
    {
        switch (timeBase) 
        {
            case 0x00: return 1;   // 1 ms
            case 0x01: return 2;   // 2 ms
            case 0x02: return 4;   // 4 ms
            case 0x03: return 5;   // 5 ms
            case 0x10: return 10;  // 10 ms
            case 0x11: return 20;  // 20 ms
            case 0x12: return 40;  // 40 ms
            case 0x13: return 50;  // 50 ms
            default: return 4;     // Default to 4 ms
        }
    }

    private static byte encodeLiteralMmmgTimeBase(int literalTimeBaseMs)
    {
        return switch (literalTimeBaseMs)
        {
            case 1 -> (byte) 0x00;
            case 2 -> (byte) 0x01;
            case 4 -> (byte) 0x02;
            case 5 -> (byte) 0x03;
            case 10 -> (byte) 0x10;
            case 20 -> (byte) 0x11;
            case 40 -> (byte) 0x12;
            case 50 -> (byte) 0x13;
            default -> (byte) 0x11;
        };
    }

    private static void recordExclusiveVoice(byte[] exclusiveVoice)
    {
        byte[] copy = Arrays.copyOf(exclusiveVoice, exclusiveVoice.length);
        exclusiveVoices.add(copy);
        startupPackets.add(copy.clone());
    }

    private static byte[] normalizeVoiceWavePacket(byte[] chunkData)
    {
        if (chunkData == null || chunkData.length == 0)
        {
            return null;
        }
        if (chunkData.length >= 4
                && (chunkData[0] & 0xFF) == 0xFF
                && (chunkData[1] & 0xFF) == 0xF1)
        {
            return Arrays.copyOfRange(chunkData, 4, chunkData.length);
        }
        return Arrays.copyOf(chunkData, chunkData.length);
    }

    private static String readChunkId(byte[] data, int offset)
    {
        if (offset + 4 > data.length)
        {
            return "";
        }
        return "" + (char) data[offset] + (char) data[offset + 1] + (char) data[offset + 2] + (char) data[offset + 3];
    }

    private static int readInt32(byte[] data, int offset)
    {
        if (offset + 4 > data.length)
        {
            return 0;
        }
        return (data[offset] & 0xFF) << 24
                | (data[offset + 1] & 0xFF) << 16
                | (data[offset + 2] & 0xFF) << 8
                | (data[offset + 3] & 0xFF);
    }

    // Helper method to get the timebase readable string based on its byte value
    private static String getTimeBaseDescription(byte timeBaseValue) 
    {
        for (SimpleEntry<Byte, String> entry : timebases) 
        {
            if (entry.getKey().equals(timeBaseValue)) 
            {
                return entry.getValue();
            }
        }
        return "Unknown TimeBase";
    }

    private static boolean usesMidiPercussionChannel(int smafChannel)
    {
        if (smafChannel < 0 || smafChannel >= channelData.length)
        {
            return false;
        }

        ChannelData data = channelData[smafChannel];
        return data.usingDrumBank || data.channelType == (byte) 0x03;
    }

    private static int resolveMidiChannel(int smafChannel)
    {
        if (usesMidiPercussionChannel(smafChannel))
        {
            return 9;
        }

        return Math.max(0, Math.min(15, smafChannel));
    }

    private static void setChannelMessage(ShortMessage message, int command, int smafChannel, int data1, int data2) throws InvalidMidiDataException
    {
        message.setMessage(command, resolveMidiChannel(smafChannel), data1, data2);
    }

    private static byte clampMidiNote(int note)
    {
        return (byte) Math.max(0, Math.min(127, note));
    }

    private static int encodeCentered7PitchBend(int value)
    {
        int centered = Math.max(0, Math.min(0x7F, value)) - 64;
        return Math.max(0, Math.min(0x3FFF, 0x2000 + (centered << 7)));
    }

    private static int encodeCentered8PitchBend(int value)
    {
        int centered = Math.max(0, Math.min(0xFF, value)) - 128;
        return Math.max(0, Math.min(0x3FFF, 0x2000 + (centered << 6)));
    }

    public static List<DecodedChannelState> decodedChannelStates()
    {
        List<DecodedChannelState> states = new ArrayList<>(channelData == null ? 0 : channelData.length);
        if (channelData == null)
        {
            return states;
        }

        for (int i = 0; i < channelData.length; i++)
        {
            ChannelData data = channelData[i];
            if (data == null)
            {
                continue;
            }
            states.add(new DecodedChannelState(
                    i,
                    resolveMidiChannel(i),
                    data.usingDrumBank,
                    data.channelType & 0xFF,
                    data.velocity & 0xFF,
                    data.octaveShift));
        }
        return states;
    }

    private static byte[] toByteArray(List<Byte> bytes)
    {
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++)
        {
            result[i] = bytes.get(i);
        }
        return result;
    }

    private static void recordInternalLegacyYamahaStateEvent(int tick, int sourceBank, int channelInBank, int command, int value)
    {
        sequenceSysExEvents.add(new SequenceSysExEvent(
                tick,
                sourceBank,
                new byte[]{
                        0x72,
                        (byte) (command & 0x7f),
                        (byte) (channelInBank & 0x03),
                        (byte) value
                }));
    }

    private static void recordPcmSequenceTrigger(int startTick,
                                                 int gateTime,
                                                 int noteValue,
                                                 int velocity,
                                                 int channel,
                                                 int midiChannel,
                                                 int midiNote)
    {
        int gateTimeMs = gateTime * timeBasetoMs(TimeBase_G);
        int triggerTick = startTick + gateTimeMs;
        pcmDataPositions.put(triggerTick, noteValue);
        pcmDataVelocities.put(triggerTick, velocity);
        pcmSequenceTriggers.add(new PcmSequenceTrigger(
                triggerTick,
                startTick,
                gateTime,
                gateTimeMs,
                noteValue,
                velocity,
                channel,
                midiChannel,
                midiNote));
    }

    public static record SequenceSysExEvent(int tick, int sourceBank, byte[] data)
    {
    }

    public static record SequenceUserEvent(int tick, int eventId)
    {
    }

    public static record PcmSequenceTrigger(int triggerTick,
                                            int startTick,
                                            int gateTime,
                                            int gateTimeMs,
                                            int noteValue,
                                            int velocity,
                                            int smafChannel,
                                            int midiChannel,
                                            int midiNote)
    {
    }

    public static record DecodedChannelState(int smafChannel,
                                             int midiChannel,
                                             boolean usingDrumBank,
                                             int channelType,
                                             int velocity,
                                             int octaveShift)
    {
    }
}

class ChannelData 
{
    public boolean keyControlBasic, led, vibStatus, usingDrumBank;
    public byte keyControl, channelType, octaveShift, velocity, program;

    ChannelData() 
    {
        keyControl = 0x1;
        channelType = 0x0;
        led = false;
        vibStatus = false;
        octaveShift = 0;
        usingDrumBank = false;
        velocity = 64; // Default SMAF channel velocity is 64
        program = 0;
    }
}

class SmafCRC 
{
    private static final int CHAR_BIT = 8;
    private static final int UCHAR_MAX = 0xFF;
    private static final int CRCPOLY1 = 0x1021;
    private static final int[] crctable = new int[UCHAR_MAX + 1];

    static { makeCRCTable(); }

    private static void makeCRCTable() 
    {
        for (int i = 0; i <= UCHAR_MAX; i++) 
        {
            int r = i << (16 - CHAR_BIT);
            for (int j = 0; j < CHAR_BIT; j++) 
            {
                if ((r & 0x8000) != 0) { r = (r << 1) ^ CRCPOLY1; } 
                else { r <<= 1; }
            }
            crctable[i] = r & 0xFFFF;
        }
    }

    public static int makeCRC(int n, byte[] c) 
    {
        int r = 0xFFFF;
        int i = 0;
        while(--n >= 0) 
        {
            r = (r << CHAR_BIT) ^ crctable[(byte) (((r >> (16 - CHAR_BIT)) ^ c[i++])) & 0xFF];
        }
        return ~r & 0xFFFF; // Return the inverted CRC
    }

    public static boolean verifySmafCRC(byte[] fileChunk) 
    {
        if (fileChunk.length < 2) { return false; }

        int providedCRC = ((fileChunk[fileChunk.length - 2] & 0xFF) << 8) | (fileChunk[fileChunk.length - 1] & 0xFF);

        int calculatedCRC = makeCRC(fileChunk.length - 2, fileChunk);

        return calculatedCRC == providedCRC;
    }
}

// Huffman decoding largely based on how MMFtool does it
class HuffmanDecoder 
{
    // Huffman tree variables
    private static final int N = 256; // Number of characters (decoded values go from 0x00 to 0xFF in range)
    private static final int[] left = new int[2 * N - 1];
    private static final int[] right = new int[2 * N - 1];
    private static int avail = 0;

    // Bit-reading vars and methods
    private static byte[] data;
    private static int size;
    private static int byteOffset;
    private static int bitOffset;
    private static byte currentByte;

    public static int bitRead() 
    {
        if (bitOffset == 8) 
        {
            if (++byteOffset >= size){ return -1; }
            bitOffset = 0;
            currentByte = data[byteOffset];
        }

        return (currentByte >> (7 - bitOffset++)) & 0x01;
    }

    public static int bitNRead(int n) 
    {
        int bits = 0;
        for (int i = 0; i < n; i++) 
        {
            int bit = bitRead();
            if (bit == -1) { return 0; } // End of data
            bits = (bits << 1) | bit;
        }
        return bits;
    }

    // Huffman Tree and decoding
    public static int readTree(boolean init) 
    {
        int b = bitRead();
        if (init) { avail = N; }

        if (b == -1) { return -1; }
        if (b == 1) 
        {
            int i = avail++;
            if (avail > 2 * N - 1) { return -1; }
            if ((left[i] = readTree(false)) == -1)  { return -1; }
            if ((right[i] = readTree(false)) == -1) { return -1; }
            return i;
        } 
        else { return bitNRead(8); }
    }

    public static byte[] huffmanDecode(int destSize, byte[] src) 
    {
        final byte[] decodedData = new byte[destSize];
        int root;
        data = src;
        size = src.length;
        byteOffset = -1;
        bitOffset = 8;
        
        if ((root = readTree(true)) == -1) { return null; }

        for (int k = 0; k < destSize; k++) 
        {
            int j = root;
            while (j >= N) 
            {
                int b = bitRead();
                if (b == -1) { return null; }
                j = (b == 1) ? right[j] : left[j];
            }
            decodedData[k] = (byte) j;
        }

        return decodedData;
    }
}

