| Copyright (C) 2009 The Sipdroid Open Source Project. The following article is part of Sipdroid. Sipdroid is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

## User Interface ##

### How can I dial my contacts over Sipdroid / over Phone? ###

In settings you can specify your preferred call type. This is used when clicking a "Call" tab of a contact. 

### How can I modify phone numbers on the fly? Where is the prefix option? ###

The _prefix_ option has been replaced by a more generic and flexible _search & replace_ feature. The "search & replace" option, available in **Advanced Options**, has the following form: `<search>,<replace>`

`<search>` is a [Java regular expression](http://java.sun.com/docs/books/tutorial/essential/regex/) with one or more [groups](http://java.sun.com/docs/books/tutorial/essential/regex/groups.html) which can be then use in `<replace>`

`<replace>` is a string with reference to group(s) from the `<search>` field.

If the regular expression is not valid or the format is wrong, the original number is used.

Examples:

| _Search & Replace_ | _Description_ |
|:-------------------|:--------------|
| `\+*1*(.*),1\1`    | fix for the common issue of contacts with or without 1 and +1 |
| `\+41(.*),0041\1`  | replace +41791111111 with 0041791111111 |
| `(.*),123\1`       | add 123 in front of the number (prefix) |
| `(.*),\1123`       | add 123 at the end of the number (postfix) |

### How do I exclude certain numbers from Sipdroid? ###

Choose **Advanced Options** and enter your pattern in the **exclude pattern** field.
Numbers can be exluded by type and/or number.
<br> Format is pattern,pattern,pattern...<br>
<br>
To exclude by <b>phone type</b> you can enter single or combination of the three supported types:<br>
<br>e.g. h,m,w or ho,mo,wo or home,mobile,work (excludes all home, mobile and work numbers)<br>
<br>
To exclude by <b>numbers</b> enter any java regular expression:<br>
<br>e.g. \A\+01 (excludes all numbers starting with +01).<br>
<br>
To exclude by <b>both</b> phone type and number:<br>
<br>e.g. m,1234 (exludes all mobile and numbers that contain 1234).<br>
<br>
<h3>What is "Trigger Callback/Callthru"?</h3>

Suppose you are outside your wireless LAN, or got a mobile Internet connection, but not good enough for a SIP call. Normally the dialer would fall back to Phone in this case.<br>
<br>
<ul><li>Enabling "trigger callback" will instead ask the PBX to initiate a callback to your mobile number, and connect the call to the dialed contact.<br>
<ul><li>This makes sense if you like the PBX features (like calling a SIP URI, simultaneous outbound, etc.), or if it's cheaper than a call from your mobile. I was using the function while roaming with a local SIM card. Some plans also allow you to get unlimited calls from a certain number. That number could be assigned to your PBX.</li></ul></li></ul>

<ul><li>Enabling "trigger callthru" will call a PBX's trunk/DID (or any other calling card platform), dial PIN & destination number as DTMF tones, and connect you.<br>
<ul><li>This is nice if you have unlimited minutes to a certain number or nationwide, and want to use them to dial out or dial internationally.</li></ul></li></ul>

Exclude pattern and search & replace get applied in the same way as for SIP calls.<br>
<br>
### How much will Sipdroid drain the battery? (Constantly running Internet connection?) ###

When using [pbxes.org](http://pbxes.org) the expected stand-by times are the same as without Sipdroid running in the background (we observed about three days of stand-by time).  See [article](http://code.google.com/p/sipdroid/wiki/NewStandbyTechnique) for background.

### How much bandwidth is required? ###

Sipdroid uses G.711 A-law to transmit voice which needs about 80 kBit/s in each direction. This corresponds to a total of 1.2 MB per minute.

Over EDGE or when optionally enabled for all calls Sipdroid uses GSM codec to compress to about 30 kBit/s in each direction resulting in a total of 0.5 MB per minute.

## Legal ##

### How can I prepare for mobile VoIP? ###

Sipdroid allows you to choose where you will use VoIP, on WLANs only, on 3G, or EDGE networks. If you are going to use VoIP services outside private wireless networks, it's your responsibility to check the contractual terms with the mobile operator:

Often mobile phone operators forbid using VoIP in the contract's fine print. The European Union is already looking at whether blocks on VoIP service by Europe's mobile phone operators might breach competition laws.

The following plans are suitable for unlimited hotspot/3G usage:

  * USA: "T-Mobile has **no restrictions** on using SIP as a signaling protocol." ([source](http://groups.google.com/group/android-developers/browse_thread/thread/9e471b66e7f397b1/d5051b3d37c79beb?hl=en&ie=UTF-8&q=sipdroid#d5051b3d37c79beb))
  * Germany: [debitel Talk and Surf S](https://www.debitel.de/privat_shop/mobilfunk/tarife/uebersicht/tariftabelle/debitel-talk-and-surf-s-im-dt.-t-mobile-netz-22723.php), all T-Mobile and Vodafone plans with VoIP Option, and all [O2 customers with Internet Packs](http://www.de.o2.com/ext/o2/wizard/index?page_id=15698;tree_id=303;category_id=;year=;page=1;state=online;style=portal)

[Add](http://code.google.com/p/sipdroid/w/edit/FAQ) more!

### What type of warranty comes with Sipdroid? ###

This project publishes Sipdroid for free under the terms of [GNU General Public License v3](http://www.gnu.org/licenses/gpl.html). The first public version is beta for software testing. So please allow for some issues and incompatibilities at the beginning.

### What build environment is needed for Sipdroid? ###

If you are interested in developing and contributing to the project please install [Eclipse](http://developer.android.com/sdk/1.1_r1/installing.html) and [Subclipse](http://subclipse.tigris.org/install.html) for building the source code.

After the sources are done downloading, you see the following in "Package Explorer"
in Eclipse.

> SipUA 326 [Trunk: trunk](http://sipdroid.googlecode.com/svn,)

Right click on SipUA and select Properties in the drop down menu (last option). Click
on Android (assumes the Android SDK is installed and configured in Eclipse). In the
"Project Build Target" select "Android 1.6" (Target Name).

<h2>SIP Providers</h2>

<h3>What SIP providers is Sipdroid compatible with?</h3>

Sipdroid runs standard SIP. For full interoperability register Sipdroid to <a href='http://pbxes.org'>pbxes.org</a>. From there register your SIP accounts.<br>
<br>
Possible issues when registering directly to other SIP servers are:<br>
<br>
<ul><li>Battery drain on 3G/EDGE,<br>
</li><li>Unreliable incoming calls,<br>
</li><li>One way voice, or<br>
</li><li>No connection at all</li></ul>

<b>As you can see there is not just one issue. When Nokia released its SIP client in 2006 PBXes were the first to support it. It's a bunch of adjustments to account for phones that are not plugged into a single network, are battery powered and Java based. So unless you are a developer we don't recommend spending much time in getting your plain-vanilla Asterisk box suited for Sipdroid. Instead register your Asterisk to PBXes, too.</b>

For the future it is expected that SIP providers will become more interoperable (see also <a href='http://code.google.com/p/sipdroid/wiki/NewStandbyTechnique'>article</a>). As Sipdroid is a true open source project attracting many software developers, the application and its documentation will be updated as more information gets available.<br>
<br>
<h3>What advantages do I get by signing up for the Free Account at PBXes?</h3>

<ul><li>You can register several trunks to use multiple telephony service providers of your choice.<br>
</li><li><a href='http://pbxes.org'>pbxes.org</a> routes incoming calls over Sipdroid or Phone to you. If you are online you get them over VoIP, if off line you get them over GSM.</li></ul>

<h3>Which features are supported in Sipdroid?</h3>

Sipdroid presently supports basic call. All the typical PBX features are configured from the PC. This concept is much like the concept of Google's services that allow you e.g. to manage your calendar on the PC and access it remotely on your phone. The following features are provided by the Free Account on <a href='http://pbxes.org'>pbxes.org</a>:<br>
<br>
<ul><li>Change number format (e.g. convert the + codes)<br>
</li><li>Music on hold<br>
</li><li>Support of several modes for DTMF tones<br>
</li><li>Support for NAT (network address translation)<br>
</li><li>Simultaneous Outbound Calling<br>
</li><li>Screening anonymous callers<br>
</li><li>Time-based routing for incoming calls<br>
</li><li>Attended Call Transfer<br>
</li><li>Conferences<br>
</li><li>Trigger callback or callthru (if no suitable data network available)</li></ul>

You get even more <a href='http://pbxes.org/iptel_virtual-pbx.html'>features</a> on the paid accounts (e.g. Call Recording, Improved Audio, Handoff of calls between networks).<br>
<br>
<h3>How can I set up PBXes?</h3>

Please have a look at the <a href='http://pbxes.org/community_e.php?display=wiki'>HELP section</a>. Under "Getting started" you find an introduction to the necessary steps.<br>
<br>
<b>It's all web based. No installation required.</b>

First you create extension(s) and trunk(s). Perform an echo test by dialing <code>*</code>43. Then you add at least one inbound and one outbound route. Leave the trunk name empty on your inbound route. The Android specific details can be found under "Mobile Extensions".<br>
<br>
The username to be entered in Sipdroid consists of account name, a dash ('-') and the extension number, e.g. "myuser-200". The password is that of the extension, not the password of your PBXes account.<br>
