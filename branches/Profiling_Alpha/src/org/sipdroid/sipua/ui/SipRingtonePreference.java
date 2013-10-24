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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

public class SipRingtonePreference extends RingtonePreference 
{   
	private Context mContext;
	
    public SipRingtonePreference(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
    	mContext = context;
    }

    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) 
    {    	
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        ringtonePickerIntent.putExtras(new Intent( RingtoneManager.ACTION_RINGTONE_PICKER));
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) 
    {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		edit.putString(org.sipdroid.sipua.ui.Settings.PREF_SIPRINGTONE, ringtoneUri != null ? ringtoneUri.toString() : org.sipdroid.sipua.ui.Settings.DEFAULT_SIPRINGTONE);		
		edit.commit();        
    }

    @Override
    protected Uri onRestoreRingtone() 
    {
        String uriString = PreferenceManager.getDefaultSharedPreferences(mContext).getString(org.sipdroid.sipua.ui.Settings.PREF_SIPRINGTONE,
        		Settings.System.DEFAULT_RINGTONE_URI.toString());
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;        
    }    
}
