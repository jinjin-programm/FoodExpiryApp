#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_modelLoaded = false;

extern "C" {

JNIEXPORT jint JNICALL Java_com_example_foodexpiryapp_1presentation_1ui_1llm_LlamaBridge_00024Companion_nativeLoadModel(
        JNIEnv* env, jobject thiz, jstring modelPath, jint contextSize, jint threads) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s with context=%d threads=%d", path, contextSize, threads);
    
    // TODO: Actually load the model using dlopen and function pointers
    // For now, just mark as loaded
    g_modelLoaded = true;
    
    env->ReleaseStringUTFChars(modelPath, path);
    LOGI("Model load initiated (stub)");
    return 0;
}

JNIEXPORT void JNICALL Java_com_example_foodexpiryapp_1presentation_1ui_1llm_LlamaBridge_00024Companion_nativeFreeModel(
        JNIEnv* env, jobject thiz) {
    LOGI("Freeing model");
    g_modelLoaded = false;
}

JNIEXPORT jstring JNICALL Java_com_example_foodexpiryapp_1presentation_1ui_1llm_LlamaBridge_00024Companion_nativeGenerate(
        JNIEnv* env, jobject thiz, jstring prompt) {
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating response for prompt (length: %d)", strlen(promptStr));
    
    // Return a response - in real implementation this would call llama.cpp
    const char* response = 
        "1. Food item name: Apple\n"
        "2. Expiry date: not visible\n"
        "3. Confidence: medium";
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF(response);
}

JNIEXPORT jboolean JNICALL Java_com_example_foodexpiryapp_1presentation_1ui_1llm_LlamaBridge_00024Companion_nativeIsLoaded(
        JNIEnv* env, jobject thiz) {
    return g_modelLoaded ? JNI_TRUE : JNI_FALSE;
}

}
