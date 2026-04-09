#include <jni.h>
#include <android/log.h>
#include <string>
#include <sstream>
#include <memory>
#include <mutex>

#include "llm/llm.hpp"

#define TAG "MnnLlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

using MNN::Transformer::Llm;
using MNN::Transformer::ChatMessage;
using MNN::Transformer::ChatMessages;

struct LlmInstance {
    std::unique_ptr<Llm> llm;
    std::string last_response;
    bool is_loaded = false;
};

static std::mutex g_llm_mutex;

// Forward declarations for helper functions
static std::string extractJson(const std::string& text);
static std::string escapeJson(const std::string& text);

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeCreateLlm(
        JNIEnv *env, jobject thiz, jstring modelDir, jint threadNum, jstring memoryMode) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    const char *dir = env->GetStringUTFChars(modelDir, nullptr);
    const char *mode = env->GetStringUTFChars(memoryMode, nullptr);
    LOGI("nativeCreateLlm: modelDir=%s, threadNum=%d, memoryMode=%s", dir, threadNum, mode);

    try {
        auto* instance = new LlmInstance();

        // Step 1: Create LLM instance from config path
        // config.json must exist in modelDir
        std::string config_path = std::string(dir) + "/config.json";
        instance->llm.reset(Llm::createLLM(config_path));
        if (!instance->llm) {
            LOGE("nativeCreateLlm: createLLM failed for %s", config_path.c_str());
            env->ReleaseStringUTFChars(modelDir, dir);
            env->ReleaseStringUTFChars(memoryMode, mode);
            delete instance;
            return 0;
        }

        // Step 2: Set configuration
        std::string config_json = "{";
        config_json += "\"thread_num\":" + std::to_string(threadNum) + ",";
        config_json += "\"use_mmap\":true,";
        config_json += "\"tmp_path\":\"" + std::string(dir) + "\",";
        config_json += "\"memory_mode\":\"" + std::string(mode) + "\"";

        // Low memory mode settings
        if (std::string(mode) == "low") {
            config_json += ",\"precision\":\"low\"";
        } else {
            config_json += ",\"precision\":\"high\"";
        }

        config_json += ",\"enable_thinking\":false";

        config_json += "}";

        LOGI("nativeCreateLlm: config=%s", config_json.c_str());
        instance->llm->set_config(config_json);

        // Step 3: Load model
        LOGI("nativeCreateLlm: loading model...");
        instance->is_loaded = instance->llm->load();
        if (!instance->is_loaded) {
            LOGE("nativeCreateLlm: load() failed");
            env->ReleaseStringUTFChars(modelDir, dir);
            env->ReleaseStringUTFChars(memoryMode, mode);
            delete instance;
            return 0;
        }

        env->ReleaseStringUTFChars(modelDir, dir);
        env->ReleaseStringUTFChars(memoryMode, mode);

        LOGI("nativeCreateLlm: success (instance=%p)", instance);
        return reinterpret_cast<jlong>(instance);
    } catch (const std::exception& e) {
        LOGE("nativeCreateLlm: exception: %s", e.what());
        env->ReleaseStringUTFChars(modelDir, dir);
        env->ReleaseStringUTFChars(memoryMode, mode);
        return 0;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeRunInference(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jbyteArray imageData, jint width, jint height) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance || !instance->llm || !instance->is_loaded) {
        LOGE("nativeRunInference: invalid handle or model not loaded");
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }

    try {
        // Build system prompt
        std::string system_prompt =
            "You are a food identification assistant. Identify the food in the image and respond ONLY with a JSON object in this exact format:\n"
            "{\"name\": \"english name\", \"name_zh\": \"中文名\", \"confidence\": 0.95}\n\n"
            "Rules:\n"
            "- \"name\" must be in English, lowercase\n"
            "- \"name_zh\" must be in Traditional Chinese (繁體中文)\n"
            "- \"confidence\" must be 0.0-1.0\n"
            "- Respond with ONLY the JSON, no other text";

        // Build conversation history
        ChatMessages history;
        history.emplace_back("system", system_prompt);
        history.emplace_back("user", "What food is in this image?");

        // Prepare output stream to capture response
        std::stringstream response_stream;
        std::string response_str;

        // Step 1: Run prefill (response with max_new_tokens=0 means prefill only)
        instance->llm->response(history, &response_stream, "\xef\xb8\xae", 0);

        // Step 2: Stepping decode — generate tokens one at a time
        const int max_new_tokens = 128;
        for (int i = 0; i < max_new_tokens; i++) {
            instance->llm->generate(1);

            // Check if generation is done
            auto* context = instance->llm->getContext();
            if (context &&
                (context->status == MNN::Transformer::LlmStatus::NORMAL_FINISHED ||
                 context->status == MNN::Transformer::LlmStatus::MAX_TOKENS_FINISHED)) {
                break;
            }
        }

        response_str = response_stream.str();

        LOGI("nativeRunInference: response=%s (len=%zu)", response_str.c_str(), response_str.length());

        // Step 3: Parse JSON from response (extract JSON object if embedded in markdown)
        std::string json_result = extractJson(response_str);

        if (json_result.empty()) {
            json_result = "{\"name\":\"Unknown\",\"name_zh\":\"未知\",\"confidence\":0.0,\"raw_response\":\"" +
                          escapeJson(response_str) + "\"}";
        }

        instance->last_response = response_str;
        return env->NewStringUTF(json_result.c_str());
    } catch (const std::exception& e) {
        LOGE("nativeRunInference: exception: %s", e.what());
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeDestroyLlm(
        JNIEnv *env, jobject thiz, jlong nativeHandle) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance) {
        LOGE("nativeDestroyLlm: invalid handle");
        return;
    }

    try {
        if (instance->llm) {
            instance->llm->reset();
        }
        LOGI("nativeDestroyLlm: destroyed instance=%p", instance);
        delete instance;
    } catch (const std::exception& e) {
        LOGE("nativeDestroyLlm: exception: %s", e.what());
        delete instance;
    }
}

// Helper: extract JSON object from response text
static std::string extractJson(const std::string& text) {
    // Try to find JSON object boundaries
    int brace_depth = 0;
    int start = -1;
    for (size_t i = 0; i < text.length(); i++) {
        if (text[i] == '{') {
            if (start == -1) start = (int)i;
            brace_depth++;
        } else if (text[i] == '}') {
            brace_depth--;
            if (brace_depth == 0 && start != -1) {
                return text.substr(start, i - start + 1);
            }
        }
    }

    // Try to find JSON array
    brace_depth = 0;
    start = -1;
    for (size_t i = 0; i < text.length(); i++) {
        if (text[i] == '[') {
            if (start == -1) start = (int)i;
            brace_depth++;
        } else if (text[i] == ']') {
            brace_depth--;
            if (brace_depth == 0 && start != -1) {
                std::string inner = text.substr(start + 1, i - start - 1);
                // Try to find JSON object inside array
                std::string json_obj = extractJson(inner);
                if (!json_obj.empty()) return json_obj;
            }
        }
    }

    return "";
}

// Helper: escape string for JSON embedding
static std::string escapeJson(const std::string& text) {
    std::string result;
    result.reserve(text.size() + 10);
    for (char c : text) {
        switch (c) {
            case '"': result += "\\\""; break;
            case '\\': result += "\\\\"; break;
            case '\n': result += "\\n"; break;
            case '\r': result += "\\r"; break;
            case '\t': result += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 32) {
                    char buf[8];
                    snprintf(buf, sizeof(buf), "\\u%04x", static_cast<unsigned char>(c));
                    result += buf;
                } else {
                    result += c;
                }
        }
    }
    return result;
}
