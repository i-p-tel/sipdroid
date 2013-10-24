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

public class Call {
    /* Enums */

    public enum State {
        IDLE, ACTIVE, HOLDING, DIALING, ALERTING, INCOMING, WAITING, DISCONNECTED;

        public boolean isAlive() {
            return !(this == IDLE || this == DISCONNECTED);
        }

        public boolean isRinging() {
            return this == INCOMING || this == WAITING;
        }

        public boolean isDialing() {
            return this == DIALING || this == ALERTING;
        }
    }
    State mState = State.IDLE;
    Connection earliest;
    public long base;

    /* Instance Methods */

    /** Do not modify the List result!!! This list is not yours to keep
     *  It will change across event loop iterations            top
     */

    public State getState() {
    	return mState;
    }
    public void setState(State state) {
    	mState = state;
    }
    public void setConn(Connection conn) {
    	earliest = conn;
    }
     
    /**
     * hasConnections
     * @return true if the call contains one or more connections
     */
    public boolean hasConnections() {
    	return true;
    }
    
    /**
     * isIdle
     * 
     * FIXME rename
     * @return true if the call contains only disconnected connections (if any)
     */
    public boolean isIdle() {
        return !getState().isAlive();
    }

    /**
     * Returns the Connection associated with this Call that was created
     * first, or null if there are no Connections in this Call
     */
    public Connection
    getEarliestConnection() {
        return earliest;
    }
    
    public boolean
    isDialingOrAlerting() {
        return getState().isDialing();
    }

    public boolean
    isRinging() {
        return getState().isRinging();
    }

}