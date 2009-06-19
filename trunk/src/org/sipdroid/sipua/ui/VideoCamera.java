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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.sipua.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoCamera extends CallScreen implements 
	SipdroidListener, SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
	
	Thread t;
	Context mContext = this;

    private static final String TAG = "videocamera";

    private static int UPDATE_RECORD_TIME = 1;
    
    private static final long NO_STORAGE_ERROR = -1L;
    private static final long CANNOT_STAT_ERROR = -2L;
    private static final long LOW_STORAGE_THRESHOLD = 512L * 1024L;

    private static final int STORAGE_STATUS_OK = 0;
    private static final int STORAGE_STATUS_LOW = 1;
    private static final int STORAGE_STATUS_NONE = 2;

    private static final float VIDEO_ASPECT_RATIO = 176.0f / 144.0f;
    VideoPreview mVideoPreview;
    SurfaceHolder mSurfaceHolder = null;
    ImageView mVideoFrame;

    private static final int MAX_RECORDING_DURATION_MS = 10 * 60 * 1000;

    private int mStorageStatus = STORAGE_STATUS_OK;

    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording = false;
    // The video file that the hardware camera is about to record into
    // (or is recording into.)
    private String mCameraVideoFilename;

    // The video file that has already been recorded, and that is being
    // examined by the user.
    private String mCurrentVideoFilename;

    boolean mPausing = false;

    int mCurrentZoomIndex = 0;

    private TextView mRecordingTimeView;
    private boolean mRecordingTimeCountsDown = false;

    ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

    View mPostPictureAlert;
    LocationManager mLocationManager = null;

    private Handler mHandler = new MainHandler();
    long started;
    
    /** This Handler is used to post message back onto the main thread of the application */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
                    if (mMediaRecorderRecording) {
                        long now = SystemClock.elapsedRealtime();
                        long delta = now - Receiver.ccCall.base;

                        // Starting a minute before reaching the max duration
                        // limit, we'll countdown the remaining time instead.
                        boolean countdown_remaining_time =
                            (now - started >= MAX_RECORDING_DURATION_MS - 60000);

                        if (countdown_remaining_time) {
                            delta = Math.max(0, MAX_RECORDING_DURATION_MS - (now-started));
                        }

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
                        mRecordingTimeView.setText(getResources().getString(R.string.card_title_in_progress)+" "+text);

                        if (mRecordingTimeCountsDown != countdown_remaining_time) {
                            // Avoid setting the color on every update, do it only
                            // when it needs changing.

                            mRecordingTimeCountsDown = countdown_remaining_time;

                            int color = getResources().getColor(
                                    countdown_remaining_time ? R.color.recording_time_remaining_text
                                                             : R.color.recording_time_elapsed_text);

                            mRecordingTimeView.setTextColor(color);
                        }

                        // Work around a limitation of the T-Mobile G1: The T-Mobile
                        // hardware blitter can't pixel-accurately scale and clip at the same time,
                        // and the SurfaceFlinger doesn't attempt to work around this limitation.
                        // In order to avoid visual corruption we must manually refresh the entire
                        // surface view when changing any overlapping view's contents.
                        mVideoPreview.invalidate();
                        mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
                    }
         }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
            	finish();
            }
        }
    };

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        requestWindowFeature(Window.FEATURE_PROGRESS);
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
        mVideoFrame = (ImageView) findViewById(R.id.video_frame);
    }

    private int getStorageStatus(boolean mayHaveSd) {
        long remaining = mayHaveSd ? getAvailableStorage() : NO_STORAGE_ERROR;
        if (remaining == NO_STORAGE_ERROR) {
            return STORAGE_STATUS_NONE;
        }
        return remaining < LOW_STORAGE_THRESHOLD
                ? STORAGE_STATUS_LOW : STORAGE_STATUS_OK;
    }

	int speakermode;

	@Override
    public void onResume() {
        super.onResume();

        mPausing = false;

        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
        mStorageStatus = getStorageStatus(true);

        initializeVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // This is similar to what mShutterButton.performClick() does,
        // but not quite the same.
        if (mMediaRecorderRecording) {
            stopVideoRecording();
        }

        mPausing = true;

        unregisterReceiver(mReceiver);
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
            	break;
                
            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
            	return true;
        }

        return super.onKeyDown(keyCode, event);
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		menu.findItem(VIDEO_MENU_ITEM).setVisible(false);
		
		return result;
	}

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mPausing) {
            // We're pausing, the screen is off and we already stopped
            // video recording. We don't want to start the camera again
            // in this case in order to conserve power.
            // The fact that surfaceChanged is called _after_ an onPause appears
            // to be legitimate since in that case the lockscreen always returns
            // to portrait orientation possibly triggering the notification.
            return;
        }

        stopVideoRecording();
        initializeVideo();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }


    /**
     * Returns
     * @return number of bytes available, or an ERROR code.
     */
    private static long getAvailableStorage() {
        try {
                String storageDirectory = Environment.getExternalStorageDirectory().toString();
                StatFs stat = new StatFs(storageDirectory);
                return ((long)stat.getAvailableBlocks() * (long)stat.getBlockSize());
        } catch (Exception ex) {
            // if we can't stat the filesystem then we don't know how many
            // free bytes exist.  It might be zero but just leave it
            // blank since we really don't know.
            return CANNOT_STAT_ERROR;
        }
    }

    private void cleanupEmptyFile() {
        if (mCameraVideoFilename != null) {
            File f = new File(mCameraVideoFilename);
            if (f.length() == 0 && f.delete()) {
              Log.v(TAG, "Empty video file deleted: " + mCameraVideoFilename);
              mCameraVideoFilename = null;
            }
        }
    }

    // initializeVideo() starts preview and prepare media recorder.
    // Returns false if initializeVideo fails
    private boolean initializeVideo() {
        Log.v(TAG, "initializeVideo");

        Intent intent = getIntent();

        releaseMediaRecorder();

        if (mSurfaceHolder == null) {
            Log.v(TAG, "SurfaceHolder is null");
            return false;
        }

        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        mMediaRecorder.setMaxDuration(MAX_RECORDING_DURATION_MS);

        if (mStorageStatus != STORAGE_STATUS_OK) {
            finish();
        } else {
                createVideoPath();
                mMediaRecorder.setOutputFile(mCameraVideoFilename);
        }

        boolean videoQualityHigh = false;

        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality = intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            videoQualityHigh = (extraVideoQuality > 0);
        }

        // Use the same frame rate for both, since internally
        // if the frame rate is too large, it can cause camera to become
        // unstable. We need to fix the MediaRecorder to disable the support
        // of setting frame rate for now.
        mMediaRecorder.setVideoFrameRate(20);
        if (videoQualityHigh) {
            mMediaRecorder.setVideoSize(352,288);
        } else {
            mMediaRecorder.setVideoSize(176,144);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        long remaining = getAvailableStorage();
        // remaining >= LOW_STORAGE_THRESHOLD at this point, reserve a quarter
        // of that to make it more likely that recording can complete successfully.
        try {
            mMediaRecorder.setMaxFileSize(remaining - LOW_STORAGE_THRESHOLD / 4);
        } catch (RuntimeException exception) {
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            //    on the size restriction.
        }

        try {
            mMediaRecorder.prepare();
        } catch (IOException exception) {
            Log.e(TAG, "prepare failed for " + mCameraVideoFilename);
            releaseMediaRecorder();
            finish();
        }
        mMediaRecorderRecording = false;

        startVideoRecording();
        return true;
    }

    private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
    
    private void createVideoPath() {
        String title = "Sipdroid";
        String cameraDirPath = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
        File cameraDir = new File(cameraDirPath);
        cameraDir.mkdirs();
        String filename = cameraDirPath + "/" + title + ".3gp";
        mCameraVideoFilename = filename;
        Log.v(TAG, "Current camera video filename: " + mCameraVideoFilename);
    }

    private void deleteCurrentVideo() {
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
        }
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (! f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    // from MediaRecorder.OnErrorListener
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            finish();
        }
    }

    // from MediaRecorder.OnInfoListener
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            finish();
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            finish();
        }
    }

    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        if (!mMediaRecorderRecording) {

            if (mStorageStatus != STORAGE_STATUS_OK) {
                Log.v(TAG, "Storage issue, ignore the start request");
                return;
            }

            // Check mMediaRecorder to see whether it is initialized or not.
            if (mMediaRecorder == null && initializeVideo() == false ) {
                Log.e(TAG, "Initialize video (MediaRecorder) failed.");
                return;
            }

            try {
                mMediaRecorder.setOnErrorListener(this);
                mMediaRecorder.setOnInfoListener(this);
                mMediaRecorder.start();   // Recording is now started
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
                return;
            }
            mMediaRecorderRecording = true;
            started = SystemClock.elapsedRealtime();
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
            setScreenOnFlag();
        
            if (Receiver.listener_video == null) {
    			Receiver.listener_video = this;
    	        (t = new Thread() {
    				public void run() {
    					int frame_size = 1400;
    					byte[] buffer = new byte[frame_size + 14];
    					buffer[12] = 4;
    					RtpPacket rtp_packet = new RtpPacket(buffer, 0);
    					RtpSocket rtp_socket = null;
    					int seqn = 0;
    					int num,number = 0,src,dest;
    					File f;
    					FileInputStream fis = null;

    					try {
    						rtp_socket = new RtpSocket(new DatagramSocket(Receiver.engine(mContext).getLocalVideo()),
    								InetAddress.getByName(Receiver.engine(mContext).getRemoteAddr()),
    								Receiver.engine(mContext).getRemoteVideo());
    					} catch (Exception e) {
    						if (!Sipdroid.release) e.printStackTrace();
    						return;
    					}		
    					while (fis == null) {
    						try {
    							f = new File(mCameraVideoFilename);
    							fis = new FileInputStream(f);
    							fis.skip(0x22);
    						} catch (Exception e) {
    							try {
    								sleep(1000);
    							} catch (InterruptedException e2) {
    								rtp_socket.getDatagramSocket().close();
    								return;
    							}
    						}
    					}
    					rtp_packet.setPayloadType(103);
    					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
    					while (Receiver.listener_video != null) {
    						num = -1;
    						try {
    							num = fis.read(buffer,14+number,frame_size-number);
    						} catch (IOException e) {
    							if (!Sipdroid.release) e.printStackTrace();
    						}
    						if (num < 0) {
    							try {
    								sleep(20);
    								System.out.println("sleep 20");
    							} catch (InterruptedException e) {
    								break;
    							}
    							continue;							
    						}
    						number += num;
    						
    						for (num = 14+number-2; num > 14; num--)
    							if (buffer[num] == 0 && buffer[num+1] == 0) break;
    						if (num == 14) {
    							num = 0;
    							rtp_packet.setMarker(false);
    						} else {	
    							num = 14+number - num;
    							rtp_packet.setMarker(true);
    						}
    						
    			 			rtp_packet.setSequenceNumber(seqn++);
    			 			rtp_packet.setPayloadLength(number-num+2);
    			 			try {
    			 				rtp_socket.send(rtp_packet);
    			 			} catch (IOException e) {
    			 				if (!Sipdroid.release) e.printStackTrace();	
    			 			}
    			 			try {
    			 				if (fis.available() < 24000)
    			 					Thread.sleep((number-num)/24);
							} catch (Exception e) {
								break;
							}
							
    			 			if (num > 0) {
    				 			num -= 2;
    				 			dest = 14;
    				 			src = 14+number - num;
    				 			number = num;
    				 			while (num-- > 0)
    				 				buffer[dest++] = buffer[src++];
    							buffer[12] = 4;
        			 			rtp_packet.setTimestamp(SystemClock.elapsedRealtime()*90);
    			 			} else {
    			 				number = 0;
    							buffer[12] = 0;
    			 			}
    					}
    					rtp_socket.getDatagramSocket().close();
    				}
    			}).start();   
            }
        	
            speakermode = Receiver.engine(this).speaker(AudioManager.MODE_NORMAL);
            RtpStreamSender.delay = 10*1024;
        }
    }

    private void stopVideoRecording() {
        Log.v(TAG, "stopVideoRecording");
        if (mMediaRecorderRecording || mMediaRecorder != null) {
    		Receiver.listener_video = null;
    		t.interrupt();
            Receiver.engine(this).speaker(speakermode);
            RtpStreamSender.delay = 0;

            if (mMediaRecorderRecording && mMediaRecorder != null) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "stop fail: " + e.getMessage());
                }

                mCurrentVideoFilename = mCameraVideoFilename;
                Log.v(TAG, "Setting current video filename: " + mCurrentVideoFilename);
                mMediaRecorderRecording = false;
            }
            releaseMediaRecorder();
            mRecordingTimeView.setVisibility(View.GONE);
        }

        deleteCurrentVideo();
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
}
