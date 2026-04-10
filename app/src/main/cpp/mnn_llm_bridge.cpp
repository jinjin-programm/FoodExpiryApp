#include <jni.h>
#include <android/log.h>
#include <string>
#include <sstream>
#include <fstream>
#include <memory>
#include <mutex>

#include "llm/llm.hpp"
#include <cv/imgcodecs.hpp>

#define TAG "MnnLlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

using MNN::Transformer::Llm;
using MNN::Transformer::ChatMessage;
using MNN::Transformer::ChatMessages;
using MNN::Transformer::MultimodalPrompt;

struct LlmInstance {
    std::unique_ptr<Llm> llm;
    std::string last_response;
    bool is_loaded = false;
};

static std::mutex g_llm_mutex;

static std::string stripThinkingProcess(const std::string& text);

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeCreateLlm(
        JNIEnv *env, jobject thiz, jstring modelDir, jint threadNum, jstring memoryMode) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    const char *dir = env->GetStringUTFChars(modelDir, nullptr);
    const char *mode = env->GetStringUTFChars(memoryMode, nullptr);
    LOGI("nativeCreateLlm: modelDir=%s, threadNum=%d, memoryMode=%s", dir, threadNum, mode);

    try {
        auto* instance = new LlmInstance();

        std::string config_path = std::string(dir) + "/config.json";
        instance->llm.reset(Llm::createLLM(config_path));
        if (!instance->llm) {
            LOGE("nativeCreateLlm: createLLM failed for %s", config_path.c_str());
            env->ReleaseStringUTFChars(modelDir, dir);
            env->ReleaseStringUTFChars(memoryMode, mode);
            delete instance;
            return 0;
        }

        std::string config_json = "{";
        config_json += "\"thread_num\":" + std::to_string(threadNum) + ",";
        config_json += "\"use_mmap\":true,";
        config_json += "\"tmp_path\":\"" + std::string(dir) + "\",";
        config_json += "\"memory_mode\":\"" + std::string(mode) + "\",";
        config_json += "\"precision\":\"high\",";
        config_json += "\"use_template\":false,";
        config_json += "\"async\":false";
        config_json += "}";

        LOGI("nativeCreateLlm: config=%s", config_json.c_str());
        instance->llm->set_config(config_json);

        instance->llm->set_config(R"({
            "jinja": {
                "context": {
                    "enable_thinking": false
                }
            }
        })");
        LOGI("nativeCreateLlm: disabled Qwen3 thinking mode (enable_thinking=false)");

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
        JNIEnv *env, jobject thiz, jlong nativeHandle, jbyteArray imageDataObj) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance || !instance->llm || !instance->is_loaded) {
        LOGE("nativeRunInference: invalid handle or model not loaded");
        return env->NewStringUTF("Error");
    }

    jsize len = env->GetArrayLength(imageDataObj);
    jbyte* buffer = env->GetByteArrayElements(imageDataObj, nullptr);
    std::vector<uint8_t> img_data(buffer, buffer + len);
    env->ReleaseByteArrayElements(imageDataObj, buffer, JNI_ABORT);

    LOGI("nativeRunInference: in-memory image size=%d bytes", len);

    try {
        ChatMessages chat = {
            {"system", "You are a food classifier. Reply with ONLY the food name, nothing else."},
            {"user", "<img>in_memory_image</img>What food is in this image? Reply with just the food name."}
        };

        std::string templated = instance->llm->apply_chat_template(chat);
        LOGI("nativeRunInference: templated prompt (len=%zu): %.200s", templated.length(), templated.c_str());

        MultimodalPrompt mm_prompt;
        mm_prompt.prompt_template = templated;

        auto image_var = MNN::CV::imdecode(img_data, MNN::CV::IMREAD_COLOR);
        if (image_var.get() != nullptr) {
            LOGI("nativeRunInference: decoded image into VARP (BGR, IMREAD_COLOR)");
            MNN::Transformer::PromptImagePart image_part;
            image_part.image_data = image_var;

            int width = 0, height = 0;
            auto info = image_var->getInfo();
            if (info) {
                auto dims = info->dim;
                int num = dims.size();
                if (num == 2 || num == 3) {
                    height = dims[0];
                    width = dims[1];
                } else if (num > 3) {
                    if (info->order == MNN::Express::NHWC) {
                        width = dims[num - 2];
                        height = dims[num - 3];
                    } else {
                        width = dims[num - 1];
                        height = dims[num - 2];
                    }
                }
            }
            LOGI("nativeRunInference: image size: %dx%d", width, height);

            image_part.width = width;
            image_part.height = height;
            mm_prompt.images["in_memory_image"] = image_part;
        } else {
            LOGE("nativeRunInference: failed to decode image via MNN::CV::imdecode");
        }

        std::stringstream response_stream;

        instance->llm->response(mm_prompt, &response_stream, "\n", 512);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInference: raw response=%s (len=%zu)", response_str.c_str(), response_str.length());

        instance->last_response = response_str;

        instance->llm->reset();
        LOGI("nativeRunInference: context reset, returning: %s (len=%zu)", response_str.c_str(), response_str.length());
        return env->NewStringUTF(response_str.c_str());
    } catch (const std::exception& e) {
        LOGE("nativeRunInference: exception: %s", e.what());
        return env->NewStringUTF("Error");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeRunInferenceWithHint(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jbyteArray imageDataObj, jstring hint) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance || !instance->llm || !instance->is_loaded) {
        LOGE("nativeRunInferenceWithHint: invalid handle or model not loaded");
        return env->NewStringUTF("Error");
    }

    jsize len = env->GetArrayLength(imageDataObj);
    jbyte* buffer = env->GetByteArrayElements(imageDataObj, nullptr);
    std::vector<uint8_t> img_data(buffer, buffer + len);
    env->ReleaseByteArrayElements(imageDataObj, buffer, JNI_ABORT);

    const char *hint_str = env->GetStringUTFChars(hint, nullptr);
    LOGI("nativeRunInferenceWithHint: image size=%d bytes, hint=%s", len, hint_str);

    try {
        std::string user_msg = "<img>in_memory_image</img>What food is in this image? Respond: [FOOD]food name[/FOOD].";

        ChatMessages chat = {
            {"system", "You are a food classifier. Reply with ONLY the food name."},
            {"user", user_msg}
        };

        std::string templated = instance->llm->apply_chat_template(chat);
        LOGI("nativeRunInferenceWithHint: templated prompt (len=%zu): %.200s", templated.length(), templated.c_str());

        MultimodalPrompt mm_prompt;
        mm_prompt.prompt_template = templated;

        auto image_var = MNN::CV::imdecode(img_data, MNN::CV::IMREAD_COLOR);
        if (image_var.get() != nullptr) {
            LOGI("nativeRunInferenceWithHint: decoded image into VARP (BGR, IMREAD_COLOR)");
            MNN::Transformer::PromptImagePart image_part;
            image_part.image_data = image_var;

            int width = 0, height = 0;
            auto info = image_var->getInfo();
            if (info) {
                auto dims = info->dim;
                int num = dims.size();
                if (num == 2 || num == 3) {
                    height = dims[0];
                    width = dims[1];
                } else if (num > 3) {
                    if (info->order == MNN::Express::NHWC) {
                        width = dims[num - 2];
                        height = dims[num - 3];
                    } else {
                        width = dims[num - 1];
                        height = dims[num - 2];
                    }
                }
            }
            LOGI("nativeRunInferenceWithHint: image size: %dx%d", width, height);

            image_part.width = width;
            image_part.height = height;
            mm_prompt.images["in_memory_image"] = image_part;
        } else {
            LOGE("nativeRunInferenceWithHint: failed to decode image via MNN::CV::imdecode");
        }

        std::stringstream response_stream;

        instance->llm->response(mm_prompt, &response_stream, "\n", 512);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInferenceWithHint: raw response=%s (len=%zu)", response_str.c_str(), response_str.length());

        env->ReleaseStringUTFChars(hint, hint_str);

        instance->last_response = response_str;

        instance->llm->reset();
        LOGI("nativeRunInferenceWithHint: context reset, returning: %s (len=%zu)", response_str.c_str(), response_str.length());
        return env->NewStringUTF(response_str.c_str());
    } catch (const std::exception& e) {
        LOGE("nativeRunInferenceWithHint: exception: %s", e.what());
        env->ReleaseStringUTFChars(hint, hint_str);
        return env->NewStringUTF("Error");
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

static std::string stripThinkingProcess(const std::string& text) {
    std::string result = text;

    auto deleteThinkPart = [](std::string s, const std::string& openTag, const std::string& closeTag) -> std::string {
        std::size_t think_start = s.find(openTag);
        if (think_start == std::string::npos) return s;
        std::size_t think_end = s.find(closeTag, think_start);
        if (think_end == std::string::npos) return s;
        think_end += closeTag.length();
        s.erase(think_start, think_end - think_start);
        return s;
    };

    result = deleteThinkPart(result, "<thinkApproach>", "</thinkApproach>");
    result = deleteThinkPart(result, "<think", "</think");

    size_t first_non_ws = result.find_first_not_of(" \t\n\r");
    if (first_non_ws != std::string::npos) {
        result = result.substr(first_non_ws);
    } else {
        return "";
    }

    size_t last_non_ws = result.find_last_not_of(" \t\n\r");
    if (last_non_ws != std::string::npos) {
        result = result.substr(0, last_non_ws + 1);
    }

    return result;
}
