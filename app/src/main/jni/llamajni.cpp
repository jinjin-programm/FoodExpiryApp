#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include "llama.h"

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_context = nullptr;
static const llama_vocab* g_vocab = nullptr;

extern "C" {

// Helper to add a token to a batch
static void llama_batch_add_helper(struct llama_batch & batch, llama_token id, llama_pos pos, llama_seq_id seq_id, bool logits) {
    batch.token[batch.n_tokens] = id;
    batch.pos[batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = 1;
    batch.seq_id[batch.n_tokens][0] = seq_id;
    batch.logits[batch.n_tokens] = logits;
    batch.n_tokens++;
}

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject thiz, jstring modelPath, jint contextSize, jint threads) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s", path);
    
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    g_model = llama_model_load_from_file(path, model_params);
    
    if (g_model == nullptr) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return -1;
    }

    g_vocab = llama_model_get_vocab(g_model);
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t)contextSize;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;
    
    g_context = llama_init_from_model(g_model, ctx_params);
    
    if (g_context == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(modelPath, path);
        return -2;
    }
    
    env->ReleaseStringUTFChars(modelPath, path);
    LOGI("Model loaded successfully");
    return 0;
}

JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeFreeModel(
        JNIEnv* env, jobject thiz) {
    LOGI("Freeing model and context");
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_vocab = nullptr;
    llama_backend_free();
}

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject thiz, jstring prompt) {
    
    if (g_context == nullptr || g_model == nullptr || g_vocab == nullptr) {
        return env->NewStringUTF("Error: Model not loaded");
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating for prompt: %s", promptStr);

    bool add_bos = llama_vocab_get_add_bos(g_vocab);

    // Tokenize prompt
    std::vector<llama_token> tokens_list;
    int n_tokens_max = strlen(promptStr) + 2;
    tokens_list.resize(n_tokens_max);
    int n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr), tokens_list.data(), tokens_list.size(), add_bos, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr), tokens_list.data(), tokens_list.size(), add_bos, true);
    }
    tokens_list.resize(n_tokens);

    env->ReleaseStringUTFChars(prompt, promptStr);

    uint32_t n_ctx = llama_n_ctx(g_context);
    if ((uint32_t)tokens_list.size() > n_ctx) {
        LOGE("Prompt too long: %zu tokens, max context: %u", tokens_list.size(), n_ctx);
        return env->NewStringUTF("Error: Prompt too long (exceeds context size)");
    }

    // Prepare batch
    llama_batch batch = llama_batch_init(tokens_list.size(), 0, 1);
    for (size_t i = 0; i < tokens_list.size(); ++i) {
        llama_batch_add_helper(batch, tokens_list[i], (llama_pos)i, 0, i == tokens_list.size() - 1);
    }

    if (llama_decode(g_context, batch) != 0) {
        LOGE("llama_decode failed (prompt)");
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Decode failed (prompt)");
    }

    std::string response = "";
    int n_cur = tokens_list.size();
    int n_decode = 0;
    const int max_tokens = 256;

    // Use greedy sampler
    struct llama_sampler * smpl = llama_sampler_init_greedy();

    while (n_decode < max_tokens) {
        const llama_token id = llama_sampler_sample(smpl, g_context, -1);

        if (llama_vocab_is_eog(g_vocab, id) || n_cur >= (int)llama_n_ctx(g_context)) {
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(g_vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response += std::string(buf, n);
        }

        // Reuse batch for next token
        batch.n_tokens = 0;
        llama_batch_add_helper(batch, id, (llama_pos)n_cur, 0, true);

        n_cur++;
        n_decode++;

        if (llama_decode(g_context, batch) != 0) {
            LOGE("llama_decode failed (generation)");
            break;
        }
    }

    llama_sampler_free(smpl);
    llama_batch_free(batch);
    LOGI("Generated response: %s", response.c_str());
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}

} // extern "C"
