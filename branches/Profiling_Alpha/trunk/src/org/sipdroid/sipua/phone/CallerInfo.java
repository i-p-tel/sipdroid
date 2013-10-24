package org.sipdroid.sipua.phone;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2006 The Android Open Source Project
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
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Config;
import android.util.Log;

/**
 * Looks up caller information for the given phone number.
 *
 * {@hide}
 */
public class CallerInfo {
    private static final String TAG = "CallerInfo";

    public static final String UNKNOWN_NUMBER = "-1";
    public static final String PRIVATE_NUMBER = "-2";

    /**
     * Please note that, any one of these member variables can be null,
     * and any accesses to them should be prepared to handle such a case.
     *
     * Also, it is implied that phoneNumber is more often populated than
     * name is, (think of calls being dialed/received using numbers where
     * names are not known to the device), so phoneNumber should serve as
     * a dependable fallback when name is unavailable.
     *
     * One other detail here is that this CallerInfo object reflects
     * information found on a connection, it is an OUTPUT that serves
     * mainly to display information to the user.  In no way is this object
     * used as input to make a connection, so we can choose to display
     * whatever human-readable text makes sense to the user for a
     * connection.  This is especially relevant for the phone number field,
     * since it is the one field that is most likely exposed to the user.
     *
     * As an example:
     *   1. User dials "911"
     *   2. Device recognizes that this is an emergency number
     *   3. We use the "Emergency Number" string instead of "911" in the
     *     phoneNumber field.
     *
     * What we're really doing here is treating phoneNumber as an essential
     * field here, NOT name.  We're NOT always guaranteed to have a name
     * for a connection, but the number should be displayable.
     */
    public String name;
    public String phoneNumber;
    public String phoneLabel;
    /* Split up the phoneLabel into number type and label name */
    public int    numberType;
    public String numberLabel;
    
    public int photoResource;
    public long person_id;
    public boolean needUpdate;
    public Uri contactRefUri;
    
    // fields to hold individual contact preference data, 
    // including the send to voicemail flag and the ringtone
    // uri reference.
    public Uri contactRingtoneUri;
    public boolean shouldSendToVoicemail;

    /**
     * Drawable representing the caller image.  This is essentially
     * a cache for the image data tied into the connection /
     * callerinfo object.  The isCachedPhotoCurrent flag indicates
     * if the image data needs to be reloaded.
     */
    public Drawable cachedPhoto;
    public boolean isCachedPhotoCurrent;

    public CallerInfo() {
    }

    /**
     * getCallerInfo given a Cursor.
     * @param context the context used to retrieve string constants
     * @param contactRef the URI to attach to this CallerInfo object
     * @param cursor the first object in the cursor is used to build the CallerInfo object.
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef, Cursor cursor) {
        
        CallerInfo info = new CallerInfo();
        info.photoResource = 0;
        info.phoneLabel = null;
        info.numberType = 0;
        info.numberLabel = null;
        info.cachedPhoto = null;
        info.isCachedPhotoCurrent = false;
        
        if (Config.LOGV) Log.v(TAG, "construct callerInfo from cursor");
        
        if (cursor != null) {
            while (cursor.moveToNext()) {

                int columnIndex;

                // Look for the number
                columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER);
                if (columnIndex != -1) {
                    info.phoneNumber = cursor.getString(columnIndex);
                }
                
                // Look for the name
                columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                if (columnIndex != -1) {
                    info.name = cursor.getString(columnIndex);
                }

                // Look for the label/type combo
                columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL);
                if (columnIndex != -1) {
                    int typeColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE);
                    if (typeColumnIndex != -1) {
                        info.numberType = cursor.getInt(typeColumnIndex);
                        info.numberLabel = cursor.getString(columnIndex);
                        info.phoneLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(),
                                info.numberType, info.numberLabel)
                                .toString();
                    }
                }

                // Look for the person ID
                columnIndex = cursor.getColumnIndex(Phone.CONTACT_ID);
                if (columnIndex != -1) {
                        info.person_id = cursor.getLong(columnIndex);
                }
            }
            cursor.close();
        }

        info.needUpdate = false;
        info.name = normalize(info.name);
        info.contactRefUri = contactRef;

        return info;
    }
    
    /**
     * getCallerInfo given a URI, look up in the call-log database
     * for the uri unique key.
     * @param context the context used to get the ContentResolver
     * @param contactRef the URI used to lookup caller id
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef) {
        
        return getCallerInfo(context, contactRef, 
                context.getContentResolver().query(contactRef, null, null, null, null));
    }
    
    private static String normalize(String s) {
        if (s == null || s.length() > 0) {
            return s;
        } else {
            return null;
        }
    }
}    
