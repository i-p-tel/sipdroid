/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.RegisterAgent;
import org.sipdroid.sipua.SipdroidEngine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CreateAccount extends Dialog {

	Context mContext;
	
	public CreateAccount(Context context) {
		super(context);
		mContext = context;
	}
	
	static String email,trunkserver,trunkuser,trunkpassword,trunkport;
	
	public static String isPossible(Context context) {
		Boolean found = false;
		email = trunkserver = null;
	   	for (int i = 0; i < SipdroidEngine.LINES; i++) {
	   		String j = (i!=0?""+i:"");
	   		String username = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_USERNAME+j, Settings.DEFAULT_USERNAME),
	   			server = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_SERVER+j, Settings.DEFAULT_SERVER);
	   		if (username.equals("") || server.equals(""))
	   			continue;
	   		if (server.contains("pbxes"))
	   			found = true;
	   		else if (i == 0 &&
	   				!PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_PROTOCOL+j, Settings.DEFAULT_PROTOCOL).equals("tcp") &&
	   				PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_3G+j, Settings.DEFAULT_3G) &&
	   				Receiver.engine(context).isRegistered(i) &&
	   				Receiver.engine(context).ras[i].CurrentState == RegisterAgent.REGISTERED) {
	   			trunkserver = server;
	   			trunkuser = username;
	   			trunkpassword = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_PASSWORD+j, Settings.DEFAULT_PASSWORD);
	   			trunkport = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_PORT+j, Settings.DEFAULT_PORT);
	   		}
	   	}
	   	if (found) return null;
        Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
        for (Account account : accounts) {
      	  email = account.name;
      	  break;
        }
        if (email == null) return null;
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setPackage("com.google.android.apps.googlevoice");
		intent.setData(Uri.fromParts("smsto", "", null));
		List<ResolveInfo> a = context.getPackageManager().queryIntentActivities(intent,PackageManager.GET_INTENT_FILTERS);
		if (a != null && a.size() != 0) {
			trunkserver = null;
			return context.getString(R.string.menu_create);
		}
		if (trunkserver != null)
			return "New PBX linked to "+trunkserver;
        return null;
	}
	
	String line;
	
    Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
			Toast.makeText(mContext, line, Toast.LENGTH_LONG).show();
			buttonCancel.setEnabled(true);
			buttonOK.setEnabled(true);
			setCancelable(true);
    	}
    };

    String generatePassword(int length)
	{
	    String availableCharacters = "";
	    String password = "";
	    
	    // Generate the appropriate character set
	    availableCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	    availableCharacters = availableCharacters + "0123456789";
	    
	    // Generate the random number generator
	    Random selector = new Random();
	    
	    // Generate the password
	    int i;
	    for(i = 0; i < length; i++)
	    {
	            password = password + availableCharacters.charAt(selector.nextInt(availableCharacters.length() - 1));
	    }
	    
	    return password;
	}

	void CreateAccountNow() {
		buttonCancel.setEnabled(false);
		buttonOK.setEnabled(false);
		setCancelable(false);
		Toast.makeText(mContext, "Please stand by while your account is being created", Toast.LENGTH_LONG).show();
        (new Thread() {
			public void run() {
				line = "Can't connect to webserver";
				try {
					String password = generatePassword(8);
					String language = Locale.getDefault().toString().substring(0,2);
					if (!language.equals("de") && !language.equals("es") && !language.equals("fr") &&
							!language.equals("it") && !language.equals("ru"))
						if (language.equals("ja"))
							language = "jp";
						else if (language.equals("zh"))
							language = "cn";
						else
							language = "en";
					String s = "https://www1.pbxes.com/config.php?m=register&a=update&f=action&username="+Uri.encode(etName.getText().toString())+"&password="
	        			+Uri.encode(etPass.getText().toString())+"&password_confirm="+Uri.encode(etConfirm.getText().toString())+"&language="+language+"&email="+Uri.encode(email)+"&land="+Uri.encode(Time.getCurrentTimezone())+
	        			"&sipdroid="+Uri.encode(password);
					if (trunkserver != null) {
						s = s+"&trunkserver="+Uri.encode(trunkserver+":"+trunkport)+
							"&trunkuser="+Uri.encode(trunkuser);
					}
			        URL url = new URL(s);
			        BufferedReader in;
					in = new BufferedReader(new InputStreamReader(url.openStream()));
					line = in.readLine();
					if (line == null) {
						in = new BufferedReader(new InputStreamReader(url.openStream()));
						line = in.readLine();
					}
					if (line != null) {
						if (line.equals("OK")) {
							Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
							edit.putString(Settings.PREF_SERVER, Settings.DEFAULT_SERVER);
							edit.putString(Settings.PREF_USERNAME, etName.getText()+"-200");
							edit.putString(Settings.PREF_DOMAIN, Settings.DEFAULT_DOMAIN);
							edit.putString(Settings.PREF_FROMUSER, Settings.DEFAULT_FROMUSER);
							edit.putString(Settings.PREF_PORT, "5061");
							edit.putString(Settings.PREF_PROTOCOL, "tcp");
							edit.putString(Settings.PREF_PASSWORD, password);
							edit.commit();
				        	Receiver.engine(mContext).updateDNS();
				       		Receiver.engine(mContext).halt();
				   			Receiver.engine(mContext).StartEngine();
							dismiss();
						}
					}
			        in.close();
				} catch (IOException e) {
					if (!Sipdroid.release) e.printStackTrace();
				}
				mHandler.sendEmptyMessage(0);
			}
		}).start();   
	}

	EditText etName,etPass,etConfirm;
	TextView tAdd;
	Button buttonCancel,buttonOK;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_dialog);
        setTitle("Create Free Account");
        buttonOK = (Button) findViewById(R.id.Button01);
		buttonOK.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				CreateAccountNow();
			}
		});
        buttonCancel = (Button) findViewById(R.id.Button02);
		buttonCancel.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				dismiss();
			}
		});
        etName = (EditText) findViewById(R.id.EditText01);
        etName.setText(email.substring(0,email.indexOf("@")));
        tAdd = (TextView) findViewById(R.id.Text01);
        tAdd.setText(email);
        etPass = (EditText) findViewById(R.id.EditText02);
        etConfirm = (EditText) findViewById(R.id.EditText03);
        
        TextView intro = (TextView) findViewById(R.id.intro);
        TextView intro2 = (TextView) findViewById(R.id.email);
        if (trunkserver != null) {
            intro.setText("To save battery life by utilizing SIP over TCP protocol, a new PBXes account is being offered to you. It will be automatically linked to your existing "+trunkserver+" account, and therefore get the same password as your "+trunkserver+" account.");
        	TextView intro3 = (TextView) findViewById(R.id.password);
        	TextView intro4 = (TextView) findViewById(R.id.password_confirm);
        	etPass.setVisibility(View.GONE);
        	etConfirm.setVisibility(View.GONE);
        	etPass.setText(trunkpassword);
        	etConfirm.setText(trunkpassword);
        	intro3.setVisibility(View.GONE);
        	intro4.setVisibility(View.GONE);
        	intro2.setText("Email Address");
        } else {
            intro.setText("A new PBXes account will be created. It will be linked to your existing Google Voice account, and therefore get the same password as your Google Voice account.");
        	intro2.setText("Google Voice Name");
        }
    }

}
