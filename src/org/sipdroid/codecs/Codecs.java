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

package org.sipdroid.codecs;

import java.util.HashMap;
import java.util.Vector;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;
import org.zoolu.sdp.MediaField;
import org.zoolu.sdp.SessionDescriptor;
import org.zoolu.sdp.AttributeField;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Codecs {
    	private static final Vector<Codec> codecs = new Vector<Codec>() {{
			add(new G722());			
//			add(new SILK24());		save space (until a common library for all bitrates gets available?)
//			add(new SILK16());
//			add(new SILK8());
			add(new alaw());
			add(new ulaw());
			add(new Speex());
			add(new GSM());
			add(new BV16());
		}};
	private static final HashMap<Integer, Codec> codecsNumbers;
	private static final HashMap<String, Codec> codecsNames;

	static {
		final int size = codecs.size();
		codecsNumbers = new HashMap<Integer, Codec>(size);
		codecsNames = new HashMap<String, Codec>(size);

		for (Codec c : codecs) {
			codecsNames.put(c.name(), c);
			codecsNumbers.put(c.number(), c);
		}

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
		String prefs = sp.getString(Settings.PREF_CODECS, Settings.DEFAULT_CODECS);
		if (prefs == null) {
			String v = "";
			SharedPreferences.Editor e = sp.edit();

			for (Codec c : codecs)
				v = v + c.number() + " ";
			e.putString(Settings.PREF_CODECS, v);
			e.commit();
		} else {
			String[] vals = prefs.split(" ");
			for (String v: vals) {
				try {
					int i = Integer.parseInt(v);
					Codec c = codecsNumbers.get(i);
					/* moves the codec to the end
					 * of the list so we end up
					 * with the new codecs (if
					 * any) at the top and the
					 * remaining ones ordered
					 * according to the user */
					if (c != null) {
						codecs.remove(c);
						codecs.add(c);
					}
				} catch (Exception e) {
					// do nothing (expecting
					// NumberFormatException and
					// indexnot found
				}
			}
		}
	}

	public static Codec get(int key) {
		return codecsNumbers.get(key);
	}

	public static Codec getName(String name) {
		return codecsNames.get(name);
	}

	public static void check() {
		HashMap<String, String> old = new HashMap<String, String>(codecs.size());

		for(Codec c : codecs) {
			c.update();
			old.put(c.name(), c.getValue());
			if (!c.isLoaded()) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
				SharedPreferences.Editor e = sp.edit();

				e.putString(c.key(), "never");
				e.commit();
			}
		}
		
		for(Codec c : codecs)
			if (!old.get(c.name()).equals("never")) {
				c.init();
				if (c.isLoaded()) {
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
					SharedPreferences.Editor e = sp.edit();
	
					e.putString(c.key(), old.get(c.name()));
					e.commit();
					c.init();
				} else
					c.fail();
			}
	}
	
	private static void addPreferences(PreferenceScreen ps) {
		Context cx = ps.getContext();
		Resources r = cx.getResources();
		ps.setOrderingAsAdded(true);

		for(Codec c : codecs) {
			ListPreference l = new ListPreference(cx);
			l.setEntries(r.getStringArray(R.array.compression_display_values));
			l.setEntryValues(r.getStringArray(R.array.compression_values));
			l.setKey(c.key());
			l.setPersistent(true);
			l.setEnabled(!c.isFailed());
			c.setListPreference(l);
			if (c.number() == 9)
				if (ps.getSharedPreferences().getString(Settings.PREF_SERVER, Settings.DEFAULT_SERVER).equals(Settings.DEFAULT_SERVER))
					l.setSummary(l.getEntry()+" ("+r.getString(R.string.settings_improve2)+")");
				else
					l.setSummary(l.getEntry()+" ("+r.getString(R.string.settings_hdvoice)+")");
			else
				l.setSummary(l.getEntry());
			l.setTitle(c.getTitle());
			ps.addPreference(l);
		}
	}

	public static int[] getCodecs() {
		Vector<Integer> v = new Vector<Integer>(codecs.size());

		for (Codec c : codecs) {
			c.update();
			if (!c.isValid())
				continue;
			v.add(c.number());
		}
		int i[] = new int[v.size()];
		for (int j = 0; j < i.length; j++)
			i[j] = v.elementAt(j);
		return i;
	}

	public static class Map {
		public int number;
		public Codec codec;
		Vector<Integer> numbers;
		Vector<Codec> codecs;

		Map(int n, Codec c, Vector<Integer> ns, Vector<Codec> cs) {
			number = n;
			codec = c;
			numbers = ns;
			codecs = cs;
		}

		public boolean change(int n) {
			int i = numbers.indexOf(n);
			
			if (i >= 0 && codecs.elementAt(i) != null) {
				codec.close();
				number = n;
				codec = codecs.elementAt(i);
				return true;
			}
			return false;
		}
		
		public String toString() {
			return "Codecs.Map { " + number + ": " + codec + "}";
		}
	};

	public static Map getCodec(SessionDescriptor offers) {
		MediaField m = offers.getMediaDescriptor("audio").getMedia(); 
		if (m==null) 
			return null;

		String proto = m.getTransport();
		//see http://tools.ietf.org/html/rfc4566#page-22, paragraph 5.14, <fmt> description 
		if ( proto.equals("RTP/AVP") || proto.equals("RTP/SAVP") ) {
			Vector<String> formats = m.getFormatList();
			Vector<String> names = new Vector<String>(formats.size());
			Vector<Integer> numbers = new Vector<Integer>(formats.size());
			Vector<Codec> codecmap = new Vector<Codec>(formats.size());

			//add all avail formats with empty names
			for (String fmt : formats) {
				try {
					int number = Integer.parseInt(fmt);
					numbers.add(number);
					names.add("");
					codecmap.add(null);
				} catch (NumberFormatException e) {
					// continue ... remote sent bogus rtp setting
				}
			};
		
			//if we have attrs for format -> set name
			Vector<AttributeField> attrs = offers.getMediaDescriptor("audio").getAttributes("rtpmap");			
			for (AttributeField a : attrs) {
				String s = a.getValue();
				// skip over "rtpmap:"
				s = s.substring(7, s.indexOf("/"));
				int i = s.indexOf(" ");
				try {
					String name = s.substring(i + 1);
					int number = Integer.parseInt(s.substring(0, i));
					int index = numbers.indexOf(number);
					if (index >=0)
						names.set(index, name.toLowerCase());
				} catch (NumberFormatException e) {
					// continue ... remote sent bogus rtp setting
				}
			}
			
			Codec codec = null;
			int index = formats.size() + 1;
			
			for (Codec c : codecs) {
				c.update();
				if (!c.isValid())
					continue;

				//search current codec in offers by name
				int i = names.indexOf(c.userName().toLowerCase());
				if (i >= 0) {
					codecmap.set(i, c);
					if ( (codec==null) || (i < index) ) {
						codec = c;
						index = i;
						break;
					}
				}
				
				//search current codec in offers by number
				i = numbers.indexOf(c.number());
				if (i >= 0) {
						if ( names.elementAt(i).equals("")) {
							codecmap.set(i, c);
							if ( (codec==null) || (i < index) )  {
								//fmt number has no attr with name 
								codec = c;
								index = i;
								break;
							}
						}
				}
			}			
			if (codec!=null) 
				return new Map(numbers.elementAt(index), codec, numbers, codecmap);
			else
				// no codec found ... we can't talk
				return null;
		} else
			/*formats of other protocols not supported yet*/
			return null;
	}

	public static class CodecSettings extends PreferenceActivity {

		private static final int MENU_UP = 0;
		private static final int MENU_DOWN = 1;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.codec_settings);

			// for long-press gesture on a profile preference
			registerForContextMenu(getListView());

			addPreferences(getPreferenceScreen());
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);

			menu.setHeaderTitle(R.string.codecs_move);
			menu.add(Menu.NONE, MENU_UP, 0,
				 R.string.codecs_move_up);
			menu.add(Menu.NONE, MENU_DOWN, 0,
				 R.string.codecs_move_down);
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {

			int posn = (int)((AdapterContextMenuInfo)item.getMenuInfo()).position;
			Codec c = codecs.elementAt(posn);
			if (item.getItemId() == MENU_UP) {
				if (posn == 0)
					return super.onContextItemSelected(item);
				Codec tmp = codecs.elementAt(posn - 1);
				codecs.set(posn - 1, c);
				codecs.set(posn, tmp);
			} else if (item.getItemId() == MENU_DOWN) {
				if (posn == codecs.size() - 1)
					return super.onContextItemSelected(item);
				Codec tmp = codecs.elementAt(posn + 1);
				codecs.set(posn + 1, c);
				codecs.set(posn, tmp);
			}
			PreferenceScreen ps = getPreferenceScreen();
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
			String v = "";
			SharedPreferences.Editor e = sp.edit();

			for (Codec d : codecs)
				v = v + d.number() + " ";
			e.putString(Settings.PREF_CODECS, v);
			e.commit();
			ps.removeAll();
			addPreferences(ps);
			return super.onContextItemSelected(item);
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference p) {
			ListPreference l = (ListPreference) p;
			for (Codec c : codecs)
				if (c.key().equals(l.getKey())) {
					c.init();
					if (!c.isLoaded()) {
						l.setValue("never");
						c.fail();
						l.setEnabled(false);
						l.setSummary(l.getEntry());
						if (l.getDialog() != null)
							l.getDialog().dismiss();
					}
				}
			return super.onPreferenceTreeClick(ps,p);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			unregisterForContextMenu(getListView());
		}
	}
}
