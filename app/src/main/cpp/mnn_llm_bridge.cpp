#include <jni.h>
#include <android/log.h>
#include <string>
#include <sstream>
#include <fstream>
#include <memory>
#include <mutex>

#include <vector>

#include "llm/llm.hpp"
#include <cv/imgcodecs.hpp>
#include "omni.hpp"

#define TAG "MnnLlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

using MNN::Transformer::Llm;
using MNN::Transformer::ChatMessages;
using MNN::Transformer::MultimodalPrompt;

struct LlmInstance {
    std::unique_ptr<Llm> llm;
    std::string last_response;
    bool is_loaded = false;
    int max_new_tokens = 128;
};

static std::mutex g_llm_mutex;

static std::string stripThinkingProcess(const std::string& text);
static std::string cleanChatTemplateTokens(const std::string& text);

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeCreateLlm(
        JNIEnv *env, jobject thiz, jstring modelDir, jint threadNum,
        jfloat temperature, jfloat topP, jint topK, jfloat repetitionPenalty, jint maxNewTokens) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    const char *dir = env->GetStringUTFChars(modelDir, nullptr);
    LOGI("nativeCreateLlm: modelDir=%s, threadNum=%d, temperature=%.2f, topP=%.2f, topK=%d, repetitionPenalty=%.2f, maxNewTokens=%d", dir, threadNum, temperature, topP, topK, repetitionPenalty, maxNewTokens);

    try {
        auto* instance = new LlmInstance();

        std::string config_path = std::string(dir) + "/config.json";
        instance->llm.reset(Llm::createLLM(config_path));
        if (!instance->llm) {
            LOGE("nativeCreateLlm: createLLM failed for %s", config_path.c_str());
            env->ReleaseStringUTFChars(modelDir, dir);
            delete instance;
            return 0;
        }

        std::string config_json = "{";
        config_json += "\"thread_num\":" + std::to_string(threadNum) + ",";
        config_json += "\"use_mmap\":true,";
        config_json += "\"use_template\":false,";
        config_json += "\"tmp_path\":\"" + std::string(dir) + "\",";
        config_json += "\"temperature\":" + std::to_string(temperature) + ",";
        config_json += "\"top_p\":" + std::to_string(topP) + ",";
        config_json += "\"topK\":" + std::to_string(topK) + ",";
        config_json += "\"repetition_penalty\":" + std::to_string(repetitionPenalty) + ",";
        config_json += "\"max_new_tokens\":" + std::to_string(maxNewTokens) + ",";
        config_json += "\"async\":false";
        config_json += "}";

        LOGI("nativeCreateLlm: config=%s", config_json.c_str());
        instance->llm->set_config(config_json);

        LOGI("nativeCreateLlm: loading model (thinking enabled by default)...");
        instance->is_loaded = instance->llm->load();
        if (!instance->is_loaded) {
            LOGE("nativeCreateLlm: load() failed");
            env->ReleaseStringUTFChars(modelDir, dir);
            delete instance;
            return 0;
        }

        LOGI("nativeCreateLlm: llm load() returned true, checking dump_config...");
        std::string config_dump = instance->llm->dump_config();
        LOGI("nativeCreateLlm: dump_config len=%zu", config_dump.length());
        LOGI("nativeCreateLlm: dump_config (first 500 chars): %.500s", config_dump.c_str());

        env->ReleaseStringUTFChars(modelDir, dir);

        LOGI("nativeCreateLlm: success (instance=%p)", instance);
        instance->max_new_tokens = maxNewTokens;
        return reinterpret_cast<jlong>(instance);
    } catch (const std::exception& e) {
        LOGE("nativeCreateLlm: exception: %s", e.what());
        env->ReleaseStringUTFChars(modelDir, dir);
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
        MultimodalPrompt mm_prompt;
        mm_prompt.prompt_template = "<|im_start|>user\n<img>image_0</img>\nWhat is in this image? Provide a brief description and then state the food name as 'The food is [name]'.<|im_end|>\n<|im_start|>assistant\n";
        LOGI("nativeRunInference: raw multimodal prompt (len=%zu): %.200s", mm_prompt.prompt_template.length(), mm_prompt.prompt_template.c_str());

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
            mm_prompt.images["image_0"] = image_part;
        } else {
            LOGE("nativeRunInference: failed to decode image via MNN::CV::imdecode");
        }

        LOGI("nativeRunInference: mm_prompt.images map size=%zu", mm_prompt.images.size());
        if (mm_prompt.images.count("image_0")) {
            const auto& img_part = mm_prompt.images.at("image_0");
            LOGI("nativeRunInference: image_0 width=%d height=%d has_data=%s",
                img_part.width, img_part.height,
                img_part.image_data.get() ? "yes" : "no");
        } else {
            LOGW("nativeRunInference: image_0 NOT FOUND in images map!");
        }

        std::stringstream response_stream;

        LOGI("nativeRunInference: calling response() with max_new_tokens=%d", instance->max_new_tokens);
        instance->llm->response(mm_prompt, &response_stream, "\n", instance->max_new_tokens);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInference: raw response=%s (len=%zu)", response_str.c_str(), response_str.length());

        std::string cleaned = stripThinkingProcess(response_str);
        cleaned = cleanChatTemplateTokens(cleaned);

        instance->last_response = response_str;

        instance->llm->reset();
        LOGI("nativeRunInference: context reset, returning: %s (len=%zu)", cleaned.c_str(), cleaned.length());
        return env->NewStringUTF(cleaned.c_str());
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
        return env->NewStringUTF("Error");
    }

    jsize len = env->GetArrayLength(imageDataObj);
    jbyte* buffer = env->GetByteArrayElements(imageDataObj, nullptr);
    std::vector<uint8_t> img_data(buffer, buffer + len);
    env->ReleaseByteArrayElements(imageDataObj, buffer, JNI_ABORT);

    const char *hint_str = env->GetStringUTFChars(hint, nullptr);

    try {
        MultimodalPrompt mm_prompt;
        std::string user_msg = "<|im_start|>user\n<img>image_0</img>\nWhat is in this image? It might be a " + std::string(hint_str) + ". Provide a brief description and then state the food name as 'The food is [name]'.<|im_end|>\n<|im_start|>assistant\n";
        mm_prompt.prompt_template = user_msg;
        LOGI("nativeRunInferenceWithHint: raw multimodal prompt (len=%zu): %.200s", mm_prompt.prompt_template.length(), mm_prompt.prompt_template.c_str());

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
            mm_prompt.images["image_0"] = image_part;
        } else {
            LOGE("nativeRunInferenceWithHint: failed to decode image via MNN::CV::imdecode");
        }

        std::stringstream response_stream;

        instance->llm->response(mm_prompt, &response_stream, "\n", instance->max_new_tokens);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInferenceWithHint: raw response=%s (len=%zu)", response_str.c_str(), response_str.length());

        env->ReleaseStringUTFChars(hint, hint_str);

        std::string cleaned = stripThinkingProcess(response_str);
        cleaned = cleanChatTemplateTokens(cleaned);

        instance->last_response = response_str;

        instance->llm->reset();
        LOGI("nativeRunInferenceWithHint: context reset, returning: %s (len=%zu)", cleaned.c_str(), cleaned.length());
        return env->NewStringUTF(cleaned.c_str());
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

    auto removeTagsOnly = [](std::string s, const std::string& openTag,
                              const std::string& closeTag) -> std::string {
        size_t pos = 0;
        while ((pos = s.find(openTag, pos)) != std::string::npos) {
            s.erase(pos, openTag.length());
            size_t closePos = s.find(closeTag, pos);
            if (closePos != std::string::npos) {
                s.erase(closePos, closeTag.length());
            }
        }
        return s;
    };

    result = removeTagsOnly(result, "<thinkApproach>", "</thinkApproach>");
    result = removeTagsOnly(result, "<think>", "</think>");
    result = removeTagsOnly(result, "<think|>", "</think|>");
    result = removeTagsOnly(result, "<thought>", "</thought>");

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

