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
import java.net.DatagramSocket;
import java.net.SocketException;

import org.sipdroid.net.KeepAliveSip;
import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

/**
 * RtpStreamReceiver is a generic stream receiver. It receives packets from RTP
 * and writes them into an OutputStream.
 */
public class RtpStreamReceiver extends Thread {

	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** Size of the read buffer */
	public static final int BUFFER_SIZE = 1024;

	/** Maximum blocking time, spent waiting for reading new bytes [milliseconds] */
	public static final int SO_TIMEOUT = 200;

	/** The RtpSocket */
	RtpSocket rtp_socket = null;

	/** Whether it is running */
	boolean running;
	boolean muted;
	public static int speakermode;
	
	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver DatagramSocket
	 */
	public RtpStreamReceiver(DatagramSocket socket) {
		init(socket);
	}

	/** Inits the RtpStreamReceiver */
	private void init(DatagramSocket socket) {
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
	
	public boolean mute() {
		muted = !muted;
		return muted;
	}

	public int speaker(int mode) {
		int old = speakermode;
		
		speakermode = mode;
		return old;
	}

	public static int powersil;
	
	void silence(short[] lin,int off,int len) {
		int i;
		
		for (i = 0; i < len; i++)
			if (lin[i+off] < 300 && lin[i+off] > -300)
				powersil++;
			else
				powersil = 0;
	}
	
	/** Runs it in a new Thread. */
	public void run() {
		if (rtp_socket == null) {
			if (DEBUG)
				println("ERROR: RTP socket is null");
			return;
		}

		byte[] buffer = new byte[BUFFER_SIZE+12];
		RtpPacket rtp_packet = new RtpPacket(buffer, 0);

		if (DEBUG)
			println("Reading blocks of max " + buffer.length + " bytes");

		running = true;
		muted = false;
		speakermode = AudioManager.MODE_IN_CALL;

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		KeepAliveSip ka = new KeepAliveSip(Receiver.engine(Receiver.mContext).sip_provider,15000);
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		int oldvibrate = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_OFF);
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
				4096, AudioTrack.MODE_STREAM);
		track.setStereoVolume(0.3f,0.3f);
		track.play();
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int user, server, lserver, luser, cnt, todo, headroom, len, timeout = 0;
		user = 0;
		lserver = 0;
		luser = -8000;
		cnt = 0;
		user += track.write(lin,0,BUFFER_SIZE);
		user += track.write(lin,0,BUFFER_SIZE);
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
		System.gc();
		while (running) {
			if (muted) {
				track.pause();
				while (running && muted) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				System.gc();
				track.play();
			}
			try {
				rtp_socket.receive(rtp_packet);
				timeout = 0;
			} catch (IOException e) {
				rtp_socket.getDatagramSocket().disconnect();
				if (++timeout >= 22) {
					Receiver.engine(Receiver.mContext).rejectcall();
					break;
				}
			}
			if (running && timeout == 0) {		
				 len = rtp_packet.getPayloadLength();		 
				 G711.alaw2linear(buffer, lin, len);
				 
	 			 if (speakermode == AudioManager.MODE_NORMAL)
	 				 silence(lin,0,len);

				 server = track.getPlaybackHeadPosition();
				 headroom = user-server;
				 
				 if (headroom < 250) {
					 todo = 625 - headroom;
					 println("insert "+todo);
					 if (todo < len)
						 user += track.write(lin,0,todo);
					 else
						 user += track.write(lin2,0,todo);
				 } 

				 if (headroom > 1000)
					 cnt += len;
				 else
					 cnt = 0;
				 
				 if (cnt > 1000 && lserver != server) {
					 todo = headroom - 625;
					 println("cut "+todo);
					 if (todo < len)
						 user += track.write(lin,todo,len-todo);
				 } else
					 user += track.write(lin,0,len);
				 
				 if (user >= luser + 8000) {
					 if (am.getMode() != speakermode) {
						 am.setMode(speakermode);
						 switch (speakermode) {
						 case AudioManager.MODE_IN_CALL:
								track.setStereoVolume(0.3f,0.3f);
								break;
						 case AudioManager.MODE_NORMAL:
								track.setStereoVolume(1f,1f);
								break;
						 }
					 }
					 luser = user;
				 }
				 lserver = server;
			}
		}
		track.stop();
		am.setMode(AudioManager.MODE_NORMAL);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,oldvibrate);
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_RING,ToneGenerator.MAX_VOLUME/4*3);
		tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
		try {
			sleep(500);
		} catch (InterruptedException e) {
		}
		tg.stopTone();
		ka.halt();
		
		rtp_socket.close();
		rtp_socket = null;

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
}
