package org.sipdroid.sipua.ui;

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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class SIP extends Activity {

	void callPSTN(String uri) {
		String number;
		
		if (uri.indexOf(":") >= 0) {
			number = uri.substring(uri.indexOf(":")+1);
			if (!number.equals("")) {
		        Intent intent = new Intent(Intent.ACTION_CALL,
		                Uri.fromParts(Uri.decode(number).contains("@")?"sipdroid":"tel", Uri.decode(number)+
		                		(PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.PREF_PREF, Settings.DEFAULT_PREF).equals(Settings.VAL_PREF_PSTN) ? "+" : ""), null));
		        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        Caller.noexclude = SystemClock.elapsedRealtime();
		        startActivity(intent);
			}
		}
	}
	
	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		Intent intent;
		Uri uri;
		Sipdroid.on(this,true);
		if ((intent = getIntent()) != null
			&& (uri = intent.getData()) != null)
				callPSTN(uri.toString());
		finish();
	}
}
