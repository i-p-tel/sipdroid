| Copyright (C) 2009 The Sipdroid Open Source Project. The following article is part of Sipdroid. Sipdroid is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|



## Introduction ##

With Sipdroid you will most probably use more than one access point, especially when using it with 3G. Our experience shows that sometimes base stations introduce packet loss due to defective links to the Internet, and access points can lose packets due to interference with other APs. This can occur at random times and be in the range of several percent making audio sound choppy from time to time.

A solution that has been proposed is sending audio data over a reliable protocol instead of UDP. Actually RTP over TCP gave higher MOS (Mean Opinion Score - measure of voice quality) than RTP over UDP in some surveys. We found two articles researching the pros and  cons and presenting developments in this area:
  * [TCP-FCW – transport protocol for real-time transmissions on high-loss networks. Sergei Kozlov, 23-02-2004.](http://www.win.tue.nl/~johanl/projects/QoS/Kozlov/tcp-fcw_natlab_03.ppt)
  * [The Internet Measurement of VoIP on different Transport Layer Protocols](http://netarchlab.tsinghua.edu.cn/~wzl/pubs/2009_ICOIN_transport_measure.pdf)

Creating a modified TCP stack is beyond the scope of Sipdroid, and it looks like modifying TCP does not necessarily improve it. A major problem is that requesting retransmission of lost packets on demand adds one (or even more) round trip time(s) to overall voice delay.

## How Sipdroid handles packet loss ##

Motivated by the fact that one packet loss will likely be followed by more if the network is faulty we instead request retransmissions for the future. When packet losses get less retransmissions are immediately stopped.

This is done when the option "Improve Audio" is activated. When packet loss climbs over 1% the call will use double bandwidth, and the ratio of lost data should drop to the square root of its original value, e.g. at a packet loss of 10% (0.1) we expect 1% lost packets (0.01) when all frames are sent twice.

Of course this won't work when bandwidth is the bottleneck. So if you don't have HSDPA/HSUPA on your 3G network you will not want to enable this option. On HSDPA/HSUPA and Wifi call quality can only get better. Latency is not affected.

## Statistics ##

The following screenshots show sample statistics. The left one was taken on an error free network. The right one displayed 5% packet loss caused by a congested network. By using retransmissions the actual number of packets lost was reduced to <0.5%. Late packets indicate jitter problems.

![http://sipdroid.googlecode.com/svn/images/stats1.png](http://sipdroid.googlecode.com/svn/images/stats1.png)
![http://sipdroid.googlecode.com/svn/images/stats2.png](http://sipdroid.googlecode.com/svn/images/stats2.png)