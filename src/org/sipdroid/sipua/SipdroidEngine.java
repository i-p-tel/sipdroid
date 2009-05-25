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

import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.zoolu.net.IpAddress;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

import android.content.Context;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class SipdroidEngine implements RegisterAgentListener {

	public static final int UNINITIALIZED = 0x0;
	public static final int INITIALIZED = 0x2;

	/** User Agent */
	private UserAgent ua;

	/** Register Agent */
	private RegisterAgent ra;

	/** UserAgentProfile */
	private UserAgentProfile user_profile;

	public SipProvider sip_provider;
	
	PowerManager.WakeLock wl;
	
	public boolean StartEngine() {
		try {
			PowerManager pm = (PowerManager) getUIContext().getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sipdroid");

			SipStack.init(null);
			SipStack.debug_level = 0;
//			SipStack.log_path = "/data/data/org.sipdroid.sipua";
			SipStack.max_retransmission_timeout = 4000;
			SipStack.transaction_timeout = 30000;
			SipStack.default_transport_protocols = new String[1];
			SipStack.default_transport_protocols[0] = SipProvider.PROTO_UDP;
			SipStack.default_port = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("port",""+SipStack.default_port));
			
			String version = "Sipdroid/" + Sipdroid.getVersion();
			SipStack.ua_info = version;
			SipStack.server_info = version;
				
			String opt_via_addr = "127.0.0.1";
			
			user_profile = new UserAgentProfile(null);
			user_profile.username = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("username",""); // modified
			user_profile.passwd = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("password","");
			user_profile.realm = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("server","");
			user_profile.from_url = user_profile.username
					+ "@"
					+ user_profile.realm;			
			user_profile.contact_url = user_profile.username
					+ "@"
					+ opt_via_addr;

			sip_provider = new SipProvider(opt_via_addr, SipStack.default_port);
			CheckEngine();
			
			ua = new UserAgent(sip_provider, user_profile);
			ra = new RegisterAgent(sip_provider, user_profile.from_url,
					user_profile.contact_url, user_profile.username,
					user_profile.realm, user_profile.passwd, this);

			register();
			listen();
		} catch (Exception E) {
		}

		return true;
	}
	
	public void CheckEngine() {
		try {
			if (!sip_provider.hasOutboundProxy())
				sip_provider.setOutboundProxy(new SocketAddress(
						IpAddress.getByName(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("dns","")),
						SipStack.default_port));
		} catch (Exception E) {
		}
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
	
	public void register() {	
		if (!Receiver.isFast() || user_profile.username.equals("") || user_profile.realm.equals("")) {
			Receiver.onText(Receiver.REGISTER_NOTIFICATION,null,0,0);
			if (wl.isHeld())
				wl.release();
		} else
		if (ra != null && ra.register()) {
			Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
			wl.acquire();
		}
	}

	public void halt() { // modified
		if (wl.isHeld())
			wl.release();
		Receiver.onText(Receiver.REGISTER_NOTIFICATION, null, 0, 0);
		if (ra != null) {
			ra.unregister();
			ra.halt();
		}
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
		Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.regok),R.drawable.sym_presence_available,0);
		if (wl.isHeld())
			wl.release();
	}

	/** When a UA failed on (un)registering. */
	public void onUaRegistrationFailure(RegisterAgent ra, NameAddress target,
			NameAddress contact, String result) {
		Receiver.onText(Receiver.REGISTER_NOTIFICATION,getUIContext().getString(R.string.regfailed)+" ("+result+")",R.drawable.sym_presence_away,0);
		if (wl.isHeld())
			wl.release();
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
	public void call(String target_url) {
		ua.printLog("UAC: CALLING " + target_url);
		
		if (!ua.user_profile.audio && !ua.user_profile.video)
		{
			 ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		ua.call(target_url, false);
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
	
	public void togglemute() {
		if (ua.call_state == UserAgent.UA_STATE_HOLD)
			ua.reInvite(null, 0);
		else
			ua.muteMediaApplication();
	}
	
	public int speaker(int mode) {
		return ua.speakerMediaApplication(mode);
	}
	
	/** When a new call is incoming */
	public void onState(int state,String text) {
			Receiver.onState(state,text);
	}

}
