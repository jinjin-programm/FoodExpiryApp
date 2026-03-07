#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include <cstdio>
#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOG_TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_context = nullptr;
static const llama_vocab* g_vocab = nullptr;
static mtmd_context* g_mtmd_ctx = nullptr;
static llama_pos g_n_past = 0;

extern "C" {

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

JNIEXPORT jint JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeLoadMmproj(
        JNIEnv* env, jobject thiz, jstring mmprojPath) {
    
    if (g_model == nullptr) {
        LOGE("Model not loaded, cannot load mmproj");
        return -1;
    }
    
    const char* path = env->GetStringUTFChars(mmprojPath, nullptr);
    LOGI("Loading mmproj from: %s", path);
    
    // Check if file exists and get size
    FILE* f = fopen(path, "rb");
    if (f == nullptr) {
        LOGE("Cannot open mmproj file: %s", path);
        env->ReleaseStringUTFChars(mmprojPath, path);
        return -2;
    }
    fseek(f, 0, SEEK_END);
    long file_size = ftell(f);
    fclose(f);
    LOGI("Mmproj file size: %ld bytes", file_size);
    
    // Set up logging for mtmd
    mtmd_helper_log_set([](ggml_log_level level, const char* text, void* user_data) {
        __android_log_print(ANDROID_LOG_INFO, "MTMD", "%s", text);
    }, nullptr);
    
    mtmd_context_params params = mtmd_context_params_default();
    params.use_gpu = false;
    params.print_timings = true;
    params.n_threads = 4;
    params.warmup = false;
    
    LOGI("Calling mtmd_init_from_file...");
    g_mtmd_ctx = mtmd_init_from_file(path, g_model, params);
    
    env->ReleaseStringUTFChars(mmprojPath, path);
    
    if (g_mtmd_ctx == nullptr) {
        LOGE("mtmd_init_from_file returned nullptr");
        return -3;
    }
    
    bool has_vision = mtmd_support_vision(g_mtmd_ctx);
    LOGI("Mmproj loaded successfully, vision support: %s", has_vision ? "yes" : "no");
    return has_vision ? 0 : -4;
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
    g_n_past = 0;
    llama_backend_free();
}

JNIEXPORT jboolean JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeHasVision(
        JNIEnv* env, jobject thiz) {
    return g_mtmd_ctx != nullptr && mtmd_support_vision(g_mtmd_ctx);
}

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject thiz, jstring prompt) {
    
    if (g_context == nullptr || g_model == nullptr || g_vocab == nullptr) {
        return env->NewStringUTF("Error: Model not loaded");
    }

    // Clear KV cache (memory) to ensure fresh generation
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem == nullptr) {
        LOGE("Failed to get llama memory");
        return env->NewStringUTF("Error: Failed to get memory");
    }
    llama_memory_clear(mem, false);
    g_n_past = 0;
    LOGI("KV cache cleared for new generation");

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
        llama_batch_add_helper(batch, tokens_list[i], (llama_pos)g_n_past, 0, i == tokens_list.size() - 1);
        g_n_past++;
    }

    if (llama_decode(g_context, batch) != 0) {
        LOGE("llama_decode failed (prompt)");
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Decode failed (prompt)");
    }

    std::string response = "";
    int n_decode = 0;
    const int max_tokens = 256;

    // Use greedy sampler
    struct llama_sampler * smpl = llama_sampler_init_greedy();

    while (n_decode < max_tokens) {
        const llama_token id = llama_sampler_sample(smpl, g_context, -1);

        if (llama_vocab_is_eog(g_vocab, id) || g_n_past >= (int)llama_n_ctx(g_context)) {
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(g_vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response += std::string(buf, n);
        }

        // Reuse batch for next token
        batch.n_tokens = 0;
        llama_batch_add_helper(batch, id, (llama_pos)g_n_past, 0, true);

        g_n_past++;
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

JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_presentation_ui_llm_LlamaBridge_nativeGenerateWithImage(
        JNIEnv* env, jobject thiz, jstring prompt, jbyteArray imageData, jint width, jint height) {
    
    if (g_context == nullptr || g_model == nullptr || g_vocab == nullptr) {
        return env->NewStringUTF("Error: Model not loaded");
    }
    
    if (g_mtmd_ctx == nullptr) {
        return env->NewStringUTF("Error: Vision model not loaded (mmproj required)");
    }
    
    // Clear KV cache
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem) {
        llama_memory_clear(mem, false);
    }
    g_n_past = 0;

    // Get image data (expecting RGB format)
    jbyte* data = env->GetByteArrayElements(imageData, nullptr);
    jsize dataLen = env->GetArrayLength(imageData);
    
    LOGI("Processing image: %dx%d, data length: %d", width, height, dataLen);
    
    // Create bitmap from RGB data
    mtmd_bitmap* bitmap = mtmd_bitmap_init((uint32_t)width, (uint32_t)height, (const unsigned char*)data);
    env->ReleaseByteArrayElements(imageData, data, JNI_ABORT);
    
    if (bitmap == nullptr) {
        LOGE("Failed to create bitmap");
        return env->NewStringUTF("Error: Failed to create image bitmap");
    }
    
    // Get prompt
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Vision prompt: %s", promptStr);
    
    // Prepare input text with image marker
    std::string fullPrompt = std::string(mtmd_default_marker()) + "\n" + promptStr;
    
    mtmd_input_text text;
    text.text = fullPrompt.c_str();
    text.add_special = true;
    text.parse_special = true;
    
    // Tokenize with image
    mtmd_input_chunks* chunks = mtmd_input_chunks_init();
    const mtmd_bitmap* bitmaps[] = { bitmap };
    
    int32_t res = mtmd_tokenize(g_mtmd_ctx, chunks, &text, bitmaps, 1);
    if (res != 0) {
        LOGE("mtmd_tokenize failed: %d", res);
        mtmd_bitmap_free(bitmap);
        mtmd_input_chunks_free(chunks);
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("Error: Failed to tokenize image");
    }
    
    // Evaluate chunks
    llama_pos new_n_past;
    res = mtmd_helper_eval_chunks(g_mtmd_ctx, g_context, chunks, g_n_past, 0, 512, true, &new_n_past);
    
    mtmd_bitmap_free(bitmap);
    mtmd_input_chunks_free(chunks);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    if (res != 0) {
        LOGE("mtmd_helper_eval_chunks failed: %d", res);
        return env->NewStringUTF("Error: Failed to process image");
    }
    
    g_n_past = new_n_past;
    LOGI("Image processed, n_past: %d", g_n_past);
    
    // Generate response
    std::string response = "";
    int n_decode = 0;
    const int max_tokens = 512;
    
    llama_batch batch = llama_batch_init(1, 0, 1);
    struct llama_sampler* smpl = llama_sampler_init_greedy();
    
    while (n_decode < max_tokens) {
        const llama_token id = llama_sampler_sample(smpl, g_context, -1);
        
        if (llama_vocab_is_eog(g_vocab, id) || g_n_past >= (int)llama_n_ctx(g_context)) {
            break;
        }
        
        char buf[128];
        int n = llama_token_to_piece(g_vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response += std::string(buf, n);
        }
        
        batch.n_tokens = 0;
        llama_batch_add_helper(batch, id, (llama_pos)g_n_past, 0, true);
        
        g_n_past++;
        n_decode++;
        
        if (llama_decode(g_context, batch) != 0) {
            LOGE("llama_decode failed (generation)");
            break;
        }
    }
    
    llama_sampler_free(smpl);
    llama_batch_free(batch);
    
    LOGI("Vision response: %s", response.c_str());
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}

} // extern "C"