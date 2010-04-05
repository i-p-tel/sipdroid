/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package org.sipdroid.media;

import java.io.IOException;
import java.net.SocketException;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.sipdroid.codecs.Codecs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;

/**
 * RtpStreamReceiver is a generic stream receiver. It receives packets from RTP
 * and writes them into an OutputStream.
 */
public class RtpStreamReceiver extends Thread {

	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** Payload type */
	Codecs.Map p_type;

	static String codec = "";

	/** Size of the read buffer */
	public static final int BUFFER_SIZE = 1024;

	/** Maximum blocking time, spent waiting for reading new bytes [milliseconds] */
	public static final int SO_TIMEOUT = 200;

	/** The RtpSocket */
	RtpSocket rtp_socket = null;

	/** Whether it is running */
	boolean running;
	AudioManager am;
	ContentResolver cr;
	public static int speakermode;
	
	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver SipdroidSocket
	 */
	public RtpStreamReceiver(SipdroidSocket socket, Codecs.Map payload_type) {
		init(socket);
		p_type = payload_type;
	}

	/** Inits the RtpStreamReceiver */
	private void init(SipdroidSocket socket) {
		if (socket != null)
			rtp_socket = new RtpSocket(socket);
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}

	/** Stops running */
	public void halt() {
		running = false;
	}
	
	public int speaker(int mode) {
		int old = speakermode;
		
		if (Receiver.headset > 0 && mode == AudioManager.MODE_NORMAL)
			return old;
		saveVolume();
		setMode(speakermode = mode);
		restoreVolume();
		return old;
	}

	static ToneGenerator ringbackPlayer;
	static int oldvol = -1;

