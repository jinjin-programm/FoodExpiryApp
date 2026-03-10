#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include "llama.h"

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state for text-only inference
static llama_model* g_model = nullptr;
static llama_context* g_context = nullptr;
static const llama_vocab* g_vocab = nullptr;

extern "C" {

// Helper to add tokens to batch
static void batch_add_token(llama_batch& batch, llama_token id, llama_pos pos) {
    batch.token[batch.n_tokens] = id;
    batch.pos[batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = 1;
    batch.seq_id[batch.n_tokens][0] = 0;
    batch.logits[batch.n_tokens] = false;
    batch.n_tokens++;
}

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject thiz, jstring modelPath, jint contextSize, jint threads) {
    
    if (g_model != nullptr) {
        LOGI("Model already loaded, freeing previous instance");
        llama_free(g_context);
        llama_model_free(g_model);
        g_context = nullptr;
        g_model = nullptr;
        g_vocab = nullptr;
    }
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading text-only model from: %s", path);
    LOGI("Context size: %d, Threads: %d", contextSize, threads);
    
    // Initialize llama backend
    llama_backend_init();
    
    // Load model with optimized settings for mobile
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;  // Use memory mapping for faster load
    model_params.check_tensors = false;  // Skip tensor validation for speed
    
    g_model = llama_model_load_from_file(path, model_params);
    
    if (g_model == nullptr) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return -1;
    }

    g_vocab = llama_model_get_vocab(g_model);
    if (g_vocab == nullptr) {
        LOGE("Failed to get vocabulary from model");
        llama_model_free(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(modelPath, path);
        return -2;
    }
    
    // Create context with mobile-optimized settings
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t)contextSize;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;
    ctx_params.rope_scaling_type = LLAMA_ROPE_SCALING_TYPE_UNSPECIFIED;
    ctx_params.cb_eval = nullptr;
    ctx_params.cb_eval_user_data = nullptr;
    
    g_context = llama_init_from_model(g_model, ctx_params);
    
    if (g_context == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        g_vocab = nullptr;
        env->ReleaseStringUTFChars(modelPath, path);
        return -3;
    }
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    // Log model info
    uint32_t n_ctx = llama_n_ctx(g_context);
    uint32_t n_embd = llama_model_n_embd(g_model);
    uint32_t n_layer = llama_model_n_layer(g_model);
    LOGI("Model loaded successfully: n_ctx=%u, n_embd=%u, n_layer=%u", n_ctx, n_embd, n_layer);
    
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
    LOGI("Model freed successfully");
}

JNIEXPORT jboolean JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeIsLoaded(
        JNIEnv* env, jobject thiz) {
    return g_model != nullptr && g_context != nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject thiz, jstring prompt, jint maxTokens) {
    
    if (g_context == nullptr || g_model == nullptr || g_vocab == nullptr) {
        return env->NewStringUTF("Error: Model not loaded");
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating response for prompt (%zu chars)", strlen(promptStr));

    // Clear KV cache for fresh generation
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem) {
        llama_memory_clear(mem, false);
    }
    LOGI("KV cache cleared");

    // Tokenize prompt
    bool add_bos = llama_vocab_get_add_bos(g_vocab);
    bool add_eos = llama_vocab_get_add_eos(g_vocab);
    
    std::vector<llama_token> tokens;
    int n_max = strlen(promptStr) + 256;
    tokens.resize(n_max);
    
    int n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr), 
                                   tokens.data(), tokens.size(), add_bos, false);
    
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr),
                                   tokens.data(), tokens.size(), add_bos, false);
    }
    tokens.resize(n_tokens);
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    LOGI("Tokenized to %d tokens", n_tokens);

    // Validate context size
    uint32_t n_ctx = llama_n_ctx(g_context);
    if ((uint32_t)n_tokens + maxTokens > n_ctx) {
        LOGE("Prompt too long: %d tokens, max: %u", n_tokens, n_ctx - maxTokens);
        return env->NewStringUTF("Error: Prompt too long");
    }

    // Create batch for prompt
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    
    for (int i = 0; i < n_tokens; i++) {
        batch_add_token(batch, tokens[i], (llama_pos)i);
    }
    // Set logits for last token
    batch.logits[batch.n_tokens - 1] = true;

    // Decode prompt
    int result = llama_decode(g_context, batch);
    if (result != 0) {
        LOGE("Failed to decode prompt: %d", result);
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Failed to process prompt");
    }
    
    llama_batch_free(batch);
    LOGI("Prompt decoded successfully");

    // Generate tokens
    std::string response;
    int n_generated = 0;
    llama_pos n_past = (llama_pos)n_tokens;
    
    // Use greedy sampling for speed
    struct llama_sampler* sampler = llama_sampler_init_greedy();
    
    llama_batch gen_batch = llama_batch_init(1, 0, 1);
    
    while (n_generated < maxTokens) {
        // Sample next token
        llama_token new_token = llama_sampler_sample(sampler, g_context, -1);
        
        // Check for EOS
        if (llama_vocab_is_eog(g_vocab, new_token)) {
            LOGI("Reached EOS after %d tokens", n_generated);
            break;
        }
        
        // Convert token to text
        char buf[128];
        int n = llama_token_to_piece(g_vocab, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) {
            response.append(buf, n);
        }
        
        // Prepare batch for next token
        gen_batch.n_tokens = 0;
        batch_add_token(gen_batch, new_token, n_past);
        gen_batch.logits[0] = true;
        
        // Decode
        result = llama_decode(g_context, gen_batch);
        if (result != 0) {
            LOGE("Failed to decode token %d: %d", n_generated, result);
            break;
        }
        
        n_past++;
        n_generated++;
        
        // Memory check
        if (n_past >= (llama_pos)n_ctx - 1) {
            LOGI("Reached context limit at %d tokens", n_generated);
            break;
        }
    }
    
    llama_sampler_free(sampler);
    llama_batch_free(gen_batch);
    
    LOGI("Generated %d tokens: %s", n_generated, response.substr(0, 100).c_str());
    
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadMmproj(
        JNIEnv* env, jobject thiz, jstring mmprojPath) {
    
    const char* path = env->GetStringUTFChars(mmprojPath, nullptr);
    LOGI("Mmproj loading requested: %s", path);
    LOGI("Note: Vision support not implemented in this build");
    env->ReleaseStringUTFChars(mmprojPath, path);
    
    // Vision/multimodal support not implemented
    // Return non-zero to indicate failure
    return -1;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called for text-only llama.cpp");
    return JNI_VERSION_1_6;
}

} // extern "C"