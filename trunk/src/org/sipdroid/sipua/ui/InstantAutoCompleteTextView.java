package org.sipdroid.sipua.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class InstantAutoCompleteTextView extends AutoCompleteTextView {
	public InstantAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (hasWindowFocus && getAdapter() != null && getAdapter().getCount() > 0)
			showDropDown();
	}
	
	@Override
	public boolean enoughToFilter() {
		return true;
	}
}
