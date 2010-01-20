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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.sipua.*;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.Connection;

	public class Receiver extends BroadcastReceiver {

		final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
		final static String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
		final static String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
		final static String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
		final static String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
		final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
		final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
		final static String ACTION_DEVICE_IDLE = "com.android.server.WifiManager.action.DEVICE_IDLE";
		
		public final static int REGISTER_NOTIFICATION = 1;
		public final static int CALL_NOTIFICATION = 2;
		public final static int MISSED_CALL_NOTIFICATION = 3;
		public final static int AUTO_ANSWER_NOTIFICATION = 4;
		public final static int MWI_NOTIFICATION = 5;
		
		final static long[] vibratePattern = {0,1000,1000};
		
		public static int docked = -1,headset = -1,screen = -1;
		public static SipdroidEngine mSipdroidEngine;
		
		public static Context mContext;
		public static SipdroidListener listener_video;
		public static Call ccCall;
		public static Connection ccConn;
		public static int call_state;
		
		public static String pstn_state;
		public static long pstn_time;
		public static String MWI_account;
		private static String laststate,lastnumber;	
		
		public static MediaPlayer ringbackPlayer;

		public static SipdroidEngine engine(Context context) {
			mContext = context;
			if (mSipdroidEngine == null) {
				mSipdroidEngine = new SipdroidEngine();
				mSipdroidEngine.StartEngine();
			} else
				mSipdroidEngine.CheckEngine();
        	if (!RegisterService.started || screen > 0)
        		context.startService(new Intent(context,RegisterService.class));
			return mSipdroidEngine;
		}
		
		public static Ringtone oRingtone;
		static PowerManager.WakeLock wl;
				
		public static void stopRingtone() {
			android.os.Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			v.cancel();
			if (Receiver.oRingtone != null) {
				Ringtone ringtone = Receiver.oRingtone;
				Receiver.oRingtone = null;
				ringtone.stop();
			}
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
					lock(true);
					enable_wifi(true);
					RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
					String text = caller.toString();
					if (text.indexOf("<sip:") >= 0 && text.indexOf("@") >= 0)
						text = text.substring(text.indexOf("<sip:")+5,text.indexOf("@"));
					String text2 = caller.toString();
					if (text2.indexOf("\"") >= 0)
						text2 = text2.substring(text2.indexOf("\"")+1,text2.lastIndexOf("\""));
					broadcastCallStateChanged("RINGING", caller);
			        mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
					ccCall.setState(Call.State.INCOMING);
					ccConn.setUserData(null);
					ccConn.setAddress(text,text2);
					ccConn.setIncoming(true);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
					int rm = am.getRingerMode();
					int vs = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			        KeyguardManager mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
					if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("auto_on", false) &&
							!mKeyguardManager.inKeyguardRestrictedInputMode())
						v.vibrate(vibratePattern,1);
					else {
						if ((pstn_state == null || pstn_state.equals("IDLE")) &&
								(rm == AudioManager.RINGER_MODE_VIBRATE ||
								(rm == AudioManager.RINGER_MODE_NORMAL && vs == AudioManager.VIBRATE_SETTING_ON)))
							v.vibrate(vibratePattern,1);
						if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {				 
							String sUriSipRingtone = PreferenceManager.getDefaultSharedPreferences(mContext).getString("sipringtone",
									Settings.System.DEFAULT_RINGTONE_URI.toString());
							if(!TextUtils.isEmpty(sUriSipRingtone)) {
								oRingtone = RingtoneManager.getRingtone(mContext, Uri.parse(sUriSipRingtone));
								oRingtone.play();	
							}
						}
					}
					moveTop();
					if (wl == null) {
						PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
						wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
								PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.onState");
					}
					wl.acquire();
		        	Checkin.checkin(true);
					break;
				case UserAgent.UA_STATE_OUTGOING_CALL:
					lock(true);
					RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
					onText(MISSED_CALL_NOTIFICATION, null, 0,0);
					engine(mContext).register();
					broadcastCallStateChanged("OFFHOOK", caller);
					ccCall.setState(Call.State.DIALING);
					ccConn.setUserData(null);
					ccConn.setAddress(caller,caller);
					ccConn.setIncoming(false);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					moveTop();
		        	Checkin.checkin(true);
					break;
				case UserAgent.UA_STATE_IDLE:
					lock(false);
					broadcastCallStateChanged("IDLE", null);
					onText(CALL_NOTIFICATION, null, 0,0);
					ccCall.setState(Call.State.DISCONNECTED);
					if (listener_video != null)
						listener_video.onHangup();
					stopRingtone();
					if (wl != null && wl.isHeld())
						wl.release();
			        mContext.startActivity(createIntent(InCallScreen.class));
					ccConn.log(ccCall.base);
					ccConn.date = 0;
					engine(mContext).listen();
					break;
				case UserAgent.UA_STATE_INCALL:
					broadcastCallStateChanged("OFFHOOK", null);
					if (ccCall.base == 0) {
						ccCall.base = SystemClock.elapsedRealtime();
					}
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,ccCall.base);
					ccCall.setState(Call.State.ACTIVE);
					stopRingtone();
					if (wl != null && wl.isHeld())
						wl.release();
			        mContext.startActivity(createIntent(InCallScreen.class));
					break;
				case UserAgent.UA_STATE_HOLD:
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_on_hold), android.R.drawable.stat_sys_phone_call_on_hold,ccCall.base);
					ccCall.setState(Call.State.HOLDING);
			        mContext.startActivity(createIntent(InCallScreen.class));
					break;
				}
				pos(true);
				if (ringbackPlayer != null && ringbackPlayer.isPlaying()) {
					ringbackPlayer.stop();
					RtpStreamReceiver.restoreMode();
				}
			}
		}
		
		public static void onText(int type,String text,int mInCallResId,long base) {
	        NotificationManager mNotificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	        if (text != null) {
		        Notification notification = new Notification();
		        notification.icon = mInCallResId;
				if (type == MISSED_CALL_NOTIFICATION) {
			        	notification.flags |= Notification.FLAG_AUTO_CANCEL;
			        	notification.setLatestEventInfo(mContext, text, mContext.getString(R.string.app_name),
			        			PendingIntent.getActivity(mContext, 0, createCallLogIntent(), 0));
			        	if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean("notify",false)) {
				        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				        	notification.ledARGB = 0xff0000ff; /* blue */
				        	notification.ledOnMS = 125;
				        	notification.ledOffMS = 2875;
			        	}
	        	} else {
	        		switch (type) {
		        	case MWI_NOTIFICATION:
			        	notification.flags |= Notification.FLAG_AUTO_CANCEL;
						notification.contentIntent = PendingIntent.getActivity(mContext, 0, 
								createMWIIntent(), 0);	
			        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			        	notification.ledARGB = 0xff00ff00; /* green */
			        	notification.ledOnMS = 125;
			        	notification.ledOffMS = 2875;
						break;
		        	case AUTO_ANSWER_NOTIFICATION:
						notification.contentIntent = PendingIntent.getActivity(mContext, 0,
				                createIntent(AutoAnswer.class), 0);
						break;
		        	default:
						notification.contentIntent = PendingIntent.getActivity(mContext, 0,
					            createIntent(Sipdroid.class), 0);
				        if (mInCallResId == R.drawable.sym_presence_away) {
				        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				        	notification.ledARGB = 0xffff0000; /* red */
				        	notification.ledOnMS = 125;
				        	notification.ledOffMS = 2875;
				        }
		        		break;
		        	}			
		        	notification.flags |= Notification.FLAG_ONGOING_EVENT;
			        RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
	                        R.layout.ongoing_call_notification);
			        contentView.setImageViewResource(R.id.icon, notification.icon);
					if (base != 0) {
						contentView.setChronometer(R.id.text1, base, text+" (%s)", true);
					} else if (type == REGISTER_NOTIFICATION && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pos",false))
						contentView.setTextViewText(R.id.text1, text+"/"+mContext.getString(R.string.settings_pos3));
					else
						contentView.setTextViewText(R.id.text1, text);
					notification.contentView = contentView;
		        }
		        mNotificationMgr.notify(type,notification);
	        } else {
	        	mNotificationMgr.cancel(type);
	        }
	        if (type != AUTO_ANSWER_NOTIFICATION)
	        	updateAutoAnswer();
		}
		
		static void updateAutoAnswer() {
			if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("auto_ondemand", false) &&
				Sipdroid.on(mContext)) {
				if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("auto_demand", false))
					updateAutoAnswer(1);
				else
					updateAutoAnswer(0);
			} else
				updateAutoAnswer(-1);
		}
		
		private static int autoAnswerState = -1;
		
		static void updateAutoAnswer(int status) {
			if (status != autoAnswerState) {
				switch (autoAnswerState = status) {
				case 0:
					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_disabled),R.drawable.auto_answer_disabled,0);
					break;
				case 1:
					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_enabled),R.drawable.auto_answer,0);
					break;
				case -1:
					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION, null, 0, 0);
					break;
				}
			}
		}
		
		public static void registered() {
			pos(true);
		}
		
		static LocationManager lm;
		static AlarmManager am;
		static PendingIntent gps_sender,net_sender;
		static boolean net_enabled;
		
		static final int GPS_UPDATES = 4000*1000;
		static final int NET_UPDATES = 600*1000;
		
		public static void pos(boolean enable) {
	        if (lm == null) lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
			if (am == null) am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
			pos_gps(false);
			if (enable) {
				if (call_state == UserAgent.UA_STATE_IDLE && Sipdroid.on(mContext) &&
						PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pos",false) &&
						PreferenceManager.getDefaultSharedPreferences(mContext).getString("posurl","").length() > 0) {
					Location last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if (last == null || System.currentTimeMillis() - last.getTime() > GPS_UPDATES) {
						pos_gps(true);
						pos_net(false);
					}
					pos_net(true);
				} else
					pos_net(false);
			}
		}

		static void pos_gps(boolean enable) {
			if (gps_sender == null) {
		        Intent intent = new Intent(mContext, OneShotLocation.class);
		        gps_sender = PendingIntent.getBroadcast(mContext,
		                0, intent, 0);
			}
	        if (enable) {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATES, 3000, gps_sender);
				am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+10*1000, gps_sender);	        	
	        } else {
				am.cancel(gps_sender);
				lm.removeUpdates(gps_sender);
	        }
		}
		
		static void pos_net(boolean enable) {
			if (net_sender == null) {
		        Intent loopintent = new Intent(mContext, LoopLocation.class);
		        net_sender = PendingIntent.getBroadcast(mContext,
		                0, loopintent, 0);
			}
			if (net_enabled != enable) {
				if (enable) {
					lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, NET_UPDATES, 3000, net_sender);
				} else {
					lm.removeUpdates(net_sender);
				}
				net_enabled = enable;
			}
		}
		
		static void enable_wifi(boolean enable) {
			if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pos",false) ||
					PreferenceManager.getDefaultSharedPreferences(mContext).getString("posurl","").length() == 0)
				return;
        	if (enable != PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("wifi_disabled", false))
        		return;
        	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    		Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
    		
    		edit.putBoolean("wifi_disabled",!enable);
    		edit.commit();
    		lock_wifi(enable);
    		wm.setWifiEnabled(enable);
		}
		
		static WifiLock wil;
		
		static void lock_wifi(boolean lock) {
     		Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
    		
    		edit.putBoolean("wifi_locked",lock);
    		edit.commit();
			if (lock && wil == null) {
		       	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
				wil = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Sipdroid");
				wil.acquire();
			} else if (!lock && wil != null) {
				wil.release();
				wil = null;
			}
		}
		
	    Handler mHandler = new Handler() {
	    	public void handleMessage(Message msg) {
	    		lock_wifi(false);
	    	}
	    };
	    
		static PowerManager.WakeLock pwl;
		
		static void lock(boolean lock) {
			if (Build.MODEL.equals("Nexus One")) {
				if (lock) {
					if (pwl == null) {
						PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
						pwl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Sipdroid.Receiver");
					}
					pwl.acquire();
				} else if (pwl != null && pwl.isHeld())
					pwl.release();
			}
		}

		public static void url(final String opt) {
	        (new Thread() {
				public void run() {
					try {
				        URL url = new URL(PreferenceManager.getDefaultSharedPreferences(mContext).getString("posurl","")+
				        		"?"+opt);
				        BufferedReader in;
						in = new BufferedReader(new InputStreamReader(url.openStream()));
				        in.close();
					} catch (IOException e) {
						if (!Sipdroid.release) e.printStackTrace();
					}

				}
			}).start();   
		}
		
		static boolean was_playing;
		
		static void broadcastCallStateChanged(String state,String number) {
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
			if (state.equals("IDLE")) {
				if (was_playing) {
					if (pstn_state == null || pstn_state.equals("IDLE"))
						mContext.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
					was_playing = false;
				}
			} else {
				AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
				if ((laststate == null || laststate.equals("IDLE")) && (was_playing = am.isMusicActive()))
					mContext.sendBroadcast(new Intent(PAUSE_ACTION));
			}
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

		static Intent createIntent(Class<?>cls) {
        	Intent startActivity = new Intent();
        	startActivity.setClass(mContext,cls);
    	    startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	    return startActivity;
		}
		
		public static Intent createCallLogIntent() {
	        Intent intent = new Intent(Intent.ACTION_VIEW, null);
	        intent.setType("vnd.android.cursor.dir/calls");
	        return intent;
		}
		
		public static Intent createHomeIntent() {
	        Intent intent = new Intent(Intent.ACTION_MAIN, null);
	        intent.addCategory(Intent.CATEGORY_HOME);
	        return intent;
		}

	    static Intent createMWIIntent() {
			Intent intent;

			if (MWI_account != null)
				intent = new Intent(Intent.ACTION_CALL, Uri.parse(MWI_account));
			else
				intent = new Intent(Intent.ACTION_DIAL);
			return intent;
		}
		
		public static void moveTop() {
			onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call, 0);
			mContext.startActivity(createIntent(Activity2.class)); 
		}

		public static boolean on_wlan,was_fast;
		
		public static boolean isFast() {
        	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        	WifiInfo wi = wm.getConnectionInfo();

        	if (wi != null) {
        		if (!Sipdroid.release) Log.i("SipUA:","isFast() "+WifiInfo.getDetailedStateOf(wi.getSupplicantState())
        				+" "+wi.getIpAddress());
	        	if (wi.getIpAddress() != 0 && (WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.OBTAINING_IPADDR
	        			|| WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.CONNECTED)) {
	        		on_wlan = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("wlan",false);
	        		return was_fast = on_wlan;
	        	}
        	}
        	on_wlan = false;
         	return was_fast = isFast2();
		}
		
		static boolean isFast2() {
        	TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        	if (Sipdroid.market)
        		return false;
        	if (tm.getNetworkType() >= TelephonyManager.NETWORK_TYPE_UMTS)
        		return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("3g",false);
        	if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE)
       			return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("edge",false);
        	return false;
		}
		
	    @Override
		public void onReceive(Context context, Intent intent) {
	        String intentAction = intent.getAction();
	        if (!Sipdroid.on(context)) return;
        	if (!Sipdroid.release) Log.i("SipUA:",intentAction);
        	if (mContext == null) mContext = context;
	        if (intentAction.equals(Intent.ACTION_BOOT_COMPLETED)){
	        	engine(context).register();
	        } else
	        if (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	        	engine(context).register();
			} else
	        if (intentAction.equals(ACTION_DATA_STATE_CHANGED)) {
	        	if (was_fast != isFast())
	        		engine(context).register();
			} else
	        if (intentAction.equals(ACTION_PHONE_STATE_CHANGED) &&
	        		!intent.getBooleanExtra(context.getString(R.string.app_name),false)) {
	    		pstn_state = intent.getStringExtra("state");
	    		pstn_time = SystemClock.elapsedRealtime();
	    		if (pstn_state.equals("IDLE") && call_state != UserAgent.UA_STATE_IDLE)
	    			broadcastCallStateChanged(null,null);
	    		if ((pstn_state.equals("OFFHOOK") && call_state == UserAgent.UA_STATE_INCALL) ||
		    			(pstn_state.equals("IDLE") && call_state == UserAgent.UA_STATE_HOLD))
		    			engine(context).togglehold();
	        } else
	        if (intentAction.equals(ACTION_DOCK_EVENT)) {
	        	docked = intent.getIntExtra(EXTRA_DOCK_STATE, -1);
	        	if (call_state == UserAgent.UA_STATE_INCALL)
	        		if (docked > 0)
	    				engine(mContext).speaker(AudioManager.MODE_NORMAL);
	        		else
	        			engine(mContext).speaker(AudioManager.MODE_IN_CALL);
	        } else
		    if (intentAction.equals(Intent.ACTION_HEADSET_PLUG)) {
		        headset = intent.getIntExtra("state", -1);
		        if (call_state == UserAgent.UA_STATE_INCALL && headset > 0)
        			engine(mContext).speaker(AudioManager.MODE_IN_CALL);
	        } else
	        if (intentAction.equals(Intent.ACTION_SCREEN_ON)) {
	        	mHandler.removeMessages(0);
	        	lock_wifi(true);
	        	screen = 1;
	           	context.startService(new Intent(context,RegisterService.class));
	        } else
	        if (intentAction.equals(Intent.ACTION_USER_PRESENT)) {
	        	enable_wifi(true);
	        } else
	        if (intentAction.equals(Intent.ACTION_SCREEN_OFF)) {
	        	screen = 0;
	        	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	        	WifiInfo wi = wm.getConnectionInfo();
	        	if (wm.getWifiState() != WifiManager.WIFI_STATE_ENABLED || wi == null || wi.getSupplicantState() != SupplicantState.COMPLETED
	        			|| wi.getIpAddress() == 0)
	        		mHandler.sendEmptyMessageDelayed(0, 120*1000);
	        } else
	        if (intentAction.equals(ACTION_DEVICE_IDLE)) {
	        	lock_wifi(false);
	        } else
	        if (intentAction.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
	        	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	        	WifiInfo wi = wm.getConnectionInfo();
	        	if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED && wi != null && wi.getSupplicantState() == SupplicantState.DORMANT &&
	        			!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("wifi_locked", false))
	        		enable_wifi(false);
	        }
		}   
}
