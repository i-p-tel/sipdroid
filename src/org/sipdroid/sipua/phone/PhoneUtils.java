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

package org.sipdroid.sipua.phone;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.sipdroid.sipua.*;

public class PhoneUtils {
    private static final String LOG_TAG = "PhoneUtils";
    private static final boolean DBG = false;
/**
 * Class returned by the startGetCallerInfo call to package a temporary
 * CallerInfo Object, to be superceded by the CallerInfo Object passed
 * into the listener when the query with token mAsyncQueryToken is complete.
 */
public static class CallerInfoToken {
    /**indicates that there will no longer be updates to this request.*/
    public boolean isFinal;

    public CallerInfo currentInfo;
    public CallerInfoAsyncQuery asyncQuery;
}

/**
 * Start a CallerInfo Query based on the earliest connection in the call.
 */
static CallerInfoToken startGetCallerInfo(Context context, Call call,
        CallerInfoAsyncQuery.OnQueryCompleteListener listener, Object cookie) {
    Connection conn = call.getEarliestConnection();
    return startGetCallerInfo(context, conn, listener, cookie);
}

/**
 * place a temporary callerinfo object in the hands of the caller and notify
 * caller when the actual query is done.
 */
static CallerInfoToken startGetCallerInfo(Context context, Connection c,
        CallerInfoAsyncQuery.OnQueryCompleteListener listener, Object cookie) {
    CallerInfoToken cit;

    if (c == null) {
        //TODO: perhaps throw an exception here.
        cit = new CallerInfoToken();
        cit.asyncQuery = null;
        return cit;
    }

    // There are now 3 states for the userdata.
    //   1. Uri - query has not been executed yet
    //   2. CallerInfoToken - query is executing, but has not completed.
    //   3. CallerInfo - query has executed.
    // In each case we have slightly different behaviour:
    //   1. If the query has not been executed yet (Uri or null), we start
    //      query execution asynchronously, and note it by attaching a
    //      CallerInfoToken as the userData.
    //   2. If the query is executing (CallerInfoToken), we've essentially
    //      reached a state where we've received multiple requests for the
    //      same callerInfo.  That means that once the query is complete,
    //      we'll need to execute the additional listener requested.
    //   3. If the query has already been executed (CallerInfo), we just
    //      return the CallerInfo object as expected.
    //   4. Regarding isFinal - there are cases where the CallerInfo object
    //      will not be attached, like when the number is empty (caller id
    //      blocking).  This flag is used to indicate that the
    //      CallerInfoToken object is going to be permanent since no
    //      query results will be returned.  In the case where a query
    //      has been completed, this flag is used to indicate to the caller
    //      that the data will not be updated since it is valid.
    //
    //      Note: For the case where a number is NOT retrievable, we leave
    //      the CallerInfo as null in the CallerInfoToken.  This is
    //      something of a departure from the original code, since the old
    //      code manufactured a CallerInfo object regardless of the query
    //      outcome.  From now on, we will append an empty CallerInfo
    //      object, to mirror previous behaviour, and to avoid Null Pointer
    //      Exceptions.
    Object userDataObject = c.getUserData();
    if (userDataObject instanceof Uri) {
        //create a dummy callerinfo, populate with what we know from URI.
        cit = new CallerInfoToken();
        cit.currentInfo = new CallerInfo();
        cit.asyncQuery = CallerInfoAsyncQuery.startQuery(QUERY_TOKEN, context,
                (Uri) userDataObject, sCallerInfoQueryListener, c);
        cit.asyncQuery.addQueryListener(QUERY_TOKEN, listener, cookie);
        cit.isFinal = false;

        c.setUserData(cit);

        if (DBG) log("startGetCallerInfo: query based on Uri: " + userDataObject);

    } else if (userDataObject == null) {
        // No URI, or Existing CallerInfo, so we'll have to make do with
        // querying a new CallerInfo using the connection's phone number.
        String number = c.getAddress();

        cit = new CallerInfoToken();
        cit.currentInfo = new CallerInfo();

        if (DBG) log("startGetCallerInfo: number = " + number);

        // handling case where number is null (caller id hidden) as well.
        if (!TextUtils.isEmpty(number)) {
            cit.currentInfo.phoneNumber = number;
             cit.asyncQuery = CallerInfoAsyncQuery.startQuery(QUERY_TOKEN, context,
                    number, c.getAddress2(), sCallerInfoQueryListener, c);
            cit.asyncQuery.addQueryListener(QUERY_TOKEN, listener, cookie);
            cit.isFinal = false;
        } else {
            // This is the case where we are querying on a number that
            // is null or empty, like a caller whose caller id is
            // blocked or empty (CLIR).  The previous behaviour was to
            // throw a null CallerInfo object back to the user, but
            // this departure is somewhat cleaner.
            if (DBG) log("startGetCallerInfo: No query to start, send trivial reply.");
            cit.isFinal = true; // please see note on isFinal, above.
        }

        c.setUserData(cit);

        if (DBG) log("startGetCallerInfo: query based on number: " + number);

    } else if (userDataObject instanceof CallerInfoToken) {
        // query is running, just tack on this listener to the queue.
        cit = (CallerInfoToken) userDataObject;

        // handling case where number is null (caller id hidden) as well.
        if (cit.asyncQuery != null) {
            cit.asyncQuery.addQueryListener(QUERY_TOKEN, listener, cookie);

            if (DBG) log("startGetCallerInfo: query already running, adding listener: " +
                    listener.getClass().toString());
        } else {
            if (DBG) log("startGetCallerInfo: No query to attach to, send trivial reply.");
            if (cit.currentInfo == null) {
                cit.currentInfo = new CallerInfo();
            }
            cit.isFinal = true; // please see note on isFinal, above.
        }
    } else {
        cit = new CallerInfoToken();
        cit.currentInfo = (CallerInfo) userDataObject;
        cit.asyncQuery = null;
        cit.isFinal = true;
        // since the query is already done, call the listener.
        if (DBG) log("startGetCallerInfo: query already done, returning CallerInfo");
    }
    return cit;
}
/**
 * Implemented for CallerInfo.OnCallerInfoQueryCompleteListener interface.
 * Updates the connection's userData when called.
 */
private static final int QUERY_TOKEN = -1;
static CallerInfoAsyncQuery.OnQueryCompleteListener sCallerInfoQueryListener =
    new CallerInfoAsyncQuery.OnQueryCompleteListener () {
        public void onQueryComplete(int token, Object cookie, CallerInfo ci){
            if (DBG) log("query complete, updating connection.userdata");

            ((Connection) cookie).setUserData(ci);
        }
    };

/**
 * Returns a single "name" for the specified given a CallerInfo object.
 * If the name is null, return defaultString as the default value, usually
 * context.getString(R.string.unknown).
 */
static String getCompactNameFromCallerInfo(CallerInfo ci, Context context) {
    if (DBG) log("getCompactNameFromCallerInfo: info = " + ci);

    String compactName = null;
    if (ci != null) {
        compactName = ci.name;
        if (compactName == null) {
            compactName = ci.phoneNumber;
        }
    }
    // TODO: figure out UNKNOWN, PRIVATE numbers?
    if (compactName == null) {
        compactName = context.getString(R.string.unknown);
    }
    return compactName;
}
private static void log(String msg) {
    Log.d(LOG_TAG, "[PhoneUtils] " + msg);
}}
