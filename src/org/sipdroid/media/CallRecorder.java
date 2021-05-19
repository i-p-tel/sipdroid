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

import java.io.File;

import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;

import android.text.format.Time;

// Simple class to support call recording.
public class CallRecorder
{
	// True if we have finished writing the outgoing stream.
	boolean outgoingStopped = false;
	// True if we have finished the incoming one. When both true the output file is closed.
	boolean incomingStopped = false;
	// The output wav file.
	WavWriter callWav = null;	
	
	// Existing files are silently overwritten!
	public CallRecorder(String filename,int sample_rate)
	{
		if (filename == null)
		{
			Time t = new Time();
			t.setToNow();
			filename = t.format2445(); // Create filename from current date.
		}
		
		// If this fails, all of the other calls just silently return immediately.
		if (Receiver.mContext == null)
			return;
		callWav = new WavWriter(Receiver.mContext.getExternalFilesDir(null) + "/" + filename + ".wav",sample_rate);
	}
	
	// Write data received from the internet.
	public void writeIncoming(short[] buffer, int offs, int len)
	{
		if (callWav == null)
			return;
		callWav.writeLeft(buffer, offs, len);
	}
	// Write audio from the mic.
	public void writeOutgoing(short[] buffer, int offs, int len)
	{
		if (callWav == null)
			return;
		callWav.writeRight(buffer, offs, len);
	}
	// We won't write any more incoming data.
	public void stopIncoming()
	{
		incomingStopped = true;
		checkClose();
	}
	// We won't write any more outgoing data.
	public void stopOutgoing()
	{
		outgoingStopped = true;
		checkClose();
	}

	// Check to see if no more data will be written. If so close the wav file.
	private void checkClose()
	{
		if (!outgoingStopped || !incomingStopped)
			return;
		
		if (callWav == null)
			return;
	
		callWav.close();
		callWav = null;
	}
}
