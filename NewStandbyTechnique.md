| Copyright (C) 2009 The Sipdroid Open Source Project. The following article is part of Sipdroid. Sipdroid is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|



## Introduction ##

When Sipdroid was first published the phone had to be woken up by cellular calls to see incoming SIP invites. The reason for this early approach was that the developer version of Cupcake left wireless LAN completely inactive during sleep mode. When Cupcake was finalized wireless LAN and 3G radio still ran while the screen was turned off. We did not change anything to above concept to avoid permanently sending keep alive packets over 3G which would have caused the radio to stay on most of the time thus totally draining the battery within about 15 hours.

Although this scheme simplified software design users complained about complicated set-up and call answer.

In **Sipdroid release 1.0.2** a totally new scheme was introduced. The first developer who mentioned the idea for Sipdroid was [John Kielkopf](http://code.google.com/p/sipdroid/issues/detail?id=17&can=1). Later we discovered that there were already several others (e.g. [Nokia](http://research.nokia.com/files/NRCTR2008002.pdf)) who did research in this area before.

Sipdroid developers [zlabunk and wavehill09](http://code.google.com/p/sipdroid/people/list)  laid more fundaments by adding vibrate and ringtone functionality.

## NAT traversal ##

Most networks, no matter if LAN, wireless or mobile, today use NAT routers to provide enough addresses to their clients. The NAT proxy cannot keep connections open forever. Depending on protocol type typical timeout values are: (reproduced from [here](http://research.nokia.com/files/NRCTR2008002.pdf))

| Protocol | Timeout |
|:---------|:--------|
| UDP      | 40s to 300s |
| TCP      | 30min to 1440min |

TCP timeouts are much longer. So for NAT traversal much less packets have to be sent over TCP compared to UDP.

## The new technique ##

Sipdroid now uses TCP for the signaling connection and keeps the corresponding port open.

Sipdroid's "mothership" [PBXes](http://pbxes.org) (we took this nice image from [Arnon's blog](http://www.morethantechnical.com/2009/07/16/voip-for-android-is-in-town/)) had also to be extended because - as most SIP services - it did not support TCP before.

So if your service cannot handle SIP over TCP you can use PBXes to translate between the protocols. Most commonly UDP protocol is used to carry SIP signaling and voice packets. However, when SIP was [introduced](http://tools.ietf.org/html/rfc2543) TCP had already been included as an option.

Sipdroid now also contains the necessary code to ring and turn on the screen when a call comes in.

## Dynamic IP ##

Wireless LAN routers can change their public IPs without notice. Sipdroid now sends probe packets every minute over WLANs to detect such changes.

## Results ##

Standby times have been measured under the following parameters:

  * 3G and wireless LAN: 3 bars
  * operating system: ADP1.5
  * server platform: PBXes

| Network Type | Standby Time |
|:-------------|:-------------|
| Wireless LAN | 70h          |
| 3G (TCP)     | 140h         |
| 3G (UDP)     | 15h          |

3G offers superior standby performance. Wireless LAN is better when talking. This is supported by Android's default policy switching off Wireless LAN while sleeping. Sipdroid keeps WLAN active while in call.

So Sipdroid could maintain its early standby times while extending functionality to a self-contained phone.

## Implications for the future ##

More and more always-on services evolve. At the moment we know of VoIP (of course), the Google services (Mail, IM, Sync), and E-Mail. Although TCP based software does only transmit a few keepalive packets, every service lowers standby time by about 10%. So for the future we want to look for other data activity and piggyback keep alive transmission to them.