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
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;
import org.sipdroid.sipua.ui.Sipdroid;
import org.sipdroid.codecs.Codecs;
import org.sipdroid.codecs.G711;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

/**
 * RtpStreamSender is a generic stream sender. It takes an InputStream and sends
 * it through RTP.
 */
public class RtpStreamSender extends Thread {
	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** The RtpSocket */
	RtpSocket rtp_socket = null;

	/** Payload type */
	Codecs.Map p_type;

	/** Number of frame per second */
	int frame_rate;

	/** Number of bytes per frame */
	int frame_size;

	/**
	 * Whether it works synchronously with a local clock, or it it acts as slave
	 * of the InputStream
	 */
	boolean do_sync = true;

	/**
	 * Synchronization correction value, in milliseconds. It accellarates the
	 * sending rate respect to the nominal value, in order to compensate program
	 * latencies.
	 */
	int sync_adj = 0;

	/** Whether it is running */
	boolean running = false;
	boolean muted = false;
	
	//DTMF change
	String dtmf = "";
	int dtmf_payload_type = 101;
	
	private static HashMap<Character, Byte> rtpEventMap = new HashMap<Character,Byte>(){{
		put('0',(byte)0);
		put('1',(byte)1);
		put('2',(byte)2);
		put('3',(byte)3);
		put('4',(byte)4);
		put('5',(byte)5);
		put('6',(byte)6);
		put('7',(byte)7);
		put('8',(byte)8);
		put('9',(byte)9);
		put('*',(byte)10);
		put('#',(byte)11);
		put('A',(byte)12);
		put('B',(byte)13);
		put('C',(byte)14);
		put('D',(byte)15);
	}};
	//DTMF change 
	
	CallRecorder call_recorder = null;
	
	/**
	 * Constructs a RtpStreamSender.
	 * 
	 * @param input_stream
	 *            the stream to be sent
	 * @param do_sync
	 *            whether time synchronization must be performed by the
	 *            RtpStreamSender, or it is performed by the InputStream (e.g.
	 *            the system audio input)
	 * @param payload_type
	 *            the payload type
	 * @param frame_rate
	 *            the frame rate, i.e. the number of frames that should be sent
	 *            per second; it is used to calculate the nominal packet time
	 *            and,in case of do_sync==true, the next departure time
	 * @param frame_size
	 *            the size of the payload
	 * @param src_socket
	 *            the socket used to send the RTP packet
	 * @param dest_addr
	 *            the destination address
	 * @param dest_port
	 *            the destination port
	 */
	public RtpStreamSender(boolean do_sync, Codecs.Map payload_type,
			       long frame_rate, int frame_size,
			       SipdroidSocket src_socket, String dest_addr,
			       int dest_port, CallRecorder rec) {
		init(do_sync, payload_type, frame_rate, frame_size,
				src_socket, dest_addr, dest_port);
		call_recorder = rec;
	}

