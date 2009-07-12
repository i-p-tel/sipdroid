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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

	public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);
			if (getPreferenceScreen().getSharedPreferences().getString("server","").equals("")) {
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();
				
				edit.putBoolean("wlan", true);
				edit.putString("port", ""+SipStack.default_port);
				edit.putString("server", "pbxes.org");
				edit.putString("pref", "SIP");				
				edit.commit();
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
 	        	if (!key.equals("dns")) {
 	        		Editor edit = sharedPreferences.edit();
 	        		edit.putString("dns", "");
 	        		edit.commit();
 		        	Receiver.engine(this).updateDNS();
 		        	Receiver.engine(this).halt();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
		    		Receiver.engine(this).StartEngine();
		    		updateSummaries();        
	 	        }
        }
        
        public void updateSummaries() {
        	getPreferenceScreen().findPreference("username").setSummary(getPreferenceScreen().getSharedPreferences().getString("username", "")); 
        	getPreferenceScreen().findPreference("server").setSummary(getPreferenceScreen().getSharedPreferences().getString("server", "")); 
        	getPreferenceScreen().findPreference("port").setSummary(getPreferenceScreen().getSharedPreferences().getString("port", ""));
        	getPreferenceScreen().findPreference("prefix").setSummary(getPreferenceScreen().getSharedPreferences().getString("prefix", "")); 
        	getPreferenceScreen().findPreference("minedge").setSummary("Signal >= "+getPreferenceScreen().getSharedPreferences().getString("minedge", "4")); 
        	getPreferenceScreen().findPreference("maxpoll").setSummary("Signal <= "+getPreferenceScreen().getSharedPreferences().getString("maxpoll", "1")); 
        	getPreferenceScreen().findPreference("excludepat").setSummary(getPreferenceScreen().getSharedPreferences().getString("excludepat", "")); 
        	getPreferenceScreen().findPreference("posurl").setSummary(getPreferenceScreen().getSharedPreferences().getString("posurl", "")); 
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
           	if (getPreferenceScreen().getSharedPreferences().getString("posurl", "").length() > 0) {
        		getPreferenceScreen().findPreference("pos").setEnabled(true);
        		getPreferenceScreen().findPreference("callback").setEnabled(true);
           	} else {
        		getPreferenceScreen().findPreference("pos").setEnabled(false);
        		getPreferenceScreen().findPreference("callback").setEnabled(false);
           	}
       }
	}
