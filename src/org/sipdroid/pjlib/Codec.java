package org.sipdroid.pjlib;

import java.lang.String;

public class Codec {
    public static native int open(String codec_id);
    public static native int decode(byte alaw[], short lin[], int frames);
    public static native int encode(short lin[], int offset, byte alaw[], int frames);
    public static native int close();
}
