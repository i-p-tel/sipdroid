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

package org.sipdroid.sipua.ui;

import org.sipdroid.media.G711;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.CallCard;
import org.sipdroid.sipua.phone.Phone;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class Sipdroid extends Activity {

	public static final boolean release = true;
	public static final boolean market = false;

	Phone ccPhone;
	
	/* Following the menu item constants which will be used for menu creation */
	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFIGURE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int CALL_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int HANG_UP_MENU_ITEM = FIRST_MENU_ID + 3;
	public static final int HOLD_MENU_ITEM = FIRST_MENU_ID + 4;
	public static final int MUTE_MENU_ITEM = FIRST_MENU_ID + 5;
	public static final int ABOUT_MENU_ITEM = FIRST_MENU_ID + 6;
	public static final int DTMF_MENU_ITEM = FIRST_MENU_ID + 7;

	private static Receiver m_receiver;
	private static AlertDialog m_AlertDlg;
	AutoCompleteTextView sip_uri_box;
	CallCard mCallCard;

	public Context getUIContext() {
		return this;
	}

	public void onDestroy() {
		super.onDestroy();
		if (m_receiver != null) {
			unregisterReceiver(m_receiver);
			m_receiver = null;
		}
		Receiver.reRegister(0);
//		mContext = null;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Receiver.isTop = false;
    	if (!release) Log.i("SipUA:","on pause");
		if (Receiver.keepTop) Receiver.moveTop();
	}
	
	void moveBack() {
		if (!Receiver.ccConn.isIncoming() || Receiver.ccCall.base != 0) {
	        Intent intent = new Intent(Intent.ACTION_VIEW, null);
	        intent.setType("vnd.android.cursor.dir/calls");
	        startActivity(intent);
		}
		moveTaskToBack(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCALL:
			callCardMenuButtonHint.setVisibility(View.VISIBLE);
		default:
			inCall(true);
			break;
		case UserAgent.UA_STATE_IDLE:
			callCardMenuButtonHint.setVisibility(View.INVISIBLE);
			if (Receiver.ccConn != null && Receiver.ccConn.date != 0) {
		        (new Thread() {
					public void run() {
						try {
							sleep(2000);
						} catch (InterruptedException e) {
						}
						moveBack();
					}
				}).start();   
			} else if (Receiver.isTop) {
				moveBack();
			} else
				inCall(false);
			break;
		}
		Receiver.isTop = true;
		if (Receiver.ccCall != null) mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
	}
	
	ViewGroup mInCallPanel;
	TextView callCardMenuButtonHint;
	
    public void initInCallScreen() {
        mInCallPanel = (ViewGroup) findViewById(R.id.inCallPanel);
        View callCardLayout = getLayoutInflater().inflate(
                    R.layout.call_card_popup,
                    mInCallPanel);
        mCallCard = (CallCard) callCardLayout.findViewById(R.id.callCard);
        mCallCard.reset();
        callCardMenuButtonHint = mCallCard.getMenuButtonHint();
        mCallCard.displayOnHoldCallStatus(ccPhone,null);
        mCallCard.displayOngoingCallStatus(ccPhone,null);
        
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
    }
    
    void inCall(boolean incall) {
    	if (incall) {
    		mInCallPanel.setVisibility(View.VISIBLE);
    		findViewById(R.id.sec_lvl_layout).setVisibility(View.INVISIBLE);
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    	} else {
       		mInCallPanel.setVisibility(View.INVISIBLE);
    		findViewById(R.id.sec_lvl_layout).setVisibility(View.VISIBLE);
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    	}
    }
    
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		G711.init();
        Receiver.engine(this).register();
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sipdroid);
		sip_uri_box = (AutoCompleteTextView) findViewById(R.id.txt_callee);
		sip_uri_box.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN &&
		        		keyCode == KeyEvent.KEYCODE_ENTER) {
		          call_menu();
		          return true;
		        }
		        return false;
		    }
		});
		initInCallScreen();

		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentfilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(m_receiver = new Receiver(), intentfilter);           
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem m = menu.add(0, ABOUT_MENU_ITEM, 0, R.string.menu_about);
		m.setIcon(R.drawable.icon32);
		m = menu.add(0, CALL_MENU_ITEM, 0, R.string.menu_call);
		m.setIcon(R.drawable.sym_call);
		m = menu.add(0, CONFIGURE_MENU_ITEM, 0, R.string.menu_settings);
		m.setIcon(R.drawable.configure);
		
		m = menu.add(0, HOLD_MENU_ITEM, 0, R.string.menu_hold);
		m.setIcon(R.drawable.sym_call_hold_on);
		m = menu.add(0, HANG_UP_MENU_ITEM, 0, R.string.menu_endCall);
		m.setIcon(R.drawable.sym_call_end);
		m = menu.add(0, MUTE_MENU_ITEM, 0, R.string.menu_mute);
		m.setIcon(R.drawable.mute);
		m = menu.add(0, DTMF_MENU_ITEM, 0, R.string.menu_dtmf);
		m.setIcon(R.drawable.sym_incoming_call_answer_options);
				
		return result;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		if (Receiver.call_state == UserAgent.UA_STATE_INCALL || Receiver.call_state == UserAgent.UA_STATE_HOLD) callCardMenuButtonHint.setVisibility(View.VISIBLE);	
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		callCardMenuButtonHint.setVisibility(View.INVISIBLE);
			if (Receiver.call_state != UserAgent.UA_STATE_IDLE) 
			{
				menu.findItem(CALL_MENU_ITEM).setVisible(false);
				menu.findItem(HANG_UP_MENU_ITEM).setVisible(true);
				if (Receiver.call_state == UserAgent.UA_STATE_INCALL || Receiver.call_state == UserAgent.UA_STATE_HOLD) {
					menu.findItem(HOLD_MENU_ITEM).setVisible(true);
					menu.findItem(MUTE_MENU_ITEM).setVisible(true);
					menu.findItem(DTMF_MENU_ITEM).setVisible(true);
				} else {
					menu.findItem(HOLD_MENU_ITEM).setVisible(false);
					menu.findItem(MUTE_MENU_ITEM).setVisible(false);
					menu.findItem(DTMF_MENU_ITEM).setVisible(false);
				}
				menu.findItem(CONFIGURE_MENU_ITEM).setVisible(false);
				menu.findItem(ABOUT_MENU_ITEM).setVisible(false);
			} 
			else 
			{
				menu.findItem(CALL_MENU_ITEM).setVisible(true);
				menu.findItem(HANG_UP_MENU_ITEM).setVisible(false);
				menu.findItem(HOLD_MENU_ITEM).setVisible(false);
				menu.findItem(MUTE_MENU_ITEM).setVisible(false);
				menu.findItem(CONFIGURE_MENU_ITEM).setVisible(true);
				menu.findItem(ABOUT_MENU_ITEM).setVisible(true);
				menu.findItem(DTMF_MENU_ITEM).setVisible(false);
			}
		
		return result;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        		Receiver.engine(this).answercall();
        		return true;
        	}
        	break;
        	
        case KeyEvent.KEYCODE_CALL:
        	switch (Receiver.call_state) {
        	case UserAgent.UA_STATE_INCOMING_CALL: // does not come thru any more
        		Receiver.engine(this).answercall();
        		return true;
        	case UserAgent.UA_STATE_INCALL:
        	case UserAgent.UA_STATE_HOLD:
       			Receiver.engine(this).togglehold();
       			return true;
        	case UserAgent.UA_STATE_IDLE:
				String target = this.sip_uri_box.getText().toString();
				if (target.length() != 0) {
					Receiver.engine(this).call(target);
					return true;
				}	 
				break;
        	default:
	            // consume KEYCODE_CALL so PhoneWindow doesn't do anything with it
	            return true;
        	}
        	break;

        // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
        // The standard system-wide handling of the ENDCALL key
        // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
        // already implements exactly what the UI spec wants,
        // namely (1) "hang up" if there's a current active call,
        // or (2) "don't answer" if there's a current ringing call.

        case KeyEvent.KEYCODE_BACK:
        	if (!release) Log.i("SipUA:","keycode back "+(SystemClock.uptimeMillis()-event.getEventTime()));
            if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
            	moveTaskToBack(true);
            else
    			Receiver.engine(this).rejectcall();      
            return true;

        case KeyEvent.KEYCODE_CAMERA:
            // Disable the CAMERA button while in-call since it's too
            // easy to press accidentally.
        	if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
        		return true;
        	break;
    }
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	void call_menu()
	{
		String target = this.sip_uri_box.getText().toString();
		if (m_AlertDlg != null) 
		{
			m_AlertDlg.cancel();
		}
		if (target.length() == 0)
			m_AlertDlg = new AlertDialog.Builder(this)
				.setMessage(R.string.empty)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon22)
				.setCancelable(true)
				.show();
		else if (!Receiver.isFast())
			m_AlertDlg = new AlertDialog.Builder(this)
				.setMessage(R.string.notfast)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon22)
				.setCancelable(true)
				.show();
		else
			Receiver.engine(this).call(target);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
		Intent intent = null;

		switch (item.getItemId()) {
		case ABOUT_MENU_ITEM:
			if (m_AlertDlg != null) 
			{
				m_AlertDlg.cancel();
			}
			m_AlertDlg = new AlertDialog.Builder(this)
			.setMessage(getString(R.string.about).replace("\\n","\n"))
			.setTitle(getString(R.string.menu_about))
			.setIcon(R.drawable.icon22)
			.setCancelable(true)
			.show();
			break;
			
		case CALL_MENU_ITEM: 
			call_menu();
			break;
			
		case HANG_UP_MENU_ITEM:
			Receiver.engine(this).rejectcall();
			break;
			
		case HOLD_MENU_ITEM:
			Receiver.engine(this).togglehold();
			break;
			
		case MUTE_MENU_ITEM:
			Receiver.engine(this).togglemute();
			break;
					
		case CONFIGURE_MENU_ITEM: {
			try {
				intent = new Intent(this, org.sipdroid.sipua.ui.Settings.class);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
			}
		}
			break;
		case DTMF_MENU_ITEM: {
			try {
				intent = new Intent(this, org.sipdroid.sipua.ui.DTMF.class);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
			}
		}
			break;
		}

		return result;
	}
	
}