package org.sipdroid.sipua.ui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class SIPUri extends Activity {

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (!Receiver.engine(this).isRegistered())
			Receiver.engine(this).register();

		Uri uri = getIntent().getData();
		String target = uri.getSchemeSpecificPart();
		Log.v("SIPUri", "sip uri: " + uri);
		Receiver.engine(this).call(target);
	}
}
