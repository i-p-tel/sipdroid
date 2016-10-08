package org.sipdroid.sipua.ui;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;

public class SettingsNew {
	static void ignoreBattery(Context context) {
		try {
			Intent intent = new Intent();
			String packageName = context.getPackageName();
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			Method m = pm.getClass().getMethod("isIgnoringBatteryOptimizations",new Class[] { String.class });
			
			if (!(Boolean)m.invoke(pm,packageName)) {
			    intent.setAction("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
			    intent.setData(Uri.parse("package:" + packageName));
			}
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
