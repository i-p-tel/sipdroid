| Copyright (C) 2010 The Sipdroid Open Source Project. The following article is part of Sipdroid. Sipdroid is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|



## Introduction ##

Bluetooth support is still marked "experimental" in Sipdroid. This article explains why, what's possible and how. First, Bluetooth is supported starting with Android "Froyo". If Bluetooth has been enabled and paired to a suitable device Sipdroid will show a "Bluetooth" button while in call. The wireless settings within Sipdroid allow to switch on Bluetooth for all calls by default.

## Bluetooth/WLAN interference ##

Sipdroid calls are often placed over Bluetooth which operates on the same wireless frequency as WLAN. Like surfing over WLAN during a regular Bluetooth call we observed dropouts which do not occur when calling over 3G, this disabling Wi-Fi while Bluetooth is enabled.

If your phone is rooted you can write the line "Master=true" to /etc/bluetooth/audio.conf (as outlined in http://code.google.com/p/android/issues/detail?id=2765). This workaround solved all such interference issues for us!

## Handsfree vs. Headset vs. A2DP profiles ##

An issue has been reported about A2DP headsets staying silent with Sipdroid (see http://code.google.com/p/sipdroid/issues/detail?id=561). We have not been able to reproduce this at i-p-tel yet.

However, we found that Bluetooth devices supporting Handsfree profile show similar issues. Reason here is that they require not only a SCO audio connection to be established but also call control messages over the ACL link (which is still exclusively maintained by the built-in Phone application).

A workaround for this issue is disabling Handsfree profile to make the device work over Headset profile. We installed the "Autostart (Root)" app, and put "/system/bin/sdptool del 0x10003" into "/data/opt/autostart.sh". This worked as long as Bluetooth is enabled, and hasn't been disabled since boot, and the Bluetooth connection is established from the device to your phone.

## ZOMM device ##

![http://store.zomm.com/product_images/p/881/WHITE_ZOMM_ST_FRONT__14846_thumb.png](http://store.zomm.com/product_images/p/881/WHITE_ZOMM_ST_FRONT__14846_thumb.png)

A nice gadget we have successfully been using with above mentioned tweaks is the "ZOMM". It's speakerphone features strong echo cancellation suitable for VoIP needs.