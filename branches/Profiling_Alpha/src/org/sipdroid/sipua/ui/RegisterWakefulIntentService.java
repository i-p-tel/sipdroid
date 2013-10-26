package org.sipdroid.sipua.ui;

import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class RegisterWakefulIntentService extends WakefulIntentService {

	public RegisterWakefulIntentService() {
		super("RegisterWakefulIntentService");
	}	

	@Override
	protected void doWakefulWork(Intent intent) {
		Receiver.engine(this).register();
	}

}
