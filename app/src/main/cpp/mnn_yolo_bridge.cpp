#include <jni.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <memory>
#include <mutex>

// MNN headers for real inference (when model is available)
#include <MNN/Interpreter.hpp>
#include <MNN/ImageProcess.hpp>
#include <MNN/expr/Expr.hpp>

#define TAG "MnnYoloBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

/**
 * YOLO instance holding MNN interpreter and session.
 * Similar to LlmInstance pattern in mnn_llm_bridge.cpp.
 */
struct YoloInstance {
    std::unique_ptr<MNN::Interpreter> interpreter;
    MNN::Session* session = nullptr;
    int inputSize = 640;  // YOLO input resolution (640x640)
    bool is_loaded = false;
};

static std::mutex g_yolo_mutex;

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_yolo_MnnYoloNative_nativeCreateYolo(
        JNIEnv *env, jobject thiz, jstring modelAssetPath, jint threadNum) {
    std::lock_guard<std::mutex> lock(g_yolo_mutex);

    const char *assetPath = env->GetStringUTFChars(modelAssetPath, nullptr);
    LOGI("nativeCreateYolo: assetPath=%s, threadNum=%d", assetPath, threadNum);

    try {
        auto* instance = new YoloInstance();

        // STUB MODE: For now, verify the asset exists and return a dummy handle.
        // Real MNN YOLO session creation will be implemented when a real model is available.
        // Per D-01: Real model from github.com/tishuo-wang/YOLO_CLIP_targetDetection (converted to MNN)
        // Per D-02: YOLOv11 (YOLO26) alternative if reference model difficult to convert.

        // TODO: Implement real MNN YOLO session creation:
        // 1. Load model from assets via AAssetManager
        // 2. Create MNN::Interpreter from model data
        // 3. Create session with threadNum and backend config
        // 4. Store session in YoloInstance

        LOGW("nativeCreateYolo: STUB MODE — returning dummy handle (model not yet implemented)");
        LOGI("nativeCreateYolo: stub success (instance=%p)", instance);

        env->ReleaseStringUTFChars(modelAssetPath, assetPath);
        // Return dummy handle (1) to indicate success in stub mode
        // Cast to jlong via pointer to allow proper cleanup in destroy
        return reinterpret_cast<jlong>(instance);
    } catch (const std::exception& e) {
        LOGE("nativeCreateYolo: exception: %s", e.what());
        env->ReleaseStringUTFChars(modelAssetPath, assetPath);
        return 0;
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_foodexpiryapp_inference_yolo_MnnYoloNative_nativeRunDetection(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jbyteArray bitmapData,
        jint width, jint height) {
    std::lock_guard<std::mutex> lock(g_yolo_mutex);

    auto* instance = reinterpret_cast<YoloInstance*>(nativeHandle);
    if (!instance) {
        LOGE("nativeRunDetection: invalid handle");
        return env->NewFloatArray(0);
    }

    // Validate bitmap dimensions per T-08-01-01 (threat mitigation)
    if (width <= 0 || height <= 0) {
        LOGE("nativeRunDetection: invalid bitmap dimensions %dx%d", width, height);
        return env->NewFloatArray(0);
    }

    jsize dataLen = env->GetArrayLength(bitmapData);
    LOGI("nativeRunDetection: image data=%d bytes, dimensions=%dx%d", dataLen, width, height);

    // STUB MODE: Return empty FloatArray until real MNN YOLO inference is implemented.
    // Per D-05: Output format must match MnnYoloPostprocessor.parseDetections expectation:
    //   [x1, y1, x2, y2, confidence, class_id, ...class_scores] per row (6+ values per row)
    //
    // TODO: Implement real MNN YOLO inference:
    // 1. Decode JPEG byte array to bitmap via MNN::CV::imdecode
    // 2. Preprocess: letterbox resize to 640x640, normalize to [0,1]
    // 3. Feed input tensor to MNN session
    // 4. Run session
    // 5. Extract output tensor
    // 6. Postprocess: filter by confidence threshold, format as FloatArray
    //    Each row: [x1, y1, x2, y2, confidence, class_id] in 640x640 pixel space

    LOGW("nativeRunDetection: STUB MODE — returning empty detection array");
    return env->NewFloatArray(0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_inference_yolo_MnnYoloNative_nativeDestroyYolo(
        JNIEnv *env, jobject thiz, jlong nativeHandle) {
    std::lock_guard<std::mutex> lock(g_yolo_mutex);

    auto* instance = reinterpret_cast<YoloInstance*>(nativeHandle);
    if (!instance) {
        LOGE("nativeDestroyYolo: invalid handle");
        return;
    }

    try {
        // TODO: Release MNN session and interpreter when real inference is implemented
        // if (instance->session && instance->interpreter) {
        //     instance->interpreter->releaseSession(instance->session);
        // }
        LOGI("nativeDestroyYolo: destroyed instance=%p", instance);
        delete instance;
    } catch (const std::exception& e) {
        LOGE("nativeDestroyYolo: exception: %s", e.what());
        delete instance;
    }
}
