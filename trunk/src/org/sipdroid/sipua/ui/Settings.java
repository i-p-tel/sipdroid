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

import org.sipdroid.sipua.R;
import org.zoolu.sip.provider.SipStack;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

	public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

		public static int getMinEdge() {
			try {
				return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("minedge", "4"));
			} catch (NumberFormatException i) {
				return 4;
			}			
		}
		
		public static float getEarGain() {
			try {
				return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("eargain", "0.25"));
			} catch (NumberFormatException i) {
				return (float)0.25;
			}			
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);
			if (getPreferenceScreen().getSharedPreferences().getString("server","").equals("")) {
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();
				
				edit.putBoolean("wlan", true);
				edit.putString("port", ""+SipStack.default_port);
				edit.putString("server", "pbxes.org");
				edit.putString("domain", "");
				edit.putString("pref", "SIP");				
				edit.commit();
	        	Receiver.engine(this).updateDNS();
			}
			if (Sipdroid.market) {
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();

				edit.putBoolean("3g", false);
				edit.commit();
				getPreferenceScreen().findPreference("3g").setEnabled(false);
			}
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			updateSummaries();
		}
		
		@Override
		public void onDestroy()
		{
			super.onDestroy();
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}
		
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {	    
	        	if (key.equals("server")) {
	        		Editor edit = sharedPreferences.edit();
 	        		edit.putString("dns", "");
	        		edit.commit();
		        	Receiver.engine(this).updateDNS();
		        	Checkin.checkin(false);
	        		edit.putString("protocol",sharedPreferences.getString("server", "").equals("pbxes.org")?"tcp":"udp");
	        		edit.commit();
	        	} else
        		if (sharedPreferences.getBoolean("callback",false) && sharedPreferences.getBoolean("callthru",false)) {
 	        		CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference(
 	        				key.equals("callback")?"callthru":"callback");
	        		cb.setChecked(false);
	        	} else
 	        	if (key.equals("wlan") ||
 	        			key.equals("3g") ||
 	        			key.equals("username") ||
 	        			key.equals("password") ||
 	        			key.equals("domain") ||
 	        			key.equals("server") ||
 	        			key.equals("port") ||
 	        			key.equals("protocol") ||
 	        			key.equals("minedge") ||
 	        			key.equals("pos") ||
 	        			key.equals("posurl") ||
 	        			key.equals("callerid")) {
 	        		if (key.equals("wlan") || key.equals("3g"))
 	        			updateSleep();
 		        	Receiver.engine(this).halt();
		    		Receiver.engine(this).StartEngine();
		    		updateSummaries();        
	 	        }
        }

		void updateSleep() {
	        ContentResolver cr = getContentResolver();
			int get = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, -1);
			int set = get;
			boolean wlan = getPreferenceScreen().getSharedPreferences().getBoolean("wlan", false);
			boolean g3 = getPreferenceScreen().getSharedPreferences().getBoolean("3g", false);
			
			if (g3) {
				set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
				if (set != get)
					Toast.makeText(this, R.string.settings_policy_default, Toast.LENGTH_LONG).show();
			} else if (wlan) {
				set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
				if (set != get)
					Toast.makeText(this, R.string.settings_policy_never, Toast.LENGTH_LONG).show();
			}
			if (set != get)
				android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
		}
	
        public void updateSummaries() {
        	getPreferenceScreen().findPreference("callerid").setSummary(getPreferenceScreen().getSharedPreferences().getString("callerid",""));
        	getPreferenceScreen().findPreference("username").setSummary(getPreferenceScreen().getSharedPreferences().getString("username", "")); 
        	getPreferenceScreen().findPreference("server").setSummary(getPreferenceScreen().getSharedPreferences().getString("server", "")); 
        	if (getPreferenceScreen().getSharedPreferences().getString("domain","").length() == 0) {
        		getPreferenceScreen().findPreference("domain").setSummary(getString(R.string.settings_domain2));
        	} else {
        		getPreferenceScreen().findPreference("domain").setSummary(getPreferenceScreen().getSharedPreferences().getString("domain", ""));
        	}
        	getPreferenceScreen().findPreference("port").setSummary(getPreferenceScreen().getSharedPreferences().getString("port", ""));
        	getPreferenceScreen().findPreference("protocol").setSummary(getPreferenceScreen().getSharedPreferences().getString("protocol",
        		getPreferenceScreen().getSharedPreferences().getString("server", "").equals("pbxes.org")?"tcp":"udp").toUpperCase());
        	getPreferenceScreen().findPreference("search").setSummary(getPreferenceScreen().getSharedPreferences().getString("search", "")); 
        	getPreferenceScreen().findPreference("minedge").setSummary("Signal >= "+getPreferenceScreen().getSharedPreferences().getString("minedge", "4")); 
        	getPreferenceScreen().findPreference("excludepat").setSummary(getPreferenceScreen().getSharedPreferences().getString("excludepat", "")); 
        	getPreferenceScreen().findPreference("posurl").setSummary(getPreferenceScreen().getSharedPreferences().getString("posurl", "")); 
        	getPreferenceScreen().findPreference("callthru2").setSummary(getPreferenceScreen().getSharedPreferences().getString("callthru2", "")); 
        	if (getPreferenceScreen().getSharedPreferences().getString("pref", "").equals("SIP")) {
        		getPreferenceScreen().findPreference("pref").setSummary(getResources().getStringArray(R.array.pref_display_values)[0]);
        		getPreferenceScreen().findPreference("par").setEnabled(true);
        	} else {
          		getPreferenceScreen().findPreference("pref").setSummary(getResources().getStringArray(R.array.pref_display_values)[1]);
        		getPreferenceScreen().findPreference("par").setEnabled(false);
          	}
        	if (getPreferenceScreen().getSharedPreferences().getBoolean("3g", false))
        		getPreferenceScreen().findPreference("minedge").setEnabled(true);
        	else
        		getPreferenceScreen().findPreference("minedge").setEnabled(false);
        	if (getPreferenceScreen().getSharedPreferences().getBoolean("callthru", false))
        		getPreferenceScreen().findPreference("callthru2").setEnabled(true);
        	else
        		getPreferenceScreen().findPreference("callthru2").setEnabled(false);
           	if (getPreferenceScreen().getSharedPreferences().getString("posurl", "").length() > 0) {
        		getPreferenceScreen().findPreference("pos").setEnabled(true);
        		getPreferenceScreen().findPreference("callback").setEnabled(true);
           	} else {
        		getPreferenceScreen().findPreference("pos").setEnabled(false);
        		getPreferenceScreen().findPreference("callback").setEnabled(false);
           	}
       }
	}
