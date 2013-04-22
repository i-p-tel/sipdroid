/*
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of MjSip (http://www.mjsip.org)
 * 
 * MjSip is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MjSip is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MjSip; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package org.zoolu.net;

import java.net.InetSocketAddress;
import java.net.Socket; // import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * TcpSocket provides a uniform interface to TCP transport protocol, regardless
 * J2SE or J2ME is used.
 */
public class TcpSocket {
	/** Socket */
	Socket socket;

	/** Creates a new TcpSocket */
	TcpSocket() {
		socket = null;
	}

	/** Creates a new TcpSocket */
	TcpSocket(Socket sock) {
		socket = sock;
	}

	static boolean lock;
	
	/** Creates a new UdpSocket */
	public TcpSocket(IpAddress ipaddr, int port, String host) throws java.io.IOException {
//		socket = new Socket(ipaddr.getInetAddress(), port); modified
		SSLSocketFactory f = 
	         (SSLSocketFactory) SSLSocketFactory.getSocketFactory();
		if (host == null)
			socket = new Socket();
		else
			socket = f.createSocket();
		if (lock) throw new java.io.IOException();
		lock = true;
		try {
			socket.connect(new InetSocketAddress(ipaddr.toString(), port),
				Thread.currentThread().getName().equals("main")?1000:10000);
		} catch (java.io.IOException e) {
			lock = false;
			throw e;
		}
		if (host != null) {
			HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
			SSLSession s = ((SSLSocket)socket).getSession();
			if (!hv.verify(host, s)) {
				lock = false;
			    throw new java.io.IOException();
			}
		}
		lock = false;
	}

	/** Closes this socket. */
	public void close() throws java.io.IOException {
		socket.close();
	}

	/** Gets the address to which the socket is connected. */
	public IpAddress getAddress() {
		return new IpAddress(socket.getInetAddress());
	}

	/** Gets an input stream for this socket. */
	public InputStream getInputStream() throws java.io.IOException {
		return socket.getInputStream();
	}

	/** Gets the local address to which the socket is bound. */
	public IpAddress getLocalAddress() {
		return new IpAddress(socket.getLocalAddress());
	}

	/** Gets the local port to which this socket is bound. */
	public int getLocalPort() {
		return socket.getLocalPort();
	}

	/** Gets an output stream for this socket. */
	public OutputStream getOutputStream() throws java.io.IOException {
		return socket.getOutputStream();
	}

	/** Gets the remote port to which this socket is connected. */
	public int getPort() {
		return socket.getPort();
	}

	/** Gets the socket timeout. */
	public int getSoTimeout() throws java.net.SocketException {
		return socket.getSoTimeout();
	}

	/** Enables/disables the socket timeou, in milliseconds. */
	public void setSoTimeout(int timeout) throws java.net.SocketException {
		socket.setSoTimeout(timeout);
	}

	/** Converts this object to a String. */
	public String toString() {
		return socket.toString();
	}

}
