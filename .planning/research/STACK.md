# Technology Stack

**Project:** FoodExpiryApp v2.0 — AI Vision Engine Overhaul
**Researched:** 2026-04-08
**Scope:** MNN inference engine, YOLO object detection, dynamic model download, scan UI overhaul

---

## Overview of Stack Changes

This milestone adds three major subsystems to the existing app:

1. **MNN Inference Engine** — replaces llama.cpp (GGUF) for LLM text generation (Qwen3.5-2B-MNN)
2. **YOLO Object Detection** — adds multi-object food detection in fridge photos (TensorFlow Lite or MNN)
3. **Dynamic Model Download** — downloads ~2GB models from HuggingFace at runtime instead of bundling in APK
4. **OpenCV + Scan UI** — image cropping for YOLO input, CameraX preview modifications

---

## 1. MNN Inference Engine (NEW — replaces llama.cpp)

| Technology | Version | Purpose | Why |
|---|---|---|---|
| **alibaba/MNN** | **3.5.0** (latest, released 2026-04-07) | LLM inference runtime | Official MNN Chat Android component (MnnLlmChat) provides battle-tested LLM inference. v3.5.0 adds Vulkan LLM support, TurboQuant TQ3/TQ4 KV cache quantization, multi-turn prompt cache, and tokenizer refactor (20x+ load speedup). Directly supports Qwen3.5. |
| **taobao-mnn/Qwen3.5-2B-MNN** | N/A (HF model) | LLM model (4-bit quantized, ~2GB) | Pre-exported MNN format with config.json, llm.mnn, llm.mnn.weight, tokenizer.mtok. No conversion needed. Apache-2.0 license. |
| **MNN Android AAR** | From MNN v3.5.0 | Prebuilt MNN libraries for Android | Build via `project/android/build_64.sh -DMNN_BUILD_LLM=true -DMNN_OPENCL=true`. Produces libMNN.so + libllm.so. Avoids maintaining custom CMake build for MNN. |

### MNN Integration Architecture

**Recommended approach:** Use MNN as a prebuilt AAR/library, NOT building from source in the app's CMake.

```
app/
  libs/
    MNN-release.aar          # Prebuilt from MNN project/android/
  src/main/
    jniLibs/arm64-v8a/
      libMNN.so              # MNN core
      libllm.so              # MNN LLM engine
    java/.../inference/
      MnnLlmHelper.kt        # JNI wrapper for LLM inference
      ModelConfig.kt         # config.json generation/management
```

**MNN config.json for Qwen3.5-2B-MNN (recommended mobile settings):**
```json
{
    "backend_type": "cpu",
    "thread_num": 4,
    "precision": "low",
    "memory": "low",
    "use_mmap": true,
    "kvcache_mmap": true,
    "attention_mode": 8,
    "sampler_type": "mixed",
    "mixed_samplers": ["topK", "tfs", "typical", "topP", "min_p", "temperature"],
    "temperature": 0.7,
    "top_k": 40,
    "top_p": 0.9,
    "reuse_kv": true,
    "max_new_tokens": 512
}
```

**Key config choices for mobile:**
- `use_mmap: true` — maps model weights to disk, avoids OOM on memory-constrained devices (CRITICAL for 2GB model)
- `kvcache_mmap: true` — same for KV cache during generation
- `attention_mode: 8` — FlashAttention enabled, no KV quantization (good balance for 2B model)
- `precision: "low"` — uses FP16 where possible
- `memory: "low"` — runtime quantization to reduce memory

### CMakeLists.txt Changes

Replace the existing llama.cpp-based CMake with MNN:

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("foodexpiryapp_jni")

set(CMAKE_CXX_STANDARD 17)

# MNN LLM (replaces llama.cpp)
add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libMNN.so)

add_library(llm SHARED IMPORTED)
set_target_properties(llm PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllm.so)

