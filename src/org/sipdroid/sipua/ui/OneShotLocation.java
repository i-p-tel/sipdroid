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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

public class OneShotLocation extends BroadcastReceiver {

	Location loc;
	Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {

    	loc = (Location)intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
    	mContext = context;
    	if (loc != null) {
    		Receiver.pos(false);
	        (new Thread() {
				public void run() {
					try {
				        URL url = new URL(PreferenceManager.getDefaultSharedPreferences(mContext).getString("posurl","")+
				        		"?lat="+loc.getLatitude()+"&lon="+loc.getLongitude()+"&rad="+loc.getAccuracy());
				        BufferedReader in;
							in = new BufferedReader(new InputStreamReader(url.openStream()));
				        in.close();
					} catch (IOException e) {
						if (!Sipdroid.release) e.printStackTrace();
					}

				}
			}).start();   
    	}
    }
}
