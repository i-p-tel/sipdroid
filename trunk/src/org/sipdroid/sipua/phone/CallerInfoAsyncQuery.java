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

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

/**
 * ASYNCHRONOUS QUERY API
 */

public class CallerInfoAsyncQuery {
    
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CallerInfoAsyncQuery";
    
    private static final int EVENT_NEW_QUERY = 1;
    private static final int EVENT_ADD_LISTENER = 2;
    private static final int EVENT_END_OF_QUEUE = 3;
    private static final int EVENT_EMERGENCY_NUMBER = 4;
    private static final int EVENT_VOICEMAIL_NUMBER = 5;

    private CallerInfoAsyncQueryHandler mHandler;

    /**
     * Interface for a CallerInfoAsyncQueryHandler result return.
     */
    public interface OnQueryCompleteListener {
        /**
         * Called when the query is complete.  
         */  
        public void onQueryComplete(int token, Object cookie, CallerInfo ci);
    }
    
    
    /**
     * Wrap the cookie from the WorkerArgs with additional information needed by our
     * classes. 
     */
    private static final class CookieWrapper {
        public OnQueryCompleteListener listener;
        public Object cookie;
        public int event;
        public String number,number2;
    }    
    
    
    /**
     * Simple exception used to communicate problems with the query pool.
     */
    public static class QueryPoolException extends SQLException {
        public QueryPoolException(String error) {
            super(error);
        }
    }
    
    /**
     * Our own implementation of the AsyncQueryHandler.
     */
    private class CallerInfoAsyncQueryHandler extends AsyncQueryHandler {
        
        /**
         * The information relevant to each CallerInfo query.  Each query may have multiple
         * listeners, so each AsyncCursorInfo is associated with 2 or more CookieWrapper
         * objects in the queue (one with a new query event, and one with a end event, with
         * 0 or more additional listeners in between).
         */
        private Context mQueryContext;
        private Uri mQueryUri;
        private CallerInfo mCallerInfo;
        
        /**
         * Our own query worker thread.
         * 
         * This thread handles the messages enqueued in the looper.  The normal sequence
         * of events is that a new query shows up in the looper queue, followed by 0 or
         * more add listener requests, and then an end request.  Of course, these requests
         * can be interlaced with requests from other tokens, but is irrelevant to this
         * handler since the handler has no state.
         * 
         * Note that we depend on the queue to keep things in order; in other words, the
         * looper queue must be FIFO with respect to input from the synchronous startQuery 
         * calls and output to this handleMessage call.
         * 
         * This use of the queue is required because CallerInfo objects may be accessed
         * multiple times before the query is complete.  All accesses (listeners) must be
         * queued up and informed in order when the query is complete.
         */
        protected class CallerInfoWorkerHandler extends WorkerHandler {
            public CallerInfoWorkerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                WorkerArgs args = (WorkerArgs) msg.obj;
                CookieWrapper cw = (CookieWrapper) args.cookie;
                
                if (cw == null) {
                    // Normally, this should never be the case for calls originating
                    // from within this code.
                    // However, if there is any code that this Handler calls (such as in 
                    // super.handleMessage) that DOES place unexpected messages on the
                    // queue, then we need pass these messages on.
                    if (DBG) log("Unexpected command (CookieWrapper is null): " + msg.what + 
                            " ignored by CallerInfoWorkerHandler, passing onto parent.");
                    
                    super.handleMessage(msg);
                } else {
                    
                    if (DBG) log("Processing event: " + cw.event + " token (arg1): " + msg.arg1 + 
                            " command: " + msg.what + " query URI: " + args.uri);
                    
                    switch (cw.event) {
                        case EVENT_NEW_QUERY:
                            //start the sql command.
                            super.handleMessage(msg);
                            break;

                        // shortcuts to avoid query for recognized numbers.
                        case EVENT_EMERGENCY_NUMBER:
                        case EVENT_VOICEMAIL_NUMBER:
                            
                        case EVENT_ADD_LISTENER:
                        case EVENT_END_OF_QUEUE:
                            // query was already completed, so just send the reply.
                            // passing the original token value back to the caller
                            // on top of the event values in arg1.
                            Message reply = args.handler.obtainMessage(msg.what);
                            reply.obj = args;
                            reply.arg1 = msg.arg1;
        
                            reply.sendToTarget();
        
                            break;
                        default:
                    }
                }
            }
        }
        
        
        /**
         * Asynchronous query handler class for the contact / callerinfo object.
         */
        private CallerInfoAsyncQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected Handler createHandler(Looper looper) {
            return new CallerInfoWorkerHandler(looper);
        }

