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

//import com.android.internal.telephony.Call;
//import com.android.internal.telephony.CallerInfo;
//import com.android.internal.telephony.CallerInfoAsyncQuery;
//import com.android.internal.telephony.Connection;
//import com.android.internal.telephony.Phone;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
//import android.pim.ContactsAsyncHelper;
//import android.pim.DateUtils;
import android.provider.Contacts.People;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.sipdroid.sipua.*;
import org.sipdroid.sipua.ui.Receiver;

/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class CallCard extends FrameLayout
        implements CallerInfoAsyncQuery.OnQueryCompleteListener,
                ContactsAsyncHelper.OnImageLoadCompleteListener{
    private static final String LOG_TAG = "PHONE/CallCard";
    private static final boolean DBG = false;

    // Top-level subviews of the CallCard
    private ViewGroup mMainCallCard;
    private ViewGroup mOtherCallOngoingInfoArea;
    private ViewGroup mOtherCallOnHoldInfoArea;

    // "Upper" and "lower" title widgets
    private TextView mUpperTitle;
    private ViewGroup mLowerTitleViewGroup;
    private TextView mLowerTitle;
    private ImageView mLowerTitleIcon;
    public Chronometer mElapsedTime;

    // Text colors, used with the lower title and "other call" info areas
    private int mTextColorConnected;
    private int mTextColorEnded;
    private int mTextColorOnHold;

    private ImageView mPhoto;
    private TextView mName;
    private TextView mPhoneNumber;
    private TextView mLabel;

    // "Other call" info area
    private TextView mOtherCallOngoingName;
    private TextView mOtherCallOngoingStatus;
    private TextView mOtherCallOnHoldName;
    private TextView mOtherCallOnHoldStatus;

    // Menu button hint
    private TextView mMenuButtonHint;

    // Track the state for the photo.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

    // A few hardwired constants used in our screen layout.
    // TODO: These should all really come from resources, but that's
    // nontrivial; see the javadoc for the ConfigurationHelper class.
    // For now, let's at least keep them all here in one place
    // rather than sprinkled througout this file.
    //
    static final int MAIN_CALLCARD_MIN_HEIGHT_LANDSCAPE = 200;
    static final int CALLCARD_SIDE_MARGIN_LANDSCAPE = 50;
    static final float TITLE_TEXT_SIZE_LANDSCAPE = 22F;  // scaled pixels

    public void update(int x,int y,int w,int h) {
    	setPadding(0, y, 0, 0);
    }
    
    public CallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG) log("CallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);

        // Inflate the contents of this CallCard, and add it (to ourself) as a child.
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(
                R.layout.call_card,  // resource
                this,                // root
                true);

        // create a new object to track the state for the photo.
        mPhotoTracker = new ContactsAsyncHelper.ImageTracker();
    }

    public void reset() {
        if (DBG) log("reset()...");

        // default to show ACTIVE call style, with empty title and status text
        showCallConnected();
        mUpperTitle.setText("");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");

        mMainCallCard = (ViewGroup) findViewById(R.id.mainCallCard);
        mOtherCallOngoingInfoArea = (ViewGroup) findViewById(R.id.otherCallOngoingInfoArea);
        mOtherCallOnHoldInfoArea = (ViewGroup) findViewById(R.id.otherCallOnHoldInfoArea);

        // "Upper" and "lower" title widgets
        mUpperTitle = (TextView) findViewById(R.id.upperTitle);
        mLowerTitleViewGroup = (ViewGroup) findViewById(R.id.lowerTitleViewGroup);
        mLowerTitle = (TextView) findViewById(R.id.lowerTitle);
        mLowerTitleIcon = (ImageView) findViewById(R.id.lowerTitleIcon);
        mElapsedTime = (Chronometer) findViewById(R.id.elapsedTime);

        // Text colors
        mTextColorConnected = getResources().getColor(R.color.incall_textConnected);
        mTextColorEnded = getResources().getColor(R.color.incall_textEnded);
        mTextColorOnHold = getResources().getColor(R.color.incall_textOnHold);

        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView) findViewById(R.id.photo);
        mName = (TextView) findViewById(R.id.name);
        mPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
        mLabel = (TextView) findViewById(R.id.label);

        // "Other call" info area
        mOtherCallOngoingName = (TextView) findViewById(R.id.otherCallOngoingName);
        mOtherCallOngoingStatus = (TextView) findViewById(R.id.otherCallOngoingStatus);
        mOtherCallOnHoldName = (TextView) findViewById(R.id.otherCallOnHoldName);
        mOtherCallOnHoldStatus = (TextView) findViewById(R.id.otherCallOnHoldStatus);

        // Menu Button hint
        mMenuButtonHint = (TextView) findViewById(R.id.menuButtonHint);
    }

    void updateState(Phone phone) {
        if (DBG) log("updateState(" + phone + ")...");

        // Update some internal state based on the current state of the phone.
        // TODO: This code, and updateForegroundCall() / updateRingingCall(),
        // can probably still be simplified some more.

        Phone.State state = phone.getState();  // IDLE, RINGING, or OFFHOOK
        if (state == Phone.State.RINGING) {
            // A phone call is ringing *or* call waiting
            // (ie. another call may also be active as well.)
            updateRingingCall(phone);
        } else if (state == Phone.State.OFFHOOK) {
            // The phone is off hook. At least one call exists that is
            // dialing, active, or holding, and no calls are ringing or waiting.
            updateForegroundCall(phone);
        } else {
            // Presumably IDLE:  no phone activity
            // TODO: Should we ever be in this state in the first place?
            // (Is there ever any reason to draw the in-call screen
            // if the phone is totally idle?)
            // ==> Possibly during the "call ended" state, for 5 seconds
            //     *after* a call ends...
            // For now:
            Log.w(LOG_TAG, "CallCard updateState: overall Phone state is " + state);
            updateForegroundCall(phone);
        }
    }

    private void updateForegroundCall(Phone phone) {
        if (DBG) log("updateForegroundCall()...");

        Call fgCall = phone.getForegroundCall();
        Call bgCall = phone.getBackgroundCall();

        if (fgCall.isIdle() && !fgCall.hasConnections()) {
            if (DBG) log("updateForegroundCall: no active call, show holding call");
            // TODO: make sure this case agrees with the latest UI spec.

            // Display the background call in the main info area of the
            // CallCard, since there is no foreground call.  Note that
            // displayMainCallStatus() will notice if the call we passed in is on
            // hold, and display the "on hold" indication.
            fgCall = bgCall;

            // And be sure to not display anything in the "on hold" box.
            bgCall = null;
        }

        displayMainCallStatus(phone, fgCall);
        displayOnHoldCallStatus(phone, bgCall);
        displayOngoingCallStatus(phone, null);
    }

    private void updateRingingCall(Phone phone) {
        if (DBG) log("updateRingingCall()...");

        Call ringingCall = phone.getRingingCall();
        Call fgCall = phone.getForegroundCall();
        Call bgCall = phone.getBackgroundCall();

        displayMainCallStatus(phone, ringingCall);
        displayOnHoldCallStatus(phone, bgCall);
        displayOngoingCallStatus(phone, fgCall);
    }

    /**
     * Updates the main block of caller info on the CallCard
     * (ie. the stuff in the mainCallCard block) based on the specified Call.
     */
    public void displayMainCallStatus(Phone phone, Call call) {
        if (DBG) log("displayMainCallStatus(phone " + phone
                     + ", call " + call + ", state" + call.getState() + ")...");

        Call.State state = call.getState();
        int callCardBackgroundResid = 0;

        // Background frame resources are different between portrait/landscape:
        boolean landscapeMode = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        switch (state) {
            case ACTIVE:
                showCallConnected();

                callCardBackgroundResid =
                        landscapeMode ? R.drawable.incall_frame_connected_tall_land
                        : R.drawable.incall_frame_connected_tall_port;

                // update timer field
                if (DBG) log("displayMainCallStatus: start periodicUpdateTimer");
                break;

            case HOLDING:
                showCallOnhold();

                callCardBackgroundResid =
                        landscapeMode ? R.drawable.incall_frame_hold_tall_land
                        : R.drawable.incall_frame_hold_tall_port;
                break;

            case DISCONNECTED:
                reset();
                showCallEnded();

                callCardBackgroundResid =
                        landscapeMode ? R.drawable.incall_frame_ended_tall_land
                        : R.drawable.incall_frame_ended_tall_port;

                break;

            case DIALING:
            case ALERTING:
                showCallConnecting();

                callCardBackgroundResid =
                        landscapeMode ? R.drawable.incall_frame_normal_tall_land
                        : R.drawable.incall_frame_normal_tall_port;

                break;

            case INCOMING:
            case WAITING:
                showCallIncoming();

                callCardBackgroundResid =
                        landscapeMode ? R.drawable.incall_frame_normal_tall_land
                        : R.drawable.incall_frame_normal_tall_port;
               break;

            case IDLE:
                // The "main CallCard" should never display an idle call!
                Log.w(LOG_TAG, "displayMainCallStatus: IDLE call in the main call card!");
                break;

            default:
                Log.w(LOG_TAG, "displayMainCallStatus: unexpected call state: " + state);
                break;
        }

        updateCardTitleWidgets(phone, call);

 {
            // Update onscreen info for a regular call (which presumably
            // has only one connection.)
            Connection conn = call.getEarliestConnection();

            boolean isPrivateNumber = false; // TODO: need isPrivate() API

            if (conn == null) {
                if (DBG) log("displayMainCallStatus: connection is null, using default values.");
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
                updateDisplayForPerson(null, isPrivateNumber, false, call);
            } else {
                if (DBG) log("  - CONN: " + conn + ", state = " + conn.getState());

                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(
                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
                } else {
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }

                if (runQuery) {
                    if (DBG) log("- displayMainCallStatus: starting CallerInfo query...");
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, call);
                    updateDisplayForPerson(info.currentInfo, isPrivateNumber, !info.isFinal, call);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                    if (DBG) log("- displayMainCallStatus: using data we already have...");
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                        if (DBG) log("   ==> Got CallerInfo; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, false, false, call);
                    } else if (o instanceof PhoneUtils.CallerInfoToken){
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        if (DBG) log("   ==> Got CallerInfoToken; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, false, true, call);
                    } else {
                        Log.w(LOG_TAG, "displayMainCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the CallCard is probably displaying stale info
                        // right now?)  Maybe force the CallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updatePhotoForCallState(call);

        // Set the background frame color based on the state of the call.
        setMainCallCardBackgroundResource(callCardBackgroundResid);
        // (Text colors are set in updateCardTitleWidgets().)
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.
     */
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (DBG) log("onQueryComplete: token " + token + ", cookie " + cookie + ", ci " + ci);

        if (cookie instanceof Call) {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            if (DBG) log("callerinfo query complete, updating ui from displayMainCallStatus()");
            Call call = (Call) cookie;
            updateDisplayForPerson(ci, false, false, call);
            updatePhotoForCallState(call);

        } else if (cookie instanceof TextView){
            if (DBG) log("callerinfo query complete, updating ui from ongoing or onhold");
            ((TextView) cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci, getContext()));
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    public void onImageLoadComplete(int token, Object cookie, ImageView iView,
            boolean imagePresent){
        if (cookie != null) {
            updatePhotoForCallState((Call) cookie);
        }
    }

    /**
     * Updates the "upper" and "lower" titles based on the current state of this call.
     */
    private void updateCardTitleWidgets(Phone phone, Call call) {
        if (DBG) log("updateCardTitleWidgets(call " + call + ")...");
        Call.State state = call.getState();

        // TODO: Still need clearer spec on exactly how title *and* status get
        // set in all states.  (Then, given that info, refactor the code
        // here to be more clear about exactly which widgets on the card
        // need to be set.)

        // Normal "foreground" call card:
        String cardTitle = getTitleForCallCard(call);

        if (DBG) log("updateCardTitleWidgets: " + cardTitle);

        // We display *either* the "upper title" or the "lower title", but
        // never both.
        if (state == Call.State.ACTIVE) {
            // Use the "lower title" (in green).
            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
            mLowerTitleIcon.setImageResource(R.drawable.ic_incall_ongoing);
            mLowerTitle.setText(cardTitle);
            mLowerTitle.setTextColor(mTextColorConnected);
            mElapsedTime.setTextColor(mTextColorConnected);
            mElapsedTime.setBase(call.base);
            mElapsedTime.start();
            mElapsedTime.setVisibility(View.VISIBLE);
            mUpperTitle.setText("");
        } else if (state == Call.State.DISCONNECTED) {
            // Use the "lower title" (in red).
            // TODO: We may not *always* want to use the lower title for
            // the DISCONNECTED state.  "Error" states like BUSY or
            // CONGESTION (see getCallFailedString()) should probably go
            // in the upper title, for example.  In fact, the lower title
            // should probably be used *only* for the normal "Call ended"
            // case.
            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
            mLowerTitleIcon.setImageResource(R.drawable.ic_incall_end);
            mLowerTitle.setText(cardTitle);
            mLowerTitle.setTextColor(mTextColorEnded);
            mElapsedTime.setTextColor(mTextColorEnded);
            if (call.base != 0) {
	            mElapsedTime.setBase(call.base);
	            mElapsedTime.start();
	            mElapsedTime.stop();
            } else
            	mElapsedTime.setVisibility(View.INVISIBLE);
            mUpperTitle.setText("");
        } else {
            // All other states use the "upper title":
            mUpperTitle.setText(cardTitle);
            mLowerTitleViewGroup.setVisibility(View.INVISIBLE);
            if (state != Call.State.HOLDING)
            	mElapsedTime.setVisibility(View.INVISIBLE);
        }
    }

     /**
     * Returns the "card title" displayed at the top of a foreground
     * ("active") CallCard to indicate the current state of this call, like
     * "Dialing" or "In call" or "On hold".  A null return value means that
     * there's no title string for this state.
     */
    private String getTitleForCallCard(Call call) {
        String retVal = null;
        Call.State state = call.getState();
        Context context = getContext();

        if (DBG) log("- getTitleForCallCard(Call " + call + ")...");

        switch (state) {
            case IDLE:
                break;

            case ACTIVE:
                // Title is "Call in progress".  (Note this appears in the
                // "lower title" area of the CallCard.)
                retVal = context.getString(R.string.card_title_in_progress);
                break;

            case HOLDING:
                retVal = context.getString(R.string.card_title_on_hold);
                // TODO: if this is a conference call on hold,
                // maybe have a special title here too?
                break;

            case DIALING:
            case ALERTING:
                retVal = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
                retVal = context.getString(R.string.card_title_incoming_call);
                break;

            case DISCONNECTED:
                retVal = getCallFailedString(call);
                break;
        }

        if (DBG) log("  ==> result: " + retVal);
        return retVal;
    }

    /**
     * Updates the "on hold" box in the "other call" info area
     * (ie. the stuff in the otherCallOnHoldInfo block)
     * based on the specified Call.
     * Or, clear out the "on hold" box if the specified call
     * is null or idle.
     */
    public void displayOnHoldCallStatus(Phone phone, Call call) {
        if (DBG) log("displayOnHoldCallStatus(call =" + call + ")...");
        if (call == null) {
            mOtherCallOnHoldInfoArea.setVisibility(View.GONE);
            return;
        }

        Call.State state = call.getState();
        switch (state) {
            case HOLDING:
                // Ok, there actually is a background call on hold.
                // Display the "on hold" box.
                String name;

                // First, see if we need to query.
 {
                    // perform query and update the name temporarily
                    // make sure we hand the textview we want updated to the
                    // callback function.
                    if (DBG) log("==> NOT a conf call; call startGetCallerInfo...");
                    PhoneUtils.CallerInfoToken info = PhoneUtils.startGetCallerInfo(
                            getContext(), call, this, mOtherCallOnHoldName);
                    name = PhoneUtils.getCompactNameFromCallerInfo(info.currentInfo, getContext());
                }

                mOtherCallOnHoldName.setText(name);

                // The call here is always "on hold", so use the orange "hold" frame
                // and orange text color:
                setOnHoldInfoAreaBackgroundResource(R.drawable.incall_frame_hold_short);
                mOtherCallOnHoldName.setTextColor(mTextColorOnHold);
                mOtherCallOnHoldStatus.setTextColor(mTextColorOnHold);

                mOtherCallOnHoldInfoArea.setVisibility(View.VISIBLE);

                break;

            default:
                // There's actually no call on hold.  (Presumably this call's
                // state is IDLE, since any other state is meaningless for the
                // background call.)
                mOtherCallOnHoldInfoArea.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Updates the "Ongoing call" box in the "other call" info area
     * (ie. the stuff in the otherCallOngoingInfo block)
     * based on the specified Call.
     * Or, clear out the "ongoing call" box if the specified call
     * is null or idle.
     */
    public void displayOngoingCallStatus(Phone phone, Call call) {
        if (DBG) log("displayOngoingCallStatus(call =" + call + ")...");
        if (call == null) {
            mOtherCallOngoingInfoArea.setVisibility(View.GONE);
            return;
        }

        Call.State state = call.getState();
        switch (state) {
            case ACTIVE:
            case DIALING:
            case ALERTING:
                // Ok, there actually is an ongoing call.
                // Display the "ongoing call" box.
                String name;

                // First, see if we need to query.
 {
                    // perform query and update the name temporarily
                    // make sure we hand the textview we want updated to the
                    // callback function.
                    PhoneUtils.CallerInfoToken info = PhoneUtils.startGetCallerInfo(
                            getContext(), call, this, mOtherCallOngoingName);
                    name = PhoneUtils.getCompactNameFromCallerInfo(info.currentInfo, getContext());
                }

                mOtherCallOngoingName.setText(name);

                // The call here is always "ongoing", so use the green "connected" frame
                // and green text color:
                setOngoingInfoAreaBackgroundResource(R.drawable.incall_frame_connected_short);
                mOtherCallOngoingName.setTextColor(mTextColorConnected);
                mOtherCallOngoingStatus.setTextColor(mTextColorConnected);

                mOtherCallOngoingInfoArea.setVisibility(View.VISIBLE);

                break;

            default:
                // There's actually no ongoing call.  (Presumably this call's
                // state is IDLE, since any other state is meaningless for the
                // foreground call.)
                mOtherCallOngoingInfoArea.setVisibility(View.GONE);
                break;
        }
    }


    private String getCallFailedString(Call call) {
	int resID = R.string.card_title_call_ended;

	if (Receiver.call_end_reason != -1)
	    resID = Receiver.call_end_reason;

	return getContext().getString(resID);
    }

    private void showCallConnecting() {
        if (DBG) log("showCallConnecting()...");
        // TODO: remove if truly unused
    }

    private void showCallIncoming() {
        if (DBG) log("showCallIncoming()...");
        // TODO: remove if truly unused
    }

    private void showCallConnected() {
        if (DBG) log("showCallConnected()...");
        // TODO: remove if truly unused
    }

    private void showCallEnded() {
        if (DBG) log("showCallEnded()...");
        // TODO: remove if truly unused
    }
    private void showCallOnhold() {
        if (DBG) log("showCallOnhold()...");
        // TODO: remove if truly unused
    }

    /**
     * Updates the name / photo / number / label fields on the CallCard
     * based on the specified CallerInfo.
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     */
    private void updateDisplayForPerson(CallerInfo info,
                                        boolean isPrivateNumber,
                                        boolean isTemporary,
                                        Call call) {
        if (DBG) log("updateDisplayForPerson(" + info + ")...");

        // inform the state machine that we are displaying a photo.
        mPhotoTracker.setPhotoRequest(info);
        mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);

        String name;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;

        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            if (TextUtils.isEmpty(info.name)) {
                if (TextUtils.isEmpty(info.phoneNumber)) {
                	{
                        name = getContext().getString(R.string.unknown);
                    }
                } else {
                    name = info.phoneNumber;
                }
            } else {
                name = info.name;
                displayNumber = info.phoneNumber;
                label = info.phoneLabel;
            }
            personUri = ContentUris.withAppendedId(People.CONTENT_URI, info.person_id);
        } else {
        	{
                name = getContext().getString(R.string.unknown);
            }
        }
        mName.setText(name);
        mName.setVisibility(View.VISIBLE);

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We can also avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            mPhoto.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0){
            showImage(mPhoto, info.photoResource);
        } else if (!showCachedImage(mPhoto, info)) {
            // Load the image with a callback to update the image state.
            // Use a placeholder image value of -1 to indicate no image.
            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(info, 0, this, call,
                    getContext(), mPhoto, personUri, -1);
        }
        if (displayNumber != null) {
            mPhoneNumber.setText(displayNumber);
            mPhoneNumber.setVisibility(View.VISIBLE);
        } else {
//            mPhoneNumber.setVisibility(View.GONE);
        	mPhoneNumber.setText("");
        }

        if (label != null) {
            mLabel.setText(label);
            mLabel.setVisibility(View.VISIBLE);
        } else {
//            mLabel.setVisibility(View.GONE);
        	mLabel.setText("");
        }
    }

    /**
     * Updates the CallCard "photo" IFF the specified Call is in a state
     * that needs a special photo (like "busy" or "dialing".)
     *
     * If the current call does not require a special image in the "photo"
     * slot onscreen, don't do anything, since presumably the photo image
     * has already been set (to the photo of the person we're talking, or
     * the generic "picture_unknown" image, or the "conference call"
     * image.)
     */
    private void updatePhotoForCallState(Call call) {
        if (DBG) log("updatePhotoForCallState(" + call + ")...");
        int photoImageResource = 0;

        // Check for the (relatively few) telephony states that need a
        // special image in the "photo" slot.
        Call.State state = call.getState();
        switch (state) {
            case DISCONNECTED:
                // Display the special "busy" photo for BUSY or CONGESTION.
                // Otherwise (presumably the normal "call ended" state)
                // leave the photo alone.
                Connection c = call.getEarliestConnection();
                // if the connection is null, we assume the default case,
                // otherwise update the image resource normally.
                if (c != null) {
                    Connection.DisconnectCause cause = c.getDisconnectCause();
                    if ((cause == Connection.DisconnectCause.BUSY)
                        || (cause == Connection.DisconnectCause.CONGESTION)) {
                        photoImageResource = R.drawable.picture_busy;
                    }
                } else if (DBG) {
                    log("updatePhotoForCallState: connection is null, ignoring.");
                }

                // TODO: add special images for any other DisconnectCauses?
                break;

            case DIALING:
            case ALERTING:
                photoImageResource = R.drawable.picture_dialing;
                break;

            default:
                // Leave the photo alone in all other states.
                // If this call is an individual call, and the image is currently
                // displaying a state, (rather than a photo), we'll need to update
                // the image.
                // This is for the case where we've been displaying the state and
                // now we need to restore the photo.  This can happen because we
                // only query the CallerInfo once, and limit the number of times
                // the image is loaded. (So a state image may overwrite the photo
                // and we would otherwise have no way of displaying the photo when
                // the state goes away.)

                // if the photoResource field is filled-in in the Connection's
                // caller info, then we can just use that instead of requesting
                // for a photo load.

                // look for the photoResource if it is available.
                CallerInfo ci = null;
                {
                    Connection conn = call.getEarliestConnection();
                    if (conn != null) {
                        Object o = conn.getUserData();
                        if (o instanceof CallerInfo) {
                            ci = (CallerInfo) o;
                        } else if (o instanceof PhoneUtils.CallerInfoToken) {
                            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        }
                    }
                }

                if (ci != null) {
                    photoImageResource = ci.photoResource;
                }

                // If no photoResource found, check to see if this is a conference call. If
                // it is not a conference call:
                //   1. Try to show the cached image
                //   2. If the image is not cached, check to see if a load request has been
                //      made already.
                //   3. If the load request has not been made [DISPLAY_DEFAULT], start the
                //      request and note that it has started by updating photo state with
                //      [DISPLAY_IMAGE].
                // Load requests started in (3) use a placeholder image of -1 to hide the
                // image by default.  Please refer to CallerInfoAsyncQuery.java for cases
                // where CallerInfo.photoResource may be set.
                if (photoImageResource == 0) {
  {
                        if (!showCachedImage(mPhoto, ci) && (mPhotoTracker.getPhotoState() ==
                                ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT)) {
                            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(ci,
                                    getContext(), mPhoto, mPhotoTracker.getPhotoUri(), -1);
                            mPhotoTracker.setPhotoState(
                                    ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                        }
                    }
                } else {
                    showImage(mPhoto, photoImageResource);
                    mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    return;
                }
                break;
        }

        if (photoImageResource != 0) {
            if (DBG) log("- overrriding photo image: " + photoImageResource);
            showImage(mPhoto, photoImageResource);
            // Track the image state.
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }
    }

    /**
     * Try to display the cached image from the callerinfo object.
     *
     *  @return true if we were able to find the image in the cache, false otherwise.
     */
    private static final boolean showCachedImage (ImageView view, CallerInfo ci) {
        if ((ci != null) && ci.isCachedPhotoCurrent) {
            if (ci.cachedPhoto != null) {
                showImage(view, ci.cachedPhoto);
            } else {
                showImage(view, R.drawable.picture_unknown);
            }
            return true;
        }
        return false;
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, int resource) {
        view.setImageResource(resource);
        view.setVisibility(View.VISIBLE);
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
        view.setVisibility(View.VISIBLE);
    }

    private SlidingCardManager mSlidingCardManager;
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mSlidingCardManager != null) mSlidingCardManager.handleCallCardTouchEvent(ev);
        return true;
    }
    
    public void setSlidingCardManager(SlidingCardManager slidingCardManager) {
        mSlidingCardManager = slidingCardManager;
    }

    /**
     * Sets the background drawable of the main call card.
     */
    private void setMainCallCardBackgroundResource(int resid) {
        mMainCallCard.setBackgroundResource(resid);
    }

    /**
     * Sets the background drawable of the "ongoing call" info area.
     */
    private void setOngoingInfoAreaBackgroundResource(int resid) {
        mOtherCallOngoingInfoArea.setBackgroundResource(resid);
    }

    /**
     * Sets the background drawable of the "call on hold" info area.
     */
    private void setOnHoldInfoAreaBackgroundResource(int resid) {
        mOtherCallOnHoldInfoArea.setBackgroundResource(resid);
    }

    /**
     * Returns the "Menu button hint" TextView (which is manipulated
     * directly by the InCallScreen.)
     * @see InCallScreen.updateMenuButtonHint()
     */
    public /* package */ TextView getMenuButtonHint() {
        return mMenuButtonHint;
    }

    /**
     * Updates anything about our View hierarchy or internal state
     * that needs to be different in landscape mode.
     *
     * @see InCallScreen.applyConfigurationToLayout()
     */
    /* package */ public void updateForLandscapeMode() {
        if (DBG) log("updateForLandscapeMode()...");

        // The main CallCard's minimum height is smaller in landscape mode
        // than in portrait mode.
        mMainCallCard.setMinimumHeight(MAIN_CALLCARD_MIN_HEIGHT_LANDSCAPE);

        // Add some left and right margin to the top-level elements, since
        // there's no need to use the full width of the screen (which is
        // much wider in landscape mode.)
        setSideMargins(mMainCallCard, CALLCARD_SIDE_MARGIN_LANDSCAPE);
        setSideMargins(mOtherCallOngoingInfoArea, CALLCARD_SIDE_MARGIN_LANDSCAPE);
        setSideMargins(mOtherCallOnHoldInfoArea, CALLCARD_SIDE_MARGIN_LANDSCAPE);

        // A couple of TextViews are slightly smaller in landscape mode.
        mUpperTitle.setTextSize(TITLE_TEXT_SIZE_LANDSCAPE);
    }

    /**
     * Sets the left and right margins of the specified ViewGroup (whose
     * LayoutParams object which must inherit from
     * ViewGroup.MarginLayoutParams.)
     *
     * TODO: Is there already a convenience method like this somewhere?
     */
    private void setSideMargins(ViewGroup vg, int margin) {
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) vg.getLayoutParams();
        // Equivalent to setting android:layout_marginLeft/Right in XML
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        vg.setLayoutParams(lp);
    }


    // Debugging / testing code

    private void log(String msg) {
        Log.d(LOG_TAG, "[CallCard " + this + "] " + msg);
    }
}
