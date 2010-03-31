/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
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

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <memory.h>
#include <ctype.h>
#include <jni.h>
#include <android/log.h> 

#include "spandsp.h"

/* Define codec specific settings */
#define BLOCK_LEN       160

#define LOG_TAG "gsm" // text for log tag 

#undef DEBUG_GSM

// the header length of the RTP frame (must skip when en/decoding)
#define	RTP_HDR_SIZE	12

static int codec_open = 0;

static JavaVM *gJavaVM;
const char *kInterfacePath = "org/sipdroid/pjlib/gsm";

gsm0610_state_t *gsm0610_enc_state;
gsm0610_state_t *gsm0610_dec_state;

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_GSM_open
  (JNIEnv *env, jobject obj) {
	int ret;

	if (codec_open++ != 0)
		return (jint)0;
		
	if ((gsm0610_enc_state = gsm0610_init(NULL, GSM0610_PACKING_VOIP)) == NULL)
	{
		fprintf(stderr, "    Cannot create encoder\n");
		exit(2);
	}
		
	if ((gsm0610_dec_state = gsm0610_init(NULL, GSM0610_PACKING_VOIP)) == NULL)
	{
		fprintf(stderr, "    Cannot create decoder\n");
		exit(2);
	}
	
	return (jint)0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_GSM_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {

	jshort pre_amp[BLOCK_LEN];	
	jbyte gsm0610_data[BLOCK_LEN];
		
	int ret,i,frsz=BLOCK_LEN;

	unsigned int lin_pos = 0;
	
	if (!codec_open)
		return 0;
		
#ifdef DEBUG_GSM
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
            "encoding frame size: %d\toffset: %d\n", size, offset); 		
#endif


	for (i = 0; i < size; i+=BLOCK_LEN) {
#ifdef DEBUG_GSM
		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
            "encoding frame size: %d\toffset: %d i: %d\n", size, offset, i); 		
#endif
			
		env->GetShortArrayRegion(lin, offset + i,frsz, pre_amp);

		ret=gsm0610_encode(gsm0610_enc_state, (uint8_t *) gsm0610_data, pre_amp, size);

#ifdef DEBUG_GSM
			__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
				"Enocded Bytes: %d\n", ret); 		
#endif		
        /* Write payload */		
		env->SetByteArrayRegion(encoded, RTP_HDR_SIZE+ lin_pos, ret, gsm0610_data);
		lin_pos += ret;
	}
#ifdef DEBUG_GSM
	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
        "encoding **END** frame size: %d\toffset: %d i: %d lin_pos: %d\n", size, offset, i, lin_pos);
#endif		

    return (jint)lin_pos;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_GSM_decode
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {

	jshort post_amp[BLOCK_LEN];
	jbyte gsm0610_data[BLOCK_LEN];

	int len;

	if (!codec_open)
		return 0;

#ifdef DEBUG_GSM		
	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
        "##### BEGIN DECODE ********  decoding frame size: %d\n", size); 	
#endif

	env->GetByteArrayRegion(encoded, RTP_HDR_SIZE, size, gsm0610_data);
	len = gsm0610_decode(gsm0610_dec_state, post_amp,(uint8_t *) gsm0610_data, size);

#ifdef DEBUG_GSM		
		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
			"##### DECODED length: %d\n", len); 	
#endif

	env->SetShortArrayRegion(lin, 0, len,post_amp);
	return (jint)len;
}


extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_codecs_GSM_close
    (JNIEnv *env, jobject obj) {

	if (--codec_open != 0)
		return;
		
	gsm0610_release(gsm0610_enc_state);
	gsm0610_release(gsm0610_dec_state);
	
}
