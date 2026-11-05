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
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.recompile.mobile.Mobile;

public final class WAVTools 
{

    public static final byte PCMHEADERSIZE = 44;

    public static int hostSampleRate = 0;

	static // Get the host device's sample rate when this class is initially created, getDefaultAudioSampleRate() is rather expensive to call
	{
		hostSampleRate = getDefaultAudioSampleRate();
	}

	/**
	 * Reads the format and data location, leaving the stream at the first sample.
	 * Returns format, sample rate, channels, block alignment, bit depth, data length, and data offset.
	 */
	public static final int[] readHeader(InputStream input) throws IOException
	{
		if (!"RIFF".equals(readInputStreamASCII(input, 4)))
			throw new IOException("Not a RIFF file");
		long riffSize = Integer.toUnsignedLong(readInputStreamInt32(input));
		// Callers use int offsets and in-memory byte arrays.
		if (riffSize < 4 || riffSize > Integer.MAX_VALUE - 8L)
			throw new IOException("Invalid or unsupported WAV RIFF size: " + riffSize);
		if (!"WAVE".equals(readInputStreamASCII(input, 4)))
			throw new IOException("Not a WAVE file");

		long remaining = riffSize - 4;
		int totalBytesRead = 12;
		int[] header = null;
		while (remaining > 0)
		{
			if (remaining < 8)
				throw new IOException("Truncated WAV chunk header");
			String chunkId = readInputStreamASCII(input, 4);
			long chunkSize = Integer.toUnsignedLong(readInputStreamInt32(input));
			remaining -= 8;
			totalBytesRead += 8;
			long paddedSize = chunkSize + (chunkSize & 1);
			if (paddedSize > remaining)
				throw new IOException("WAV " + chunkId + " chunk exceeds RIFF boundary");

			if ("data".equals(chunkId))
			{
				if (header == null)
					throw new IOException("WAV data chunk precedes fmt chunk");
				header[5] = (int) chunkSize;
				header[6] = totalBytesRead;
				Mobile.log(Mobile.LOG_DEBUG, WAVTools.class.getName()
						+ ": WAV format=" + header[0] + " sampleRate=" + header[1]
						+ " channels=" + header[2] + " blockAlign=" + header[3]
						+ " bitsPerSample=" + header[4] + " dataLength=" + header[5]
						+ " dataOffset=" + header[6]);
				return header;
			}

			long consumed = 0;
			if ("fmt ".equals(chunkId))
			{
				if (chunkSize < 16)
					throw new IOException("WAV fmt chunk is shorter than 16 bytes");
				int audioFormat = readInputStreamInt16(input);
				int audioChannels = readInputStreamInt16(input);
				int sampleRate = readInputStreamInt32(input);
				readInputStreamInt32(input); // Average bytes per second.
				int blockAlign = readInputStreamInt16(input);
				int bitsPerSample = readInputStreamInt16(input);
				if (audioChannels == 0 || sampleRate <= 0 || blockAlign == 0)
					throw new IOException("Invalid WAV format parameters");
				header = new int[] {audioFormat, sampleRate, audioChannels, blockAlign, bitsPerSample, 0, 0};
				consumed = 16;
			}
			// Metadata can precede fmt, and every chunk includes WORD padding.
			input.skipNBytes(paddedSize - consumed);
			totalBytesRead += (int) paddedSize;
			remaining -= paddedSize;
		}
		throw new IOException("WAV data chunk is missing");
	}

	/* Read a 16-bit little-endian unsigned integer from input.*/
	private static final int readInputStreamInt16(InputStream input) throws IOException 
	{
		int low = input.read();
		int high = input.read();
		if ((low | high) < 0) throw new EOFException("Truncated WAV header");
		return low | (high << 8);
	}

	/* Read a 32-bit little-endian signed integer from input.*/
	private static final int readInputStreamInt32(InputStream input) throws IOException 
	{
		return readInputStreamInt16(input) | (readInputStreamInt16(input) << 16);
	}

	/* Return a String containing 'n' Characters of ASCII/ISO-8859-1 text from input. */
	private static final String readInputStreamASCII(InputStream input, int nChars) throws IOException 
	{
		byte[] chars = new byte[nChars];
		readInputStreamData(input, chars, 0, nChars);
		return new String(chars, "ISO-8859-1");
	}

	/* Read 'n' Bytes from the InputStream starting from the specified offset into the output array. */
	public static final void readInputStreamData(InputStream input, byte[] output, int offset, int nBytes) throws IOException 
	{
		int end = offset + nBytes;
		while(offset < end) 
		{
			int read = input.read(output, offset, end - offset);
			if(read < 0) throw new java.io.EOFException();
			offset += read;
		}
	}

