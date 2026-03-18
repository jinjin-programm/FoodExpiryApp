#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include <cstdlib>
#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state
static llama_model* g_model = nullptr;
static llama_context* g_context = nullptr;
static const llama_vocab* g_vocab = nullptr;
static mtmd_context* g_mtmd_ctx = nullptr;

extern "C" {

// Helper to add tokens to batch
static void         batch_add_token(llama_batch& batch, llama_token id, llama_pos pos) {
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
        if (g_mtmd_ctx) {
            mtmd_free(g_mtmd_ctx);
            g_mtmd_ctx = nullptr;
        }
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
    
    if (g_mtmd_ctx) {
        mtmd_free(g_mtmd_ctx);
        g_mtmd_ctx = nullptr;
    }
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

    // Clear KV cache
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem) {
        llama_memory_clear(mem, false);
    }

    // Tokenize prompt
    bool add_bos = llama_vocab_get_add_bos(g_vocab);
    
    std::vector<llama_token> tokens;
    int n_max = strlen(promptStr) + 128;
    tokens.resize(n_max);
    
    int n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr), 
                                   tokens.data(), tokens.size(), add_bos, true);
    
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(g_vocab, promptStr, strlen(promptStr),
                                   tokens.data(), tokens.size(), add_bos, true);
    }
    tokens.resize(n_tokens);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    LOGI("Tokenized to %d tokens", n_tokens);

    // Create batch
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    for (int i = 0; i < n_tokens; i++) {
        batch_add_token(batch, tokens[i], (llama_pos)i);
    }
    batch.logits[batch.n_tokens - 1] = true;

    // Decode prompt
    if (llama_decode(g_context, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Failed to process prompt");
    }
    llama_batch_free(batch);

    // Generate
    std::string response;
    int n_generated = 0;
    llama_pos n_past = (llama_pos)n_tokens;
    struct llama_sampler* sampler = llama_sampler_init_greedy();
    llama_batch gen_batch = llama_batch_init(1, 0, 1);
    
    while (n_generated < maxTokens) {
        llama_token new_token = llama_sampler_sample(sampler, g_context, -1);
        if (llama_vocab_is_eog(g_vocab, new_token)) break;
        
        char buf[128];
        int n = llama_token_to_piece(g_vocab, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) response.append(buf, n);
        
        gen_batch.n_tokens = 0;
        batch_add_token(gen_batch, new_token, n_past);
        gen_batch.logits[0] = true;
        
        if (llama_decode(g_context, gen_batch) != 0) break;
        
        n_past++;
        n_generated++;
        if (n_past >= (llama_pos)llama_n_ctx(g_context) - 1) break;
    }
    
    llama_sampler_free(sampler);
    llama_batch_free(gen_batch);
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadMmproj(
        JNIEnv* env, jobject thiz, jstring mmprojPath) {
    
    if (g_model == nullptr) {
        LOGE("Text model must be loaded before mmproj");
        return -4;
    }

    const char* path = env->GetStringUTFChars(mmprojPath, nullptr);
    LOGI("Loading mmproj from: %s", path);
    
    struct mtmd_context_params m_params = mtmd_context_params_default();
    m_params.n_threads = 4;
    m_params.use_gpu = false;
    
    g_mtmd_ctx = mtmd_init_from_file(path, g_model, m_params);
    
    env->ReleaseStringUTFChars(mmprojPath, path);
    
    if (g_mtmd_ctx == nullptr) {
        LOGE("Failed to load mmproj");
        return -5;
    }
    
    LOGI("Mmproj loaded successfully, vision support enabled");
    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerateWithImage(
        JNIEnv* env, jobject thiz, jstring prompt, jbyteArray rgbData, jint width, jint height, jint maxTokens) {
    
    if (!g_context || !g_mtmd_ctx) {
        return env->NewStringUTF("Error: Model or Vision not loaded");
    }

    // 1. Prepare Image
    jbyte* pixels = env->GetByteArrayElements(rgbData, nullptr);
    mtmd_bitmap* bitmap = mtmd_bitmap_init((uint32_t)width, (uint32_t)height, (const unsigned char*)pixels);
    env->ReleaseByteArrayElements(rgbData, pixels, JNI_ABORT);

    if (!bitmap) {
        return env->NewStringUTF("Error: Failed to process bitmap");
    }

    // 2. Prepare Prompt and Tokenize
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    
    // Add image marker if not present
    std::string fullPrompt = promptStr;
    if (fullPrompt.find(mtmd_default_marker()) == std::string::npos) {
        fullPrompt = std::string(mtmd_default_marker()) + "\n" + fullPrompt;
    }
    
    mtmd_input_text input_text = { fullPrompt.c_str(), true, true };
    mtmd_input_chunks* chunks = mtmd_input_chunks_init();
    
    const mtmd_bitmap* bitmaps[1] = { bitmap };
    int result = mtmd_tokenize(g_mtmd_ctx, chunks, &input_text, bitmaps, 1);
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    mtmd_bitmap_free(bitmap);

    if (result != 0) {
        mtmd_input_chunks_free(chunks);
        return env->NewStringUTF("Error: Failed to tokenize multimodal input");
    }

    // 3. Inference - use helper function to handle all chunks correctly
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem) {
        llama_memory_clear(mem, false);
    }
    
    LOGI("Processing %zu multimodal chunks with helper", mtmd_input_chunks_size(chunks));
    
    llama_pos new_n_past = 0;
    int32_t eval_result = mtmd_helper_eval_chunks(g_mtmd_ctx, g_context, chunks, 
                                                   0,  // n_past (start from 0)
                                                   0,  // seq_id
                                                   512, // n_batch
                                                   true, // logits_last
                                                   &new_n_past);
    
    mtmd_input_chunks_free(chunks);
    
    if (eval_result != 0) {
        return env->NewStringUTF("Error: Failed to eval multimodal chunks");
    }
    
    llama_pos n_past = new_n_past;

    // 4. Generation
    std::string response;
    int n_generated = 0;
    struct llama_sampler* sampler = llama_sampler_init_greedy();
    llama_batch gen_batch = llama_batch_init(1, 0, 1);
    
    while (n_generated < maxTokens) {
        llama_token new_token = llama_sampler_sample(sampler, g_context, -1);
        if (llama_vocab_is_eog(g_vocab, new_token)) break;
        
        char buf[128];
        int n = llama_token_to_piece(g_vocab, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) response.append(buf, n);
        
        gen_batch.n_tokens = 0;
        batch_add_token(gen_batch, new_token, n_past);
        gen_batch.logits[0] = true;
        
        if (llama_decode(g_context, gen_batch) != 0) break;
        
        n_past++;
        n_generated++;
        if (n_past >= (llama_pos)llama_n_ctx(g_context) - 1) break;
    }
    
    llama_sampler_free(sampler);
    llama_batch_free(gen_batch);
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called with multimodal support");
    return JNI_VERSION_1_6;
}

} // extern "C"