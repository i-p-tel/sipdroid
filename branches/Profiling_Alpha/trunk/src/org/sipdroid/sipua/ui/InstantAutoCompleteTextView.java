package org.sipdroid.sipua.ui;

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
//		if (hasWindowFocus && getAdapter() != null && getAdapter().getCount() > 0)
//			showDropDown();
	}
	
	@Override
	public boolean enoughToFilter() {
		return true;
	}
}
