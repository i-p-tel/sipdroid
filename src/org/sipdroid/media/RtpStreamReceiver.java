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
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.InCallScreen;
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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

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
	public static final int BUFFER_SIZE = 1024*4;

	/** Maximum blocking time, spent waiting for reading new bytes [milliseconds] */
	public static final int SO_TIMEOUT = 1000;

	/** The RtpSocket */
	RtpSocket rtp_socket = null;

	/** Whether it is running */
	boolean running;
	AudioManager am;
	ContentResolver cr;
	public static int speakermode = -1;
	public static boolean bluetoothmode;
	CallRecorder call_recorder = null;
	
	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver SipdroidSocket
	 */
	public RtpStreamReceiver(SipdroidSocket socket, Codecs.Map payload_type, CallRecorder rec) {
		init(socket);
		p_type = payload_type;
		call_recorder = rec;
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
	
	void bluetooth() {
		speaker(AudioManager.MODE_IN_CALL);
		enableBluetooth(!bluetoothmode);
	}
	
	static boolean was_enabled;
	
	static void enableBluetooth(boolean mode) {
		if (bluetoothmode != mode && (!mode || isBluetoothAvailable())) {
			if (mode) was_enabled = true;
			Bluetooth.enable(bluetoothmode = mode);
		}
	}
	
	void cleanupBluetooth() {
		if (was_enabled && Integer.parseInt(Build.VERSION.SDK) == 8) {
			enableBluetooth(true);
			try {
				sleep(3000);
			} catch (InterruptedException e) {
			}
			if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
				android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
	
	public static boolean isBluetoothAvailable() {
		if (Receiver.headset > 0 || Receiver.docked > 0)
			return false;
		if (!isBluetoothSupported())
			return false;
		return Bluetooth.isAvailable();
	}
	
	public static boolean isBluetoothSupported() {
		if (Integer.parseInt(Build.VERSION.SDK) < 8)
			return false;
		return Bluetooth.isSupported();
	}
	
	public int speaker(int mode) {
		int old = speakermode;
		
		if ((Receiver.headset > 0 || Receiver.docked > 0 || Receiver.bluetooth > 0) &&
				mode != Receiver.speakermode())
			return old;
		if (mode == old)
			return old;
		enableBluetooth(false);
		saveVolume();
		setMode(speakermode = mode);
		setCodec();
		restoreVolume();
		if (mode == AudioManager.MODE_NORMAL && Thread.currentThread().getName().equals("main"))
			Toast.makeText(Receiver.mContext, R.string.help_speakerphone, Toast.LENGTH_LONG).show();
		return old;
	}

	static ToneGenerator ringbackPlayer;
	static int oldvol = -1;
	
	static int stream() {
		return speakermode == AudioManager.MODE_IN_CALL?AudioManager.STREAM_VOICE_CALL:AudioManager.STREAM_MUSIC;
	}
	
	public static synchronized void ringback(boolean ringback) {
		if (ringback && ringbackPlayer == null) {
	        AudioManager am = (AudioManager) Receiver.mContext.getSystemService(
                    Context.AUDIO_SERVICE);
			oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
			setMode(speakermode);
			enableBluetooth(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_BLUETOOTH,
					org.sipdroid.sipua.ui.Settings.DEFAULT_BLUETOOTH));
			am.setStreamVolume(stream(),
					PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("volume"+speakermode, 
					am.getStreamMaxVolume(stream())*
					(speakermode == AudioManager.MODE_NORMAL?4:3)/4
					),0);
			ringbackPlayer = new ToneGenerator(stream(),(int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
			ringbackPlayer.startTone(ToneGenerator.TONE_SUP_RINGTONE);
		} else if (!ringback && ringbackPlayer != null) {
			ringbackPlayer.stopTone();
			ringbackPlayer.release();
			ringbackPlayer = null;
			if (Receiver.call_state == UserAgent.UA_STATE_IDLE) {
		        AudioManager am = (AudioManager) Receiver.mContext.getSystemService(
	                    Context.AUDIO_SERVICE);
				restoreMode();
				enableBluetooth(false);
				am.setStreamVolume(AudioManager.STREAM_MUSIC,oldvol,0);
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
			if (s > smin) nearend = 6000*mu/5;
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
		r = (double)len/(100000*mu);
		if (sm > 2*smin || sm < smin/2)
			smin = sm*r + smin*(1-r);
	}
	
	void calc2(short[] lin,int off,int len) {
		int i,j;
		
		for (i = 0; i < len; i++) {
			j = lin[i+off];
			if (j > 16350)
				lin[i+off] = 16350<<1;
			else if (j < -16350)
				lin[i+off] = -16350<<1;
			else
				lin[i+off] = (short)(j<<1);
		}
	}
	
	static long down_time;
	
	public static void adjust(int keyCode,boolean down,boolean show) {
        AudioManager mAudioManager = (AudioManager) Receiver.mContext.getSystemService(
                Context.AUDIO_SERVICE);
        
		if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL)
			if (down ^ mAudioManager.getStreamVolume(stream()) == 0)
				mAudioManager.setStreamMute(stream(), down);
		if (down && down_time == 0)
			down_time = SystemClock.elapsedRealtime();
		if (!down ^ RtpStreamReceiver.speakermode != AudioManager.MODE_NORMAL)
			if (SystemClock.elapsedRealtime()-down_time < 500) {
				if (!down)
					down_time = 0;
				if (ogain > 1)
					if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
						if (gain != ogain) {
							gain = ogain;
							return;
						}
						if (mAudioManager.getStreamVolume(stream()) ==
							mAudioManager.getStreamMaxVolume(stream())) return;
						gain = ogain/2;
					} else {
						if (gain == ogain) {
							gain = ogain/2;
							return;
						}
						if (mAudioManager.getStreamVolume(stream()) == 0) return;
						gain = ogain;
					}
		        mAudioManager.adjustStreamVolume(
		                    stream(),
		                    keyCode == KeyEvent.KEYCODE_VOLUME_UP
		                            ? AudioManager.ADJUST_RAISE
		                            : AudioManager.ADJUST_LOWER,
		                    show?AudioManager.FLAG_SHOW_UI:0);
			}
		if (!down)
			down_time = 0;
	}

	static void setStreamVolume(final int stream,final int vol,final int flags) {
        (new Thread() {
			public void run() {
				AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamVolume(stream, vol, flags);
				if (stream == stream()) restored = true;
			}
        }).start();
	}
	
	static boolean restored;
	static float gain,ogain;
	
	void restoreVolume() {
		switch (getMode()) {
		case AudioManager.MODE_IN_CALL:
				int oldring = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("oldring",0);
				if (oldring > 0 && Integer.parseInt(Build.VERSION.SDK) < 25)
					setStreamVolume(AudioManager.STREAM_RING,(int)(
						am.getStreamMaxVolume(AudioManager.STREAM_RING)*
						org.sipdroid.sipua.ui.Settings.getEarGain()*3), 0);
				track.setStereoVolume(AudioTrack.getMaxVolume()*
						(ogain = org.sipdroid.sipua.ui.Settings.getEarGain()*2)
						,AudioTrack.getMaxVolume()*
						org.sipdroid.sipua.ui.Settings.getEarGain()*2);
				if (gain == 0 || ogain <= 1) gain = ogain;
				break;
		case AudioManager.MODE_NORMAL:
				track.setStereoVolume(AudioTrack.getMaxVolume(),AudioTrack.getMaxVolume());
				break;
		}
		setStreamVolume(stream(),
				PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("volume"+speakermode, 
				am.getStreamMaxVolume(stream())*
				(speakermode == AudioManager.MODE_NORMAL?4:3)/4
				),0);
	}
	
	void saveVolume() {
		if (restored) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putInt("volume"+speakermode,am.getStreamVolume(stream()));
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
	
	static boolean samsung;
	
	public static void setMode(int mode) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
		edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, true);
		edit.commit();
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			am.setSpeakerphoneOn(mode == AudioManager.MODE_NORMAL);
			if (samsung) RtpStreamSender.changed = true;
		} else
			am.setMode(mode);
	}
	
	public static void restoreMode() {
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, org.sipdroid.sipua.ui.Settings.DEFAULT_SETMODE)) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putBoolean(org.sipdroid.sipua.ui.Settings.PREF_SETMODE, false);
			edit.commit();
			if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE")) {
				AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
				if (Integer.parseInt(Build.VERSION.SDK) >= 5)
					am.setSpeakerphoneOn(false);
				else
					am.setMode(AudioManager.MODE_NORMAL);
			}
		}
	}

	void initMode() {
		samsung = Build.MODEL.contains("SAMSUNG") || Build.MODEL.contains("SPH-") ||
			Build.MODEL.contains("SGH-") || Build.MODEL.contains("GT-");
		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL &&
				(Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE")))
			setMode(AudioManager.MODE_NORMAL);	
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
			int oldring = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("oldring",0);
			if (oldring > 0 && Integer.parseInt(Build.VERSION.SDK) < 25)
					am.setStreamVolume(AudioManager.STREAM_RING, oldring, 0);
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

	public static float good, late, lost, loss, loss2;
	double avgheadroom,devheadroom;
	int avgcnt;
	public static int timeout;
	int seq;
	
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
			rtp_socket.getDatagramSocket().setSoTimeout(SO_TIMEOUT);
		} catch (SocketException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		}
		seq = 0;
	}
	
	RtpPacket rtp_packet;
	AudioTrack track;
	int maxjitter,minjitter,minjitteradjust;
	int cnt,cnt2,user,luser,luser2,lserver;
	public static int jitter,mu;
	
	void setCodec() {
		synchronized (this) {
			AudioTrack oldtrack;
			
			p_type.codec.init();
			codec = p_type.codec.getTitle();
			mu = p_type.codec.samp_rate()/8000;
			maxjitter = AudioTrack.getMinBufferSize(p_type.codec.samp_rate(), 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT);
			if (maxjitter < 2*2*1024*6*mu)
				maxjitter = 2*2*1024*6*mu;
			oldtrack = track;
			track = new AudioTrack(stream(), p_type.codec.samp_rate(), AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
					maxjitter*2, AudioTrack.MODE_STREAM);
			maxjitter /= 2*2;
			minjitter = minjitteradjust = 500*mu;
			jitter = 875*mu;
			devheadroom = Math.pow(jitter/5, 2);
			timeout = 1;
			luser = luser2 = -8000*mu;
			cnt = cnt2 = user = lserver = 0;
			if (oldtrack != null) {
				oldtrack.stop();
				oldtrack.release();
			}
		}
	}
	
	void write(short a[],int b,int c) {
		synchronized (this) {
			user += track.write(a,b,c);
		}
	}

	PowerManager.WakeLock pwl,pwl2;
	WifiManager.WifiLock wwl;
	static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	boolean lockLast,lockFirst;
	
	void lock(boolean lock) {
		try {
			if (lock) {
				boolean lockNew = keepon ||
					Receiver.call_state == UserAgent.UA_STATE_HOLD ||
					RtpStreamSender.delay != 0;
				if (lockFirst || lockLast != lockNew) {
					lockLast = lockNew;
					lock(false);
					lockFirst = false;
					if (pwl == null) {
						PowerManager pm = (PowerManager) Receiver.mContext.getSystemService(Context.POWER_SERVICE);
						pwl = pm.newWakeLock(lockNew?(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP):PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Sipdroid.Receiver");
						pwl.acquire();
					}
				}
			} else {
				lockFirst = true;
				if (pwl != null) {
					pwl.release();
					pwl = null;
				}
			}
		} catch (Exception e) {
		}
		if (lock) {
			if (pwl2 == null) {
				PowerManager pm = (PowerManager) Receiver.mContext.getSystemService(Context.POWER_SERVICE);
				WifiManager wm = (WifiManager) Receiver.mContext.getSystemService(Context.WIFI_SERVICE);
				pwl2 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sipdroid.Receiver");
				pwl2.acquire();
				wwl = wm.createWifiLock(3,"Sipdroid.Receiver");
				wwl.acquire();
			}
		} else if (pwl2 != null) {
			pwl2.release();
			pwl2 = null;
			wwl.release();
		}
	}

	void newjitter(boolean inc) {
		 if (good == 0 || lost/good > 0.01 || call_recorder != null)
			 return;
		 int newjitter = (int)Math.sqrt(devheadroom)*7 + (inc?minjitteradjust:0);
		 if (newjitter < minjitter)
			 newjitter = minjitter;
		 if (newjitter > maxjitter)
			 newjitter = maxjitter;
		 if (!inc && (Math.abs(jitter-newjitter) < minjitteradjust || newjitter >= jitter))
			 return;
		 if (inc && newjitter <= jitter)
			 return;
		 jitter = newjitter;
		 late = 0;
		 avgcnt = 0;
		 luser2 = user;
	}
	
	boolean keepon;
	
	/** Runs it in a new Thread. */
	public void run() {
		boolean nodata = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_NODATA, org.sipdroid.sipua.ui.Settings.DEFAULT_NODATA);
		keepon = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_KEEPON, org.sipdroid.sipua.ui.Settings.DEFAULT_KEEPON);

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
		enableBluetooth(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_BLUETOOTH,
				org.sipdroid.sipua.ui.Settings.DEFAULT_BLUETOOTH));
		restored = false;

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
        cr = Receiver.mContext.getContentResolver();
		saveSettings();
		Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY,Settings.System.WIFI_SLEEP_POLICY_NEVER);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,AudioManager.VIBRATE_SETTING_OFF);
		if (oldvol == -1) oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		initMode();
		setCodec();
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int server, headroom, todo, len = 0, m = 1, expseq, getseq, vm = 1, gap, gseq;
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,(int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
		track.play();
		System.gc();
		empty();
		lockFirst = true;
		while (running) {
			lock(true);
			if (Receiver.call_state == UserAgent.UA_STATE_HOLD) {
				lock(false);
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
				luser = luser2 = -8000*mu;
			}
			try {
				rtp_socket.receive(rtp_packet);
				if (timeout != 0) {
					tg.stopTone();
					track.pause();
					for (int i = maxjitter*4; i > 0; i -= BUFFER_SIZE)
						write(lin2,0,i>BUFFER_SIZE?BUFFER_SIZE:i);
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
				if (++timeout > 60) {
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
				 gap = (gseq - seq) & 0xff;
				 if (gap > 240)
					 continue;
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
						 saveVolume();
						 setCodec();
						 restoreVolume();
						 codec = p_type.codec.getTitle();
					 }
					 len = p_type.codec.decode(buffer, lin, rtp_packet.getPayloadLength());
					 
					 // Call recording: Save incoming.
					 // Data is in buffer lin, from 0 to len.
					 if (call_recorder != null)
					 	call_recorder.writeIncoming(lin, 0, len);
					 
		 			 if (speakermode == AudioManager.MODE_NORMAL)
		 				 calc(lin,0,len);
		 			 else if (gain > 1)
		 				 calc2(lin,0,len);
				 }
				 
				 if (cnt == 0)
					 avgheadroom = avgheadroom * 0.99 + (double)headroom * 0.01;
				 if (avgcnt++ > 300)
					 devheadroom = devheadroom * 0.999 + Math.pow(Math.abs(headroom - avgheadroom),2) * 0.001;

				 if (headroom < 250*mu) { 
	 				 late++;
	 				 avgcnt += 10;
	 				 if (avgcnt > 400)
	 					 newjitter(true);
					 todo = jitter - headroom;
					 write(lin2,0,todo>BUFFER_SIZE?BUFFER_SIZE:todo);
				 }

				 if (cnt > 500*mu && cnt2 < 2) {
					 todo = headroom - jitter;
					 if (todo < len)
						 write(lin,todo,len-todo);
				 } else
					 write(lin,0,len);
				 				 
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
						 loss2++;
					 } else {
						 if (m < vm) {
							 loss++;
							 loss2++;
						 }
					 }
					 good++;
					 if (good > 110) {
						 good *= 0.99;
						 lost *= 0.99;
						 loss *= 0.99;
						 loss2 *= 0.99;
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
					 if (user >= luser2 + 160000*mu)
						 newjitter(false);
				 }
				 lserver = server;
			}
		}
		lock(false);
		track.stop();
		track.release();
		tg.stopTone();
		tg.release();
		saveVolume();
		am.setStreamVolume(AudioManager.STREAM_MUSIC,oldvol,0);
		restoreSettings();
		enableBluetooth(false);
		am.setStreamVolume(AudioManager.STREAM_MUSIC,oldvol,0);
		oldvol = -1;
		p_type.codec.close();
		rtp_socket.close();
		rtp_socket = null;
		codec = "";
		
		// Call recording: stop incoming receive.
		if (call_recorder != null)
		{
			call_recorder.stopIncoming();
			call_recorder = null;
		}

		if (DEBUG)
			println("rtp receiver terminated");

		cleanupBluetooth();
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
