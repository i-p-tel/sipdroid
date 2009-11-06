/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2008 Hughes Systique Corporation, USA (http://www.hsc.com)
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

package org.sipdroid.sipua;

import java.io.IOException;
import java.net.UnknownHostException;

import org.sipdroid.net.KeepAliveSip;
import org.sipdroid.sipua.ui.LoopAlarm;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.zoolu.net.IpAddress;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class SipdroidEngine implements RegisterAgentListener {

	public static final int UNINITIALIZED = 0x0;
	public static final int INITIALIZED = 0x2;

	/** User Agent */
	private UserAgent ua;

	/** Register Agent */
	private RegisterAgent ra;

	private KeepAliveSip ka;
	
	/** UserAgentProfile */
	private UserAgentProfile user_profile;

	private SipProvider sip_provider;
	
	static PowerManager.WakeLock wl;
	
	public boolean StartEngine() {
		try {
			PowerManager pm = (PowerManager) getUIContext().getSystemService(Context.POWER_SERVICE);
			if (wl == null) wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sipdroid.SipdroidEngine");

			String opt_via_addr = IpAddress.localIpAddress;
			
			user_profile = new UserAgentProfile(null);
			user_profile.username = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("username",""); // modified
			user_profile.passwd = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("password","");
			if (PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("domain","").length() == 0) {
				user_profile.realm = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("server","");
			} else {
				user_profile.realm = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("domain","");
			}
			user_profile.from_url = user_profile.username
				+ "@"
				+ user_profile.realm;
			user_profile.contact_url = user_profile.username
				+ "@"
				+ opt_via_addr;
			if (PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("callerid","").length() == 0) {
				user_profile.callerid = user_profile.from_url;
			} else {
				user_profile.callerid = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("callerid","")
					+ "@"
					+ user_profile.realm;
			}

			SipStack.init(null);
			SipStack.debug_level = 0;
//			SipStack.log_path = "/data/data/org.sipdroid.sipua";
			SipStack.max_retransmission_timeout = 4000;
			SipStack.transaction_timeout = 30000;
			SipStack.default_transport_protocols = new String[1];
			SipStack.default_transport_protocols[0] = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("protocol",
					user_profile.realm.equals("pbxes.org")?"tcp":"udp");
			SipStack.default_port = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("port",""+SipStack.default_port));
			
			String version = "Sipdroid/" + Sipdroid.getVersion() + "/" + Build.MODEL;
			SipStack.ua_info = version;
			SipStack.server_info = version;
				
			sip_provider = new SipProvider(opt_via_addr, 0);
			CheckEngine();
			
			ua = new UserAgent(sip_provider, user_profile);
			ra = new RegisterAgent(sip_provider, user_profile.from_url,
					user_profile.contact_url, user_profile.username,
					user_profile.realm, user_profile.passwd, this, user_profile);
			ka = new KeepAliveSip(sip_provider,100000);

			register();
			listen();
		} catch (Exception E) {
		}

		return true;
	}
	
	void setOutboundProxy() {
		try {
			sip_provider.setOutboundProxy(new SocketAddress(
					IpAddress.getByName(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("dns","")),
					SipStack.default_port));
		} catch (UnknownHostException e) {
		}
	}
	
	public void CheckEngine() {
		if (!sip_provider.hasOutboundProxy())
			setOutboundProxy();
	}

	public Context getUIContext() {
		return Receiver.mContext;
	}
	
	public int getRemoteVideo() {
		return ua.remote_video_port;
	}
	
	public int getLocalVideo() {
		return ua.local_video_port;
	}
	
	public String getRemoteAddr() {
		return ua.remote_media_address;
	}
	
	public void expire() {
		if (ra != null && ra.CurrentState == RegisterAgent.REGISTERED) {
			ra.CurrentState = RegisterAgent.UNREGISTERED;
			Receiver.onText(Receiver.REGISTER_NOTIFICATION, null, 0, 0);
		}
		register();
	}
	
	public void unregister() {
		if (ra != null && ra.unregister()) {
			Receiver.alarm(0, LoopAlarm.class);
			Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
			wl.acquire();
		}		
	}
	
	public void register() {	
		if (user_profile == null || user_profile.username.equals("") ||
				user_profile.realm.equals("")) return;
		if (!Receiver.isFast()) {
			unregister();
		} else {
			if (ra != null && ra.register()) {
				Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
				wl.acquire();
			}
		}
	}

	public void halt() { // modified
		if (wl.isHeld())
			wl.release();
		if (ka != null) {
			Receiver.alarm(0, LoopAlarm.class);
			ka.halt();
		}
		Receiver.onText(Receiver.REGISTER_NOTIFICATION, null, 0, 0);
		if (ra != null)
			ra.halt();
		if (ua != null)
			ua.hangup();
		if (sip_provider != null)
			sip_provider.halt();
	}

	public boolean isRegistered()
	{
		if (ra == null)
		{
			return false;
		}
		return ra.isRegistered();
	}
	
	public void onUaRegistrationSuccess(RegisterAgent ra, NameAddress target,
			NameAddress contact, String result) {
		if (isRegistered()) {
			if (Receiver.on_wlan)
				Receiver.alarm(60, LoopAlarm.class);
			Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.regok),R.drawable.sym_presence_available,0);
		} else
			Receiver.onText(Receiver.REGISTER_NOTIFICATION, null, 0,0);
		Receiver.registered();
		ra.subattempts = 0;
		ra.startMWI();
		if (wl.isHeld())
			wl.release();
	}

	String lastmsgs;
	
    public void onMWIUpdate(boolean voicemail, int number, String vmacc) {
		if (voicemail) {
			String msgs = getUIContext().getString(R.string.voicemail);
			if (number != 0) {
				msgs = msgs + ": " + number;
			}
			Receiver.MWI_account = vmacc;
			if (lastmsgs == null || !msgs.equals(lastmsgs)) {
				Receiver.onText(Receiver.MWI_NOTIFICATION, msgs,android.R.drawable.stat_notify_voicemail,0);
				lastmsgs = msgs;
			}
		} else {
			Receiver.onText(Receiver.MWI_NOTIFICATION, null, 0,0);
			lastmsgs = null;
		}
	}

	static long lasthalt;
	
	/** When a UA failed on (un)registering. */
	public void onUaRegistrationFailure(RegisterAgent ra, NameAddress target,
			NameAddress contact, String result) {
		Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.regfailed)+" ("+result+")",R.drawable.sym_presence_away,0);
		if (wl.isHeld())
			wl.release();
		if (SystemClock.uptimeMillis() > lasthalt + 45000) {
			lasthalt = SystemClock.uptimeMillis();
			sip_provider.haltConnections();
		}
		updateDNS();
		ra.stopMWI();
	}
	
	public void updateDNS() {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(getUIContext()).edit();
		try {
			edit.putString("dns", IpAddress.getByName(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("server","")).toString());
		} catch (UnknownHostException e1) {
			return;
		}
		edit.commit();
		setOutboundProxy();
	}

	/** Receives incoming calls (auto accept) */
	public void listen() 
	{
		ua.printLog("UAS: WAITING FOR INCOMING CALL");
		
		if (!ua.user_profile.audio && !ua.user_profile.video)
		{
			ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		
		ua.listen();
	}
	
	public void info(char c) {
		ua.info(c);
	}
	
	/** Makes a new call */
	public boolean call(String target_url) {
		ua.printLog("UAC: CALLING " + target_url);
		
		if (!isRegistered() || !Receiver.isFast()) {
			if (PreferenceManager.getDefaultSharedPreferences(getUIContext()).getBoolean("callback",false) &&
					PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("posurl","").length() > 0) {
				Receiver.url("n="+Uri.decode(target_url));
				return true;
			}
			return false;
		}

		if (!ua.user_profile.audio && !ua.user_profile.video)
		{
			 ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		return ua.call(target_url, false);
	}

	public void answercall() 
	{
		ua.accept();
	}

	public void rejectcall() {
		ua.printLog("UA: HANGUP");
		ua.hangup();
	}

	public void togglehold() {
		ua.reInvite(null, 0);
	}

	public void transfer(String number) {
		ua.callTransfer(number, 0);
	}
	
	public void togglemute() {
		if (ua.muteMediaApplication())
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.menu_mute), android.R.drawable.stat_notify_call_mute,Receiver.ccCall.base);
		else
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);			
	}
	
	public int speaker(int mode) {
		if (mode == AudioManager.MODE_NORMAL)
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.menu_speaker), android.R.drawable.stat_sys_speakerphone,Receiver.ccCall.base);
		else
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);
		return ua.speakerMediaApplication(mode);
	}
	
	/** When a new call is incoming */
	public void onState(int state,String text) {
			Receiver.onState(state,text);
	}

	public void keepAlive() {
		if (ka != null && Receiver.on_wlan && isRegistered())
			try {
				ka.sendToken();
				Receiver.alarm(60, LoopAlarm.class);
			} catch (IOException e) {
				if (!Sipdroid.release) e.printStackTrace();
			}
	}
}
