package org.sipdroid.sipua.ui;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2007 The Android Open Source Project
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.R;

import android.content.Context;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoCamera extends CallScreen implements 
	SipdroidListener, SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaPlayer.OnErrorListener, OnClickListener, OnLongClickListener {
	
	Thread t;
	Context mContext = this;

    private static final String TAG = "videocamera";

    private static int UPDATE_RECORD_TIME = 1;
    
    private static final float VIDEO_ASPECT_RATIO = 176.0f / 144.0f;
    VideoPreview mVideoPreview;
    SurfaceHolder mSurfaceHolder = null;
    VideoView mVideoFrame;
    MediaController mMediaController;

    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording = false;

    private TextView mRecordingTimeView,mFPS;

    ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

    View mPostPictureAlert;
    LocationManager mLocationManager = null;

    private Handler mHandler = new MainHandler();
	LocalSocket receiver,sender;
	LocalServerSocket lss;
	int obuffering,opos;
	int fps;
	
    /** This Handler is used to post message back onto the main thread of the application */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
                        long now = SystemClock.elapsedRealtime();
                        long delta = now - Receiver.ccCall.base;

                        long seconds = (delta + 500) / 1000;  // round to nearest
                        long minutes = seconds / 60;
                        long hours = minutes / 60;
                        long remainderMinutes = minutes - (hours * 60);
                        long remainderSeconds = seconds - (minutes * 60);

                        String secondsString = Long.toString(remainderSeconds);
                        if (secondsString.length() < 2) {
                            secondsString = "0" + secondsString;
                        }
                        String minutesString = Long.toString(remainderMinutes);
                        if (minutesString.length() < 2) {
                            minutesString = "0" + minutesString;
                        }
                        String text = minutesString + ":" + secondsString;
                        if (hours > 0) {
                            String hoursString = Long.toString(hours);
                            if (hoursString.length() < 2) {
                                hoursString = "0" + hoursString;
                            }
                            text = hoursString + ":" + text;
                        }
                       	mRecordingTimeView.setText(text);
                        if (fps != 0) mFPS.setText(fps+(videoQualityHigh?"h":"l")+"fps");
                       	if (mVideoFrame != null) {
                       		int buffering = mVideoFrame.getBufferPercentage(),pos = mVideoFrame.getCurrentPosition();
                            if (buffering != 100 && buffering != 0) {
                            	mMediaController.show();
                            }
                            if (buffering != 0 && !mMediaRecorderRecording) mVideoPreview.setVisibility(View.INVISIBLE);
                            if (((obuffering != buffering && buffering == 100) || (opos == 0 && pos > 0)) && rtp_socket != null) {
        						RtpPacket keepalive = new RtpPacket(new byte[12],0);
        						keepalive.setPayloadType(125);
        						try {
									rtp_socket.send(keepalive);
								} catch (IOException e) {
								}
                            }
                            obuffering = buffering;
                            opos = pos;
                      	}
                        
                        // Work around a limitation of the T-Mobile G1: The T-Mobile
                        // hardware blitter can't pixel-accurately scale and clip at the same time,
                        // and the SurfaceFlinger doesn't attempt to work around this limitation.
                        // In order to avoid visual corruption we must manually refresh the entire
                        // surface view when changing any overlapping view's contents.
                        mVideoPreview.invalidate();
                        mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
         }
    };

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setScreenOnFlag();
        setContentView(R.layout.video_camera);

        mVideoPreview = (VideoPreview) findViewById(R.id.camera_preview);
        mVideoPreview.setAspectRatio(VIDEO_ASPECT_RATIO);

        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceCreated / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);
        mFPS = (TextView) findViewById(R.id.fps);
        mVideoFrame = (VideoView) findViewById(R.id.video_frame);
    }

	int speakermode;
	boolean justplay;

	@Override
    public void onStart() {
        super.onStart();
        speakermode = Receiver.engine(this).speaker(AudioManager.MODE_NORMAL);
        videoQualityHigh = PreferenceManager.getDefaultSharedPreferences(mContext).getString(org.sipdroid.sipua.ui.Settings.PREF_VQUALITY, org.sipdroid.sipua.ui.Settings.DEFAULT_VQUALITY).equals("high");
        if ((intent = getIntent()).hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality = intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            videoQualityHigh = (extraVideoQuality > 0);
        }
	}

	@Override
    public void onResume() {
		if (!Sipdroid.release) Log.i("SipUA:","on resume");
        justplay = intent.hasExtra("justplay");
        if (!justplay) {
			receiver = new LocalSocket();
			try {
				lss = new LocalServerSocket("Sipdroid");
				receiver.connect(new LocalSocketAddress("Sipdroid"));
				receiver.setReceiveBufferSize(500000);
				receiver.setSendBufferSize(500000);
				sender = lss.accept();
				sender.setReceiveBufferSize(500000);
				sender.setSendBufferSize(500000);
			} catch (IOException e1) {
				if (!Sipdroid.release) e1.printStackTrace();
				super.onResume();
				finish();
				return;
			}
	        checkForCamera();
            mVideoPreview.setVisibility(View.VISIBLE);
	        if (!mMediaRecorderRecording) initializeVideo();
	        startVideoRecording();
        } else if (Receiver.engine(mContext).getRemoteVideo() != 0 && PreferenceManager.getDefaultSharedPreferences(this).getString(org.sipdroid.sipua.ui.Settings.PREF_SERVER, org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER).equals(org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER)) {
        	mVideoFrame.setVideoURI(Uri.parse("rtsp://"+Receiver.engine(mContext).getRemoteAddr()+"/"+
        		Receiver.engine(mContext).getRemoteVideo()+"/sipdroid"));
        	mVideoFrame.setMediaController(mMediaController = new MediaController(this));
        	mVideoFrame.setOnErrorListener(this);
        	mVideoFrame.requestFocus();
        	mVideoFrame.start();
        }

        mRecordingTimeView.setText("");
        mRecordingTimeView.setVisibility(View.VISIBLE);
        mHandler.removeMessages(UPDATE_RECORD_TIME);
        mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // This is similar to what mShutterButton.performClick() does,
        // but not quite the same.
        if (mMediaRecorderRecording) {
            stopVideoRecording();

            try {
    			lss.close();
    	        receiver.close();
    	        sender.close();
    		} catch (IOException e) {
    			if (!Sipdroid.release) e.printStackTrace();
    		}
        }

        Receiver.engine(this).speaker(speakermode);
		finish();
    }

	/*
     * catch the back and call buttons to return to the in call activity.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
        	// finish for these events
            case KeyEvent.KEYCODE_CALL:
       			Receiver.engine(this).togglehold();            	
            case KeyEvent.KEYCODE_BACK:
            	finish();
            	return true;
                
            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
            	return true;
            	
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            	RtpStreamReceiver.adjust(keyCode,true);
            	return true;
        }

        return super.onKeyDown(keyCode, event);
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		if (mMediaRecorderRecording) menu.findItem(VIDEO_MENU_ITEM).setVisible(false);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case VIDEO_MENU_ITEM:
			intent.removeExtra("justplay");
			onResume();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (!justplay && !mMediaRecorderRecording) initializeVideo();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    boolean isAvailableSprintFFC,useFront = true;
    
	private void checkForCamera()
	{
		try
		{
			Class.forName("android.hardware.HtcFrontFacingCamera");
			isAvailableSprintFFC = true;
		}
		catch (Exception ex)
		{
			isAvailableSprintFFC = false;
		}
	}

    boolean videoQualityHigh;
    Camera mCamera;
    
    // initializeVideo() starts preview and prepare media recorder.
    // Returns false if initializeVideo fails
    private boolean initializeVideo() {
        Log.v(TAG, "initializeVideo");
        
        if (mSurfaceHolder == null) {
            Log.v(TAG, "SurfaceHolder is null");
            return false;
        }

        mMediaRecorderRecording = true;

        if (mMediaRecorder == null)
        	mMediaRecorder = new MediaRecorder();
        else
        	mMediaRecorder.reset();
        if (mCamera != null) {
        	if (Integer.parseInt(Build.VERSION.SDK) >= 8)
        		VideoCameraNew2.reconnect(mCamera);
        	mCamera.release();
        	mCamera = null;
        }

        if (useFront && Integer.parseInt(Build.VERSION.SDK) >= 5) {
			if (isAvailableSprintFFC)
			{
				try
				{
					Method method = Class.forName("android.hardware.HtcFrontFacingCamera").getDeclaredMethod("getCamera", null);
					mCamera = (Camera) method.invoke(null, null);
				}
				catch (Exception ex)
				{
					Log.d(TAG, ex.toString());
				}
			} else {
				mCamera = Camera.open(); 
				Camera.Parameters parameters = mCamera.getParameters(); 
				parameters.set("camera-id", 2); 
				mCamera.setParameters(parameters); 
			}
			VideoCameraNew.unlock(mCamera);
			mMediaRecorder.setCamera(mCamera);
	        mVideoPreview.setOnClickListener(this);
        }
        mVideoPreview.setOnLongClickListener(this);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(sender.getFileDescriptor());

        if (videoQualityHigh) {
            mMediaRecorder.setVideoSize(352,288);
        } else {
            mMediaRecorder.setVideoSize(176,144);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.start();
        } catch (IOException exception) {
            releaseMediaRecorder();
            finish();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            if (mCamera != null) {
	        	if (Integer.parseInt(Build.VERSION.SDK) >= 8)
	        		VideoCameraNew2.reconnect(mCamera);
	        	mCamera.release();
	        	mCamera = null;
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
        
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            finish();
        }
    }

    boolean change;
    
    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");

            if (Receiver.listener_video == null) {
    			Receiver.listener_video = this;   	
                RtpStreamSender.delay = 1;

                try {
					if (rtp_socket == null)
						rtp_socket = new RtpSocket(new SipdroidSocket(Receiver.engine(mContext).getLocalVideo()),
							InetAddress.getByName(Receiver.engine(mContext).getRemoteAddr()),
							Receiver.engine(mContext).getRemoteVideo());
				} catch (Exception e) {
					if (!Sipdroid.release) e.printStackTrace();
					return;
				}		
				
    	        (t = new Thread() {
    				public void run() {
    					int frame_size = 1400;
    					byte[] buffer = new byte[frame_size + 14];
    					buffer[12] = 4;
    					RtpPacket rtp_packet = new RtpPacket(buffer, 0);
    					int seqn = 0;
    					int num,number = 0,src,dest,len = 0,head = 0,lasthead = 0,lasthead2 = 0,cnt = 0,stable = 0;
    					long now,lasttime = 0;
    					double avgrate = videoQualityHigh?45000:24000;
    					double avglen = avgrate/20;
    					
    					InputStream fis = null;
						try {
		   					fis = receiver.getInputStream();
						} catch (IOException e1) {
							if (!Sipdroid.release) e1.printStackTrace();
							rtp_socket.getDatagramSocket().close();
							return;
						}
						
     					rtp_packet.setPayloadType(103);
    					while (Receiver.listener_video != null && videoValid()) {
    						num = -1;
    						try {
    							num = fis.read(buffer,14+number,frame_size-number);
    						} catch (IOException e) {
    							if (!Sipdroid.release) e.printStackTrace();
    							break;
    						}
    						if (num < 0) {
    							try {
    								sleep(20);
    							} catch (InterruptedException e) {
    								break;
    							}
    							continue;							
    						}
    						number += num;
    						head += num;
    						try {
								now = SystemClock.elapsedRealtime();
								if (lasthead != head+fis.available() && ++stable >= 5 && now-lasttime > 700) {
									if (cnt != 0 && len != 0)
										avglen = len/cnt;
									if (lasttime != 0) {
										fps = (int)((double)cnt*1000/(now-lasttime));
										avgrate = (double)((head+fis.available())-lasthead2)*1000/(now-lasttime);
									}
									lasttime = now;
									lasthead = head+fis.available();
									lasthead2 = head;
									len = cnt = stable = 0;
								}
							} catch (IOException e1) {
    							if (!Sipdroid.release) e1.printStackTrace();
    							break;
							}
    						
        					for (num = 14; num <= 14+number-2; num++)
    							if (buffer[num] == 0 && buffer[num+1] == 0) break;
    						if (num > 14+number-2) {
    							num = 0;
    							rtp_packet.setMarker(false);
    						} else {	
    							num = 14+number - num;
    							rtp_packet.setMarker(true);
    						}
    						
    			 			rtp_packet.setSequenceNumber(seqn++);
    			 			rtp_packet.setPayloadLength(number-num+2);
    			 			if (seqn > 10) try {
    			 				rtp_socket.send(rtp_packet);
        			 			len += number-num;
    			 			} catch (IOException e) {
    			 				if (!Sipdroid.release) e.printStackTrace();
    			 				break;
    			 			}
							
    			 			if (num > 0) {
    				 			num -= 2;
    				 			dest = 14;
    				 			src = 14+number - num;
    				 			if (num > 0 && buffer[src] == 0) {
    				 				src++;
    				 				num--;
    				 			}
    				 			number = num;
    				 			while (num-- > 0)
    				 				buffer[dest++] = buffer[src++];
    							buffer[12] = 4;
    							
    							cnt++;
    							try {
    								if (avgrate != 0)
    									Thread.sleep((int)(avglen/avgrate*1000));
								} catch (Exception e) {
    								break;
								}
        			 			rtp_packet.setTimestamp(SystemClock.elapsedRealtime()*90);
    			 			} else {
    			 				number = 0;
    							buffer[12] = 0;
    			 			}
    			 			if (change) {
    			 				change = false;
    			 				long time = SystemClock.elapsedRealtime();
    			 				
    	    					try {
    								while (fis.read(buffer,14,frame_size) > 0 &&
    										SystemClock.elapsedRealtime()-time < 3000);
    							} catch (Exception e) {
    							}
    			 				number = 0;
    							buffer[12] = 0;
    			 			}
    					}
    					rtp_socket.getDatagramSocket().close();
    					try {
							while (fis.read(buffer,0,frame_size) > 0);
						} catch (IOException e) {
						}
    				}
    			}).start();   
            }
    }

    private void stopVideoRecording() {
        Log.v(TAG, "stopVideoRecording");
        if (mMediaRecorderRecording || mMediaRecorder != null) {
    		Receiver.listener_video = null;
    		t.interrupt();
            RtpStreamSender.delay = 0;

            if (mMediaRecorderRecording && mMediaRecorder != null) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "stop fail: " + e.getMessage());
                }

                mMediaRecorderRecording = false;
            }
            releaseMediaRecorder();
        }
    }

    private void setScreenOnFlag() {
        Window w = getWindow();
        final int keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if ((w.getAttributes().flags & keepScreenOnFlag) == 0) {
            w.addFlags(keepScreenOnFlag);
        }
    }

	public void onHangup() {
		finish();
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	RtpStreamReceiver.adjust(keyCode,false);
        	return true;
        case KeyEvent.KEYCODE_ENDCALL:
        	if (Receiver.pstn_state == null ||
				(Receiver.pstn_state.equals("IDLE") && (SystemClock.elapsedRealtime()-Receiver.pstn_time) > 3000)) {
        			Receiver.engine(mContext).rejectcall();
        			return true;		
        	}
        	break;
		}
		return false;
	}
	
	static TelephonyManager tm;
	
	static boolean videoValid() {
		if (Receiver.on_wlan)
			return true;
		if (tm == null) tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.getNetworkType() < TelephonyManager.NETWORK_TYPE_UMTS)
			return false;
		return true;	
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		return true;
	}

	@Override
	public void onClick(View v) {
		useFront = !useFront;
		initializeVideo();
		change = true;
	}

	@Override
	public boolean onLongClick(View v) {
		videoQualityHigh = !videoQualityHigh;
		initializeVideo();
		change = true;
		return true;
	}
	
}
