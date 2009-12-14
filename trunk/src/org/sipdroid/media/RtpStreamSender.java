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
import java.util.Random;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.sipdroid.pjlib.Codec;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

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
	int p_type;

	/** Number of frame per second */
	long frame_rate;

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
	public RtpStreamSender(boolean do_sync,
			int payload_type, long frame_rate, int frame_size,
			SipdroidSocket src_socket, String dest_addr, int dest_port) {
		init(do_sync, payload_type, frame_rate, frame_size,
				src_socket, dest_addr, dest_port);
	}

	/** Inits the RtpStreamSender */
	private void init(boolean do_sync,
			int payload_type, long frame_rate, int frame_size,
			SipdroidSocket src_socket, String dest_addr,
			int dest_port) {
		this.p_type = payload_type;
		this.frame_rate = frame_rate;
		this.frame_size = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("server","").equals("pbxes.org")?
				(payload_type == 3?960:1024):frame_size; //15
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
	
	/** Runs it in a new Thread. */
	public void run() {
		if (rtp_socket == null)
			return;
		byte[] buffer = new byte[frame_size + 12];
		RtpPacket rtp_packet = new RtpPacket(buffer, 0);
		rtp_packet.setPayloadType(p_type);
		int seqn = 0;
		long time = 0;
		double p = 0;
		TelephonyManager tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		boolean improve = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean("improve",false);
		boolean useGSM = !PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString("compression","edge").equals("never");
		running = true;
		m = 1;

		if (DEBUG)
			println("Reading blocks of " + buffer.length + " bytes");

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
				AudioRecord.getMinBufferSize(8000, 
						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
						AudioFormat.ENCODING_PCM_16BIT)*3/2);
		short[] lin = new short[frame_size*11];
		int num,ring = 0;
		random = new Random();
		InputStream alerting = null;
		try {
			alerting = Receiver.mContext.getAssets().open("alerting");
		} catch (IOException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		}
		switch (p_type) {
		case 3:
			Codec.init();
			break;
		case 0:
		case 8:
			G711.init();
			break;
		}
		record.startRecording();
		while (running) {
			 if (muted || Receiver.call_state == UserAgent.UA_STATE_HOLD) {
				record.stop();
				while (running && (muted || Receiver.call_state == UserAgent.UA_STATE_HOLD)) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				record.startRecording();
			 }
			 num = record.read(lin,(ring+delay)%(frame_size*11),frame_size);

			 if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) {
 				 calc(lin,(ring+delay)%(frame_size*11),num);
 	 			 if (RtpStreamReceiver.nearend != 0)
	 				 noise(lin,(ring+delay)%(frame_size*11),num,p);
	 			 else if (nearend == 0)
	 				 p = 0.9*p + 0.1*s;
			 }
			 if (Receiver.call_state != UserAgent.UA_STATE_INCALL && alerting != null) {
				 try {
					if (alerting.available() < num)
						alerting.reset();
					alerting.read(buffer,12,num);
				 } catch (IOException e) {
					if (!Sipdroid.release) e.printStackTrace();
				 }
				 switch (p_type) {// have to add ulaw case?
				 case 3:
					 G711.alaw2linear(buffer, lin, num);
					 num = Codec.encode(lin, 0, buffer, num);
					 break;
				 case 0:
					 G711.alaw2linear(buffer, lin, num);
					 G711.linear2ulaw(lin, 0, buffer, num);
					 break;
				 }
			 } else {
				 switch (p_type) {
				 case 3:
					 num = Codec.encode(lin, ring%(frame_size*11), buffer, num);
					 break;
				 case 0:
					 G711.linear2ulaw(lin, ring%(frame_size*11), buffer, num);
					 break;
				 case 8:
					 G711.linear2alaw(lin, ring%(frame_size*11), buffer, num);
					 break;
				 }
			 }
 			 ring += frame_size;
 			 rtp_packet.setSequenceNumber(seqn++);
 			 rtp_packet.setTimestamp(time);
 			 rtp_packet.setPayloadLength(num);
 			 try {
 				 rtp_socket.send(rtp_packet);
 				 if (m == 2)
 					 rtp_socket.send(rtp_packet);
 			 } catch (IOException e) {
 			 }
			 time += frame_size;
 			 if (improve && RtpStreamReceiver.good != 0 &&
 					 RtpStreamReceiver.loss/RtpStreamReceiver.good > 0.01 &&
 					 (Receiver.on_wlan || tm.getNetworkType() != TelephonyManager.NETWORK_TYPE_EDGE))        	
 				 m = 2;
 			 else
 				 m = 1;
 			 if (useGSM && p_type == 8 && !Receiver.on_wlan && tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE) {
 				 rtp_packet.setPayloadType(p_type = 3);
 				 if (frame_size == 1024) {
 					 frame_size = 960;
 					 ring = 0;
 				 }
 			 }
		}
		record.stop();
		
		rtp_socket.close();
		rtp_socket = null;

		if (DEBUG)
			println("rtp sender terminated");
	}

	/** Debug output */
	private static void println(String str) {
		if (!Sipdroid.release) System.out.println("RtpStreamSender: " + str);
	}

}