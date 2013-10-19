/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package org.sipdroid.net;

import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.message.Message;

/**
 * KeepAliveSip thread, for keeping the connection up toward a target SIP node
 * (e.g. toward the serving proxy/gw or a remote UA). It periodically sends
 * keep-alive tokens in order to refresh TCP connection timeouts and/or NAT
 * TCP/UDP session timeouts.
 */
public class KeepAliveSip {
	/** SipProvider */
	SipProvider sip_provider;

	/** Sip message */
	Message message = null;

	public KeepAliveSip(SipProvider sip_provider) {
		this.sip_provider = sip_provider;
		if (message == null) {
			message = new Message("\r\n");
		}
	}

	/** Sends the keep-alive packet now. */
	public void sendToken() throws java.io.IOException { // do send?
		if (sip_provider != null) {
			sip_provider.sendMessage(message);
		}
	}

	/** Gets a String representation of the Object */
	public String toString() {
		String str = null;
		if (sip_provider != null) {
			str = "sip:" + sip_provider.getViaAddress() + ":"
					+ sip_provider.getPort();
		}
		return str;
	}

}