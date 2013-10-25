package org.zoolu.net;

import android.annotation.TargetApi;
import android.os.Build;
import java.net.NetworkInterface;
import java.net.SocketException;

public class IpAddress_SDK9 {

	@TargetApi(Build.VERSION_CODES.GINGERBREAD) static public boolean isInterfaceUp(NetworkInterface intf) throws SocketException
	{
		return intf.isUp();
	}
}
