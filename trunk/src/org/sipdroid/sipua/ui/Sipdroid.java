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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.pjlib.Codec;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/////////////////////////////////////////////////////////////////////
// this the main activity of Sipdroid
// for modifying it additional terms according to section 7, GPL apply
// see ADDITIONAL_TERMS.txt
/////////////////////////////////////////////////////////////////////
public class Sipdroid extends Activity {

	public static final boolean release = true;
	public static final boolean market = false;

	/* Following the menu item constants which will be used for menu creation */
	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFIGURE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int CALL_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int ABOUT_MENU_ITEM = FIRST_MENU_ID + 3;
	public static final int EXIT_MENU_ITEM = FIRST_MENU_ID + 4;

	private static AlertDialog m_AlertDlg;
	AutoCompleteTextView sip_uri_box;

	private boolean pjlib_initialized = false;
	private String final_so_path = "/data/data/org.sipdroid.sipua/pjlib_linker_jni.so";
	private String linker_library_lock_file = "/cache/pjlib_linker_lock";
	private boolean lib_exists, lock_file_exists;
	
	@Override
	public void onStart() {
		super.onStart();
		
		lock_file_exists = (new File(linker_library_lock_file).exists());
		if (lock_file_exists) { pjlib_initialized = true;}
		
		if (!pjlib_initialized) {
			if (!lib_exists) {
				Log.e("sipdroid", "extracting library: " + final_so_path);
				try {
					ZipFile zip = new ZipFile("/data/app/org.sipdroid.sipua.apk");
					ZipEntry zipen = zip.getEntry("assets/pjlib_linker_jni.so");
					InputStream is = zip.getInputStream(zipen);
					OutputStream os = new FileOutputStream(final_so_path);
					byte[] buf = new byte[8092];
					int n;
					while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
					os.flush();
					os.close();
					is.close();
				} catch (Exception ex) {
					Log.e("sipdroid", "failed to extract library: " + ex);
				}
			}
			
			lib_exists = (new File(final_so_path).exists());
			
			if (!lib_exists) { 
				Log.e("sipdroid", "cannot find pj library");
				return;
			}
			
			Log.e("sipdroid", "loading pjlib linker library...");
			try {
				System.load(final_so_path);
			} catch (Exception ex) {
				Log.e("sipdroid", "System.load failed: " + ex);
			}

			Codec.open("gsm");
			pjlib_initialized = true;
		}
		
		if (!Receiver.engine(this).isRegistered())
			Receiver.engine(this).register();
		if (Receiver.engine(this).isRegistered()) {
		    ContentResolver content = getContentResolver();
		    Cursor cursor = content.query(Calls.CONTENT_URI,
		            PROJECTION, Calls.NUMBER+" like ?", new String[] { "%@%" }, Calls.DEFAULT_SORT_ORDER);
		    CallsAdapter adapter = new CallsAdapter(this, cursor);
		    sip_uri_box.setAdapter(adapter);
		}
	}
	
	public static class CallsAdapter extends CursorAdapter implements Filterable {
	    public CallsAdapter(Context context, Cursor c) {
	        super(context, c);
	        mContent = context.getContentResolver();
	    }
	
	    public View newView(Context context, Cursor cursor, ViewGroup parent) {
	        final LayoutInflater inflater = LayoutInflater.from(context);
	        final TextView view = (TextView) inflater.inflate(
	                android.R.layout.simple_dropdown_item_1line, parent, false);
	        view.setText(cursor.getString(1));
	        return view;
	    }
	
	    @Override
	    public void bindView(View view, Context context, Cursor cursor) {
	        ((TextView) view).setText(cursor.getString(1));
	    }
	
	    @Override
	    public String convertToString(Cursor cursor) {
	        return cursor.getString(1);
	    }
	
	    @Override
	    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
	        if (getFilterQueryProvider() != null) {
	            return getFilterQueryProvider().runQuery(constraint);
	        }
	
	        StringBuilder buffer;
	        String[] args;
	        buffer = new StringBuilder();
	        buffer.append(Calls.NUMBER);
	        buffer.append(" LIKE ?");
	        args = new String[] { (constraint != null && constraint.length() > 0?
	       				constraint.toString() : "%@") + "%"};
	
	        return mContent.query(Calls.CONTENT_URI, PROJECTION,
	                buffer.toString(), args,
	                Calls.DEFAULT_SORT_ORDER);
	    }
	
	    private ContentResolver mContent;        
	}
	
	private static final String[] PROJECTION = new String[] {
        Calls._ID,
        Calls.NUMBER,
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
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
		sip_uri_box.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				call_menu();
			}
		});
		on(this,true);
	}

	public static boolean on(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("on",false);
	}
	
	public static void on(Context context,boolean on) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putBoolean("on",on);
		edit.commit();
        if (on) Receiver.engine(context).isRegistered();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE) Receiver.moveTop();
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem m = menu.add(0, ABOUT_MENU_ITEM, 0, R.string.menu_about);
		m.setIcon(android.R.drawable.ic_menu_info_details);
		m = menu.add(0, EXIT_MENU_ITEM, 0, R.string.menu_exit);
		m.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		m = menu.add(0, CALL_MENU_ITEM, 0, R.string.menu_call);
		m.setIcon(android.R.drawable.ic_menu_call);
		m = menu.add(0, CONFIGURE_MENU_ITEM, 0, R.string.menu_settings);
		m.setIcon(android.R.drawable.ic_menu_preferences);
						
		return result;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {        	
        case KeyEvent.KEYCODE_CALL:
			String target = this.sip_uri_box.getText().toString();
			if (target.length() != 0) {
				call_menu();
				return true;
			}	 
			break;

        case KeyEvent.KEYCODE_BACK:
            moveTaskToBack(true);
            return true;

        }
		return super.onKeyDown(keyCode, event);
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
		else if (!Receiver.engine(this).call(target))
			m_AlertDlg = new AlertDialog.Builder(this)
				.setMessage(R.string.notfast)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon22)
				.setCancelable(true)
				.show();
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
			.setMessage(getString(R.string.about).replace("\\n","\n").replace("${VERSION}", getVersion(this)))
			.setTitle(getString(R.string.menu_about))
			.setIcon(R.drawable.icon22)
			.setCancelable(true)
			.show();
			break;
			
		case CALL_MENU_ITEM: 
			call_menu();
			break;
			
		case EXIT_MENU_ITEM: 
			Receiver.reRegister(0);
			Receiver.engine(this).unregister();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
			}
			on(this,false);
			Receiver.pos(true);
			Receiver.engine(this).halt();
			Receiver.mSipdroidEngine = null;
			stopService(new Intent(this,RegisterService.class));
			finish();
			break;
			
		case CONFIGURE_MENU_ITEM: {
			try {
				intent = new Intent(this, org.sipdroid.sipua.ui.Settings.class);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
			}
		}
			break;
		}

		return result;
	}
	
	public static String getVersion() {
		return getVersion(Receiver.mContext);
	}
	
	public static String getVersion(Context context) {
		final String unknown = "Unknown";
		
		if (context == null) {
			return unknown;
		}
		
		try {
			return context.getPackageManager()
				   .getPackageInfo(context.getPackageName(), 0)
				   .versionName;
		} catch(NameNotFoundException ex) {}
		
		return unknown;		
	}
}
