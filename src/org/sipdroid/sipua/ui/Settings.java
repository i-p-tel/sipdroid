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

import org.sipdroid.pjlib.Codec;
import org.sipdroid.sipua.R;
import org.zoolu.sip.provider.SipStack;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

	public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnClickListener {

		public static float getEarGain() {
			try {
				return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Receiver.headset > 0?"heargain":"eargain", "0.25"));
			} catch (NumberFormatException i) {
				return (float)0.25;
			}			
		}

		public static float getMicGain() {
			if (Receiver.headset > 0)
				return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("hmicgain", "1.0"));
			return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("micgain", "0.25"));
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);
			if (getPreferenceScreen().getSharedPreferences().getString("server","").equals("")) {
				CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference("wlan");
				cb.setChecked(true);
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();
				
				edit.putString("port", ""+SipStack.default_port);
				edit.putString("server", "pbxes.org");
				edit.putString("pref", "SIP");				
				edit.commit();
	        	Receiver.engine(this).updateDNS();
			}
			if (!getPreferenceScreen().getSharedPreferences().contains("stun")) {
				CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference("stun");
				cb.setChecked(false);
			}			
			if (getPreferenceScreen().getSharedPreferences().getString("stun_server","").equals("")) {
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();
				
				edit.putString("stun_server", "stun.ekiga.net");
				edit.putString("stun_server_port", "3478");				
				edit.commit();
				Receiver.engine(this).updateDNS();
			}			

			if (!getPreferenceScreen().getSharedPreferences().contains("MWI_enabled")) {
				CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference("MWI_enabled");
				cb.setChecked(true);
			}
			if (Sipdroid.market) {
				CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference("3g");
				cb.setChecked(false);
				CheckBoxPreference cb2 = (CheckBoxPreference) getPreferenceScreen().findPreference("edge");
				cb2.setChecked(false);
				getPreferenceScreen().findPreference("3g").setEnabled(false);
				getPreferenceScreen().findPreference("edge").setEnabled(false);
			}
			Codec.init();
			if (!Codec.loaded) {
				Editor edit = getPreferenceScreen().getSharedPreferences().edit();

				edit.putString("compression", "never");
				edit.commit();
				getPreferenceScreen().findPreference("compression").setEnabled(false);
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
		
		EditText transferText;
		
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {	    
        		if (sharedPreferences.getString("port","").equals("0")) {
        			transferText = new InstantAutoCompleteTextView(this,null);
        			transferText.setInputType(InputType.TYPE_CLASS_NUMBER);

        			new AlertDialog.Builder(this)
        			.setTitle(Receiver.mContext.getString(R.string.settings_port))
        			.setView(transferText)
        			.setPositiveButton(android.R.string.ok, this)
        			.show();
        			return;
        		} else
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
 	        			key.equals("edge") ||
 	        			key.equals("username") ||
 	        			key.equals("password") ||
 	        			key.equals("domain") ||
 	        			key.equals("server") ||
 	        			key.equals("port") ||
 	        			key.equals("stun") ||
 	        			key.equals("stun_server") ||
 	        			key.equals("stun_server_port") ||
 	        			key.equals("protocol") ||
 	        			key.equals("edge") ||
 	        			key.equals("pos") ||
 	        			key.equals("posurl") ||
 	        			key.equals("fromuser") ||
 	        			key.equals("auto_ondemand") ||
 	        			key.equals("MWI_enabled")) {
 		        	Receiver.engine(this).halt();
		    		Receiver.engine(this).StartEngine();
	 	        }
        		if (key.equals("wlan") || key.equals("3g") || key.equals("edge") || key.equals("ownwifi"))
        			updateSleep();
	    		updateSummaries();        
        }

		void updateSleep() {
	        ContentResolver cr = getContentResolver();
			int get = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, -1);
			int set = get;
			boolean wlan = getPreferenceScreen().getSharedPreferences().getBoolean("wlan", false);
			boolean g3 = getPreferenceScreen().getSharedPreferences().getBoolean("3g", false);
			boolean ownwifi = getPreferenceScreen().getSharedPreferences().getBoolean("ownwifi", false);
			
			if (g3 && !ownwifi) {
				set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
				if (set != get)
					Toast.makeText(this, R.string.settings_policy_default, Toast.LENGTH_LONG).show();
			} else if (wlan || ownwifi) {
				set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
				if (set != get)
					Toast.makeText(this, R.string.settings_policy_never, Toast.LENGTH_LONG).show();
			}
			if (set != get)
				android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
		}
	
		void fill(String pref,String def,int val,int disp) {
			int i;
			
        	for (i = 0; i < getResources().getStringArray(val).length; i++)
            	if (getPreferenceScreen().getSharedPreferences().getString(pref, def).equals(getResources().getStringArray(val)[i]))
            		getPreferenceScreen().findPreference(pref).setSummary(getResources().getStringArray(disp)[i]);
        }

		public void updateSummaries() {
        	getPreferenceScreen().findPreference("username").setSummary(getPreferenceScreen().getSharedPreferences().getString("username", "")); 
        	getPreferenceScreen().findPreference("server").setSummary(getPreferenceScreen().getSharedPreferences().getString("server", ""));

        	getPreferenceScreen().findPreference("stun_server").setSummary(getPreferenceScreen().getSharedPreferences().getString("stun_server", ""));
        	getPreferenceScreen().findPreference("stun_server_port").setSummary(getPreferenceScreen().getSharedPreferences().getString("stun_server_port", ""));
        	
        	if (getPreferenceScreen().getSharedPreferences().getString("domain","").length() == 0) {
        		getPreferenceScreen().findPreference("domain").setSummary(getString(R.string.settings_domain2));
        	} else {
        		getPreferenceScreen().findPreference("domain").setSummary(getPreferenceScreen().getSharedPreferences().getString("domain", ""));
        	}
        	if (getPreferenceScreen().getSharedPreferences().getString("fromuser","").length() == 0) {
        		getPreferenceScreen().findPreference("fromuser").setSummary(getString(R.string.settings_callerid2));
        	} else {
        		getPreferenceScreen().findPreference("fromuser").setSummary(getPreferenceScreen().getSharedPreferences().getString("fromuser", ""));
        	}
        	getPreferenceScreen().findPreference("port").setSummary(getPreferenceScreen().getSharedPreferences().getString("port", ""));
        	getPreferenceScreen().findPreference("protocol").setSummary(getPreferenceScreen().getSharedPreferences().getString("protocol",
        		getPreferenceScreen().getSharedPreferences().getString("server", "").equals("pbxes.org")?"tcp":"udp").toUpperCase());
        	getPreferenceScreen().findPreference("search").setSummary(getPreferenceScreen().getSharedPreferences().getString("search", "")); 
        	getPreferenceScreen().findPreference("excludepat").setSummary(getPreferenceScreen().getSharedPreferences().getString("excludepat", "")); 
        	getPreferenceScreen().findPreference("posurl").setSummary(getPreferenceScreen().getSharedPreferences().getString("posurl", "")); 
        	getPreferenceScreen().findPreference("callthru2").setSummary(getPreferenceScreen().getSharedPreferences().getString("callthru2", "")); 
        	if (!getPreferenceScreen().getSharedPreferences().getString("pref", "").equals("PSTN")) {
        		getPreferenceScreen().findPreference("par").setEnabled(true);
        	} else {
        		getPreferenceScreen().findPreference("par").setEnabled(false);
          	}
        	fill("compression","edge",R.array.compression_values,R.array.compression_display_values);
        	fill("eargain","0.25",R.array.eargain_values,R.array.eargain_display_values);
        	fill("micgain","0.25",R.array.eargain_values,R.array.eargain_display_values);
        	fill("heargain","0.25",R.array.eargain_values,R.array.eargain_display_values);
        	fill("hmicgain","1.0",R.array.eargain_values,R.array.eargain_display_values);
        	if (getPreferenceScreen().getSharedPreferences().getBoolean("stun", false)) {
        		getPreferenceScreen().findPreference("stun_server").setEnabled(true);
        		getPreferenceScreen().findPreference("stun_server_port").setEnabled(true);
        	} else {
        		getPreferenceScreen().findPreference("stun_server").setEnabled(false);
        		getPreferenceScreen().findPreference("stun_server_port").setEnabled(false);       	
        	}
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

        @Override
		public void onClick(DialogInterface arg0, int arg1) {
    		Editor edit = getPreferenceScreen().getSharedPreferences().edit();
     		edit.putString("port", transferText.getText().toString());
    		edit.commit();
		}
	}
