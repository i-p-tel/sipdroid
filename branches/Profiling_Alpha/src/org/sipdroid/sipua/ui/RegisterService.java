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

import org.sipdroid.media.RtpStreamReceiver;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class RegisterService extends Service {
	Receiver m_receiver;
	Caller m_caller;
	boolean m_bOldVPN = false;
	boolean m_bOldOwnWifi = false;
	boolean m_bOldSelectWifi = false;
	
    public void onDestroy() {
		super.onDestroy();
		if (m_receiver != null) {
			unregisterReceiver(m_receiver);
			m_receiver = null;
		}
		Receiver.alarm(0, RegisterServiceAlarm.class);
	}
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	if (Receiver.mContext == null) Receiver.mContext = this;
        if (m_receiver == null) {
        	registerReceiver();     
        }
        Receiver.engine(this).isRegistered();
        
        RtpStreamReceiver.restoreSettings();
    }
    
	private void registerReceiver() {
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentfilter.addAction(Receiver.ACTION_DATA_STATE_CHANGED);
		intentfilter.addAction(Receiver.ACTION_PHONE_STATE_CHANGED);
		intentfilter.addAction(Receiver.ACTION_DOCK_EVENT);
		intentfilter.addAction(Intent.ACTION_HEADSET_PLUG);
		intentfilter.addAction(Receiver.ACTION_SCO_AUDIO_STATE_CHANGED);
		m_bOldOwnWifi = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_OWNWIFI, org.sipdroid.sipua.ui.Settings.DEFAULT_OWNWIFI);
		if (m_bOldOwnWifi) {
			intentfilter.addAction(Intent.ACTION_USER_PRESENT);
			intentfilter.addAction(Intent.ACTION_SCREEN_OFF);
			intentfilter.addAction(Intent.ACTION_SCREEN_ON);
		}
		m_bOldVPN = Receiver.on_vpn();
		if (m_bOldVPN)
			intentfilter.addAction(Receiver.ACTION_VPN_CONNECTIVITY);
		
		m_bOldSelectWifi = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_SELECTWIFI, org.sipdroid.sipua.ui.Settings.DEFAULT_SELECTWIFI);
		if (m_bOldSelectWifi) {
			intentfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
			intentfilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		}
		registerReceiver(m_receiver = new Receiver(), intentfilter);
	}
    
    
    @Override
    public void onStart(Intent intent, int id) {
         super.onStart(intent,id);
         
         if (m_bOldOwnWifi != PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_OWNWIFI, org.sipdroid.sipua.ui.Settings.DEFAULT_OWNWIFI)
        || 	 m_bOldVPN != Receiver.on_vpn()
        ||   m_bOldSelectWifi != PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_SELECTWIFI, org.sipdroid.sipua.ui.Settings.DEFAULT_SELECTWIFI)) {
	         if (m_receiver != null)
	 			unregisterReceiver(m_receiver);
	         registerReceiver();
         }
         
         Receiver.alarm(10*60, RegisterServiceAlarm.class);
    }

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
}
