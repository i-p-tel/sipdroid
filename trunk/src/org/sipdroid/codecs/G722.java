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
package org.sipdroid.codecs;

import org.sipdroid.sipua.ui.Sipdroid;

class G722 extends CodecBase implements Codec {

	/*
		Acceptable values for bitrate are
		48000, 56000 or 64000
 	 */
	private static final int DEFAULT_BITRATE = 64000;

	G722() {
		CODEC_NAME = "G722 HD Voice";
		CODEC_USER_NAME = "G722";
		CODEC_DESCRIPTION = "64kbit";
		CODEC_NUMBER = 9;
		CODEC_DEFAULT_SETTING = "wlanor3g";
		CODEC_SAMPLE_RATE = 16000;
		CODEC_FRAME_SIZE = 320;		
		super.update();
	}


	void load() {
		try {
			System.loadLibrary("g722_jni");
			super.load();
		} catch (Throwable e) {
			if (!Sipdroid.release) e.printStackTrace();
		}
    
	}  
 
	public native int open(int brate);
	public native int decode(byte encoded[], short lin[], int size);
	public native int encode(short lin[], int offset, byte encoded[], int size);
	public native void close();

	public void init() {
		load();
		if (isLoaded())
			open(DEFAULT_BITRATE);
	}
}
