package com.jstun.demo;

import java.net.BindException;
import java.net.InetAddress;

public class DiscoveryTestDemo implements Runnable {
	InetAddress iaddress;
	
	public DiscoveryTestDemo(InetAddress iaddress) {
		this.iaddress = iaddress;
	}
	
	public void run() {
		try {
			DiscoveryTest test = new DiscoveryTest(iaddress, "jstun.javawi.de", 3478);
			//DiscoveryTest test = new DiscoveryTest(iaddress, "stun.sipgate.net", 10000);
			// iphone-stun.freenet.de:3478
			// larry.gloo.net:3478
			// stun.xten.net:3478
			// stun.sipgate.net:10000
			System.out.println(test.test());
		} catch (BindException be) {
			System.out.println(iaddress.toString() + ": " + be.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
