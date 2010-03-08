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

import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class GSM extends CodecBase implements Codec {
	GSM() {
		CODEC_NAME = "GSM";
		CODEC_USER_NAME = "GSM";
		CODEC_DESCRIPTION = "13kbit";
		CODEC_NUMBER = 3;
		CODEC_DEFAULT_SETTING = "edge";
		/* up convert original compression parameter for this codec */
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
		String pref = sp.getString(Settings.PREF_COMPRESSION, Settings.DEFAULT_COMPRESSION);
		if (pref != null) {
			SharedPreferences.Editor e = sp.edit();
			e.remove("compression");
			e.putString(CODEC_NAME, pref);
			e.commit();
		}
		super.update();
	}

	public void init() {
		load();
		if (isLoaded())
			org.sipdroid.pjlib.Codec.init();
	}

	void load() {
		if(org.sipdroid.pjlib.Codec.loaded)
			super.load();
	}
    
	public int decode(byte encoded[], short lin[], int frames) {
		byte[] buf = new byte[33+12];
		int i;

		for (i = 12; i < 45; i++)
			buf[i] = encoded[i];
		return org.sipdroid.pjlib.Codec.decode(buf, lin, 0);
	}

	public int encode(short lin[], int offset, byte encoded[], int frames) {
		return org.sipdroid.pjlib.Codec.encode(lin, offset, encoded, frames);
	}

	public void close() {
//		org.sipdroid.pjlib.Codec.close();
	}

}