# App JNI bridge
add_library(app_jni SHARED app_jni.cpp)
target_include_directories(app_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    # MNN headers from the AAR or prebuilt
)
target_link_libraries(app_jni MNN llm android log)
```

### Dependencies to REMOVE

| Library | Why Remove |
|---|---|
| `com.llama4j:gguf:0.1.1` | Replaced entirely by MNN inference |
| `org.tensorflow:tensorflow-lite:*` | YOLO will use MNN or dedicated TFLite (see below) |
| `libllama.so, libggml.so, libggml-base.so, libggml-cpu.so, libmtmd.so` | All llama.cpp native libs replaced by MNN |

### Confidence: HIGH
- MNN v3.5.0 release notes verified on GitHub (2026-04-07)
- Qwen3.5-2B-MNN confirmed on HuggingFace (taobao-mnn org)
- Android build instructions verified in MNN official docs

---

## 2. YOLO Object Detection (NEW)

| Technology | Version | Purpose | Why |
|---|---|---|---|
| **Ultralytics YOLO11n** (export to ONNX → MNN) | 8.4.x | Food object detection model | YOLO11n is the smallest YOLO11 variant (~2.6M params). Export to ONNX then convert to MNN format for unified inference pipeline. Avoids adding a second inference engine. |
| **MNN** (same as above) | 3.5.0 | YOLO inference runtime | Single inference engine for both YOLO and LLM. MNNConvert handles ONNX→MNN conversion. MNN's NMS support built-in. |

### YOLO Export Pipeline

```
Python training (ultralytics)
    → export ONNX: yolo export model=yolo11n_food.pt format=onnx
    → convert to MNN: MNNConvert --modelFile model.onnx --MNNModel yolo.mnn --weightQuantBits=4
    → host on HuggingFace for dynamic download
```

### Alternative: TensorFlow Lite (if MNN YOLO has issues)

| Technology | Version | Purpose | Why |
|---|---|---|---|
| `org.tensorflow:tensorflow-lite` | 2.14.0 | YOLO inference fallback | Already in the project. YOLO→TFLite is well-documented. But adds a second inference engine. |

**Recommendation:** Start with MNN for YOLO (unified engine). Fall back to TFLite only if MNN's NMS or post-processing has issues with the YOLO model.

### YOLO Input Requirements

- Input size: 640×640 (YOLO11n standard)
- Format: NCHW, normalized [0,1] FP32
- Preprocessing: letterbox resize, normalize, convert to tensor
- Post-processing: NMS, confidence threshold filtering

### Confidence: MEDIUM
- MNN supports ONNX→MNN conversion (documented)
- MNN has been used for object detection models (confirmed in MNN docs)
- LOW confidence on MNN's built-in NMS quality — may need custom NMS in Kotlin
- **Phase-specific research needed:** Test MNNConvert with YOLO11n ONNX output

---

## 3. Dynamic Model Download (NEW)

| Technology | Version | Purpose | Why |
|---|---|---|---|
| **OkHttp** | 4.12.0 (already in project) | HTTP client for downloads | Already a dependency. Supports streaming downloads, progress tracking, resume on failure. |
| **WorkManager** | 2.9.0 (already in project) | Background download scheduling | Already a dependency. Handles download retry, device constraints (charging, unmetered WiFi). |
| **HuggingFace Hub API** | REST API | Model file listing + download URLs | `https://huggingface.co/api/models/{repo_id}` for file manifest. Direct download URLs: `https://huggingface.co/{repo_id}/resolve/main/{filename}` |

### Download Architecture

```
ModelDownloadService (WorkManager Worker)
    ├── HuggingFaceApiClient (Retrofit)
    │   └── GET /api/models/taobao-mnn/Qwen3.5-2B-MNN
    │       → file listing with sizes, SHA256 hashes
    ├── ModelDownloadManager
    │   ├── enqueueDownload(modelId, files[])
    │   ├── trackProgress(modelId) → Flow<DownloadProgress>
    │   ├── pauseDownload() / resumeDownload()
    │   └── verifyIntegrity(file, expectedSha256)
    └── ModelFileManager
        ├── getModelDir(modelId) → File
        ├── getModelSize() → Long
        ├── deleteModel(modelId)
        └── isModelComplete(modelId) → Boolean
```

### HuggingFace Direct Download URLs