	/* 
	 * Builds a WAV header that describes the decoded ADPCM file on the first 44 bytes. 
	 * Data: little-endian, 16-bit, signed, same sample rate and channels as source IMA ADPCM.
	 */
	public static final void buildHeader(byte[] buffer, final short numChannels, final int sampleRate, final short numBits, final int sampleDataLength) 
	{ 
		final short bitsPerSample = numBits;   /* 16-bit or 8-bit PCM */
		final short audioFormat = 1;           /* WAV linear PCM */
		final int subChunkSize = 16;           /* Fixed size for Wav Linear PCM */
		final int chunk = 0x52494646;          /* 'RIFF' */ 
		final int format = 0x57415645;         /* 'WAVE' */ 
		final int subChunk1 = 0x666d7420;      /* 'fmt ' */ 
		final int subChunk2 = 0x64617461;      /* 'data' */ 

		/* 
		 * Frame size is fairly standard, and PCM's fixed sample size makes it so the frameSize is either 2 bytes 
		 * for mono, or 4 bytes for stereo.
		 */
		final short frameSize = (short) (numChannels * (bitsPerSample / 8));

		/* 
		 * Represents how many bytes are streamed per second. With all of the data above, it's trivial to
		 * calculate by getting the sample rate, the amount of channels and bytes per sample (bitsPerSample / 8)
		 */
		final int bytesPerSec = sampleRate * numChannels * (bitsPerSample / 8);
		
		/* NOTE: ChunkSize is the total file size - 8 bytes */
		writeIntBE(buffer, 0, chunk);                 // ChunkID
		writeIntLE(buffer, 4, buffer.length - 8);     // ChunkSize
		writeIntBE(buffer, 8, format);                // Format
		writeIntBE(buffer, 12, subChunk1);            // SubchunkID (fmt)
		writeIntLE(buffer, 16, subChunkSize);         // SubchunkSize
		writeShort(buffer, 20, audioFormat);          // Audioformat
		writeShort(buffer, 22, numChannels);          // NumChannels
		writeIntLE(buffer, 24, sampleRate);           // SampleRate
		writeIntLE(buffer, 28, bytesPerSec);          // ByteRate
		writeShort(buffer, 32, frameSize);            // BlockAlign
		writeShort(buffer, 34, bitsPerSample);        // BitsPerSample
		writeIntBE(buffer, 36, subChunk2);            // Subchunk2ID (data)
		writeIntLE(buffer, 40, sampleDataLength);     // Subchunk2 Size
	}

	private static final void writeIntLE(byte[] buffer, int index, int value) 
	{
		buffer[index] = (byte) (value & 0xFF);
		buffer[index + 1] = (byte) ((value >> 8) & 0xFF);
		buffer[index + 2] = (byte) ((value >> 16) & 0xFF);
		buffer[index + 3] = (byte) ((value >> 24) & 0xFF);
	}

	// A few of the header fields are big endian
	private static final void writeIntBE(byte[] buffer, int index, int value) 
	{
		buffer[index] = (byte) ((value >> 24) & 0xFF);
		buffer[index + 1] = (byte) ((value >> 16) & 0xFF);
		buffer[index + 2] = (byte) ((value >> 8) & 0xFF);
		buffer[index + 3] = (byte) (value & 0xFF);
	}

	private static final void writeShort(byte[] buffer, int index, short value) 
	{
		buffer[index] = (byte) (value & 0xFF);
		buffer[index + 1] = (byte) ((value >> 8) & 0xFF);
	}

    /* ------------------------------- NON-ADPCM SECTION -------------------------------*/


	// These will convert from different PCM formats to either 8 or 16-bit Signed PCM:
	public static final byte[] convert4BitWav(byte[] input, int numChannels, int sampleRate, boolean is2Complement) 
	{
		byte[] convertedWav = new byte[2*input.length];
		
		for (int i = 0; i < input.length; i++) 
		{
			// Get the upper 4 bits (MSB) and lower 4 bits (LSB), since we have 2 samples per byte on the original 4-bit wav
			int upperNibble = (input[i] >> 4) & 0x0F;
			int lowerNibble = input[i] & 0x0F;

			if(is2Complement)
			{
				upperNibble ^= 0x08;
				lowerNibble ^= 0x08;
			}

			convertedWav[i * 2] = (byte) (upperNibble < 8 ? upperNibble * 17 : (upperNibble - 8) * 17);
			convertedWav[i * 2 + 1] = (byte) (lowerNibble < 8 ? lowerNibble * 17 : (lowerNibble - 8) * 17);
		}

		return upsample(convertedWav, sampleRate, hostSampleRate, (short) numChannels, (short) 8, convertedWav.length);
	}

	// This one pretty much just converts from 2's complement to binary offset if needed, and builds a header (8-bit wav are unsigned binary offset instead of signed 2's complement like 16+ bits)
	public static final byte[] convert8BitWav(byte[] input, int numChannels, int sampleRate, boolean is2Complement) 
	{
		if (is2Complement) 
		{
			for (int i = 0; i < input.length; i++) 
			{
				input[i] = (byte) (input[i] ^ 0x80);
			}
		}
		
		return upsample(input, sampleRate, hostSampleRate, (short) numChannels, (short) 8, input.length);
	}