	public static synchronized void ringback(boolean ringback) {
		if (ringback && ringbackPlayer == null) {
	        AudioManager am = (AudioManager) Receiver.mContext.getSystemService(
                    Context.AUDIO_SERVICE);
			oldvol = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			setMode(Receiver.docked > 0 && Receiver.headset <= 0?AudioManager.MODE_NORMAL:AudioManager.MODE_IN_CALL);
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
					PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("volume"+speakermode, 
					am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)*
					(speakermode == AudioManager.MODE_NORMAL?4:3)/4
					),0);
			ringbackPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,(int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
			ringbackPlayer.startTone(ToneGenerator.TONE_SUP_RINGTONE);
		} else if (!ringback && ringbackPlayer != null) {
			ringbackPlayer.stopTone();
			ringbackPlayer.release();
			ringbackPlayer = null;
			if (Receiver.call_state == UserAgent.UA_STATE_IDLE) {
		        AudioManager am = (AudioManager) Receiver.mContext.getSystemService(
	                    Context.AUDIO_SERVICE);
				restoreMode();
				am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,oldvol,0);
				oldvol = -1;
			}
		}
	}
	
	double smin = 200,s;
	public static int nearend;
	
	void calc(short[] lin,int off,int len) {
		int i,j;
		double sm = 30000,r;
		
		for (i = 0; i < len; i += 5) {
			j = lin[i+off];
			s = 0.03*Math.abs(j) + 0.97*s;
			if (s < sm) sm = s;
			if (s > smin) nearend = 3000/5;
			else if (nearend > 0) nearend--;
		}
		for (i = 0; i < len; i++) {
			j = lin[i+off];
			if (j > 6550)
				lin[i+off] = 6550*5;
			else if (j < -6550)
				lin[i+off] = -6550*5;
			else
				lin[i+off] = (short)(j*5);
		}
		r = (double)len/100000;
		smin = sm*r + smin*(1-r);
	}
	
	public static void adjust(int keyCode) {
        AudioManager mAudioManager = (AudioManager) Receiver.mContext.getSystemService(
                    Context.AUDIO_SERVICE);
        mAudioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP
                            ? AudioManager.ADJUST_RAISE
                            : AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI);
	}

	static void setStreamVolume(final int stream,final int vol,final int flags) {
        (new Thread() {
			public void run() {
				AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamVolume(stream, vol, flags);
				if (stream == AudioManager.STREAM_VOICE_CALL) restored = true;
			}
        }).start();
	}
	
	static boolean restored;
	
	void restoreVolume() {
		switch (getMode()) {
		case AudioManager.MODE_IN_CALL:
				setStreamVolume(AudioManager.STREAM_RING,(int)(
						am.getStreamMaxVolume(AudioManager.STREAM_RING)*
						org.sipdroid.sipua.ui.Settings.getEarGain()), 0);
				track.setStereoVolume(AudioTrack.getMaxVolume()*
						org.sipdroid.sipua.ui.Settings.getEarGain()
						,AudioTrack.getMaxVolume()*
						org.sipdroid.sipua.ui.Settings.getEarGain());
				break;
		case AudioManager.MODE_NORMAL:
				track.setStereoVolume(AudioTrack.getMaxVolume(),AudioTrack.getMaxVolume());
				break;
		}
		setStreamVolume(AudioManager.STREAM_VOICE_CALL,
				PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("volume"+speakermode, 
				am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)*
				(speakermode == AudioManager.MODE_NORMAL?4:3)/4
				),0);
	}
	
	void saveVolume() {
		if (restored) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putInt("volume"+speakermode,am.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
			edit.commit();
		}
	}
	
	void saveSettings() {
		if (!PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_OLDVALID, org.sipdroid.sipua.ui.Settings.DEFAULT_OLDVALID)) {
			int oldvibrate = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			int oldvibrate2 = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
			if (!PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).contains(org.sipdroid.sipua.ui.Settings.PREF_OLDVIBRATE2))
				oldvibrate2 = AudioManager.VIBRATE_SETTING_ON;
			int oldpolicy = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
					Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putInt(org.sipdroid.sipua.ui.Settings.PREF_OLDVIBRATE, oldvibrate);
			edit.putInt(org.sipdroid.sipua.ui.Settings.PREF_OLDVIBRATE2, oldvibrate2);
			edit.putInt(org.sipdroid.sipua.ui.Settings.PREF_OLDPOLICY, oldpolicy);
			edit.putInt(org.sipdroid.sipua.ui.Settings.PREF_OLDRING, am.getStreamVolume(AudioManager.STREAM_RING));
			edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_OLDVALID, true);
			edit.commit();
		}
	}
	
	public static int getMode() {
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			return am.isSpeakerphoneOn()?AudioManager.MODE_NORMAL:AudioManager.MODE_IN_CALL;
		else
			return am.getMode();
	}
	
	public static void setMode(int mode) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
		edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, mode != AudioManager.MODE_NORMAL);
		edit.commit();
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			am.setSpeakerphoneOn(mode == AudioManager.MODE_NORMAL);
		else
			am.setMode(mode);
	}
	
	public static void restoreMode() {
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, org.sipdroid.sipua.ui.Settings.DEFAULT_SETMODE)) {
			if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE")) {
				setMode(AudioManager.MODE_NORMAL);
			} else {
				Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
				edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, false);
				edit.commit();
			}
		}
	}

	public static void restoreSettings() {
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_OLDVALID, org.sipdroid.sipua.ui.Settings.DEFAULT_OLDVALID)) {
			AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
	        ContentResolver cr = Receiver.mContext.getContentResolver();
			int oldvibrate = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(org.sipdroid.sipua.ui.Settings.PREF_OLDVIBRATE, org.sipdroid.sipua.ui.Settings.DEFAULT_OLDVIBRATE);
			int oldvibrate2 = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(org.sipdroid.sipua.ui.Settings.PREF_OLDVIBRATE2, org.sipdroid.sipua.ui.Settings.DEFAULT_OLDVIBRATE2);
			int oldpolicy = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(org.sipdroid.sipua.ui.Settings.PREF_OLDPOLICY, org.sipdroid.sipua.ui.Settings.DEFAULT_OLDPOLICY);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,oldvibrate);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,oldvibrate2);
			Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, oldpolicy);
			am.setStreamVolume(AudioManager.STREAM_RING, PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("oldring",0), 0);
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_OLDVALID, false);
			edit.commit();
			PowerManager pm = (PowerManager) Receiver.mContext.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
					PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.RtpStreamReceiver");
			wl.acquire(1000);
		}
		restoreMode();
	}

	public static float good, late, lost, loss;
	double avgheadroom;
	public static int timeout;
	
	void empty() {
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1);
			for (;;)
				rtp_socket.receive(rtp_packet);
		} catch (SocketException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		} catch (IOException e) {
		}
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1000);
		} catch (SocketException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		}
	}
	
	RtpPacket rtp_packet;
	AudioTrack track;
	int maxjitter,minjitter,minjitteradjust;
	public static int jitter,mu;
	
	void setCodec() {
		p_type.codec.init();
		codec = p_type.codec.getTitle();
		mu = p_type.codec.samp_rate()/8000;
		maxjitter = AudioTrack.getMinBufferSize(p_type.codec.samp_rate(), 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		if (maxjitter < 2*2*BUFFER_SIZE*3*mu)
			maxjitter = 2*2*BUFFER_SIZE*3*mu;
		track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, p_type.codec.samp_rate(), AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
				maxjitter, AudioTrack.MODE_STREAM);
		maxjitter /= 2*2;
		minjitter = minjitteradjust = 500*mu;
		jitter = 875*mu;
	}
	
	/** Runs it in a new Thread. */
	public void run() {
		boolean nodata = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_NODATA, org.sipdroid.sipua.ui.Settings.DEFAULT_NODATA);
		
		if (rtp_socket == null) {
			if (DEBUG)
				println("ERROR: RTP socket is null");
			return;
		}

		byte[] buffer = new byte[BUFFER_SIZE+12];
		rtp_packet = new RtpPacket(buffer, 0);

		if (DEBUG)
			println("Reading blocks of max " + buffer.length + " bytes");

		running = true;
		speakermode = Receiver.docked > 0 && Receiver.headset <= 0?AudioManager.MODE_NORMAL:AudioManager.MODE_IN_CALL;
		restored = false;

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
        cr = Receiver.mContext.getContentResolver();
		saveSettings();
		Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY,Settings.System.WIFI_SLEEP_POLICY_NEVER);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,AudioManager.VIBRATE_SETTING_OFF);
		if (oldvol != -1) oldvol = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		setCodec();
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int user, server, lserver, luser, headroom, minheadroom, cnt, todo, len = 0, seq = 0, cnt2, m = 1,
			expseq, getseq, vm = 1, gap, gseq, luser2;
		timeout = 1;
		luser = luser2 = -8000*mu;
		cnt = cnt2 = user = lserver = 0;
		minheadroom = maxjitter*2;
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,(int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
		track.play();
		empty();
		System.gc();
		while (running) {
			if (Receiver.call_state == UserAgent.UA_STATE_HOLD) {
				tg.stopTone();
				track.pause();
				while (running && Receiver.call_state == UserAgent.UA_STATE_HOLD) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				track.play();
				System.gc();
				timeout = 1;
				seq = 0;
				luser = luser2 = -8000*mu;
			}
			try {
				rtp_socket.receive(rtp_packet);
				if (timeout != 0) {
					tg.stopTone();
					track.pause();
					for (int i = maxjitter*2; i > 0; i -= BUFFER_SIZE)
						user += track.write(lin2,0,i>BUFFER_SIZE?BUFFER_SIZE:i);
					cnt += maxjitter*2;
					track.play();
					empty();
				}
				timeout = 0;
			} catch (IOException e) {
				if (timeout == 0 && nodata) {
					tg.startTone(ToneGenerator.TONE_SUP_RINGTONE);
				}
				rtp_socket.getDatagramSocket().disconnect();
				if (++timeout > 22) {
					Receiver.engine(Receiver.mContext).rejectcall();
					break;
				}
			}
			if (running && timeout == 0) {		
				 gseq = rtp_packet.getSequenceNumber();
				 if (seq == gseq) {
					 m++;
					 continue;
				 }
				 server = track.getPlaybackHeadPosition();
				 headroom = user-server;
				 
				 if (headroom > 2*jitter)
					 cnt += len;
				 else
					 cnt = 0;
				 
				 if (lserver == server)
					 cnt2++;
				 else
					 cnt2 = 0;

				 if (cnt <= 500*mu || cnt2 >= 2 || headroom - jitter < len ||
						 p_type.codec.number() != 8 || p_type.codec.number() != 0) {
					 if (rtp_packet.getPayloadType() != p_type.number && p_type.change(rtp_packet.getPayloadType())) {
						 track.stop();
						 track.release();
						 setCodec();
						 timeout = 1;
						 luser = luser2 = -8000*mu;
						 cnt = cnt2 = user = lserver = 0;
					 }
					 len = p_type.codec.decode(buffer, lin, rtp_packet.getPayloadLength());
					 
		 			 if (speakermode == AudioManager.MODE_NORMAL)
		 				 calc(lin,0,len);
				 }
				 
				 avgheadroom = avgheadroom * 0.99 + (double)headroom * 0.01;
				 if (headroom < minheadroom)
					 minheadroom = headroom;
	 			 if (headroom < 250*mu) { 
	 				 late++;
					 if (good != 0 && lost/good < 0.01)
						 if (late/good > 0.01 && jitter + minjitteradjust < maxjitter) {
							 jitter += minjitteradjust;
							 late = 0;
							 luser2 = user;
							 minheadroom = maxjitter*2;
						 }
					todo = jitter - headroom;
					user += track.write(lin2,0,todo>BUFFER_SIZE?BUFFER_SIZE:todo);
				 }

				 if (cnt > 500*mu && cnt2 < 2) {
					 todo = headroom - jitter;
					 if (todo < len)
						 user += track.write(lin,todo,len-todo);
				 } else
					 user += track.write(lin,0,len);
				 
				 if (seq != 0) {
					 getseq = gseq&0xff;
					 expseq = ++seq&0xff;
					 if (m == RtpStreamSender.m) vm = m;
					 gap = (getseq - expseq) & 0xff;
					 if (gap > 0) {
						 if (gap > 100) gap = 1;
						 loss += gap;
						 lost += gap;
						 good += gap - 1;
					 } else {
						 if (m < vm)
							 loss++;
					 }
					 good++;
					 if (good > 100) {
						 good *= 0.99;
						 lost *= 0.99;
						 loss *= 0.99;
						 late *= 0.99;
					 }
				 }
				 m = 1;
				 seq = gseq;
				 
				 if (user >= luser + 8000*mu && (
						 Receiver.call_state == UserAgent.UA_STATE_INCALL ||
						 Receiver.call_state == UserAgent.UA_STATE_OUTGOING_CALL)) {
					 if (luser == -8000*mu || getMode() != speakermode) {
						 saveVolume();
						 setMode(speakermode);
						 restoreVolume();
					 }
					 luser = user;
					 if (user >= luser2 + 160000*mu && good != 0 && lost/good < 0.01 && avgheadroom > minheadroom) {
						 int newjitter = (int)avgheadroom - minheadroom + minjitter;
						 if (jitter-newjitter > minjitteradjust)
							 jitter = newjitter;
						 minheadroom = maxjitter*2;
						 luser2 = user;
					 }
				 }
				 lserver = server;
			}
		}
		track.stop();
		track.release();
		tg.stopTone();
		tg.release();
		saveVolume();
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,oldvol,0);
		if (Build.MODEL.contains("Samsung"))
			while (RtpStreamSender.m != 0)
				try {
					sleep(1000);
				} catch (InterruptedException e) {
				}
		restoreSettings();
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,oldvol,0);
		oldvol = -1;
		tg = new ToneGenerator(AudioManager.STREAM_RING,ToneGenerator.MAX_VOLUME/4*3);
		tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
		try {
			sleep(500);
		} catch (InterruptedException e) {
		}
		tg.stopTone();
		tg.release();

		p_type.codec.close();
		rtp_socket.close();
		rtp_socket = null;
		codec = "";

		if (DEBUG)
			println("rtp receiver terminated");
	}

	/** Debug output */
	private static void println(String str) {
		if (!Sipdroid.release) System.out.println("RtpStreamReceiver: " + str);
	}

	public static int byte2int(byte b) { // return (b>=0)? b : -((b^0xFF)+1);
		// return (b>=0)? b : b+0x100;
		return (b + 0x100) % 0x100;
	}

	public static int byte2int(byte b1, byte b2) {
		return (((b1 + 0x100) % 0x100) << 8) + (b2 + 0x100) % 0x100;
	}

	public static String getCodec() {
		return codec;
	}
}