For `taobao-mnn/Qwen3.5-2B-MNN`:
```
Base: https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN/resolve/main/
Files:
  - config.json
  - llm.mnn
  - llm.mnn.weight (largest file, ~1.5GB+)
  - llm_config.json
  - tokenizer.mtok
```

### Download UX Requirements

- Show download progress with % and MB/total
- Support download pause/resume (critical for ~2GB download)
- Verify SHA256 after download
- Handle WiFi-only preference
- Handle storage space check before download
- Download in foreground (with notification) or background via WorkManager

### Retrofit API Interface

```kotlin
interface HuggingFaceApi {
    @GET("api/models/{repoId}")
    suspend fun getModelInfo(@Path("repoId") repoId: String): ModelInfoResponse

    // Direct download via OkHttp streaming (not Retrofit)
}
```

### Confidence: HIGH
- OkHttp already in project (v4.12.0)
- WorkManager already in project (v2.9.0)
- HuggingFace API URLs verified from HF docs
- Direct file download URLs follow standard HF pattern

---

## 4. OpenCV for Image Preprocessing (NEW)

| Technology | Version | Purpose | Why |
|---|---|---|---|
| **OpenCV Android SDK** | **4.10.0** | Image preprocessing for YOLO (letterbox resize, crop, normalize) | Industry standard for image manipulation. Provides optimized native implementations. The Java API is sufficient for letterbox resize and normalization — no JNI needed. |

### Integration Approach

**Use OpenCV as a Gradle dependency (NOT the full SDK manager):**

```kotlin
// In app/build.gradle.kts
implementation("org.opencv:opencv:4.10.0")
```

Or via the OpenCV Android SDK AAR from Maven:
```kotlin
implementation("org.openpnp:opencv:4.9.0-0")
```

> **NOTE:** OpenCV's official Maven artifacts have inconsistent versioning. The most reliable approach is:
> 1. Download `opencv-android-sdk` from https://opencv.org/releases/
> 2. Import `opencv-410.aar` as `libs/opencv-410.aar`
> 3. Add `implementation(files("libs/opencv-410.aar"))`

### What OpenCV is Used For

| Operation | OpenCV Function | Why OpenCV |
|---|---|---|
| Letterbox resize | `Imgproc.resize()` with padding | Maintains aspect ratio, adds border padding |
| Normalization | Convert to FP32, divide by 255 | Core image processing |
| Crop from YOLO bbox | `Mat.submat()` | Efficient region extraction from camera frame |
| Color conversion | `Imgproc.cvtColor()` (RGBA→RGB) | CameraX provides RGBA, YOLO expects RGB |

### Alternative: Bitmap-based Processing (simpler but slower)

If OpenCV integration proves heavy (~50MB native lib), use Android's built-in `Bitmap`:

```kotlin
// Letterbox resize using Bitmap.createScaledBitmap + Canvas
// Normalize using Bitmap.getPixels() → FloatBuffer
// This is ~2-3x slower than OpenCV native but avoids the dependency
```

**Recommendation:** Start with Bitmap-based processing. Add OpenCV only if performance profiling shows it's needed. The YOLO11n model is fast enough that preprocessing overhead may not matter.

### Confidence: MEDIUM
- OpenCV 4.10.0 is the latest stable release (verified on opencv.org/releases)
- LOW confidence on Maven availability — OpenCV's Android SDK distribution is inconsistent
- Bitmap-based fallback is well-understood

---

## 5. Scan UI Modifications (Existing — CameraX)

| Technology | Version | Purpose | Changes |
|---|---|---|---|
| **CameraX** | 1.3.1 (already in project) | Camera preview | No version change needed. UI modifications only. |
| **AndroidX ConstraintLayout** | 2.1.4 (already in project) | Horizontal frame layout | ConstraintSet modifications for landscape preview |
| **Material Design 3** | 1.11.0 (already in project) | Capture button, progress UI | Custom views for capture animation and loading states |

### CameraX Changes Needed

