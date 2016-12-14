package org.sipdroid.sipua.ui;

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

import java.util.HashMap;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.CallCard;
import org.sipdroid.sipua.phone.Phone;
import org.sipdroid.sipua.phone.SlidingCardManager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

public class InCallScreen extends CallScreen implements View.OnClickListener, SensorEventListener {

	final int MSG_ANSWER = 1;
	final int MSG_ANSWER_SPEAKER = 2;
	final int MSG_BACK = 3;
	final int MSG_TICK = 4;
	final int MSG_POPUP = 5;
	final int MSG_ACCEPT = 6;
	final int MSG_ACCEPT_FORCE = 7;
	
	final int SCREEN_OFF_TIMEOUT = 12000;
	
	CallCard mCallCard;
	Phone ccPhone;
	int oldtimeout;
	SensorManager sensorManager;
    Sensor proximitySensor;
    boolean first;
	
	void screenOff(boolean off) {
        ContentResolver cr = getContentResolver();
        
        if (proximitySensor != null)
        	return;
        if (off) {
        	if (oldtimeout == 0) {
        		oldtimeout = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000);
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT);
        	}
        } else {
        	if (oldtimeout == 0 && Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000) == SCREEN_OFF_TIMEOUT)
        		oldtimeout = 60000;
        	if (oldtimeout != 0) {
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, oldtimeout);
        		oldtimeout = 0;
        	}
        }
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mHandler.removeMessages(MSG_BACK);
		mHandler.removeMessages(MSG_ACCEPT);
		mHandler.sendEmptyMessageDelayed(MSG_ACCEPT_FORCE, 1000);
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
			finish();
		sensorManager.unregisterListener(this);
		started = false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mHandler.removeMessages(MSG_ACCEPT_FORCE);
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
     		mHandler.sendEmptyMessageDelayed(MSG_BACK, Receiver.call_end_reason == -1?
    				2000:5000);
	    first = true;
	    pactive = false;
	    pactivetime = SystemClock.elapsedRealtime();
	    sensorManager.registerListener(this,proximitySensor,SensorManager.SENSOR_DELAY_NORMAL);
	    started = true;
	}

	@Override
	public void onPause() {
		super.onPause();
    	if (!Sipdroid.release) Log.i("SipUA:","on pause");
    	switch (Receiver.call_state) {
    	case UserAgent.UA_STATE_INCOMING_CALL:
//    		if (!RtpStreamReceiver.isBluetoothAvailable()) Receiver.moveTop();
    		break;
    	case UserAgent.UA_STATE_IDLE:
    		if (Receiver.ccCall != null)
    			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
     		mHandler.sendEmptyMessageDelayed(MSG_BACK, Receiver.call_end_reason == -1?
    				2000:5000);
    		break;
    	}
		if (t != null) {
			running = false;
			t.interrupt();
		}
		screenOff(false);
		if (mCallCard.mElapsedTime != null) mCallCard.mElapsedTime.stop();
	}
	
	void moveBack() {
		if (Receiver.ccConn != null && !Receiver.ccConn.isIncoming() && Integer.parseInt(Build.VERSION.SDK) < 25) {
			// after an outgoing call don't fall back to the contact
			// or call log because it is too easy to dial accidentally from there
	        startActivity(Receiver.createHomeIntent());
		}
		onStop();
	}
	
	Context mContext = this;

	@Override
	public void onResume() {
		super.onResume();
    	if (!Sipdroid.release) Log.i("SipUA:","on resume");
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE"))
				if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ON, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ON) &&
						!mKeyguardManager.inKeyguardRestrictedInputMode())
					mHandler.sendEmptyMessageDelayed(MSG_ANSWER, 1000);
				else if ((PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ONDEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ONDEMAND) &&
						PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_DEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_DEMAND)) ||
						(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_HEADSET, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_HEADSET) &&
								Receiver.headset > 0))
					mHandler.sendEmptyMessageDelayed(MSG_ANSWER_SPEAKER, 10000);
			break;
		case UserAgent.UA_STATE_INCALL:
			mDialerDrawer.close();
			mDialerDrawer.setVisibility(View.VISIBLE);
			if (Receiver.docked <= 0)
				screenOff(true);
			break;
		case UserAgent.UA_STATE_IDLE:
			if (!mHandler.hasMessages(MSG_BACK))
				moveBack();
			break;
		}
		if (Receiver.call_state != UserAgent.UA_STATE_INCALL) {
			mDialerDrawer.close();
			mDialerDrawer.setVisibility(View.GONE);
		}
		if (Receiver.ccCall != null) mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
        if (mSlidingCardManager != null) mSlidingCardManager.showPopup();
		mHandler.sendEmptyMessage(MSG_TICK);
		mHandler.sendEmptyMessage(MSG_POPUP);
	    if (t == null && Receiver.call_state != UserAgent.UA_STATE_IDLE) {
			mDigits.setText("");
			running = true;
	        (t = new Thread() {
				public void run() {
					int len = 0;
					long time;
					ToneGenerator tg = null;
	
					if (Settings.System.getInt(getContentResolver(),
							Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1)
						tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, (int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
					for (;;) {
						if (!running) {
							t = null;
							break;
						}
						if (len != mDigits.getText().length()) {
							time = SystemClock.elapsedRealtime();
							if (tg != null) tg.startTone(mToneMap.get(mDigits.getText().charAt(len)));
							Receiver.engine(Receiver.mContext).info(mDigits.getText().charAt(len++),250);
							time = 250-(SystemClock.elapsedRealtime()-time);
							try {
								if (time > 0) sleep(time);
							} catch (InterruptedException e) {
							}
							if (tg != null) tg.stopTone();
							try {
								if (running) sleep(250);
							} catch (InterruptedException e) {
							}
							continue;
						}
						mHandler.sendEmptyMessage(MSG_TICK);
						try {
							sleep(1000);
						} catch (InterruptedException e) {
						}
					}
					if (tg != null) tg.release();
				}
			}).start();
	    }
	}
	
    Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case MSG_ANSWER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
        			answer();
        		break;
    		case MSG_ANSWER_SPEAKER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        			answer();
    				Receiver.engine(mContext).speaker(AudioManager.MODE_NORMAL);
        		}
        		break;
    		case MSG_BACK:
    			moveBack();
    			break;
    		case MSG_TICK:
    			mCodec.setText(RtpStreamReceiver.getCodec());
    			if (RtpStreamReceiver.good != 0) {
    				if (RtpStreamReceiver.timeout != 0)
    					mStats.setText("no data");
    				else if (RtpStreamSender.m > 1)
	    				mStats.setText(Math.round(RtpStreamReceiver.loss/RtpStreamReceiver.good*100)+"%loss, "+
	    						Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
	    						Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
	    						(RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
    				else
	    				mStats.setText(Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
	    						Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
	    						(RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
    				mStats.setVisibility(View.VISIBLE);
    			} else
    				mStats.setVisibility(View.GONE);
    			break;
    		case MSG_POPUP:
    	        if (mSlidingCardManager != null) mSlidingCardManager.showPopup();
    			break;
    		case MSG_ACCEPT:
    		case MSG_ACCEPT_FORCE:
    	        setScreenBacklight((float) -1);
    	        getWindow().setFlags(0, 
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	        if (mDialerDrawer != null) {
					mDialerDrawer.close();
					mDialerDrawer.setVisibility(View.VISIBLE);
    	        }
				ContentResolver cr = getContentResolver();
    			if (hapticset) {
    				Settings.System.putInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, haptic);
    				hapticset = false;
    			}
    			break;
    		}
    	}
    };

	ViewGroup mInCallPanel,mMainFrame;
	SlidingDrawer mDialerDrawer;
	public static SlidingCardManager mSlidingCardManager;
	TextView mStats;
	TextView mCodec;
	
    public void initInCallScreen() {
        mInCallPanel = (ViewGroup) findViewById(R.id.inCallPanel);
        mMainFrame = (ViewGroup) findViewById(R.id.mainFrame);
        View callCardLayout = getLayoutInflater().inflate(
                    R.layout.call_card_popup,
                    mInCallPanel);
        mCallCard = (CallCard) callCardLayout.findViewById(R.id.callCard);
        mCallCard.reset();

        mSlidingCardManager = new SlidingCardManager();
        mSlidingCardManager.init(ccPhone, this, mMainFrame);
        SlidingCardManager.WindowAttachNotifierView wanv =
            new SlidingCardManager.WindowAttachNotifierView(this);
	    wanv.setSlidingCardManager(mSlidingCardManager);
	    wanv.setVisibility(View.GONE);
	    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(0, 0);
	    mMainFrame.addView(wanv, lp);

	    mStats = (TextView) findViewById(R.id.stats);
	    mCodec = (TextView) findViewById(R.id.codec);
        mDialerDrawer = (SlidingDrawer) findViewById(R.id.dialer_container);
        mCallCard.displayOnHoldCallStatus(ccPhone,null);
        mCallCard.displayOngoingCallStatus(ccPhone,null);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        	mCallCard.updateForLandscapeMode();
        
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

	    mDigits = (EditText) findViewById(R.id.digits);
        mDisplayMap.put(R.id.one, '1');
        mDisplayMap.put(R.id.two, '2');
        mDisplayMap.put(R.id.three, '3');
        mDisplayMap.put(R.id.four, '4');
        mDisplayMap.put(R.id.five, '5');
        mDisplayMap.put(R.id.six, '6');
        mDisplayMap.put(R.id.seven, '7');
        mDisplayMap.put(R.id.eight, '8');
        mDisplayMap.put(R.id.nine, '9');
        mDisplayMap.put(R.id.zero, '0');
        mDisplayMap.put(R.id.pound, '#');
        mDisplayMap.put(R.id.star, '*');
        
        mToneMap.put('1', ToneGenerator.TONE_DTMF_1);
        mToneMap.put('2', ToneGenerator.TONE_DTMF_2);
        mToneMap.put('3', ToneGenerator.TONE_DTMF_3);
        mToneMap.put('4', ToneGenerator.TONE_DTMF_4);
        mToneMap.put('5', ToneGenerator.TONE_DTMF_5);
        mToneMap.put('6', ToneGenerator.TONE_DTMF_6);
        mToneMap.put('7', ToneGenerator.TONE_DTMF_7);
        mToneMap.put('8', ToneGenerator.TONE_DTMF_8);
        mToneMap.put('9', ToneGenerator.TONE_DTMF_9);
        mToneMap.put('0', ToneGenerator.TONE_DTMF_0);
        mToneMap.put('#', ToneGenerator.TONE_DTMF_P);
        mToneMap.put('*', ToneGenerator.TONE_DTMF_S);

        View button;
        for (int viewId : mDisplayMap.keySet()) {
            button = findViewById(viewId);
            button.setOnClickListener(this);
        }
    }
    
	Thread t;
	EditText mDigits;
	boolean running;
	public static boolean started;
    private static final HashMap<Integer, Character> mDisplayMap =
        new HashMap<Integer, Character>();
    private static final HashMap<Character, Integer> mToneMap =
        new HashMap<Character, Integer>();
    
	public void onClick(View v) {
        int viewId = v.getId();

        // if the button is recognized
        if (mDisplayMap.containsKey(viewId)) {
                    appendDigit(mDisplayMap.get(viewId));
        }
    }

    void appendDigit(final char c) {
        mDigits.getText().append(c);
    }

    @Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.incall);
		
		initInCallScreen();
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(!android.os.Build.BRAND.equalsIgnoreCase("archos"))
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }
		
	public void reject() {
		if (Receiver.ccCall != null) {
			Receiver.stopRingtone();
			Receiver.ccCall.setState(Call.State.DISCONNECTED);
			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
			mDialerDrawer.close();
			mDialerDrawer.setVisibility(View.GONE);
	        if (mSlidingCardManager != null)
	        	mSlidingCardManager.showPopup();
		}
        (new Thread() {
			public void run() {
        		Receiver.engine(mContext).rejectcall();
			}
		}).start();   	
    }
	
	public void answer() {
        (new Thread() {
			public void run() {
				Receiver.engine(mContext).answercall();
			}
		}).start();   
		if (Receiver.ccCall != null) {
			Receiver.ccCall.setState(Call.State.ACTIVE);
			Receiver.ccCall.base = SystemClock.elapsedRealtime();
			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
			mDialerDrawer.setVisibility(View.VISIBLE);
	        if (mSlidingCardManager != null)
	        	mSlidingCardManager.showPopup();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL && mSlidingCardManager == null) {
        		answer();
				return true;
        	}
        	break;
        
        case KeyEvent.KEYCODE_CALL:
        	switch (Receiver.call_state) {
        	case UserAgent.UA_STATE_INCOMING_CALL:
        		answer();
        		break;
        	case UserAgent.UA_STATE_INCALL:
        	case UserAgent.UA_STATE_HOLD:
       			Receiver.engine(this).togglehold();
       			break;
        	}
            // consume KEYCODE_CALL so PhoneWindow doesn't do anything with it
            return true;

        case KeyEvent.KEYCODE_BACK:
        	if (mDialerDrawer.isOpened())
        		mDialerDrawer.animateClose();
            return true;

        case KeyEvent.KEYCODE_CAMERA:
            // Disable the CAMERA button while in-call since it's too
            // easy to press accidentally.
        	return true;
        	
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        		Receiver.stopRingtone();
        		return true;
        	}
        	RtpStreamReceiver.adjust(keyCode,true,!(pactive || SystemClock.elapsedRealtime()-pactivetime < 1000));
        	return true;
        }
        if (Receiver.call_state == UserAgent.UA_STATE_INCALL) {
	        char number = event.getNumber();
	        if (Character.isDigit(number) || number == '*' || number == '#') {
	        	appendDigit(number);
	        	return true;
	        }
        }
        return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem m = menu.add(0, DTMF_MENU_ITEM, 0, R.string.menu_dtmf);
		m.setIcon(R.drawable.ic_menu_dial_pad);
		return result;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		menu.findItem(DTMF_MENU_ITEM).setVisible(Receiver.call_state == UserAgent.UA_STATE_INCALL);
		return !(pactive || SystemClock.elapsedRealtime()-pactivetime < 1000);
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DTMF_MENU_ITEM:
			mDialerDrawer.animateOpen();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	RtpStreamReceiver.adjust(keyCode,false,!(pactive || SystemClock.elapsedRealtime()-pactivetime < 1000));
        	return true;
        case KeyEvent.KEYCODE_ENDCALL:
        	if (Receiver.pstn_state == null ||
				(Receiver.pstn_state.equals("IDLE") && (SystemClock.elapsedRealtime()-Receiver.pstn_time) > 3000)) {
        			reject();      
        			return true;		
        	}
        	break;
		}
		Receiver.pstn_time = 0;
		return false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	void setScreenBacklight(float a) {
        WindowManager.LayoutParams lp = getWindow().getAttributes(); 
        lp.screenBrightness = a; 
        getWindow().setAttributes(lp);		
	}

	static final float PROXIMITY_THRESHOLD = 5.0f;
	public static boolean pactive;
	public static long pactivetime;
	static int haptic;
	static boolean hapticset;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		boolean keepon = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_KEEPON, org.sipdroid.sipua.ui.Settings.DEFAULT_KEEPON);
		if (first) {
			first = false;
			return;
		}
		float distance = event.values[0];
        boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
		if (!keepon ||
				Receiver.call_state == UserAgent.UA_STATE_HOLD)
			active = false;
        pactive = active;
        pactivetime = SystemClock.elapsedRealtime();
        if (!active) {
     		mHandler.sendEmptyMessageDelayed(MSG_ACCEPT, 1000);
     		return;
        }
        mHandler.removeMessages(MSG_ACCEPT);
        setScreenBacklight((float) 0.1);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        closeOptionsMenu();
		mDialerDrawer.close();
		mDialerDrawer.setVisibility(View.GONE);
        ContentResolver cr = getContentResolver();
		if (!hapticset) {
			haptic = Settings.System.getInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
			hapticset = true;
		}
		Settings.System.putInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
	}
}
