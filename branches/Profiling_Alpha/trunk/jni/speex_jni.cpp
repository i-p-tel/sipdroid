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

#include <speex/speex.h>

// the header length of the RTP frame (must skip when en/decoding)
static const int rtp_header = 12;

static int codec_open = 0;

static int dec_frame_size;
static int enc_frame_size;

static SpeexBits ebits, dbits;
void *enc_state;
void *dec_state;

static JavaVM *gJavaVM;
const char *kInterfacePath = "org/sipdroid/pjlib/Speex";

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Speex_open
  (JNIEnv *env, jobject obj, jint compression) {
	int tmp;

	if (codec_open++ != 0)
		return (jint)0;

	speex_bits_init(&ebits);
	speex_bits_init(&dbits);

	enc_state = speex_encoder_init(&speex_nb_mode); 
	dec_state = speex_decoder_init(&speex_nb_mode); 
	tmp = compression;
	speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);
	speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);
	speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);

	return (jint)0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Speex_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {

        jshort buffer[enc_frame_size];
        jbyte output_buffer[enc_frame_size];
	int nsamples = (size-1)/enc_frame_size + 1;
	int i, tot_bytes = 0;

	if (!codec_open)
		return 0;

	speex_bits_reset(&ebits);

	for (i = 0; i < nsamples; i++) {
		env->GetShortArrayRegion(lin, offset + i*enc_frame_size,
					 enc_frame_size, buffer);
		speex_encode_int(enc_state, buffer, &ebits);
	}
	// SDP frame terminator
	speex_bits_pack(&ebits, 0xf, 5);
	tot_bytes = speex_bits_write(&ebits, (char *)output_buffer,
				     enc_frame_size);
	env->SetByteArrayRegion(encoded, rtp_header, tot_bytes,
				output_buffer);

        return (jint)tot_bytes;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Speex_decode
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {

        jbyte buffer[dec_frame_size];
        jshort output_buffer[dec_frame_size];
        jsize encoded_length = size;

	if (!codec_open)
		return 0;

	env->GetByteArrayRegion(encoded, rtp_header, encoded_length, buffer);
	speex_bits_read_from(&dbits, (char *)buffer, encoded_length);
	speex_decode_int(dec_state, &dbits, output_buffer);
	env->SetShortArrayRegion(lin, 0, dec_frame_size,
				 output_buffer);

	return (jint)dec_frame_size;
}

extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_codecs_Speex_close
    (JNIEnv *env, jobject obj) {

	if (--codec_open != 0)
		return;

	speex_bits_destroy(&ebits);
	speex_bits_destroy(&dbits);
	speex_decoder_destroy(dec_state); 
	speex_encoder_destroy(enc_state); 
}
