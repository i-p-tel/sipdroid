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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.sipdroid.codecs.Codecs;
import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.sipua.R;
import org.zoolu.sip.provider.SipStack;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnClickListener {
	// Current settings handler
	private static SharedPreferences settings;
	// Context definition
	private Settings context = null;

	// Path where to store all profiles - !!!should be replaced by some system variable!!!
	private final static String profilePath = "/sdcard/Sipdroid/";
	// Path where is stored the shared preference file - !!!should be replaced by some system variable!!!
	private final String sharedPrefsPath = "/data/data/org.sipdroid.sipua/shared_prefs/";
	// Shared preference file name - !!!should be replaced by some system variable!!!
	private final String sharedPrefsFile = "org.sipdroid.sipua_preferences";
	// List of profile files available on the SD card
	private String[] profileFiles = null;
	// Which profile file to delete
	private int profileToDelete;

	// IDs of the menu items
	private static final int MENU_IMPORT = 0;
	private static final int MENU_DELETE = 1;

	// All possible values of the PREF_PREF preference (see bellow) 
	public static final String VAL_PREF_PSTN = "PSTN";
	public static final String VAL_PREF_SIP = "SIP";
	public static final String VAL_PREF_SIPONLY = "SIPONLY";
	public static final String VAL_PREF_ASK = "ASK";

	/*-
	 * ****************************************
	 * **** HOW TO USE SHARED PREFERENCES *****
	 * ****************************************
	 * 
	 * If you need to check the existence of the preference key
	 *   in this class:		contains(PREF_USERNAME)
	 *   in other classes:	PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).contains(Settings.PREF_USERNAME) 
	 * If you need to check the existence of the key or check the value of the preference
	 *   in this class:		getString(PREF_USERNAME, "").equals("")
	 *   in other classes:	PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Settings.PREF_USERNAME, "").equals("")
	 * If you need to get the value of the preference
	 *   in this class:		getString(PREF_USERNAME, DEFAULT_USERNAME)
	 *   in other classes:	PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Settings.PREF_USERNAME, Settings.DEFAULT_USERNAME)
	 */

	// Name of the keys in the Preferences XML file
	public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWORD = "password";
	public static final String PREF_SERVER = "server";
	public static final String PREF_DOMAIN = "domain";
	public static final String PREF_FROMUSER = "fromuser";
	public static final String PREF_PORT = "port";
	public static final String PREF_PROTOCOL = "protocol";
	public static final String PREF_WLAN = "wlan";
	public static final String PREF_3G = "3g";
	public static final String PREF_EDGE = "edge";
	public static final String PREF_VPN = "vpn";
	public static final String PREF_PREF = "pref";
	public static final String PREF_AUTO_ON = "auto_on";
	public static final String PREF_AUTO_ONDEMAND = "auto_on_demand";
	public static final String PREF_AUTO_HEADSET = "auto_headset";
	public static final String PREF_MWI_ENABLED = "MWI_enabled";
	public static final String PREF_NOTIFY = "notify";
	public static final String PREF_NODATA = "nodata";
	public static final String PREF_SIPRINGTONE = "sipringtone";
	public static final String PREF_SEARCH = "search";
	public static final String PREF_EXCLUDEPAT = "excludepat";
	public static final String PREF_EARGAIN = "eargain";
	public static final String PREF_MICGAIN = "micgain";
	public static final String PREF_HEARGAIN = "heargain";
	public static final String PREF_HMICGAIN = "hmicgain";
	public static final String PREF_OWNWIFI = "ownwifi";
	public static final String PREF_STUN = "stun";
	public static final String PREF_STUN_SERVER = "stun_server";
	public static final String PREF_STUN_SERVER_PORT = "stun_server_port";
	
	// MMTel configurations (added by mandrajg)
	public static final String PREF_MMTEL = "mmtel";
	public static final String PREF_MMTEL_QVALUE = "mmtel_qvalue";
	
	public static final String PREF_PAR = "par";
	public static final String PREF_IMPROVE = "improve";
	public static final String PREF_POSURL = "posurl";
	public static final String PREF_POS = "pos";
	public static final String PREF_CALLBACK = "callback";
	public static final String PREF_CALLTHRU = "callthru";
	public static final String PREF_CALLTHRU2 = "callthru2";
	public static final String PREF_CODECS = "codecs_new";
	public static final String PREF_DNS = "dns";
	public static final String PREF_VQUALITY = "vquality";
	public static final String PREF_MESSAGE = "vmessage";
	public static final String PREF_BLUETOOTH = "bluetooth";
	public static final String PREF_KEEPON = "keepon";
	public static final String PREF_SELECTWIFI = "selectwifi";
	
	// Default values of the preferences
	public static final String	DEFAULT_USERNAME = "";
	public static final String	DEFAULT_PASSWORD = "";
	public static final String	DEFAULT_SERVER = "pbxes.org";
	public static final String	DEFAULT_DOMAIN = "";
	public static final String	DEFAULT_FROMUSER = "";
	public static final String	DEFAULT_PORT = "" + SipStack.default_port;
	public static final String	DEFAULT_PROTOCOL = "tcp";
	public static final boolean	DEFAULT_WLAN = true;
	public static final boolean	DEFAULT_3G = false;
	public static final boolean	DEFAULT_EDGE = false;
	public static final boolean	DEFAULT_VPN = false;
	public static final String	DEFAULT_PREF = VAL_PREF_SIP;
	public static final boolean	DEFAULT_AUTO_ON = false;
	public static final boolean	DEFAULT_AUTO_ONDEMAND = false;
	public static final boolean	DEFAULT_AUTO_HEADSET = false;
	public static final boolean	DEFAULT_MWI_ENABLED = true;
	public static final boolean	DEFAULT_NOTIFY = false;
	public static final boolean	DEFAULT_NODATA = false;
	public static final String	DEFAULT_SIPRINGTONE = "";
	public static final String	DEFAULT_SEARCH = "";
	public static final String	DEFAULT_EXCLUDEPAT = "";
	public static final float	DEFAULT_EARGAIN = (float) 0.25;
	public static final float	DEFAULT_MICGAIN = (float) 0.25;
	public static final float	DEFAULT_HEARGAIN = (float) 0.25;
	public static final float	DEFAULT_HMICGAIN = (float) 1.0;
	public static final boolean	DEFAULT_OWNWIFI = false;
	public static final boolean	DEFAULT_STUN = false;
	public static final String	DEFAULT_STUN_SERVER = "stun.ekiga.net";
	public static final String	DEFAULT_STUN_SERVER_PORT = "3478";
	
	// MMTel configuration (added by mandrajg)
	public static final boolean	DEFAULT_MMTEL = false;
	public static final String	DEFAULT_MMTEL_QVALUE = "1.00";	

	public static final boolean	DEFAULT_PAR = false;
	public static final boolean	DEFAULT_IMPROVE = false;
	public static final String	DEFAULT_POSURL = "";
	public static final boolean	DEFAULT_POS = false;
	public static final boolean	DEFAULT_CALLBACK = false;
	public static final boolean	DEFAULT_CALLTHRU = false;
	public static final String	DEFAULT_CALLTHRU2 = "";
	public static final String	DEFAULT_CODECS = null;
	public static final String	DEFAULT_DNS = "";
	public static final String  DEFAULT_VQUALITY = "low";
	public static final boolean DEFAULT_MESSAGE = false;
	public static final boolean DEFAULT_BLUETOOTH = false;
	public static final boolean DEFAULT_KEEPON = false;
	public static final boolean DEFAULT_SELECTWIFI = false;

	// An other preference keys (not in the Preferences XML file)
	public static final String PREF_OLDVALID = "oldvalid";
	public static final String PREF_SETMODE = "setmode";
	public static final String PREF_OLDVIBRATE = "oldvibrate";
	public static final String PREF_OLDVIBRATE2 = "oldvibrate2";
	public static final String PREF_OLDPOLICY = "oldpolicy";
	public static final String PREF_OLDRING = "oldring";
	public static final String PREF_AUTO_DEMAND = "auto_demand";
	public static final String PREF_WIFI_DISABLED = "wifi_disabled";
	public static final String PREF_ON_VPN = "on_vpn";
	public static final String PREF_NODEFAULT = "nodefault";
	public static final String PREF_ON = "on";
	public static final String PREF_PREFIX = "prefix";
	public static final String PREF_COMPRESSION = "compression";
	//public static final String PREF_RINGMODEx = "ringmodeX";
	//public static final String PREF_VOLUMEx = "volumeX";

	// Default values of the other preferences
	public static final boolean	DEFAULT_OLDVALID = false;
	public static final boolean	DEFAULT_SETMODE = false;
	public static final int		DEFAULT_OLDVIBRATE = 0;
	public static final int		DEFAULT_OLDVIBRATE2 = 0;
	public static final int		DEFAULT_OLDPOLICY = 0;
	public static final int		DEFAULT_OLDRING = 0;
	public static final boolean	DEFAULT_AUTO_DEMAND = false;
	public static final boolean	DEFAULT_WIFI_DISABLED = false;
	public static final boolean DEFAULT_ON_VPN = false;
	public static final boolean	DEFAULT_NODEFAULT = false;
	public static final boolean	DEFAULT_ON = false;
	public static final String	DEFAULT_PREFIX = "";
	public static final String	DEFAULT_COMPRESSION = null;
	//public static final String	DEFAULT_RINGTONEx = "";
	//public static final String	DEFAULT_VOLUMEx = "";

	public static float getEarGain() {
		try {
			return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Receiver.headset > 0 ? PREF_HEARGAIN : PREF_EARGAIN, "" + DEFAULT_EARGAIN));
		} catch (NumberFormatException i) {
			return DEFAULT_EARGAIN;
		}			
	}

	public static float getMicGain() {
		if (Receiver.headset > 0 || Receiver.bluetooth > 0) {
			return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(PREF_HMICGAIN, "" + DEFAULT_HMICGAIN));
		}

		return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(PREF_MICGAIN, "" + DEFAULT_MICGAIN));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    	if (Receiver.mContext == null) Receiver.mContext = this;
		addPreferencesFromResource(R.xml.preferences);
		setDefaultValues();
		setSettingsTitle();
		Codecs.check();
	}
	
	void reload() {
		setPreferenceScreen(null);
		addPreferencesFromResource(R.xml.preferences);		
	}

	private void setDefaultValues() {
		settings = getSharedPreferences(sharedPrefsFile, MODE_PRIVATE);

		if (settings.getString(PREF_SERVER, "").equals("")) {
			CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference(PREF_WLAN);
			cb.setChecked(true);
			Editor edit = settings.edit();

			edit.putString(PREF_PORT, DEFAULT_PORT);
			edit.putString(PREF_SERVER, DEFAULT_SERVER);
			edit.putString(PREF_PREF, DEFAULT_PREF);				
			edit.putString(PREF_PROTOCOL, DEFAULT_PROTOCOL);
			edit.commit();
        	Receiver.engine(this).updateDNS();
        	reload();
		}
		if (settings.getString(PREF_STUN_SERVER, "").equals("")) {
			Editor edit = settings.edit();

			edit.putString(PREF_STUN_SERVER, DEFAULT_STUN_SERVER);
			edit.putString(PREF_STUN_SERVER_PORT, DEFAULT_STUN_SERVER_PORT);				
			edit.commit();
			reload();
		}

		if (! settings.contains(PREF_MWI_ENABLED)) {
			CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference(PREF_MWI_ENABLED);
			cb.setChecked(true);
		}
		if (Sipdroid.market) {
			CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference(PREF_3G);
			cb.setChecked(false);
			CheckBoxPreference cb2 = (CheckBoxPreference) getPreferenceScreen().findPreference(PREF_EDGE);
			cb2.setChecked(false);
			getPreferenceScreen().findPreference(PREF_3G).setEnabled(false);
			getPreferenceScreen().findPreference(PREF_EDGE).setEnabled(false);
		}

		settings.registerOnSharedPreferenceChangeListener(this);

		updateSummaries();		
	}

	private void setSettingsTitle() {
		if (! settings.getString(PREF_USERNAME, "").equals("") && ! settings.getString(PREF_SERVER, "").equals("")) {
			setTitle(getString(R.string.settings_profile_title, getProfileNameString()));		
		} else {
			setTitle(getString(R.string.settings_title));		
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Get the content of the directory
    	profileFiles = getProfileList();

    	// Create menu items - show only if there are already some profiles
        if (profileFiles != null && profileFiles.length > 0) {
	        menu.add(0, MENU_IMPORT, 0, getString(R.string.settings_profile_menu_import)).setIcon(android.R.drawable.ic_menu_upload);
        	menu.add(0, MENU_DELETE, 0, getString(R.string.settings_profile_menu_delete)).setIcon(android.R.drawable.ic_menu_delete);
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
    	context = this;

    	switch (item.getItemId()) {
            case MENU_IMPORT:
            	// Get the content of the directory
            	profileFiles = getProfileList();
            	if (profileFiles != null && profileFiles.length > 0) {
	            	// Show dialog with the files
	    			new AlertDialog.Builder(this)
	    			.setTitle(getString(R.string.settings_profile_dialog_profiles_title))
	    			.setIcon(android.R.drawable.ic_menu_upload)
	    			.setItems(profileFiles, profileOnClick)
	    			.show();
            	} else {
	                Toast.makeText(this, "No profile found.", Toast.LENGTH_SHORT).show();
            	}
                return true;

            case MENU_DELETE:
            	// Get the content of the directory
            	profileFiles = getProfileList();
            	new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_profile_dialog_delete_title))
                .setIcon(android.R.drawable.ic_menu_delete)
    			.setItems(profileFiles, new DialogInterface.OnClickListener() {
    				// Ask the user to be sure to delete it
    				public void onClick(DialogInterface dialog, int whichItem) {
        				profileToDelete = whichItem;
    					new AlertDialog.Builder(context)
    	                .setIcon(android.R.drawable.ic_dialog_alert)
    	                .setTitle(getString(R.string.settings_profile_dialog_delete_title))
    	                .setMessage(getString(R.string.settings_profile_dialog_delete_text, profileFiles[whichItem]))
    	                .setPositiveButton(android.R.string.ok, deleteOkButtonClick)
    	                .setNegativeButton(android.R.string.cancel, null)
    	                .show();
    				}
    			})
                .show();
                return true;
        }

        return false;
    }

    public static String[] getProfileList() {
    	File dir = new File(profilePath);
    	return dir.list();
    }

    private String getProfileNameString() {
    	return getProfileNameString(settings);
    }

    public static String getProfileNameString(SharedPreferences s) {
    	String provider = s.getString(PREF_SERVER, DEFAULT_SERVER);

    	if (! s.getString(PREF_DOMAIN, "").equals("")) {
    		provider = s.getString(PREF_DOMAIN, DEFAULT_DOMAIN);
    	}

    	return s.getString(PREF_USERNAME, DEFAULT_USERNAME) + "@" + provider;
    }

    private void exportSettings() {
        try {
        	// Create the directory for the profiles
        	new File(profilePath).mkdirs();

        	// Copy shared preference file on the SD card
        	copyFile(new File(sharedPrefsPath + sharedPrefsFile + ".xml"), new File(profilePath + getProfileNameString()));
        } catch (Exception e) {
//            Toast.makeText(this, getString(R.string.settings_profile_export_error), Toast.LENGTH_SHORT).show();
        }
    }

	private OnClickListener profileOnClick = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichItem) {
			boolean wlan = settings.getBoolean(PREF_WLAN, DEFAULT_WLAN);
			boolean g3 = settings.getBoolean(PREF_3G, DEFAULT_3G) ||
				settings.getBoolean(PREF_EDGE, DEFAULT_EDGE);
			boolean ownwifi = settings.getBoolean(PREF_OWNWIFI, DEFAULT_OWNWIFI);
			boolean message = settings.getBoolean(PREF_MESSAGE, DEFAULT_MESSAGE);

			try {
				copyFile(new File(profilePath + profileFiles[whichItem]), new File(sharedPrefsPath + sharedPrefsFile + ".xml"));
            } catch (Exception e) {
                Toast.makeText(context, getString(R.string.settings_profile_import_error), Toast.LENGTH_SHORT).show();
                return;
            }

   			setDefaultValues();
           	setSettingsTitle();

           	// Restart the engine
       		Receiver.engine(context).halt();
   			Receiver.engine(context).StartEngine();
   			
   			settings.unregisterOnSharedPreferenceChangeListener(context);
   			reload();
   			settings.registerOnSharedPreferenceChangeListener(context);
   			updateSummaries();
   			if (wlan != settings.getBoolean(PREF_WLAN, DEFAULT_WLAN) ||
   					g3 != (settings.getBoolean(PREF_3G, DEFAULT_3G) ||
   						settings.getBoolean(PREF_EDGE, DEFAULT_EDGE)) ||
   					ownwifi != settings.getBoolean(PREF_OWNWIFI, DEFAULT_OWNWIFI))
   				updateSleep();
   			if (message) {
   	    		Editor edit = settings.edit();
   	    		edit.putBoolean(PREF_MESSAGE, true);
   	    		edit.commit();
   			}
		}
	};

	private OnClickListener deleteOkButtonClick = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
        	File profile = new File(profilePath + profileFiles[profileToDelete]);
        	boolean rv = false;
        	// Check if the file exists and try to delete it
        	if (profile.exists()) {
        		rv = profile.delete();
        	}
        	if (rv) {
        		Toast.makeText(context, getString(R.string.settings_profile_delete_confirmation), Toast.LENGTH_SHORT).show();
        	} else {
        		Toast.makeText(context, getString(R.string.settings_profile_delete_error), Toast.LENGTH_SHORT).show();
        	}
		}
	};

    public void copyFile(File in, File out) throws Exception {
        FileInputStream  fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }

	@Override
	public void onDestroy()	{
		super.onDestroy();

		settings.unregisterOnSharedPreferenceChangeListener(this);
	}

	EditText transferText;

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (!Thread.currentThread().getName().equals("main"))
    		return;
		if (sharedPreferences.getString(PREF_PORT, DEFAULT_PORT).equals("0")) {
			transferText = new InstantAutoCompleteTextView(this,null);
			transferText.setInputType(InputType.TYPE_CLASS_NUMBER);

			new AlertDialog.Builder(this)
			.setTitle(Receiver.mContext.getString(R.string.settings_port))
			.setView(transferText)
			.setPositiveButton(android.R.string.ok, this)
			.show();
			return;
		} else if (key.equals(PREF_SERVER)) {
    		Editor edit = sharedPreferences.edit();
    		edit.putString(PREF_DNS, DEFAULT_DNS);
    		edit.commit();
        	Receiver.engine(this).updateDNS();
        	Checkin.checkin(false);
			ListPreference lp = (ListPreference) getPreferenceScreen().findPreference(PREF_PROTOCOL);
			lp.setValue(sharedPreferences.getString(PREF_SERVER, DEFAULT_SERVER).equals(DEFAULT_SERVER) ? "tcp" : "udp");
        } else if (sharedPreferences.getBoolean(PREF_CALLBACK, DEFAULT_CALLBACK) && sharedPreferences.getBoolean(PREF_CALLTHRU, DEFAULT_CALLTHRU)) {
    		CheckBoxPreference cb = (CheckBoxPreference) getPreferenceScreen().findPreference(key.equals(PREF_CALLBACK) ? PREF_CALLTHRU : PREF_CALLBACK);
    		cb.setChecked(false);
	    } else if (key.equals(PREF_WLAN) ||
        			key.equals(PREF_3G) ||
        			key.equals(PREF_EDGE) ||
        			key.equals(PREF_USERNAME) ||
        			key.equals(PREF_PASSWORD) ||
        			key.equals(PREF_DOMAIN) ||
        			key.equals(PREF_SERVER) ||
        			key.equals(PREF_PORT) ||
        			key.equals(PREF_STUN) ||
        			key.equals(PREF_STUN_SERVER) ||
        			key.equals(PREF_STUN_SERVER_PORT) ||
        			key.equals(PREF_MMTEL) ||			// (added by mandrajg)
        			key.equals(PREF_MMTEL_QVALUE) ||	// (added by mandrajg)
        			key.equals(PREF_PROTOCOL) ||
        			key.equals(PREF_VPN) ||
        			key.equals(PREF_POS) ||
        			key.equals(PREF_POSURL) ||
        			key.equals(PREF_FROMUSER) ||
        			key.equals(PREF_AUTO_ONDEMAND) ||
        			key.equals(PREF_MWI_ENABLED)) {
        	Receiver.engine(this).halt();
    		Receiver.engine(this).StartEngine();
    		setSettingsTitle();
		}
		if (key.equals(PREF_WLAN) || key.equals(PREF_3G) || key.equals(PREF_EDGE) || key.equals(PREF_OWNWIFI)) {
			updateSleep();
		}

		updateSummaries();
		
		// Export settings only if there is some username and server
		if (! settings.getString(PREF_USERNAME, "").equals("") && ! settings.getString(PREF_SERVER, "").equals("")) {
			exportSettings();
		}
		
    }

	void updateSleep() {
        ContentResolver cr = getContentResolver();
		int get = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, -1);
		int set = get;
		boolean wlan = settings.getBoolean(PREF_WLAN, DEFAULT_WLAN);
		boolean g3 = settings.getBoolean(PREF_3G, DEFAULT_3G) ||
			settings.getBoolean(PREF_EDGE, DEFAULT_EDGE);
		boolean ownwifi = settings.getBoolean(PREF_OWNWIFI, DEFAULT_OWNWIFI);

		if (g3 && ! ownwifi) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
			if (set != get) {
				Toast.makeText(this, R.string.settings_policy_default, Toast.LENGTH_LONG).show();
			}
		} else if (wlan || ownwifi) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
			if (set != get) {
				Toast.makeText(this, R.string.settings_policy_never, Toast.LENGTH_LONG).show();
			}
		}
		if (set != get) {
			android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
		}
	}

	void fill(String pref,String def,int val,int disp) {
    	for (int i = 0; i < getResources().getStringArray(val).length; i++) {
        	if (settings.getString(pref, def).equals(getResources().getStringArray(val)[i])) {
        		getPreferenceScreen().findPreference(pref).setSummary(getResources().getStringArray(disp)[i]);
        	}
    	}
    }

	public void updateSummaries() {
    	getPreferenceScreen().findPreference(PREF_USERNAME).setSummary(settings.getString(PREF_USERNAME, DEFAULT_USERNAME)); 
    	getPreferenceScreen().findPreference(PREF_SERVER).setSummary(settings.getString(PREF_SERVER, DEFAULT_SERVER));

    	getPreferenceScreen().findPreference(PREF_STUN_SERVER).setSummary(settings.getString(PREF_STUN_SERVER, DEFAULT_STUN_SERVER));
    	getPreferenceScreen().findPreference(PREF_STUN_SERVER_PORT).setSummary(settings.getString(PREF_STUN_SERVER_PORT, DEFAULT_STUN_SERVER_PORT));

       	// MMTel settings (added by mandrajg)
       	getPreferenceScreen().findPreference(PREF_MMTEL_QVALUE).setSummary(settings.getString(PREF_MMTEL_QVALUE, DEFAULT_MMTEL_QVALUE));	
    	
    	if (settings.getString(PREF_DOMAIN, DEFAULT_DOMAIN).length() == 0) {
    		getPreferenceScreen().findPreference(PREF_DOMAIN).setSummary(getString(R.string.settings_domain2));
    	} else {
    		getPreferenceScreen().findPreference(PREF_DOMAIN).setSummary(settings.getString(PREF_DOMAIN, DEFAULT_DOMAIN));
    	}
    	if (settings.getString(PREF_FROMUSER,DEFAULT_FROMUSER).length() == 0) {
    		getPreferenceScreen().findPreference(PREF_FROMUSER).setSummary(getString(R.string.settings_callerid2));
    	} else {
    		getPreferenceScreen().findPreference(PREF_FROMUSER).setSummary(settings.getString(PREF_FROMUSER, DEFAULT_FROMUSER));
    	}
    	getPreferenceScreen().findPreference(PREF_PORT).setSummary(settings.getString(PREF_PORT, DEFAULT_PORT));
    	getPreferenceScreen().findPreference(PREF_PROTOCOL).setSummary(settings.getString(PREF_PROTOCOL,
    	settings.getString(PREF_SERVER, DEFAULT_SERVER).equals(DEFAULT_SERVER) ? "tcp" : "udp").toUpperCase());
    	getPreferenceScreen().findPreference(PREF_SEARCH).setSummary(settings.getString(PREF_SEARCH, DEFAULT_SEARCH)); 
    	getPreferenceScreen().findPreference(PREF_EXCLUDEPAT).setSummary(settings.getString(PREF_EXCLUDEPAT, DEFAULT_EXCLUDEPAT)); 
    	getPreferenceScreen().findPreference(PREF_POSURL).setSummary(settings.getString(PREF_POSURL, DEFAULT_POSURL)); 
    	getPreferenceScreen().findPreference(PREF_CALLTHRU2).setSummary(settings.getString(PREF_CALLTHRU2, DEFAULT_CALLTHRU2)); 
    	if (! settings.getString(PREF_PREF, DEFAULT_PREF).equals(VAL_PREF_PSTN)) {
    		getPreferenceScreen().findPreference(PREF_PAR).setEnabled(true);
    	} else {
    		getPreferenceScreen().findPreference(PREF_PAR).setEnabled(false);
      	}
    	fill(PREF_EARGAIN,  "" + DEFAULT_EARGAIN,  R.array.eargain_values, R.array.eargain_display_values);
    	fill(PREF_MICGAIN,  "" + DEFAULT_MICGAIN,  R.array.eargain_values, R.array.eargain_display_values);
    	fill(PREF_HEARGAIN, "" + DEFAULT_HEARGAIN, R.array.eargain_values, R.array.eargain_display_values);
    	fill(PREF_HMICGAIN, "" + DEFAULT_HMICGAIN, R.array.eargain_values, R.array.eargain_display_values);
    	fill(PREF_VQUALITY,      DEFAULT_VQUALITY, R.array.vquality_values,R.array.vquality_display_values);
    	if (settings.getBoolean(PREF_STUN, DEFAULT_STUN)) {
    		getPreferenceScreen().findPreference(PREF_STUN_SERVER).setEnabled(true);
    		getPreferenceScreen().findPreference(PREF_STUN_SERVER_PORT).setEnabled(true);
    	} else {
    		getPreferenceScreen().findPreference(PREF_STUN_SERVER).setEnabled(false);
    		getPreferenceScreen().findPreference(PREF_STUN_SERVER_PORT).setEnabled(false);       	
    	}
    	
    	// MMTel configuration (added by mandrajg)
    	if (settings.getBoolean(PREF_MMTEL, DEFAULT_MMTEL)) {
    		getPreferenceScreen().findPreference(PREF_MMTEL_QVALUE).setEnabled(true);
    	} else {
    		getPreferenceScreen().findPreference(PREF_MMTEL_QVALUE).setEnabled(false);       	
    	}
    	
    	if (settings.getBoolean(PREF_CALLTHRU, DEFAULT_CALLTHRU)) {
    		getPreferenceScreen().findPreference(PREF_CALLTHRU2).setEnabled(true);
    	} else {
    		getPreferenceScreen().findPreference(PREF_CALLTHRU2).setEnabled(false);
    	}
       	if (! settings.getString(PREF_POSURL, DEFAULT_POSURL).equals(DEFAULT_POSURL)) {
    		getPreferenceScreen().findPreference(PREF_POS).setEnabled(! DEFAULT_POS);
    		getPreferenceScreen().findPreference(PREF_CALLBACK).setEnabled(! DEFAULT_CALLBACK);
       	} else {
    		getPreferenceScreen().findPreference(PREF_POS).setEnabled(DEFAULT_POS);
    		getPreferenceScreen().findPreference(PREF_CALLBACK).setEnabled(DEFAULT_CALLBACK);
       	}
       	getPreferenceScreen().findPreference(PREF_BLUETOOTH).setEnabled(RtpStreamReceiver.isBluetoothSupported());
    }

    @Override
	public void onClick(DialogInterface arg0, int arg1) {
		Editor edit = settings.edit();
 		edit.putString(PREF_PORT, transferText.getText().toString());
		edit.commit();
	}
}