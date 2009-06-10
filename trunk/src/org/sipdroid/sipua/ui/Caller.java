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
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.text.TextUtils;
import android.util.Log;

public class Caller extends BroadcastReceiver {
	    @Override
		public void onReceive(Context context, Intent intent) {
	        String intentAction = intent.getAction();
	        if (intentAction.equals(Intent.ACTION_NEW_OUTGOING_CALL))
	        {
	            String number = getResultData();
        		if (!Sipdroid.release) Log.i("SipUA:","outgoing call");
    			boolean sip_type = PreferenceManager.getDefaultSharedPreferences(context).getString("pref","").equals("SIP");
    			if (number.endsWith("+")) 
    			{
    				sip_type = !sip_type;
    				number = number.substring(0,number.length()-1);
    			}
    			if (!sip_type)
    			{
    				setResultData(number);
    			} 
    			else 
    			{
	        		if (number != null && Receiver.engine(context).isRegistered() && Receiver.isFast(true)
	        				&& !intent.getBooleanExtra("android.phone.extra.ALREADY_CALLED",false)) 
	        		{
	    				String sPrefix = PreferenceManager.getDefaultSharedPreferences(context).getString("prefix", "");
	    				if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("par",false)) 
	    				{
	    					String orig = intent.getStringExtra("android.phone.extra.ORIGINAL_URI");
	     					if (orig.lastIndexOf("/phones") >= 0) 
	    					{
	    						orig = orig.substring(0,orig.lastIndexOf("/phones")+7);
	        					Uri contactRef = Uri.parse(orig);
	        				    final String[] PHONES_PROJECTION = new String[] {
	         				        People.Phones.NUMBER, // 0
	        				        People.Phones.TYPE, // 1
	        				    };
	        			        Cursor phonesCursor = context.getContentResolver().query(contactRef, PHONES_PROJECTION, null, null,
	        			                Phones.ISPRIMARY + " DESC");
	        			        if (phonesCursor != null) 
	        			        {	        			        	
	        			        	number = "";
	        			            while (phonesCursor.moveToNext()) 
	        			            {
	        			                final int type = phonesCursor.getInt(1);
	        			                final String n = phonesCursor.getString(0);
	         			                if (TextUtils.isEmpty(n)) continue;
	         			                if (type == Phones.TYPE_MOBILE || type == Phones.TYPE_HOME || type == Phones.TYPE_WORK) 
	         			                {
	         			                	if (!number.equals("")) number = number + "&";
	         			                	number = number + sPrefix + n;	         			                	
	        			                }
	        			            }
	        			            phonesCursor.close();
	        			        }
	        				}        					
	    				}
	    				else 
	    				{
							number = sPrefix + number;    		        	
	    			    }   					
	    				Receiver.engine(context).call(number);
		            	setResultData(null);
	        		}
	            }
	        }
	    }
}