	/** Inits the RtpStreamSender */
	private void init(boolean do_sync, Codecs.Map payload_type,
			  long frame_rate, int frame_size,
			  SipdroidSocket src_socket, String dest_addr,
			  int dest_port) {
		this.p_type = payload_type;
		this.frame_rate = (int)frame_rate;
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Settings.PREF_SERVER, "").equals(Settings.DEFAULT_SERVER))
			switch (payload_type.codec.number()) {
			case 0:
			case 8:
				this.frame_size = 1024;
				break;
			case 9:
				this.frame_size = 960;
				break;
			default:
				this.frame_size = frame_size;
				break;
			}
		else
			this.frame_size = frame_size;
		this.do_sync = do_sync;
		try {
			rtp_socket = new RtpSocket(src_socket, InetAddress
					.getByName(dest_addr), dest_port);
		} catch (Exception e) {
			if (!Sipdroid.release) e.printStackTrace();
		}
	}

	/** Sets the synchronization adjustment time (in milliseconds). */
	public void setSyncAdj(int millisecs) {
		sync_adj = millisecs;
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}
	
	public boolean mute() {
		return muted = !muted;
	}

	public static int delay = 0;
	public static boolean changed;
	
	/** Stops running */
	public void halt() {
		running = false;
	}

	Random random;
	double smin = 200,s;
	int nearend;
	
	void calc(short[] lin,int off,int len) {
		int i,j;
		double sm = 30000,r;
		
		for (i = 0; i < len; i += 5) {
			j = lin[i+off];
			s = 0.03*Math.abs(j) + 0.97*s;
			if (s < sm) sm = s;
			if (s > smin) nearend = 3000*mu/5;
			else if (nearend > 0) nearend--;
		}
		r = (double)len/(100000*mu);
		if (sm > 2*smin || sm < smin/2)
			smin = sm*r + smin*(1-r);
	}

	void calc1(short[] lin,int off,int len) {
		int i,j;
		
		for (i = 0; i < len; i++) {
			j = lin[i+off];
			lin[i+off] = (short)(j>>2);
		}
	}

	void calc2(short[] lin,int off,int len) {
		int i,j;
		
		for (i = 0; i < len; i++) {
			j = lin[i+off];
			lin[i+off] = (short)(j>>1);
		}
	}

	void calc10(short[] lin,int off,int len) {
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

	void noise(short[] lin,int off,int len,double power) {
		int i,r = (int)(power*2);
		short ran;

		if (r == 0) r = 1;
		for (i = 0; i < len; i += 4) {
			ran = (short)(random.nextInt(r*2)-r);
			lin[i+off] = ran;
			lin[i+off+1] = ran;
			lin[i+off+2] = ran;
			lin[i+off+3] = ran;
		}
	}
	
	public static int m;
	int mu;
	
	/** Runs it in a new Thread. */
	public void run() {
		WifiManager wm = (WifiManager) Receiver.mContext.getSystemService(Context.WIFI_SERVICE);
		long lastscan = 0,lastsent = 0;

		if (rtp_socket == null)
			return;
		int seqn = 0;
		long time = 0;
		double p = 0;
		boolean improve = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Settings.PREF_IMPROVE, Settings.DEFAULT_IMPROVE);
		boolean selectWifi = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_SELECTWIFI, org.sipdroid.sipua.ui.Settings.DEFAULT_SELECTWIFI);
		int micgain = 0;
		long last_tx_time = 0;
		long next_tx_delay;
		long now;
		running = true;
		m = 1;
		int dtframesize = 4;
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		mu = p_type.codec.samp_rate()/8000;
		int min = AudioRecord.getMinBufferSize(p_type.codec.samp_rate(), 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		if (min == 640) {
			if (frame_size == 960) frame_size = 320;
			if (frame_size == 1024) frame_size = 160;
			min = 4096*3/2;
		} else if (min < 4096) {
			if (min <= 2048 && frame_size == 1024) frame_size /= 2;
			min = 4096*3/2;
		} else if (min == 4096) {
			min *= 3/2;
			if (frame_size == 960) frame_size = 320;
		} else {
			if (frame_size == 960) frame_size = 320;
			if (frame_size == 1024) frame_size = 160; // frame_size *= 2;
		}
		frame_rate = p_type.codec.samp_rate()/frame_size;
		long frame_period = 1000 / frame_rate;
		frame_rate *= 1.5;
		byte[] buffer = new byte[frame_size + 12];
		RtpPacket rtp_packet = new RtpPacket(buffer, 0);
		rtp_packet.setPayloadType(p_type.number);
		if (DEBUG)
			println("Reading blocks of " + buffer.length + " bytes");
		
		println("Sample rate  = " + p_type.codec.samp_rate());
		println("Buffer size = " + min);

		AudioRecord record = null;
		
		short[] lin = new short[frame_size*(frame_rate+2)];
		int num,ring = 0,pos;
		random = new Random();
		InputStream alerting = null;
		try {
			alerting = Receiver.mContext.getAssets().open("alerting");
		} catch (IOException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		}
		p_type.codec.init();
		while (running) {
			 if (changed || record == null) {
				if (record != null) {
					record.stop();
					record.release();
					if (RtpStreamReceiver.samsung) {
						AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
						am.setMode(AudioManager.MODE_IN_CALL);
						am.setMode(AudioManager.MODE_NORMAL);
					}
				}
				changed = false;
				record = new AudioRecord(MediaRecorder.AudioSource.MIC, p_type.codec.samp_rate(), AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
							min);
				if (record.getState() != AudioRecord.STATE_INITIALIZED) {
					Receiver.engine(Receiver.mContext).rejectcall();
					record = null;
					break;
				}
				if (android.os.Build.VERSION.SDK_INT >= 16) {
					RtpStreamSenderNew_SDK16.aec(record);
				}
				record.startRecording();
				micgain = (int)(Settings.getMicGain()*10);
			 }
			 if (muted || Receiver.call_state == UserAgent.UA_STATE_HOLD) {
				if (Receiver.call_state == UserAgent.UA_STATE_HOLD)
					RtpStreamReceiver.restoreMode();
				record.stop();
				while (running && (muted || Receiver.call_state == UserAgent.UA_STATE_HOLD)) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				record.startRecording();
			 }
			 //DTMF change start
			 if (dtmf.length() != 0) {
	 			 byte[] dtmfbuf = new byte[dtframesize + 12];
				 RtpPacket dt_packet = new RtpPacket(dtmfbuf, 0);
				 dt_packet.setPayloadType(dtmf_payload_type);
 				 dt_packet.setPayloadLength(dtframesize);
				 dt_packet.setSscr(rtp_packet.getSscr());
				 long dttime = time;
				 int duration;
				 
	 			 for (int i = 0; i < 6; i++) { 
 	 				 time += 160;
 	 				 duration = (int)(time - dttime);
	 				 dt_packet.setSequenceNumber(seqn++);
	 				 dt_packet.setTimestamp(dttime);
	 				 dtmfbuf[12] = rtpEventMap.get(dtmf.charAt(0));
	 				 dtmfbuf[13] = (byte)0x0a;
	 				 dtmfbuf[14] = (byte)(duration >> 8);
	 				 dtmfbuf[15] = (byte)duration;
	 				 try {
						rtp_socket.send(dt_packet);
						sleep(20);
	 				 } catch (Exception e1) {
	 				 }
	 			 }
	 			 for (int i = 0; i < 3; i++) {
	 				 duration = (int)(time - dttime);
	 				 dt_packet.setSequenceNumber(seqn);
	 				 dt_packet.setTimestamp(dttime);
	 				 dtmfbuf[12] = rtpEventMap.get(dtmf.charAt(0));
	 				 dtmfbuf[13] = (byte)0x8a;
	 				 dtmfbuf[14] = (byte)(duration >> 8);
	 				 dtmfbuf[15] = (byte)duration;
	 				 try {
						rtp_socket.send(dt_packet);
	 				 } catch (Exception e1) {
	 				 }	 			 
	 			 }
	 			 time += 160; seqn++;
				dtmf=dtmf.substring(1);
			 }
			 //DTMF change end

			 if (frame_size < 480) {
				 now = System.currentTimeMillis();
				 next_tx_delay = frame_period - (now - last_tx_time);
				 last_tx_time = now;
				 if (next_tx_delay > 0) {
					 try {
						 sleep(next_tx_delay);
					 } catch (InterruptedException e1) {
					 }
					 last_tx_time += next_tx_delay-sync_adj;
				 }
			 }
			 pos = Integer.parseInt(Build.VERSION.SDK) == 21?0:((ring+delay*frame_rate*frame_size/2)%(frame_size*(frame_rate+1)));
			 num = record.read(lin,pos,frame_size);
			 if (num <= 0)
				 continue;
			 if (!p_type.codec.isValid())
				 continue;
			 
			 // Call recording: Save the frame to the CallRecorder.
			 if (call_recorder != null)
			 	call_recorder.writeOutgoing(lin, pos, num);

			 if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) {
 				 calc(lin,pos,num);
 	 			 if (RtpStreamReceiver.nearend != 0 && RtpStreamReceiver.down_time == 0)
	 				 noise(lin,pos,num,p/2);
	 			 else if (nearend == 0)
	 				 p = 0.9*p + 0.1*s;
 			 } else switch (micgain) {
 			 case 1:
 				 calc1(lin,pos,num);
 				 break;
 			 case 2:
 				 calc2(lin,pos,num);
 				 break;
 			 case 10:
 				 calc10(lin,pos,num);
 				 break;
 			 }
			 if (Receiver.call_state != UserAgent.UA_STATE_INCALL &&
					 Receiver.call_state != UserAgent.UA_STATE_OUTGOING_CALL && alerting != null) {
				 try {
					if (alerting.available() < num/mu)
						alerting.reset();
					alerting.read(buffer,12,num/mu);
				 } catch (IOException e) {
					if (!Sipdroid.release) e.printStackTrace();
				 }
				 if (p_type.codec.number() != 8) {
					 G711.alaw2linear(buffer, lin, num, mu);
					 num = p_type.codec.encode(lin, 0, buffer, num);
				 }
			 } else {
				 num = p_type.codec.encode(lin, Integer.parseInt(Build.VERSION.SDK) == 21?0:(ring%(frame_size*(frame_rate+1))), buffer, num);
			 }
			 
 			 ring += frame_size;
 			 rtp_packet.setSequenceNumber(seqn++);
 			 rtp_packet.setTimestamp(time);
 			 rtp_packet.setPayloadLength(num);
 			 now = SystemClock.elapsedRealtime();
 			 if (RtpStreamReceiver.timeout == 0 || Receiver.on_wlan || now-lastsent > 500)
	 			 try {
	 				 lastsent = now;
	 				 rtp_socket.send(rtp_packet);
	 				 if (m > 1 && (RtpStreamReceiver.timeout == 0 || Receiver.on_wlan))
	 					 for (int i = 1; i < m; i++)
	 						 rtp_socket.send(rtp_packet);
	 			 } catch (Exception e) {
	 			 }
 			 if (p_type.codec.number() == 9)
 				 time += frame_size/2;
 			 else
 				 time += frame_size;
 			 if (RtpStreamReceiver.good != 0 &&
 					 RtpStreamReceiver.loss2/RtpStreamReceiver.good > 0.01) {
 				 if (selectWifi && Receiver.on_wlan && now-lastscan > 10000) {
 					 wm.startScan();
 					 lastscan = now;
 				 }
 				 if (improve && delay == 0 &&
 						 (p_type.codec.number() == 0 || p_type.codec.number() == 8 || p_type.codec.number() == 9))        	
 					 m = 2;
 				 else
 					 
 					 m = 1;
 			 } else
 				 m = 1;
		}
		if (Integer.parseInt(Build.VERSION.SDK) < 5)
			while (RtpStreamReceiver.getMode() == AudioManager.MODE_IN_CALL)
				try {
					sleep(1000);
				} catch (InterruptedException e) {
				}
		if (record != null) {
			record.stop();
			record.release();
		}
		m = 0;
		
		p_type.codec.close();
		rtp_socket.close();
		rtp_socket = null;
		
		// Call recorder: stop recording outgoing.
		if (call_recorder != null)
		{
			call_recorder.stopOutgoing();
			call_recorder = null;
		}

		if (DEBUG)
			println("rtp sender terminated");
	}

	/** Debug output */
	private static void println(String str) {
		if (!Sipdroid.release) System.out.println("RtpStreamSender: " + str);
	}

	/** Set RTP payload type of outband DTMF packets. **/  
	public void setDTMFpayloadType(int payload_type){
		dtmf_payload_type = payload_type; 
	}
	
	/** Send outband DTMF packets */
	public void sendDTMF(char c) {
		dtmf = dtmf+c; // will be set to 0 after sending tones
	}
	//DTMF change
}
