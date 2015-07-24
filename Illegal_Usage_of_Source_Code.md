

## Illegal Usage of Sipdroid by Gizmo5, Inc. ##

### Resolved ###

On July 23, 2009 Gizmo5 provided a link to the modified source code at the place that is required by the GPL open source license.

Regarding the missing copyright message we have added additional terms according to GPL
section 7 for future cases.

### Violations ###

Gizmo5 published Guava which is based on Sipdroid.

  * No source code was offered on their [download site](http://gizmo5.com/guava).
  * The copyright message accessible after first startup of the program start has been removed.

### Protecting our Rights ###

  * Sent Email to Gizmo5

<pre>
-------- Original-Nachricht --------<br>
Betreff: From Sipdroid Open Source Project...<br>
Datum: Tue, 21 Jul 2009 00:34:55 +0200<br>
Organisation: i-p-tel GmbH<br>
An: press@gizmo5inc.com<br>
<br>
Dear sirs,<br>
<br>
you published "Guava" in violation of the GNU public license.<br>
<br>
Furthermore you removed the copyright notice. This is violating copyright laws.<br>
<br>
This is highly abominable because you're stealing intellectual property. Please stop immediately. We will inform legal authorities.<br>
<br>
--<br>
Regards,<br>
Pascal Merle<br>
</pre>

  * Sent takedown notice under the DMCA to Google Android team

![http://sipdroid.googlecode.com/svn/images/gizmo_copyright_violation.png](http://sipdroid.googlecode.com/svn/images/gizmo_copyright_violation.png)

  * Sent GPL violation reports to http://gpl-violations.org and [Freedom Task Force (FTF)](http://fsfe.org/projects/ftf/reporting-fixing-violations.en.html)

### Reactions ###

  * For a few days Michael Robertson provided a [download link](http://gizmo5.com/guava.tgz) for the modified source on his [Guava Feedback Forum](http://guava.uservoice.com/pages/23533-guava-/suggestions/263364-make-the-source-code-available-for-download). The entry has later been hidden from index and search by marking it "other".

  * And he replied by Email

<pre>
Pascal,<br>
<br>
We did not publish Guava in violation of the GNU public license. The<br>
about screen says "Guava is based on SIPdroid." and lists all the<br>
copyright holders of the software. I'm not sure why you're telling<br>
people we removed the copyright notices. I guess you want to stir up<br>
controversy. If it helps people find out about SIPdroid and GUAVA then<br>
I'm all for that. I actually appreciate you reaching out reporters and<br>
telling them about GUAVA because it helps get the word out. When<br>
combined with Google Voice it's really a fantastic value for consumers<br>
- a totally free phone with US number for making/receiving calls AND<br>
SMS without paying the carriers. Yeah.<br>
<br>
In addition the source code is available at: http://gizmo5.com/guava.tgz<br>
<br>
But you already know that and want to stir up controversy. That's cool<br>
by me.<br>
<br>
I appreciate your enthusiasm to stand up for open source, but I don't<br>
understand why you didn't just send me email if you had an issue? Or<br>
send an email to the blog http://www.noSIMcard.com ? I'm also a big<br>
believer in open source. I've spent millions of my own money support<br>
open source initiatives and more money from the companies that I've<br>
started and run to support open source. Of course people know I did<br>
Lindows/Linspire and we built lots of code and paid for other code<br>
which went back to the community. A small example is Firefox's<br>
'underline in red when I do a misspelling' - that's code I paid to<br>
have written and gave to Mozilla. Way back at MP3.com we gave money to<br>
support an unknown open source database called Mysql which went onto<br>
bigger things.<br>
<br>
The ironic thing is that I tried to contact you at PBXes.org and let<br>
you know about our project but there were no email addresses listed on<br>
your website that I could find. So I tried guessing your address:<br>
pascal@pbxes.org and pascal.merle@pbxes.org and those both bounced. So<br>
then I tried submitting a customer support ticket, but your system is<br>
broken and doesn't allow new users to signup (at least it didn't for<br>
any of the several usernames I tried to register). Sorry you didn't<br>
get a heads-up on our project.<br>
<br>
I see the complaints from some about you tying your company pbxes.org<br>
into the software. I see from this thread that some people have a<br>
problem with that. You can't please all the people all the time.<br>
<br>
As we stated on the blog, GUAVA is alpha now so lots of issues.<br>
Biggest issue is that the software does not automatically re-register<br>
whenever the net changes or has a hiccup. This causes the software to<br>
become unregistered if you leave it running for awhile or change wifi<br>
networks. We're rewriting the registration portion of the code to put<br>
it in its own thread so it will automatically reconnect if there's a<br>
disturbance in the network. Hope to have that done later this week.<br>
<br>
You might want to also look at the code we added to use TCP for<br>
registration in addition to UDP. We've found this often helps traverse<br>
troublesome NATS and routers.<br>
<br>
-- MR<br>
</pre>

  * Chris DiBona from Google responded

<pre>
Hi Pascal and other Sipdroiders.<br>
A couple of things;<br>
1) The Guava package is not in the android market, they host it on<br>
their site.<br>
2) The about box does give credit, and shows the truncated GPL, noting<br>
the copyright holders including siphone, sipdroid, hughes systique,<br>
andoird and university of parma, it.<br>
That's all I wanted to share at this point. It is kind of mistargeted<br>
in that you dmca'd google as we aren't involved with gizmo5 and we<br>
don't host the app on the android market. Did you email michael<br>
robertson over at Gizmo5? He's all over the internet. Press emails<br>
sometimes sit in limbo at companies.<br>
Good luck and happy hacking..<br>
Chris<br>
</pre>

### Claims ###

Users should not have to jump thru five hoops if they want to get the source code. When taking intellectual property from an open source project one expects a publisher to:

  * Mention the original authors right away. In this case only after downloading and completing 5 required of about 10 settings, the menu and copyright box get accessible.
  * Offer sources and binary in the same place.

### Discussion ###

A [discussion](http://groups.google.com/group/sipdroid-developers/browse_thread/thread/72cf357c21667ec3) on this issue is held in our developers group.