#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_modelLoaded = false;

extern "C" {

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject thiz, jstring modelPath, jint contextSize, jint threads) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s with context=%d threads=%d", path, contextSize, threads);
    
    // TODO: Actually load the model using llama.cpp
    // For now, just mark as loaded to test the bridge
    g_modelLoaded = true;
    
    env->ReleaseStringUTFChars(modelPath, path);
    LOGI("Model load bridge check successful");
    return 0;
}

JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeFreeModel(
        JNIEnv* env, jobject thiz) {
    LOGI("Freeing model");
    g_modelLoaded = false;
}

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject thiz, jstring prompt) {
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating response for prompt (length: %zu)", strlen(promptStr));
    
    // Return a response - in real implementation this would call llama.cpp
    const char* response = 
        "1. Food item name: Apple\n"
        "2. Expiry date: not visible\n"
        "3. Confidence: medium";
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF(response);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}

} // extern "C"
