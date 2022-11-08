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

 
#include <jni.h>

#include <string.h>
#include <unistd.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <android/log.h> 

#define LOG_TAG "bv16" // text for log tag 

#ifdef __cplusplus
extern "C" {
#endif

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16.h"
#include "utility.h"
#if G192BITSTREAM
#include "g192.h"
#else
#include "bitpack.h"
#endif
#include "memutil.h" 

#ifdef __cplusplus
}
#endif



// the header length of the RTP frame (must skip when en/decoding)
#define	RTP_HDR_SIZE	12
// size of BV16 packed bitstream (RFC4298)
#define	BITSTREAM_SIZE	10	

static int codec_open = 0;

void *enc_bs;
void *dec_bs;
void *enc_state;
void *dec_state;

jshort enc_buffer[FRSZ];
jbyte enc_output_buffer[FRSZ];

jbyte dec_buffer[FRSZ];
jshort dec_output_buffer[FRSZ];

int sizestate, sizebitstream, frsz;

static JavaVM *gJavaVM;
const char *kInterfacePath = "org/sipdroid/pjlib/BV16Fixedp";

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_BV16_open
  (JNIEnv *env, jobject obj) {
	int tmp;

	if (codec_open++ != 0)
		return (jint)0;

    sizebitstream = sizeof(struct BV16_Bit_Stream);
	frsz = FRSZ;
   
    sizestate = sizeof(struct BV16_Encoder_State);
    enc_state = allocWord16(0,sizeof(struct BV16_Encoder_State)/2-1);
    Reset_BV16_Encoder((struct BV16_Encoder_State*)enc_state);

    sizestate = sizeof(struct BV16_Decoder_State);
    dec_state = allocWord16(0,sizeof(struct BV16_Decoder_State)/2-1);
    Reset_BV16_Decoder((struct BV16_Decoder_State*)dec_state);	
	
    enc_bs = allocWord16(0,sizebitstream/2-1);
    dec_bs = allocWord16(0,sizebitstream/2-1);
	
	return (jint)0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_BV16_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {


	int i;
	unsigned int lin_pos = 0;

	if (!codec_open)
		return 0;
		
//    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//            "encoding frame size: %d\toffset: %d\n", size, offset); 		

	for (i = 0; i < size; i+=FRSZ) {
//		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//            "encoding frame size: %d\toffset: %d i: %d\n", size, offset, i); 		
	
		env->GetShortArrayRegion(lin, offset + i,frsz, enc_buffer);
		BV16_Encode((struct BV16_Bit_Stream*) enc_bs,(struct BV16_Encoder_State*) enc_state, enc_buffer);
		BV16_BitPack( (UWord8 *) enc_output_buffer, (struct BV16_Bit_Stream*) enc_bs );
		env->SetByteArrayRegion(encoded, RTP_HDR_SIZE+ lin_pos, BITSTREAM_SIZE, enc_output_buffer);
		lin_pos += BITSTREAM_SIZE;
	}
//	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//        "encoding **END** frame size: %d\toffset: %d i: %d lin_pos: %d\n", size, offset, i, lin_pos); 	
    return (jint)lin_pos;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_BV16_decode
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {

	unsigned int lin_pos = 0;
	
	jbyte	i;

	if (!codec_open)
		return 0;

//	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//        "**** BEGIN DECODE ********  decoding frame size: %d lin_pos %d last i: %d\n", size, lin_pos, i); 	
	
	for (i=0; i<size; i=i+BITSTREAM_SIZE) {
//		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//			"**** I DECODE ******  decoding frame size: %d lin_pos %d last i: %d\n", size, lin_pos, i); 
			
		env->GetByteArrayRegion(encoded, i+RTP_HDR_SIZE, BITSTREAM_SIZE,dec_buffer);

		BV16_BitUnPack((UWord8 *)dec_buffer, (struct BV16_Bit_Stream*)dec_bs ); 
		BV16_Decode((struct BV16_Bit_Stream*) dec_bs,(struct BV16_Decoder_State*) dec_state,
			(Word16 *) dec_output_buffer);   

		env->SetShortArrayRegion(lin, lin_pos, size,dec_output_buffer);
		lin_pos = lin_pos + size;
	}

	
//    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, 
//        "decoding frame size: %d lin_pos %d last i: %d\n", size, lin_pos, i); 	

	return (jint)lin_pos;
}

extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_codecs_BV16_close
    (JNIEnv *env, jobject obj) {

	if (--codec_open != 0)
		return;

    deallocWord16((Word16 *) enc_state, 0, sizestate/2-1);
    deallocWord16((Word16 *) dec_state, 0, sizestate/2-1);
    deallocWord16((Word16 *) dec_bs, 0, sizebitstream/2-1);
    deallocWord16((Word16 *) enc_bs, 0, sizebitstream/2-1);
}
