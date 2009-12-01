#include <jni.h>
#include <JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>

#include <string.h>
#include <unistd.h>
#include <pthread.h>

/* Include all PJMEDIA-CODEC headers. */
#include <pjlib.h>
#include <pjmedia.h>
#include <pjmedia-codec.h>

pj_pool_factory *mem;

static JavaVM *gJavaVM;
static jobject gInterfaceObject, gDataObject;
const char *kInterfacePath = "org/sipdroid/pjlib/Codec";

#ifdef __cplusplus
extern "C" {
#endif

enum op
{
    OP_GET  = 1,
    OP_PUT  = 2,
    OP_GET_PUT = 4,
    OP_PUT_GET = 8
};

enum clock_rate
{
    K8	= 1,
    K16	= 2,
};

struct codec_port
{
    pjmedia_port     base;
    pjmedia_endpt   *endpt;
    pjmedia_codec   *codec;
    pj_status_t	   (*codec_deinit)();
    pj_uint8_t	     pkt[640];
    short	     pcm[160];
};

pj_pool_t *pool;
pjmedia_port *port;
unsigned cnt, samples_per_frame;
pjmedia_port *gen_port;

struct codec_port *cp;
const pjmedia_codec_info *ci;
pjmedia_codec_param param;
pj_status_t status;

pj_caching_pool caching_pool;
pjmedia_frame input_frm, output_frm, input_pcm, encoded_frame;
pj_str_t tmp;
pjmedia_codec_mgr *cm;

unsigned int pcm_size=160; 

pjmedia_frame input_frames[50], pcm_frame;
pj_timestamp ts; 

static void err(const char *op, pj_status_t status)
{
    char errmsg[PJ_ERR_MSG_SIZE];
    pj_strerror(status, errmsg, sizeof(errmsg));
    PJ_LOG(3,("", "%s error: %s", op, errmsg));
    LOGE(op);
    LOGE(errmsg);
}

#define CHECK(op)   do { \
			status = op; \
			if (status != PJ_SUCCESS) { \
			    err(#op, status); \
			    return status; \
			} \
		    } \
		    while (0)

static void callback_handler(char *s) {
    int status;
    JNIEnv *env;
    bool isAttached = false;
   
    status = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);
    if(status < 0) {
        LOGE("callback_handler: failed to get JNI environment, "
             "assuming native thread");
        status = gJavaVM->AttachCurrentThread(&env, NULL);
        if(status < 0) {
            LOGE("callback_handler: failed to attach "
                 "current thread");
            return;
        }
        isAttached = true;
    }
    /* Construct a Java string */
    jstring js = env->NewStringUTF(s);
    jclass interfaceClass = env->GetObjectClass(gInterfaceObject);
    if(!interfaceClass) {
        LOGE("callback_handler: failed to get class reference");
        if(isAttached) gJavaVM->DetachCurrentThread();
        return;
    }
    /* Find the callBack method ID */
    jmethodID method = env->GetStaticMethodID(
        interfaceClass, "callBack", "(Ljava/lang/String;)V");
    if(!method) {
        LOGE("callback_handler: failed to get method ID");
        if(isAttached) gJavaVM->DetachCurrentThread();
        return;
    }
    env->CallStaticVoidMethod(interfaceClass, method, js);
    if(isAttached) gJavaVM->DetachCurrentThread();
}

void *native_thread_start(void *arg) {
    sleep(1);
    callback_handler((char *) "Called from native thread");
}

JNIEXPORT jint JNICALL Java_org_sipdroid_pjlib_codec_open
  (JNIEnv *env, jclass cls, jstring codec_id) {
    int rc = 0;
    unsigned count = 1;

    LOGE("Intializing PJLIB...");
    pj_init();
    pj_caching_pool_init(&caching_pool, &pj_pool_factory_default_policy, 0);

    pj_log_set_decor(PJ_LOG_HAS_NEWLINE);
    pj_log_set_level(3);

    mem = &caching_pool.factory;

    char* ctmp;
    jboolean iscopy;
    const char *codec_str = env->GetStringUTFChars(codec_id, &iscopy);
    ctmp = const_cast<char*>(codec_str);
    const pj_str_t pj_codec_str = pj_str(ctmp);

    unsigned clock_rate = 8000;

    LOGE("pj_pool_create");

    pool = pj_pool_create(mem, "pool", 1024, 1024, NULL);

    LOGE("PJ_POOL_ZALLOC_T");
    cp = PJ_POOL_ZALLOC_T(pool, struct codec_port);

    LOGE("pjmedia_endpt_create");
    status = pjmedia_endpt_create(mem, NULL, 0, &cp->endpt);
    if (status != PJ_SUCCESS)
	return NULL;

    cm = pjmedia_endpt_get_codec_mgr(cp->endpt);
#if PJMEDIA_HAS_G711_CODEC
    CHECK( pjmedia_codec_g711_init(cp->endpt) );
#endif
#if PJMEDIA_HAS_GSM_CODEC
    CHECK( pjmedia_codec_gsm_init(cp->endpt) );
#endif
#if PJMEDIA_HAS_ILBC_CODEC
    CHECK( pjmedia_codec_ilbc_init(cp->endpt, 30) );
#endif
#if PJMEDIA_HAS_SPEEX_CODEC
    CHECK( pjmedia_codec_speex_init(cp->endpt, 0, 5, 5) );
#endif
#if PJMEDIA_HAS_G722_CODEC
    CHECK( pjmedia_codec_g722_init(cp->endpt) );
#endif

     LOGE("pjmedia_codec_mgr_find_codecs_by_id: %s", codec_str);
     CHECK( status = pjmedia_codec_mgr_find_codecs_by_id(cm,
						 &pj_codec_str, &count, &ci, NULL) );
    if (status != PJ_SUCCESS) {
        LOGE("Cannot find codec");
	return NULL;
    }


    LOGE("pjmedia_codec_mgr_get_default_param");
    CHECK( status = pjmedia_codec_mgr_get_default_param(cm, ci, &param) );

    if (status != PJ_SUCCESS) {
        LOGE("pjmedia_codec_mgr_get_default_param failed");
	return NULL;
    }
    
    //param.setting.vad = 1;

    LOGE("pjmedia_codec_mgr_alloc_codec");
    CHECK( status = pjmedia_codec_mgr_alloc_codec(cm, ci, &cp->codec) );
    if (status != PJ_SUCCESS) {
        LOGE("Cannot allocate codec");
	return NULL;
    }


    LOGE("codec->op->init"); // channels=%d frm_ptime=%s", ci->channel_cnt, param.info.frm_ptime);
    status = (*cp->codec->op->init)(cp->codec, pool);
    if (status != PJ_SUCCESS)
	return NULL;

     LOGE("codec->op->open");
    status = cp->codec->op->open(cp->codec, &param);
    if (status != PJ_SUCCESS)
	return NULL;

    samples_per_frame = param.info.clock_rate * param.info.frm_ptime / 1000;

    LOGE("Finished initializing codec...");
    LOGE(" -> clock_rate=%d channel_count=%d samples_per_frame=%d pcm_bits_per_sample=%d", param.info.clock_rate, param.info.channel_cnt, \
									samples_per_frame, param.info.pcm_bits_per_sample);
    return (jint)PJ_SUCCESS;
}

