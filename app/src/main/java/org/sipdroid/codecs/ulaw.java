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


class ulaw extends CodecBase implements Codec {
	ulaw() {
		CODEC_NAME = "PCMU";
		CODEC_USER_NAME = "PCMU";
		CODEC_DESCRIPTION = "64kbit";
		CODEC_NUMBER = 0;
		CODEC_DEFAULT_SETTING = "wlanor3g";

		load();
	}

	public void init() {
		G711.init();
	}
    
	public int decode(byte enc[], short lin[], int frames) {
		G711.ulaw2linear(enc, lin, frames);

		return frames;
	}

	public int encode(short lin[], int offset, byte enc[], int frames) {
		G711.linear2ulaw(lin, offset, enc, frames);

		return frames;
	}

	public void close() {
	}
}
