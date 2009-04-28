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

package org.sipdroid.sipua;

import java.util.Vector;

import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.authentication.DigestAuthentication;
import org.zoolu.sip.header.AuthorizationHeader;
import org.zoolu.sip.header.ContactHeader;
import org.zoolu.sip.header.ExpiresHeader;
import org.zoolu.sip.header.Header;
import org.zoolu.sip.header.StatusLine;
import org.zoolu.sip.header.WwwAuthenticateHeader;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.message.MessageFactory;
import org.zoolu.sip.message.SipMethods;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.transaction.TransactionClient;
import org.zoolu.sip.transaction.TransactionClientListener;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

/**
 * Register User Agent. It registers (one time or periodically) a contact
 * address with a registrar server.
 */
public class RegisterAgent implements TransactionClientListener {
	/** Max number of registration attempts. */
	static final int MAX_ATTEMPTS = 3;
	
	/* States for the RegisterAgent Module */
	public static final int UNREGISTERED = 0;
	public static final int REGISTERING = 1;
	public static final int REGISTERED = 2;
	public static final int DEREGISTERING = 3;
	
	/** RegisterAgent listener */
	RegisterAgentListener listener;

	/** SipProvider */
	SipProvider sip_provider;

	/** User's URI with the fully qualified domain name of the registrar server. */
	NameAddress target;

	/** User name. */
	String username;

	/** User realm. */
	String realm;

	/** User's passwd. */
	String passwd;

	/** Nonce for the next authentication. */
	String next_nonce;

	/** Qop for the next authentication. */
	String qop;

	/** User's contact address. */
	NameAddress contact;

	/** Expiration time. */
	int expire_time;

	/** Whether keep on registering. */
	boolean loop;

	/** Event logger. */
	Log log;

	/** Number of registration attempts. */
	int attempts;

	/** Current State of the registrar component */
	int CurrentState = UNREGISTERED;

	/** Creates a new RegisterAgent. */
	public RegisterAgent(SipProvider sip_provider, String target_url,
			String contact_url, RegisterAgentListener listener) {
		init(sip_provider, target_url, contact_url, listener);
	}

	/**
	 * Creates a new RegisterAgent with authentication credentials (i.e.
	 * username, realm, and passwd).
	 */
	public RegisterAgent(SipProvider sip_provider, String target_url,
			String contact_url, String username, String realm, String passwd,
			RegisterAgentListener listener) {
		
		init(sip_provider, target_url, contact_url, listener);
		
		// authentication specific parameters
		this.username = username;
		this.realm = realm;
		this.passwd = passwd;
	}

	public void halt() {
		this.listener = null;
	}
	
	/** Inits the RegisterAgent. */
	private void init(SipProvider sip_provider, String target_url,
			String contact_url, RegisterAgentListener listener) {
		
		this.listener = listener;
		this.sip_provider = sip_provider;
		this.log = sip_provider.getLog();
		this.target = new NameAddress(target_url);
		this.contact = new NameAddress(contact_url);
		this.expire_time = SipStack.default_expires;
		
		// authentication
		this.username = null;
		this.realm = null;
		this.passwd = null;
		this.next_nonce = null;
		this.qop = null;
		this.attempts = 0;
	}

	/** Whether it is periodically registering. */
	public boolean isRegistered() {
		return (CurrentState == REGISTERED || CurrentState == REGISTERING);
	}
	
	/** Registers with the registrar server. */
	public boolean register() {
		return register(expire_time);
	}

	/** Registers with the registrar server for <i>expire_time</i> seconds. */
	public boolean register(int expire_time) {
		attempts = 0;
		if (expire_time > 0)
		{
			//Update this to be the default registration duration for next
			//instances as well.
			
			if (CurrentState != UNREGISTERED && CurrentState != REGISTERED)
			{
				return false;
			}
			this.expire_time = expire_time;
			CurrentState = REGISTERING;
		}
		else
		{
			if (CurrentState != REGISTERED)
			{
				//This is an error condition we must exit, we should not de-register if
				//we have not registered at all
				return false;
			}
			//this is the case for de-registration
			expire_time = 0;
			CurrentState = DEREGISTERING;
		}
		
		//Create message re
		Message req = MessageFactory.createRegisterRequest(sip_provider,
				target, target, contact);
		
		req.setExpiresHeader(new ExpiresHeader(String.valueOf(expire_time)));
		
		//create and fill the authentication params this is done when
		//the UA has been challenged by the registrar or intermediate UA
		if (next_nonce != null) 
		{
			AuthorizationHeader ah = new AuthorizationHeader("Digest");
			
			ah.addUsernameParam(username);
			ah.addRealmParam(realm);
			ah.addNonceParam(next_nonce);
			ah.addUriParam(req.getRequestLine().getAddress().toString());
			ah.addQopParam(qop);
			String response = (new DigestAuthentication(SipMethods.REGISTER,
					ah, null, passwd)).getResponse();
			ah.addResponseParam(response);
			req.setAuthorizationHeader(ah);
		}
		
		if (expire_time > 0)
		{
			printLog("Registering contact " + contact + " (it expires in "
					+ expire_time + " secs)", LogLevel.HIGH);
		}
		else
		{
			printLog("Unregistering contact " + contact, LogLevel.HIGH);
		}
		
		TransactionClient t = new TransactionClient(sip_provider, req, this);
		t.request();
		
		return true;
	}

