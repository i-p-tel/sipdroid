package org.sipdroid.sipua.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.sipdroid.sipua.UserAgentProfile;
import org.zoolu.tools.Random;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class Checkin {
	
	static long hold;
	static int createButton;
	
	static void url(final String opt,final boolean in_call) {
        (new Thread() {
			public void run() {
				try {
			        URL url = new URL(opt);
			        String line;
			        String[] lines;
			        BufferedReader in;
			        
			        if (!in_call)
						try {
							sleep(3000);
						} catch (InterruptedException e) {
						}
					in = new BufferedReader(new InputStreamReader(url.openStream()));
					for (;;) {
						line = in.readLine();
						if (line == null) break;
						lines = line.split(" ");
						if (lines.length == 2) {
							if (lines[0].equals("createButton"))
								createButton = Integer.valueOf(lines[1]);
							else {
								int i = 0;
								for (UserAgentProfile user_profile : Receiver.engine(Receiver.mContext).user_profiles) {
									if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Settings.PREF_DNS+i, Settings.DEFAULT_DNS).equals(lines[0]) ||
											(user_profile != null && user_profile.realm != null &&
													user_profile.realm.contains(lines[0]))) {
										if (in_call) {
											hold = SystemClock.elapsedRealtime();
											Receiver.engine(Receiver.mContext).rejectcall();
										}
										Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lines[1]));
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										Receiver.mContext.startActivity(intent);
									}
									i++;
								}
							}
						}
					}
			        in.close();
				} catch (Exception e) {
					if (!Sipdroid.release) e.printStackTrace();
				}

			}
		}).start();   
	}

	public static void checkin(boolean in_call) {
		if (!in_call || SystemClock.elapsedRealtime() < hold + 5*60*1000 ||
				Random.nextInt(5) == 1)
			url("http://sipdroid.googlecode.com/svn/images/checkin",in_call);
	}
}