JNIEXPORT jint JNICALL Java_org_sipdroid_pjlib_codec_encode
    (JNIEnv *env, jclass cls, jshortArray lin, jint offset, jbyteArray encoded, jint pcm_samples) {

        int encoded_frame_len = 0, output_frame_size = 33;
        jshort pcm_buffer[pcm_samples];
        jbyte output_buffer[output_frame_size];

        env->GetShortArrayRegion(lin, offset, pcm_samples, pcm_buffer);

        input_pcm.buf = pcm_buffer;
	input_pcm.size = sizeof(pcm_buffer);
        encoded_frame.buf = output_buffer;
	encoded_frame.size = output_frame_size;

	CHECK( cp->codec->op->encode(cp->codec, &input_pcm, input_pcm.size, &encoded_frame) );
        env->SetByteArrayRegion(encoded, 12, encoded_frame.size, output_buffer);

        return (jint)encoded_frame.size;
}

JNIEXPORT jint JNICALL Java_org_sipdroid_pjlib_codec_decode
    (JNIEnv *env, jclass cls, jbyteArray encoded, jshortArray lin, jint frames) {

	unsigned int i;
	unsigned int encoded_frames_int = 250, lin_len = 0, lin_pos = 0, buf_size = 0;
        short pcm_segment[160];

        jbyte encoded_buffer[512];
        jsize encoded_length = env->GetArrayLength(encoded);
	jsize buf_len = encoded_length - 12;

        env->GetByteArrayRegion(encoded, 12, buf_len, encoded_buffer);
	CHECK( cp->codec->op->parse(cp->codec, (char*)encoded_buffer,
				      buf_len, &ts,
				      &encoded_frames_int, input_frames) );

    for (i=0; i<encoded_frames_int; ++i) {
        pcm_frame.buf = cp->pcm;
        pcm_frame.size = 320;

        CHECK( cp->codec->op->decode(cp->codec, &input_frames[i], 320, &pcm_frame) );

        env->SetShortArrayRegion(lin, lin_pos, 160, cp->pcm);
        lin_pos = lin_pos + 160;
    }

   return (jint)lin_pos;
}

JNIEXPORT jint JNICALL Java_org_sipdroid_pjlib_codec_close
    (JNIEnv *env, jclass cls) {

    LOGE("Closing PJLIB...");
    pj_caching_pool_destroy(&caching_pool);
    pjmedia_port_destroy(port);
    pj_pool_release(pool);
    pj_shutdown();

    return (jint)0;
}

void initClassHelper(JNIEnv *env, const char *path, jobject *objptr) {
    jclass cls = env->FindClass(path);
    if(!cls) {
        LOGE("initClassHelper: failed to get %s class reference", path);
        return;
    }
    jmethodID constr = env->GetMethodID(cls, "<init>", "()V");
    if(!constr) {
        LOGE("initClassHelper: failed to get %s constructor", path);
        return;
    }
    jobject obj = env->NewObject(cls, constr);
    if(!obj) {
        LOGE("initClassHelper: failed to create a %s object", path);
        return;
    }
    (*objptr) = env->NewGlobalRef(obj);

}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env;
    gJavaVM = vm;
    LOGI("JNI_OnLoad called");
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("Failed to get the environment using GetEnv()");
        return -1;
    }
        initClassHelper(env, kInterfacePath, &gInterfaceObject);
        
        JNINativeMethod methods[] = {
            {
                "open",
                "(Ljava/lang/String;)I",
                (void *) Java_org_sipdroid_pjlib_codec_open
            },
            {
                "encode",
                "([SI[BI)I",
                (void *) Java_org_sipdroid_pjlib_codec_encode
            },
            {
                "decode",
                "([B[SI)I",
                (void *) Java_org_sipdroid_pjlib_codec_decode
            },
            {
                "close",
                "()I",
                (void *) Java_org_sipdroid_pjlib_codec_close
            }
        };
        if(android::AndroidRuntime::registerNativeMethods(
            env, kInterfacePath, methods, NELEM(methods)) != JNI_OK) {
            LOGE("Failed to register native methods");
            return -1;
        }
    return JNI_VERSION_1_4;
}

#ifdef __cplusplus
}
#endif