        /**
         * Overrides onQueryComplete from AsyncQueryHandler.
         * 
         * This method takes into account the state of this class; we construct the CallerInfo
         * object only once for each set of listeners. When the query thread has done its work
         * and calls this method, we inform the remaining listeners in the queue, until we're 
         * out of listeners.  Once we get the message indicating that we should expect no new 
         * listeners for this CallerInfo object, we release the AsyncCursorInfo back into the 
         * pool.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (DBG) log("query complete for token: " + token);
            
            //get the cookie and notify the listener.
            CookieWrapper cw = (CookieWrapper) cookie;
            if (cw == null) {
                // Normally, this should never be the case for calls originating
                // from within this code.
                // However, if there is any code that calls this method, we should 
                // check the parameters to make sure they're viable.
                if (DBG) log("Cookie is null, ignoring onQueryComplete() request.");
                return;
            }
            
            if (cw.event == EVENT_END_OF_QUEUE) {
                release();
                return;
            }

            // check the token and if needed, create the callerinfo object.
            if (mCallerInfo == null) {
                if ((mQueryContext == null) || (mQueryUri == null)) {
                    throw new QueryPoolException
                            ("Bad context or query uri, or CallerInfoAsyncQuery already released.");
                }
                
                { 
                    mCallerInfo = CallerInfo.getCallerInfo(mQueryContext, mQueryUri, cursor);
                    // Use the number entered by the user for display.
                    if (!TextUtils.isEmpty(cw.number)) {
                    	if (mCallerInfo.name == null && !cw.number.equals(cw.number2))
                    		mCallerInfo.name = cw.number2;
                    	mCallerInfo.phoneNumber = PhoneNumberUtils.formatNumber(cw.number);
                    }
                }
                
                if (DBG) log("constructing CallerInfo object for token: " + token);
                
                //notify that we can clean up the queue after this.
                CookieWrapper endMarker = new CookieWrapper();
                endMarker.event = EVENT_END_OF_QUEUE;
                startQuery (token, endMarker, null, null, null, null, null);
            }
            
            //notify the listener that the query is complete.
            if (cw.listener != null) {
                if (DBG) log("notifying listener: " + cw.listener.getClass().toString() + 
                        " for token: " + token);
                cw.listener.onQueryComplete(token, cw.cookie, mCallerInfo);
            }
        }
    }
    
    /**
     * Private constructor for factory methods.
     */
    private CallerInfoAsyncQuery() {
    }

    
    /**
     * Factory method to start query with a Uri query spec
     */
    public static CallerInfoAsyncQuery startQuery(int token, Context context, Uri contactRef, 
            OnQueryCompleteListener listener, Object cookie) {
        
        CallerInfoAsyncQuery c = new CallerInfoAsyncQuery();
        c.allocate(context, contactRef);

        if (DBG) log("starting query for URI: " + contactRef + " handler: " + c.toString());
        
        //create cookieWrapper, start query
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.event = EVENT_NEW_QUERY;
        
        c.mHandler.startQuery (token, cw, contactRef, null, null, null, null);
        
        return c;
    }
    
    /**
     * Factory method to start query with a number
     */
    public static CallerInfoAsyncQuery startQuery(int token, Context context, String number, String number2, 
            OnQueryCompleteListener listener, Object cookie) {
        //contruct the URI object and start Query.
    	String number_search = number;
        if (number.contains("&"))
        	number_search = number.substring(0,number.indexOf("&"));
        Uri contactRef = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, number_search);
        
        CallerInfoAsyncQuery c = new CallerInfoAsyncQuery();
        c.allocate(context, contactRef);

        if (DBG) log("starting query for number: " + number + " handler: " + c.toString());
        
        //create cookieWrapper, start query
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.number = number;
        cw.number2 = number2;

        cw.event = EVENT_NEW_QUERY;
        c.mHandler.startQuery (token, cw, contactRef, null, null, null, null);
        
        return c;
   }
    
    /**
     * Method to add listeners to a currently running query
     */
    public void addQueryListener(int token, OnQueryCompleteListener listener, Object cookie) {

        if (DBG) log("adding listener to query: " + mHandler.mQueryUri + " handler: " + 
                mHandler.toString());
        
        //create cookieWrapper, add query request to end of queue.
        CookieWrapper cw = new CookieWrapper();
        cw.listener = listener;
        cw.cookie = cookie;
        cw.event = EVENT_ADD_LISTENER;
        
        mHandler.startQuery (token, cw, null, null, null, null, null);
    }

    /**
     * Method to create a new CallerInfoAsyncQueryHandler object, ensuring correct
     * state of context and uri.
     */
    private void allocate (Context context, Uri contactRef) {
        if ((context == null) || (contactRef == null)){
            throw new QueryPoolException("Bad context or query uri.");
        }
        mHandler = new CallerInfoAsyncQueryHandler(context);
        mHandler.mQueryContext = context;
        mHandler.mQueryUri = contactRef;
    }

    /**
     * Releases the relevant data.
     */
    private void release () {
        mHandler.mQueryContext = null;
        mHandler.mQueryUri = null;
        mHandler.mCallerInfo = null;
        mHandler = null;
    }
    
    /**
     * static logging method
     */
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }   
}
