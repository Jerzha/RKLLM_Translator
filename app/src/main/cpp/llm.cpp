#include <android/log.h>
#include <jni.h>
#include "rkllm.h"

#define TAG "LLmJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LLmJniEnv {
    JNIEnv *env;
    jobject thiz;
    jclass clazz;
};

void callbackToJava(const char *text, int state, LLmJniEnv *jenv) {
    jmethodID method = jenv->env->GetMethodID(jenv->clazz, "callbackFromNative", "(Ljava/lang/String;I)V");
    jstring jText = text ? jenv->env->NewStringUTF(text) : jenv->env->NewStringUTF("");
    jenv->env->CallVoidMethod(jenv->thiz, method, jText, state);
}

void callback(RKLLMResult *result, void *userdata, LLMCallState state) {
    auto jenv = (LLmJniEnv *)userdata;

    if (state == RKLLM_RUN_FINISH) {
        LOGI("<FINISH/>");
        callbackToJava(nullptr, 0, jenv);
        delete jenv;
    } else if (state == RKLLM_RUN_ERROR) {
        LOGE("<ERROR/>");
        callbackToJava(nullptr, -1, jenv);
        delete jenv;
    } else if (state == RKLLM_RUN_GET_LAST_HIDDEN_LAYER) {
        LOGW("<get last_hidden_layer/>");
        callbackToJava(nullptr, -2, jenv);
    } else if (state == RKLLM_RUN_NORMAL) {
        //LOGD("NM: [%d] %s", result->token_id, result->text);
        callbackToJava(result->text, 1, jenv);
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_rockchip_llm_translator_RKllm_initLLm(JNIEnv *env, jobject thiz, jstring model_path) {
    const char* modelPath = env->GetStringUTFChars(model_path, nullptr);
    LLMHandle llmHandle = nullptr;

    //设置参数及初始化
    RKLLMParam param = rkllm_createDefaultParam();
    param.model_path = modelPath;

    //设置采样参数
    param.top_k = 1;
    param.top_p = 0.95;
    param.temperature = 0.8;
    param.repeat_penalty = 1.1;
    param.frequency_penalty = 0.0;
    param.presence_penalty = 0.0;

    param.max_new_tokens = 128000;
    param.max_context_len = 128000;
    param.skip_special_token = true;
    param.extend_param.base_domain_id = 0;

    LOGD("rkllm init with module: %s", modelPath);
    int ret = rkllm_init(&llmHandle, &param, callback);
    if (ret == 0){
        LOGD("rkllm init success\n");
    } else {
        LOGE("rkllm init failed\n");
    }

    return (jlong)llmHandle;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rockchip_llm_translator_RKllm_deinitLLm(JNIEnv *env, jobject thiz, jlong handle) {
    rkllm_destroy((LLMHandle)handle);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rockchip_llm_translator_RKllm_infer(JNIEnv *env, jobject thiz, jlong handle,
                                             jstring text) {
    auto *jnienv = new LLmJniEnv {
        .env = env,
        .thiz = thiz,
        .clazz = env->GetObjectClass(thiz),
    };

    RKLLMInput rkllm_input = {};
    RKLLMInferParam rkllm_infer_params = {};
    const char* sText = env->GetStringUTFChars(text, nullptr);

    rkllm_infer_params.mode = RKLLM_INFER_GENERATE;
    rkllm_input.input_type = RKLLM_INPUT_PROMPT;
    rkllm_input.prompt_input = (char *)sText;

    rkllm_run((LLMHandle)handle, &rkllm_input, &rkllm_infer_params, jnienv);
}