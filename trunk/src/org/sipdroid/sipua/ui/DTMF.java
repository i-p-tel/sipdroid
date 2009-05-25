package org.sipdroid.sipua.ui;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2006 The Android Open Source Project
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

import org.sipdroid.sipua.R;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class DTMF extends CallScreen implements SipdroidListener,View.OnClickListener {
	Thread t;
	
	public void onHangup() {
		finish();
	}
	
	EditText mDigits;
    private static final HashMap<Integer, Character> mDisplayMap =
        new HashMap<Integer, Character>();
	
	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		Receiver.screenOff(false);
		setContentView(R.layout.dtmf_twelve_key_dialer);
		Receiver.listener = this;
	    mDigits = (EditText) findViewById(R.id.digits);
	    mDigits.setText("");

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

        View button;
        for (int viewId : mDisplayMap.keySet()) {
            button = findViewById(viewId);
            button.setOnClickListener(this);
        }
        
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        
        (t = new Thread() {
			public void run() {
				int len = 0;
				for (;;) {
					if (len != mDigits.getText().length()) {
						Receiver.engine(Receiver.mContext).info(mDigits.getText().charAt(len++));
						continue;
					}
					if (Receiver.listener == null) break;
					try {
						sleep(10000);
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();   
	}

	public void onDestroy() {
		super.onDestroy();
		Receiver.listener = null;
		t.interrupt();
    	Receiver.screenOff(true);
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

		menu.findItem(DTMF_MENU_ITEM).setVisible(false);
		menu.findItem(VIDEO_MENU_ITEM).setVisible(Receiver.engine(this).getRemoteVideo() != 0);
		
		return result;
	}

	int speakermode;
	
    @Override
    protected void onResume() {
        super.onResume();
        speakermode = Receiver.engine(this).speaker(AudioManager.MODE_NORMAL);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Receiver.engine(this).speaker(speakermode);
    }

	public void onClick(View v) {
        int viewId = v.getId();

        // if the button is recognized
        if (mDisplayMap.containsKey(viewId)) {
                    appendDigit(mDisplayMap.get(viewId));
        }
    }

    void appendDigit(final char c) {
        mDigits.getText().append(c);
        t.interrupt();
    }
}
