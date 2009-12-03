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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class RegisterService extends Service {
	Receiver m_receiver;
	Caller m_caller;
	
    public void onDestroy() {
		super.onDestroy();
		if (m_receiver != null) {
			unregisterReceiver(m_receiver);
			m_receiver = null;
		}
		if (m_caller != null) {
			unregisterReceiver(m_caller);
			m_caller = null;
		}
		Receiver.alarm(0, OneShotAlarm2.class);
	}
    
    @Override
    public void onCreate() {
    	super.onCreate();
        if (m_receiver == null) {
			 IntentFilter intentfilter = new IntentFilter();
			 intentfilter.addAction(Receiver.ACTION_DATA_STATE_CHANGED);
			 intentfilter.addAction(Receiver.ACTION_PHONE_STATE_CHANGED);
			 intentfilter.addAction(Receiver.ACTION_DOCK_EVENT);
	         registerReceiver(m_receiver = new Receiver(), intentfilter);      
        }
        if (m_caller == null) {
        	IntentFilter intentfilter = new IntentFilter();
			 intentfilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
			 intentfilter.setPriority(-1);
	         registerReceiver(m_caller = new Caller(), intentfilter);      
        }
        Receiver.engine(this).isRegistered();
    }
    
    @Override
    public void onStart(Intent intent, int id) {
         super.onStart(intent,id);
         Receiver.alarm(10*60, OneShotAlarm2.class);
    }

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
}
