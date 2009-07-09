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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.sipdroid.net.KeepAliveSip;
import org.sipdroid.sipua.*;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.Connection;

	public class Receiver extends BroadcastReceiver {

		public final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
		public final static String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
		
		public final static int REGISTER_NOTIFICATION = 1;
		public final static int CALL_NOTIFICATION = 2;
		
		public static boolean keepTop,isTop;
		private static int oldtimeout;

		public static SipdroidEngine mSipdroidEngine;
		public static Context mContext;
		
		public static SipdroidListener listener,listener_video;
		public static Call ccCall;
		public static Connection ccConn;
		public static int call_state;
		public static String pstn_state;
		static int cellAsu = -1;
		public static String laststate,lastnumber;
				
		public static SipdroidEngine engine(Context context) {
			mContext = context;
			if (mSipdroidEngine == null) {
				mSipdroidEngine = new SipdroidEngine();
				mSipdroidEngine.StartEngine();
			} else
				mSipdroidEngine.CheckEngine();
        	context.startService(new Intent(context,RegisterService.class));
			return mSipdroidEngine;
		}
		
		public static void onState(int state,String caller) {
			if (ccCall == null) {
		        ccCall = new Call();
		        ccConn = new Connection();
		        ccCall.setConn(ccConn);
		        ccConn.setCall(ccCall);
			}
			if (call_state != state) {
				call_state = state;
				android.os.Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
				switch(call_state)
				{
				case UserAgent.UA_STATE_INCOMING_CALL:
					String text = caller.toString();
					if (text.indexOf("<sip:") >= 0 && text.indexOf("@") >= 0)
						text = text.substring(text.indexOf("<sip:")+5,text.indexOf("@"));
					String text2 = caller.toString();
					if (text2.indexOf("\"") >= 0)
						text2 = text2.substring(text2.indexOf("\"")+1,text2.lastIndexOf("\""));
					broadcastCallStateChanged("RINGING", caller);
			        mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
					keepTop = true;
					moveTop();
					ccCall.setState(Call.State.INCOMING);
					ccConn.setUserData(null);
					ccConn.setAddress(text,text2);
					ccConn.setIncoming(true);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
					if ((pstn_state == null || !pstn_state.equals("RINGING")) &&
							am.getRingerMode() != AudioManager.RINGER_MODE_SILENT)
						v.vibrate(5000);
					break;
				case UserAgent.UA_STATE_OUTGOING_CALL:
					engine(mContext).register();
					broadcastCallStateChanged("OFFHOOK", caller);
					moveTop();
					ccCall.setState(Call.State.DIALING);
					ccConn.setUserData(null);
					ccConn.setAddress(caller,caller);
					ccConn.setIncoming(false);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					break;
				case UserAgent.UA_STATE_IDLE:
					broadcastCallStateChanged("IDLE", null);
					screenOff(false);
					keepTop = false;
					onText(CALL_NOTIFICATION, null, 0,0);
					ccCall.setState(Call.State.DISCONNECTED);
					if (listener != null)
						listener.onHangup();
					if (listener_video != null)
						listener_video.onHangup();
					v.cancel();
					break;
				case UserAgent.UA_STATE_INCALL:
					broadcastCallStateChanged("OFFHOOK", null);
					screenOff(true);
					keepTop = false;
					if (ccCall.base == 0) {
						ccCall.base = SystemClock.elapsedRealtime();
					}
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,ccCall.base);
					ccCall.setState(Call.State.ACTIVE);
					v.cancel();
					break;
				case UserAgent.UA_STATE_HOLD:
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_on_hold), android.R.drawable.stat_sys_phone_call_on_hold,ccCall.base);
					ccCall.setState(Call.State.HOLDING);
					break;
				}
				if (call_state == UserAgent.UA_STATE_IDLE || call_state == UserAgent.UA_STATE_INCOMING_CALL) {
			        (new Thread() {
						public void run() {

							try {
								sleep(2000);
							} catch (InterruptedException e) {
							}
							switch (call_state) {
							case UserAgent.UA_STATE_IDLE:
								if (ccConn.date != 0) {
									ccConn.log(ccCall.base);
									ccConn.date = 0;
								}
								engine(mContext).listen();
								break;
							case UserAgent.UA_STATE_INCOMING_CALL:
								moveTop();
								break;
							}
						}
					}).start();   
				}
		        mContext.startActivity(createIntent(InCallScreen.class));
			}
		}
		
		public static void onText(int type,String text,int mInCallResId,long base) {
	        NotificationManager mNotificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	        if (text != null) {
		        Notification notification = new Notification();
		        notification.icon = mInCallResId;
		        notification.contentIntent = PendingIntent.getActivity(mContext, 0,
		                createIntent(Sipdroid.class), 0);
		        notification.flags |= Notification.FLAG_ONGOING_EVENT;
		        RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
                        R.layout.ongoing_call_notification);
		        contentView.setImageViewResource(R.id.icon, notification.icon);
				if (base != 0) {
					contentView.setChronometer(R.id.text1, base, text+" (%s)", true);
				} else if (type == REGISTER_NOTIFICATION && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pos",false))
					contentView.setTextViewText(R.id.text1, text+", "+mContext.getString(R.string.settings_pos3));
				else
					contentView.setTextViewText(R.id.text1, text);
				notification.contentView = contentView;
		        mNotificationMgr.notify(type,notification);
	        } else {
	        	mNotificationMgr.cancel(type);
	        }
		}
		
		public static void registered() {
			if (call_state != UserAgent.UA_STATE_INCALL && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pos",false) &&
					PreferenceManager.getDefaultSharedPreferences(mContext).getString("posurl","").length() > 0)
				pos(true);
		}
		
		public static void pos(boolean enabled) {
	        Intent intent = new Intent(mContext, OneShotLocation.class);
	        PendingIntent sender = PendingIntent.getBroadcast(mContext,
	                0, intent, 0);
	        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
			lm.removeUpdates(sender);
			if (enabled) {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, sender);
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, sender);
			}
		}

		public static void broadcastCallStateChanged(String state,String number) {
			if (state == null) {
				state = laststate;
				number = lastnumber;
			}
			Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
			intent.putExtra("state",state);
			if (number != null)
				intent.putExtra("incoming_number", number);
			intent.putExtra(mContext.getString(R.string.app_name), true);
			mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
			laststate = state;
			lastnumber = number;
		}
		
		public static void alarm(int renew_time,Class <?>cls) {
       		if (!Sipdroid.release) Log.i("SipUA:","alarm "+renew_time);
	        Intent intent = new Intent(mContext, cls);
	        PendingIntent sender = PendingIntent.getBroadcast(mContext,
	                0, intent, 0);
			AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
			if (renew_time > 0)
				am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+renew_time*1000, sender);
		}
		
		public static void reRegister(int renew_time) {
       		alarm(renew_time-15, OneShotAlarm.class);
		}

		public static void screenOff(boolean off) {
	        ContentResolver cr = mContext.getContentResolver();
	        
	        if (off) {
	        	if (oldtimeout == 0) {
	        		oldtimeout = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000);
		        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 1);
		        	Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
	        	}
	        } else {
	        	if (oldtimeout == 0 && Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000) == 1)
	        		oldtimeout = 60000;
	        	if (oldtimeout != 0) {
		        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, oldtimeout);
	        		oldtimeout = 0;
	        	}
	        }
		}
		
		public static Intent createIntent(Class<?>cls) {
        	Intent startActivity = new Intent();
        	startActivity.setClass(mContext,cls);
    	    startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	    return startActivity;
		}
		
		public static void moveTop() {
	    	if (!Sipdroid.release) Log.i("SipUA:","moveTop "+isTop);
			onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call, 0);
			if (!isTop)
				mContext.startActivity(createIntent(Activity2.class)); 
		}

		static boolean on_wlan;
		
		public static boolean isFast(boolean for_a_call) {
			Context context = mContext;
			
        	TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        	WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        	WifiInfo wi = wm.getConnectionInfo();

        	if (wi != null)
        		if (!Sipdroid.release) Log.i("SipUA:","isFast() "+WifiInfo.getDetailedStateOf(wi.getSupplicantState())+" "+cellAsu);
        	if (wi != null && WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.OBTAINING_IPADDR)
        		return on_wlan = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wlan",false);
        	on_wlan = false;
        	if (Sipdroid.market || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("3g",false))
        		return false;
        	if (tm.getNetworkType() >= TelephonyManager.NETWORK_TYPE_UMTS)
        		return true;
        	if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE)
        		if (!for_a_call)
        			return true;
        		else
        			return cellAsu >= Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("minedge", "4"));
        	return false;
		}
		
		public static KeepAliveSip ka;
		
	    @Override
		public void onReceive(Context context, Intent intent) {
	        String intentAction = intent.getAction();
        	if (!Sipdroid.release) Log.i("SipUA:",intentAction);
	        if (intentAction.equals(Intent.ACTION_BOOT_COMPLETED)){
	        	engine(context).register();
	        } else
	        if (intentAction.equals("android.intent.action.ANY_DATA_STATE")) {
	        	engine(context).register();
	        	if (keepTop) moveTop();
	        } else
			if (intentAction.equals(Intent.ACTION_SCREEN_ON)) {
			    if (keepTop) moveTop();
			} else
	        if (intentAction.equals(Intent.ACTION_SCREEN_OFF)) {
	        	screenOff(false);
	        } else
	        if (intentAction.equals(ACTION_PHONE_STATE_CHANGED) &&
	        		!intent.getBooleanExtra(context.getString(R.string.app_name),false)) {
	    		pstn_state = intent.getStringExtra("state");
	    		if (pstn_state.equals("RINGING"))
	    			engine(context).register();
	    		else
	    		if (pstn_state.equals("IDLE") && call_state != UserAgent.UA_STATE_IDLE)
	    			broadcastCallStateChanged(null,null);
	    		else
	    		if ((pstn_state.equals("OFFHOOK") && call_state == UserAgent.UA_STATE_INCALL) ||
		    			(pstn_state.equals("IDLE") && call_state == UserAgent.UA_STATE_HOLD))
		    			engine(context).togglehold();
	        } else
	        if (intentAction.equals(ACTION_SIGNAL_STRENGTH_CHANGED)) {
	        	cellAsu = intent.getIntExtra("asu", 0);
	        	if (cellAsu <= 0 || cellAsu == 99) cellAsu = 0;
	        	else if (cellAsu >= 16) cellAsu = 4;
	        	else if (cellAsu >= 8) cellAsu = 3;
	        	else if (cellAsu >= 4) cellAsu = 2;
	        	else cellAsu = 1;
	        	if (!Sipdroid.release) Log.i("SipUA:","cellAsu "+cellAsu);
	        	if (cellAsu <= Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("maxpoll", "1")) && on_wlan) {
	        		if (ka == null) {
	        			ka = new KeepAliveSip(Receiver.engine(context).sip_provider,15000);
	        			alarm(15, LoopAlarm.class);
	        		}
	        	} else
	        		if (ka != null) {
	        			ka.halt();
	        			ka = null;
	        			alarm(0, LoopAlarm.class);
	        		}	        	
	        }
		}
}