	/** Unregister with the registrar server */
	public boolean unregister() {
		return register(0);
	}

	// **************** Transaction callback functions *****************

	/** Callback function called when client sends back a failure response. */

	/** Callback function called when client sends back a provisional response. */
	public void onTransProvisionalResponse(TransactionClient transaction,
			Message resp) { // do nothing..
	}

	/** Callback function called when client sends back a success response. */
	public void onTransSuccessResponse(TransactionClient transaction,
			Message resp) 
	{
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER)) {
			
			if (resp.hasAuthenticationInfoHeader()) 
			{
				next_nonce = resp.getAuthenticationInfoHeader()
						.getNextnonceParam();
			}
			
			StatusLine status = resp.getStatusLine();
			String result = status.getCode() + " " + status.getReason();

			int expires = 0;
			if (resp.hasExpiresHeader()) 
			{
				expires = resp.getExpiresHeader().getDeltaSeconds();
			} 
			else if (resp.hasContactHeader()) 
			{
				Vector<Header> contacts = resp.getContacts().getHeaders();
				for (int i = 0; i < contacts.size(); i++) {
					int exp_i = (new ContactHeader((Header) contacts
							.elementAt(i))).getExpires();
					if (exp_i > 0 && (expires == 0 || exp_i < expires))
						expires = exp_i;
				}
			}
			
			printLog("Registration success: " + result, LogLevel.HIGH);
			
			if (CurrentState == REGISTERING)
			{
				CurrentState = REGISTERED;
				if (listener != null)
				{
					listener.onUaRegistrationSuccess(this, target, contact, result);
					Receiver.reRegister(expires);
				}
			}
			else
			{
				CurrentState = UNREGISTERED;
			}
		}
	}

	/** Callback function called when client sends back a failure response. */
	public void onTransFailureResponse(TransactionClient transaction,
			Message resp) {
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER)) {
			StatusLine status = resp.getStatusLine();
			int code = status.getCode();
			if (code == 401
					&& attempts < MAX_ATTEMPTS
					&& resp.hasWwwAuthenticateHeader()
					&& resp.getWwwAuthenticateHeader().getRealmParam()
							.equalsIgnoreCase(realm)) {
				attempts++;
				Message req = transaction.getRequestMessage();
				req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
				
				WwwAuthenticateHeader wah = resp.getWwwAuthenticateHeader();
				String qop_options = wah.getQopOptionsParam();
				
				printLog("DEBUG: qop-options: " + qop_options, LogLevel.MEDIUM);
				
				qop = (qop_options != null) ? "auth" : null;
				
				AuthorizationHeader ah = (new DigestAuthentication(
						SipMethods.REGISTER, req.getRequestLine().getAddress()
								.toString(), wah, qop, null, username, passwd))
						.getAuthorizationHeader();
				req.setAuthorizationHeader(ah);
				
				TransactionClient t = new TransactionClient(sip_provider, req,
						this);
				
				t.request();
				
				//He we need not change the current state since, in case it was
				//a case of registration, we are registering and if it was a 
				//case of de-registration then we are de-registering
				
			} else {
				String result = code + " " + status.getReason();
				
				//Since the transactions are atomic, we rollback to the 
				//previous state
				if (CurrentState == REGISTERING)
				{
					CurrentState = UNREGISTERED;
					if (listener != null)
					{
						listener.onUaRegistrationFailure(this, target, contact,
								result);
						Receiver.reRegister(1000);
					}
				}
				else
				{
					CurrentState = REGISTERED;
				}
				
				printLog("Registration failure: " + result, LogLevel.HIGH);
			}
		}
	}

	/** Callback function called when client expires timeout. */
	public void onTransTimeout(TransactionClient transaction) {
		if (transaction.getTransactionMethod().equals(SipMethods.REGISTER)) {
			printLog("Registration failure: No response from server.",
					LogLevel.HIGH);
			
			//Since the transactions are atomic, we rollback to the 
			//previous state
			
			if (CurrentState == REGISTERING)
			{
				CurrentState = UNREGISTERED;
				
				if (listener != null)
				{
					listener.onUaRegistrationFailure(this, target, contact,
							"Timeout");
					Receiver.reRegister(1000);
				}
			}
			else
			{
				CurrentState = REGISTERED;
			}
		}
	}

	// ****************************** Logs *****************************

	/** Adds a new string to the default Log */
	void printLog(String str, int level) {
		if (Sipdroid.release) return;
		if (log != null)
			log.println("RegisterAgent: " + str, level + SipStack.LOG_LEVEL_UA);
		if (level <= LogLevel.HIGH)
			System.out.println("RegisterAgent: " + str);
	}

	/** Adds the Exception message to the default Log */
	void printException(Exception e, int level) {
		if (Sipdroid.release) return;
		if (log != null)
			log.printException(e, level + SipStack.LOG_LEVEL_UA);
	}

}
