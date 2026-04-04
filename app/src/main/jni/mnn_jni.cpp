#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include "llm/llm.hpp"
#include <MNN/expr/Expr.hpp>
#include <MNN/expr/MathOp.hpp>

#define LOG_TAG "MnnJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<MNN::Transformer::Llm> g_llm;
static std::vector<MNN::Transformer::ChatMessage> g_chat_history;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_MnnBridge_nativeLoadModel(
    JNIEnv* env, jobject thiz, jstring configPath) {
    
    const char* configPathStr = env->GetStringUTFChars(configPath, nullptr);
    LOGI("Loading MNN model from: %s", configPathStr);
    
    try {
        auto* llmPtr = MNN::Transformer::Llm::createLLM(configPathStr);
        if (!llmPtr) {
            LOGE("Failed to create LLM instance");
            env->ReleaseStringUTFChars(configPath, configPathStr);
            return JNI_FALSE;
        }
        
        g_llm = std::unique_ptr<MNN::Transformer::Llm>(llmPtr);
        
        if (!g_llm->load()) {
            LOGE("Failed to load model weights");
            g_llm.reset();
            env->ReleaseStringUTFChars(configPath, configPathStr);
            return JNI_FALSE;
        }
        
        LOGI("MNN model loaded successfully");
    } catch (const std::exception& e) {
        LOGE("Exception loading model: %s", e.what());
        g_llm.reset();
        env->ReleaseStringUTFChars(configPath, configPathStr);
        return JNI_FALSE;
    }
    
    env->ReleaseStringUTFChars(configPath, configPathStr);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_MnnBridge_nativeGenerateWithImage(
    JNIEnv* env, jobject thiz, jstring prompt, jbyteArray rgbData,
    jint width, jint height, jint maxTokens) {
    
    if (!g_llm) {
        LOGE("LLM not loaded");
        return env->NewStringUTF("Error: Model not loaded");
    }
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    jbyte* pixels = env->GetByteArrayElements(rgbData, nullptr);
    int pixelCount = width * height * 3;
    
    LOGI("Running MNN inference: prompt='%s', image=%dx%d", promptStr, width, height);
    
    try {
        // Create image tensor from RGB bytes (NHWC format, float32, normalized 0-1)
        std::vector<float> imageData(pixelCount);
        const uint8_t* src = reinterpret_cast<const uint8_t*>(pixels);
        for (int i = 0; i < pixelCount; ++i) {
            imageData[i] = static_cast<float>(src[i]) / 255.0f;
        }
        
        // Create MNN VARP for image (NHWC layout)
        std::vector<int> imageShape = {1, height, width, 3};
        auto imageVar = MNN::Express::_Const(imageData.data(), imageShape, MNN::Express::NHWC, halide_type_of<float>());
        
        // Build multimodal prompt
        MNN::Transformer::MultimodalPrompt mp;
        mp.prompt_template = std::string(promptStr);
        
        MNN::Transformer::PromptImagePart imagePart;
        imagePart.image_data = imageVar;
        imagePart.width = width;
        imagePart.height = height;
        mp.images["image0"] = imagePart;
        
        // Capture output
        std::ostringstream output;
        
        // Reset context to prevent memory leak and hallucination from previous scans
        g_llm->reset();
        
        // Run inference
        g_llm->response(mp, &output, nullptr, maxTokens > 0 ? maxTokens : 96);
        
        std::string result = output.str();
        LOGI("MNN inference complete, response length: %zu", result.length());
        
        env->ReleaseStringUTFChars(prompt, promptStr);
        env->ReleaseByteArrayElements(rgbData, pixels, JNI_ABORT);
        
        return env->NewStringUTF(result.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Exception during inference: %s", e.what());
        env->ReleaseStringUTFChars(prompt, promptStr);
        env->ReleaseByteArrayElements(rgbData, pixels, JNI_ABORT);
        return env->NewStringUTF("Error: Inference failed");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_MnnBridge_nativeGenerateText(
    JNIEnv* env, jobject thiz, jstring prompt, jint maxTokens) {
    
    if (!g_llm) {
        LOGE("LLM not loaded");
        return env->NewStringUTF("Error: Model not loaded");
    }
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Running MNN text inference: prompt='%s'", promptStr);
    
    try {
        std::ostringstream output;
        g_llm->response(std::string(promptStr), &output, nullptr, maxTokens > 0 ? maxTokens : 96);
        
        std::string result = output.str();
        LOGI("MNN text inference complete, response length: %zu", result.length());
        
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF(result.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Exception during text inference: %s", e.what());
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("Error: Inference failed");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_MnnBridge_nativeReset(
    JNIEnv* env, jobject thiz) {
    if (g_llm) {
        g_llm->reset();
        g_chat_history.clear();
        LOGI("MNN LLM reset");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_MnnBridge_nativeRelease(
    JNIEnv* env, jobject thiz) {
    if (g_llm) {
        g_llm.reset();
        g_chat_history.clear();
        LOGI("MNN LLM released");
    }
}
