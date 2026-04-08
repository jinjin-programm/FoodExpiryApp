#include <jni.h>
#include <android/log.h>
#include <string>

#define TAG "MnnLlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Stub: Full implementation wraps MNN LLM C++ API (mnn/llm/llm.hpp)
// Reference: https://github.com/alibaba/MNN/tree/master/apps/Android/MnnLlmChat

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeCreateLlm(
        JNIEnv *env, jobject thiz, jstring modelDir, jint threadNum, jstring memoryMode) {
    LOGI("nativeCreateLlm called — stub implementation");
    const char *dir = env->GetStringUTFChars(modelDir, nullptr);
    const char *mode = env->GetStringUTFChars(memoryMode, nullptr);
    LOGI("  modelDir: %s, threadNum: %d, memoryMode: %s", dir, threadNum, mode);
    env->ReleaseStringUTFChars(modelDir, dir);
    env->ReleaseStringUTFChars(memoryMode, mode);
    // TODO: Create MNN LLM instance from modelDir
    // mnn::llm::create(config) — requires MNN C++ headers
    return 0; // Return 0 = not created (stub)
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeRunInference(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jbyteArray imageData, jint width, jint height) {
    LOGI("nativeRunInference called — stub implementation (handle=%lld, %dx%d)",
         (long long)nativeHandle, width, height);
    // TODO: Run inference using MNN LLM
    // llm->response(prompt) — requires MNN C++ headers
    std::string result = "{\"name\":\"Unknown\",\"name_zh\":\"未知\",\"confidence\":0.0}";
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeDestroyLlm(
        JNIEnv *env, jobject thiz, jlong nativeHandle) {
    LOGI("nativeDestroyLlm called — stub implementation (handle=%lld)", (long long)nativeHandle);
    // TODO: Destroy MNN LLM instance
}
