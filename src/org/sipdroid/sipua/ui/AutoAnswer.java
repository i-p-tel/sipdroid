package org.sipdroid.sipua.ui;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class AutoAnswer extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		
		edit.putBoolean("auto_demand", 
				!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("auto_demand", false));
		edit.commit();
		Receiver.updateAutoAnswer();
		finish();
	}
}
