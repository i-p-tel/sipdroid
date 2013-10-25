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

package org.sipdroid.sipua.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class ReRegisterAlarm extends BroadcastReceiver {

    @Override
	public void onReceive(Context context, Intent intent) {
    	Receiver.engine(context).expire();
    }
    
	public static long expire_time;
	
	public static synchronized void reRegister(int renew_time) {
		if (renew_time == 0)
			expire_time = 0;
		else {
			if (expire_time != 0 && renew_time*1000 + SystemClock.elapsedRealtime() > expire_time) return;
			expire_time = renew_time*1000 + SystemClock.elapsedRealtime();
		}
       	Receiver.alarm(renew_time - 60, ReRegisterAlarm.class);
	}
	
}