| Change | Implementation |
|---|---|
| Horizontal (landscape) photo frame | Modify PreviewView aspect ratio via ConstraintLayout constraints. Use `ResolutionSelector` to force landscape resolution. |
| Manual capture button (barcode) | Add `ImageCapture` use case. Remove continuous `ImageAnalysis` for barcode mode. User taps button → single frame analysis. |
| Remove auto-scan/flash/close buttons | Layout XML changes. Remove views from `fragment_scan.xml`. |
| Capture animation | Lottie animation or simple ViewPropertyAnimator for capture feedback. |
| AI inference loading screen | New `ScanResultFragment` or dialog with progress indicator. Show YOLO detection → LLM classification stages. |
| New scan tab for YOLO | Add ViewPager2 tab or bottom nav item for "Food Scanner" mode with different ImageAnalysis pipeline. |

### ImageCapture for Manual Barcode Mode

```kotlin
// Add to bindCameraUseCases()
val imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    .build()

// On button click
imageCapture.takePicture(
    ContextCompat.getMainExecutor(requireContext()),
    object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(imageProxy: ImageProxy) {
            // Run barcode/text analysis on single frame
            // Then show results
        }
    }
)
```

### Confidence: HIGH
- CameraX 1.3.1 is stable, well-documented
- All changes are standard Android UI work
- No new dependencies needed

---

## Complete Dependency Changes Summary

### Dependencies to ADD

```kotlin
// app/build.gradle.kts

// MNN (prebuilt AAR)
implementation(files("libs/MNN-release.aar"))

// OpenCV (if needed — see Section 4)
// implementation(files("libs/opencv-410.aar"))
```

### Dependencies to REMOVE

```kotlin
// Remove llama.cpp GGUF
// implementation("com.llama4j:gguf:0.1.1")

// Remove TensorFlow Lite (if YOLO runs on MNN)
// implementation("org.tensorflow:tensorflow-lite:2.14.0")
// implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
// implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
// implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
```

### Dependencies to KEEP (unchanged)

All existing dependencies remain: Room, Hilt, CameraX, ML Kit (barcode + OCR), WorkManager, OkHttp, Retrofit, Gson, Glide, Coil, Navigation, ViewPager2, DataStore.

### Native Libraries to REPLACE

```
REMOVE from app/src/main/jniLibs/arm64-v8a/:
  libllama.so, libggml.so, libggml-base.so, libggml-cpu.so, libmtmd.so, libomp.so

ADD to app/src/main/jniLibs/arm64-v8a/:
  libMNN.so        (~10-20MB)
  libllm.so        (~2-5MB, MNN's LLM engine — different from llama.cpp's)
  libMNN_CL.so     (~2MB, OpenCL backend — optional but recommended for GPU acceleration)
```

---

## Build Configuration Changes

### build.gradle.kts Changes

```kotlin
android {
    // No minSdk change needed (26 is sufficient for MNN)
    
    // Remove mlModelBinding if TFLite is removed
    buildFeatures {
        viewBinding = true
        buildConfig = true
        // mlModelBinding = true  // REMOVE if TFLite removed
    }
    
    // Add build config for model download URL
    defaultConfig {
        buildConfigField("String", "HF_MODEL_REPO", "\"taobao-mnn/Qwen3.5-2B-MNN\"")
        buildConfigField("String", "HF_BASE_URL", "\"https://huggingface.co\"")
    }
}

dependencies {
    // Keep existing dependencies
    // Remove GGUF and TFLite
    // Add MNN AAR
}
```

### ProGuard Rules (Release Builds)

```proguard
# MNN
-keep class com.alibaba.mnn.** { *; }
-keepclassmembers class com.alibaba.mnn.** { *; }

# HuggingFace model files (no obfuscation needed for downloaded files)
```

---

## APK Size Impact

| Component | Size Impact | Notes |
|---|---|---|
| Remove llama.cpp native libs | **-40MB** | 6 .so files removed |
| Remove TFLite native libs | **-20MB** | 4 TFLite artifacts removed |
| Add MNN native libs | **+15-25MB** | libMNN.so + libllm.so + libMNN_CL.so |
| Add OpenCV (optional) | **+50MB** | Full OpenCV SDK is large; Bitmap fallback avoids this |
| Remove GGUF Java lib | **-1MB** | llama4j:gguf artifact |
| **Net change** | **~-35 to -45MB** | Significant APK size reduction |

