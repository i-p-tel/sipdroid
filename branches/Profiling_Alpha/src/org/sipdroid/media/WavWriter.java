/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.media;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

// Simple class allowing one to write to a wav file and send the
// left and right channel data in different function calls.

public class WavWriter
{
	// The file we write the wav to.
	RandomAccessFile raf = null;
	// The number of left samples we have written. 
	int leftSamplesWritten = 0;
	// The number of left samples we have written. 
	int rightSamplesWritten = 0;
	// The positions of the first sample data byte.
	long sampleDataOffset;
	byte[] buf = new byte[10000];
	
	public WavWriter(String filename,int sample_rate)
	{
		try
		{
			raf = new RandomAccessFile(filename, "rw");
			raf.setLength(0); // Truncate the file if it already exists.

			// Write wav header.
			
			// Chunk metadata.
			raf.writeBytes("RIFF");
			// Size of chunk (fill in later). This is the size of entire file minus 8 bytes
			// (i.e. the size of everything following this int).
			raf.writeInt(0);
			
			// Chunk header.
			raf.writeBytes("WAVE");
			raf.writeBytes("fmt ");
			raf.writeInt(B2L(16)); // Size of the rest of this header. B2L is Big to Little endian.
			raf.writeShort(B2L_s(1)); // 1 = PCM.
			raf.writeShort(B2L_s(2)); // 1 = mono, 2 = stereo.
			raf.writeInt(B2L(sample_rate)); // Sample rate, 8 kHz
			raf.writeInt(B2L(sample_rate*2*2)); // Byte rate. Pretty redundant.
			raf.writeShort(B2L_s(4)); // Bytes per frame.
			raf.writeShort(B2L_s(16)); // Bits per sample.
			
			raf.writeBytes("data");
			raf.writeInt(0); // Fill in later, number of bytes of data.
			sampleDataOffset = raf.getFilePointer();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error creating output file.");
			raf = null;
		}
	}
	
	int i;

	synchronized void writeLeft(short[] buffer, int offs, int len)
	{
		if (leftSamplesWritten > 500*1024*1024)
		{
			// File too big. This is an 18 hour phone call!
			return;
		}
		
		if (raf == null)
			return;
		
		try
		{
			raf.seek(sampleDataOffset + 4 * leftSamplesWritten);
			for (i = 0; i < len; ++i)
			{
				buf[i*4+2] = 
				buf[i*4+3] = 0;			
			}
			raf.read(buf,0,len*4);
			raf.seek(sampleDataOffset + 4 * leftSamplesWritten);
			for (i = 0; i < len; ++i)
			{
				buf[i*4+1] = (byte)(buffer[offs+i]>>8);
				buf[i*4] = (byte)buffer[offs+i];				
			}
			leftSamplesWritten += len;
			raf.write(buf,0,len*4);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error writing to output file.");
		}
	}
	
	synchronized void writeRight(short[] buffer, int offs, int len)
	{
		if (rightSamplesWritten > 500*1024*1024)
		{
			// File too big. This is an 18 hour phone call!
			return;
		}
		
		if (raf == null)
			return;
		
		try
		{
			raf.seek(sampleDataOffset + 4 * rightSamplesWritten);
			for (i = 0; i < len; ++i)
			{
				buf[i*4] = 
				buf[i*4+1] = 0;			
			}
			raf.read(buf,0,len*4);
			raf.seek(sampleDataOffset + 4 * rightSamplesWritten);
			for (i = 0; i < len; ++i)
			{
				buf[i*4+3] = (byte)(buffer[offs+i]>>8);
				buf[i*4+2] = (byte)buffer[offs+i];				
			}
			rightSamplesWritten += len;
			raf.write(buf,0,len*4);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error writing to output file.");
		}
	}
	
	synchronized void close()
	{
		if (raf == null)
			return;
		try
		{
			int samplesWritten = leftSamplesWritten > rightSamplesWritten ? leftSamplesWritten : rightSamplesWritten;
			// Seek back.
			raf.seek(4);
			raf.writeInt(B2L(36 + samplesWritten * 4));
			raf.seek(40);
			raf.writeInt(B2L(samplesWritten * 4));
			raf.close();
			raf = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e("CallRecorder", "Error writing final data to output file.");
		}
	}
	
	// Convert big endian short to little endian.
	int B2L_s(int i)
	{
		return (((i >> 8) & 0x00ff) + ((i << 8) & 0xff00));
	}

	// Convert big endian int ot little endian
	int B2L(int i)
	{
		return ((i & 0xff) << 24) + ((i & 0xff00) << 8) + ((i & 0xff0000) >> 8)
				+ ((i >> 24) & 0xff);
	}
}