	// TODO: Does this kind of WAV even exist?
	public static final byte[] convert12BitWav(byte[] input, int numChannels, int sampleRate, boolean is2Complement) 
	{
		byte[] convertedWav = new byte[(int)(1.5 * input.length) + 1]; // Add an extra byte for safety

		for (int i = 0; i < input.length / 3; i++) 
		{
			int sampleIndex = i * 3;

			int sample = ((input[sampleIndex] & 0xFF) << 4) | ((input[sampleIndex + 1] & 0xFF) >> 4);

			if (is2Complement) 
			{
				convertedWav[i * 2] = (byte) (sample & 0xFF);
				convertedWav[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
			} 
			else 
			{
				sample -= 2048;
				convertedWav[i * 2] = (byte) (sample & 0xFF);
				convertedWav[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
			}
		}
		return upsample(convertedWav, sampleRate, hostSampleRate, (short) numChannels, (short) 16, convertedWav.length);
	}

	public static final byte[] convert16BitWav(byte[] input, int numChannels, int sampleRate, boolean is2Complement) 
	{
		byte[] convertedWav = new byte[input.length];

		for (int i = 0; i < input.length / 2; i++) 
		{
			int sampleIndex = i * 2;
			short sample = (short) ((input[sampleIndex] & 0xFF) | (input[sampleIndex + 1] << 8));

			if (!is2Complement) { sample ^= 0x8000; }

			convertedWav[sampleIndex] = (byte) (sample & 0xFF);
			convertedWav[sampleIndex + 1] = (byte) ((sample >> 8) & 0xFF);
		}

		return upsample(convertedWav, sampleRate, hostSampleRate, (short) numChannels, (short) 16, convertedWav.length);
	}

	public static final byte[] upsample(byte[] input, int originalSampleRate, int newSampleRate, short numChannels, short numBits, int inputLength) 
	{
		inputLength = Math.min(input.length, inputLength); // Some wav files might report a sample length bigger than the actual data (Shadow Shoot)

		final int newLength = (int) (inputLength * ((double) newSampleRate / originalSampleRate));
		final byte[] upsampled = new byte[PCMHEADERSIZE + newLength]; // Allocate for header + upsampled audio data
		final double ratio = (double) originalSampleRate / newSampleRate;
		double cosineFraction;
		int originalIndex, sample1, sample2, interpolatedValue;
		
		// No upsampling needed, just prepend a header to the data (this method is used as the final output of other PCM decoders)
		if(originalSampleRate == newSampleRate)
		{
			System.arraycopy(input, 0, upsampled, PCMHEADERSIZE, newLength);
			buildHeader(upsampled, numChannels, newSampleRate, numBits, newLength);

			return upsampled;
		}
		
		// Upsample the audio data based on how many bits per sample it has
		for (int i = 0; i < newLength; i++) 
		{
			originalIndex = (int) (i * ratio);
			cosineFraction = (1 - Math.cos(((i * ratio) - originalIndex) * Math.PI)) / 2;

			if (numBits == 8) 
			{
				sample1 = (input[originalIndex] & 0xFF);
				sample2 = (originalIndex + 1 < inputLength) ? (input[originalIndex + 1] & 0xFF) : sample1;

				// Apply cosine interpolation instead of linear interpolation (results similar to cubic interp. at very little extra cost compared to linear)
				upsampled[PCMHEADERSIZE + i] = (byte) (sample1 + (sample2 - sample1) * cosineFraction);
			} 
			else if (numBits == 16)  // For 16-bit PCM WAV, each sample takes 2 bytes
			{
				if (originalIndex * 2 + 2 >= inputLength) { break; }

				sample1 = ((input[originalIndex * 2] & 0xFF) | (input[originalIndex * 2 + 1] << 8));
				sample2 = ((originalIndex + 1) * 2 < inputLength ? 
					(input[(originalIndex + 1) * 2] & 0xFF) | (input[(originalIndex + 1) * 2 + 1] << 8) : sample1);

				interpolatedValue = (int) (sample1 + (sample2 - sample1) * cosineFraction);
				upsampled[PCMHEADERSIZE + i * 2] = (byte) (interpolatedValue & 0xFF); // Low byte
				upsampled[PCMHEADERSIZE + i * 2 + 1] = (byte) ((interpolatedValue >> 8) & 0xFF); // High byte
			}
		}

		buildHeader(upsampled, numChannels, newSampleRate, numBits, newLength);
		
		return upsampled;
	}

	public static final int getDefaultAudioSampleRate() 
	{
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		for (Mixer.Info mixerInfo : mixers) 
		{
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] lineInfos = mixer.getSourceLineInfo();
			for (Line.Info lineInfo : lineInfos) 
			{
				if (lineInfo instanceof Line.Info) 
				{
					Line line = null;
					try 
					{
						line = mixer.getLine(lineInfo);
						if (line instanceof SourceDataLine) 
						{
							return (int) ((SourceDataLine) line).getFormat().getSampleRate();
						}
					} catch (Exception e) { e.printStackTrace(); }
				}
			}
		}
		return 32000; // Default to 32KHz
	}
}