static std::string cleanChatTemplateTokens(const std::string& text) {
    std::string result = text;
    std::vector<std::string> tokens = {
        "<|im_start|>", "<|im_end|>", "\n",
        "<|begin_of_text|>", "<|start_header_id|>", "<|end_header_id|>",
        "<|eot_id|>", "<|reserved"
    };
    for (const auto& tok : tokens) {
        size_t pos = 0;
        while ((pos = result.find(tok, pos)) != std::string::npos) {
            size_t end = pos + tok.length();
            while (end < result.length() && result[end] != '>' && result[end] != '<') end++;
            if (end < result.length() && result[end] == '>') end++;
            result.erase(pos, end - pos);
        }
    }
    size_t pos = 0;
    while ((pos = result.find("<|", pos)) != std::string::npos) {
        size_t end = result.find("|>", pos);
        if (end != std::string::npos) {
            end += 2;
            result.erase(pos, end - pos);
        } else {
            pos += 2;
        }
    }
    size_t first_non_ws = result.find_first_not_of(" \t\n\r");
    if (first_non_ws != std::string::npos) {
        result = result.substr(first_non_ws);
    }
    size_t last_non_ws = result.find_last_not_of(" \t\n\r");
    if (last_non_ws != std::string::npos) {
        result = result.substr(0, last_non_ws + 1);
    }
    return result;
}
