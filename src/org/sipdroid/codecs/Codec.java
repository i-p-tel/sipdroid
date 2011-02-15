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

import android.preference.ListPreference;

/**
 * Represents the basic interface to the Codec classes All codecs need
 * to implement basic encode and decode capability Codecs which
 * inherit from {@link CodecBase} only need to implement encode,
 * decode and init
 */
public interface Codec {
	/**
	 * Decode a linear pcm audio stream
	 *
	 * @param encoded The encoded audio stream
	 *
	 * @param lin The linear pcm audio frame buffer in which to place the decoded stream
	 *
	 * @param size The size of the encoded frame
	 *
	 * @returns The size of the decoded frame
	 */
	int decode(byte encoded[], short lin[], int size);

	/**
	 * Encode a linear pcm audio stream
	 *
	 * @param lin The linear stream to encode
	 *
	 * @param offset The offset into the linear stream to begin
	 *
	 * @param encoded The buffer to place the encoded stream
	 *
	 * @param size the size of the linear pcm stream (in words)
	 *
	 * @returns the length (in bytes) of the encoded stream
	 */
	int encode(short lin[], int offset, byte alaw[], int frames);

	/**
	 * The sampling rate for this particular codec
	 */	
	int samp_rate();
	
	/**
	 * The audio frame size for this particular codec
	 */	
	int frame_size();
	
	/**
	 * Optionally used to initiallize the codec before any
	 * encoding or decoding
	 */
	void init();
	void update();
	
	/**
	 * Optionally used to free any resources allocated in init
	 * after encoding or decoding is complete
	 */
	void close();

	/**
	 * (implemented by {@link CodecBase}
	 * <p>
	 * checks to see if the user has enabled the codec.
	 *
	 * @returns true if the codec can be used
	 */
	boolean isEnabled();

	/**
	 * (implemented by {@link CodecBase}
	 * <p>
	 * Checks to see if the binary library associated with the
	 * codec (if any) loaded OK.
	 *
	 * @returns true if the codec loaded properly
	 */
	boolean isLoaded();
	boolean isFailed();
	void fail();
	boolean isValid();
	
	/**
	 * (implemented by {@link CodecBase}
	 *
	 * @returns The user friendly string for the codec (should
	 * include both the name and the bandwidth
	 */
	String getTitle();


	/**
	 * (implemented by {@link CodecBase}
	 *
	 * @returns The RTP assigned name string for the codec
	 */
	String name();
	String key();
	String getValue();

	/**
	 * (implemented by {@link CodecBase}
	 *
	 * @returns The commonly used name for the codec.
	 */
	String userName();

	/**
	 * (implemented by {@link CodecBase}
	 *
	 * @returns The RTP assigned number for the codec
	 */
	int number();

	/**
	 * (implemented by {@link CodecBase}
	 *
	 * @param l The list preference controlling this Codec
	 *
	 * Used to add listeners for preference changes and update
	 * the codec parameters accordingly.
	 */
	void setListPreference(ListPreference l);
}
