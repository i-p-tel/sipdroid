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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class OneShotLocation extends BroadcastReceiver {

	public static void receive(Context context, Intent intent) {
		Location loc;

		if (!Sipdroid.release) Log.i("SipUA:",intent.getExtras().keySet().toString());
    	if (Receiver.mContext == null) Receiver.mContext = context;
    	loc = (Location)intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
    	if (loc != null) {
    		Receiver.pos(false);
    		Receiver.url("lat="+loc.getLatitude()+"&lon="+loc.getLongitude()+"&rad="+loc.getAccuracy());
    	} else if (intent.hasExtra(Intent.EXTRA_ALARM_COUNT))
    		Receiver.pos(false);		
	}
	@Override
	
	public void onReceive(Context context, Intent intent) {
		receive(context, intent);
    }
}