Model files (~2GB for Qwen3.5-2B-MNN, ~10-30MB for YOLO11n-MNN) are downloaded at runtime, NOT bundled in APK.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|---|---|---|---|
| LLM Inference | MNN v3.5.0 | llama.cpp (current) | MNN is 2-5x faster on mobile, better memory management with mmap, official Qwen3.5 support |
| YOLO Runtime | MNN | TensorFlow Lite | Unified inference engine; TFLite adds ~20MB to APK and duplicate runtime |
| Image Preprocessing | Android Bitmap | OpenCV | Avoids 50MB dependency; sufficient performance for preprocessing |
| Model Download | OkHttp + WorkManager | Android DownloadManager | OkHttp gives progress tracking, pause/resume, integrity verification. DownloadManager is less flexible. |
| MNN Integration | Prebuilt AAR | Build from source | Building MNN from source adds 10+ min to CI, complex CMake config. Prebuilt AAR is simpler. |

---

## Installation & Setup

### Prerequisites

1. **Build MNN for Android** (one-time, produces prebuilt AAR):
   ```bash
   git clone https://github.com/alibaba/MNN.git
   cd MNN/project/android
   mkdir build_64
   ../build_64.sh -DMNN_BUILD_LLM=true -DMNN_OPENCL=true -DMNN_USE_LOGCAT=true
   # Output: MNN-release.aar in build_64/output/
   ```

2. **Export YOLO model to MNN** (one-time):
   ```bash
   pip install ultralytics
   yolo export model=yolo11n_food.pt format=onnx imgsz=640
   # Then convert with MNNConvert (included in MNN build)
   ./MNNConvert --modelFile yolo11n_food.onnx --MNNModel yolo_food.mnn \
     --weightQuantBits=4 --weightQuantBlock=128 -f ONNX --transformerFuse=1
   ```

3. **Host models on HuggingFace**:
   - Upload YOLO MNN model to your HF repo
   - Qwen3.5-2B-MNN already at `taobao-mnn/Qwen3.5-2B-MNN`

### Gradle Setup

```bash
# Copy MNN AAR to project
mkdir -p app/libs
cp MNN/project/android/build_64/output/MNN-release.aar app/libs/
```

---

## Phase-Specific Research Flags

| Topic | Research Needed | Priority |
|---|---|---|
| MNN Android AAR build process | Verify exact build_64.sh output and AAR contents | HIGH — blocker for Phase 1 |
| MNN YOLO NMS behavior | Test MNNConvert with YOLO11n ONNX; verify NMS output | HIGH — blocker for Phase 2 |
| MNN JNI bridge for LLM | Study MnnLlmChat Kotlin code for response() API | HIGH — blocker for Phase 1 |
| OpenCV vs Bitmap preprocessing | Benchmark letterbox resize performance | LOW — can defer, Bitmap fallback exists |
| HuggingFace LFS download reliability | Test large file download resilience on mobile | MEDIUM — needed for Phase 1 |
| CameraX landscape resolution selector | Verify ResolutionSelector API for forced landscape | LOW — standard CameraX API |

---

## Sources

- [MNN v3.5.0 Release Notes](https://github.com/alibaba/MNN/releases/tag/3.5.0) — HIGH confidence, official release
- [MNN LLM Documentation](https://mnn-docs.readthedocs.io/en/latest/transformers/llm.html) — HIGH confidence, official docs
- [taobao-mnn/Qwen3.5-2B-MNN on HuggingFace](https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN) — HIGH confidence, verified model page
- [Ultralytics YOLO26](https://github.com/ultralytics/ultralytics) — HIGH confidence, official repo
- [OpenCV Android](https://opencv.org/android/) — MEDIUM confidence, official page
- [HuggingFace Hub Download Guide](https://huggingface.co/docs/huggingface_hub/guides/download) — HIGH confidence, official docs
- [CameraX Developer Guide](https://developer.android.com/training/camerax) — HIGH confidence, Android official docs
