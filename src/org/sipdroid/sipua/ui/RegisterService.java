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

package org.sipdroid.sipua.ui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RegisterService extends Service {
	public static boolean hold;
	Thread t;
	
    @Override
    public void onCreate() {
    	super.onCreate();
        if (Receiver.mSipdroidEngine == null) Receiver.engine(this).register();
        (t = new Thread() {
    		public void run() {
    	    	hold = true;
    			while (hold) {
    				hold = false;
    				try {
    					sleep(45000);
    				} catch (InterruptedException e) {
    				}
    			}
    			stopSelf();
    		}
    	}).start();   
    }
    
    @Override
    public void onStart(Intent intent, int id) {
         super.onStart(intent,id);
         hold = true;
         t.interrupt();
    }

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onDestroy() {
		super.onDestroy();
		hold = false;
		t.interrupt();
	}
	
}
