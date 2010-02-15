package org.sipdroid.codecs;

import java.util.HashMap;
import java.util.Vector;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Receiver;
import org.zoolu.sdp.SessionDescriptor;
import org.zoolu.sdp.AttributeField;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Codecs {
    	private static final Vector<Codec> codecs = new Vector<Codec>() {{
			add(new GSM());
			add(new alaw());
			add(new ulaw());
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
		String prefs = sp.getString("codecs", null);
		if (prefs == null) {
			String v = "";
			SharedPreferences.Editor e = sp.edit();

			for (Codec c : codecs)
				v = v + c.number() + " ";
			e.putString("codecs", v);
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
					codecs.remove(c);
					codecs.add(c);
				} catch (Exception e) {
					// do nothing (expecting
					// NumberFormatException and
					// indexnot found
				}
			}
		}
	}

	private static boolean initialised = false;

	public static Codec get(int key) {
		return codecsNumbers.get(key);
	}

	public static Codec getName(String name) {
		return codecsNames.get(name);
	}

	private static void addPreferences(PreferenceScreen ps) {
		Context cx = ps.getContext();
		SharedPreferences sp = ps.getSharedPreferences();
		Resources r = cx.getResources();
		ps.setOrderingAsAdded(true);

		for(Codec c : codecs) {
			ListPreference l = new ListPreference(cx);
			l.setEntries(r.getStringArray(R.array.compression_display_values));
			l.setEntryValues(r.getStringArray(R.array.compression_values));
			l.setKey(c.name());
			l.setPersistent(true);
			if (c.isLoaded()) {
				l.setEnabled(true);
				c.setListPreference(l);
			} else {
				l.setValue("never");
				l.setEnabled(false);
			}
			l.setSummary(l.getEntry());
			l.setTitle(c.getTitle());
			ps.addPreference(l);
		}
	}

	public static int[] getCodecs() {
		Vector<Integer> v = new Vector<Integer>(codecs.size());
		boolean onEdge = onEdge();

		for (Codec c : codecs) {
			if (!c.isEnabled())
				continue;
			if (c.edgeOnly() && !onEdge)
				continue;
			v.add(c.number());
		}
		int i[] = new int[v.size()];
		for (int j = 0; j < i.length; j++)
			i[j] = v.elementAt(j);
		return i;
	}

	public static class Map {
		public final int number;
		public final Codec codec;

		Map(int n, Codec c) {
			number = n;
			codec = c;
		}

		public String toString() {
			return "Codecs.Map { " + number + ": " + codec + "}";
		}
	};

	public static Map getCodec(SessionDescriptor offers) {
		boolean onEdge = onEdge();
		Vector<AttributeField> attrs = offers.getMediaDescriptor("audio").getAttributes("rtpmap");
		Vector<String> names = new Vector<String>(attrs.size());
		Vector<Integer> numbers = new Vector<Integer>(attrs.size());

		for (AttributeField a : attrs) {
			String s = a.getValue();
			// skip over "rtpmap:"
			s = s.substring(7, s.indexOf("/"));
			int i = s.indexOf(" ");
			try {
				String name = s.substring(i + 1);
				int number = Integer.parseInt(s.substring(0, i));
				names.add(name);
				numbers.add(number);
			} catch (NumberFormatException e) {
				// continue ... remote sent bogus rtp setting
			}
		}

		// find the first codec in our list that's also in the offers
		for (Codec c : codecs) {
			if (!c.isEnabled())
				continue;
			if (c.edgeOnly() && !onEdge)
				continue;
			int i =  names.indexOf(c.name());
			if (i == -1)
				continue;

			return new Map(numbers.elementAt(i), c);
		}
		// no codec found ... we can't talk
		return null;
	}

	private static boolean onEdge() {
		TelephonyManager tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		return !Receiver.on_wlan && tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE;
	}

	public static class CodecSettings extends PreferenceActivity {

		private static final int MENU_UP = 0;
		private static final int MENU_DOWN = 1;
		private static final int MENU_CANCEL = 2;

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
			menu.add(Menu.NONE, MENU_CANCEL, 0, R.string.cancel);
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {

			if (item.getItemId() == MENU_CANCEL)
				return super.onContextItemSelected(item);

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
			e.putString("codecs", v);
			e.commit();
			ps.removeAll();
			addPreferences(ps);
			return super.onContextItemSelected(item);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			unregisterForContextMenu(getListView());
		}
	}
}
