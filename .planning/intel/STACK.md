# Technology Stack

**Analysis Date:** 2026-04-21

## Languages

**Primary:**
- Kotlin 2.0.21 - All application logic, ViewModels, UseCases, Repositories, UI layer
- C++ 17 - Native JNI bridges for MNN inference (`app/src/main/cpp/mnn_llm_bridge.cpp`, `app/src/main/cpp/mnn_yolo_bridge.cpp`)

**Secondary:**
- Java - MNN AAR vendor code (`com.taobao.android.mnn.*` in `app/src/main/java/com/taobao/android/mnn/`)

## Runtime

**Environment:**
- Android SDK 36 (compile), Min SDK 26 (Android 8.0), Target SDK 34 (Android 14)
- Java 17 (source + target compatibility, with desugaring)
- NDK: arm64-v8a only (no x86/armeabi support)

**Package Manager:**
- Gradle (Kotlin DSL) with AGP 8.9.1
- Build files: `build.gradle.kts` (root), `app/build.gradle.kts` (app module)
- Settings: `settings.gradle.kts`
- Gradle JVM args: `-Xmx6144m`

## Frameworks

**Core:**
- AndroidX AppCompat 1.6.1 - Base activity/fragment support
- AndroidX Core-KTX 1.12.0 - Kotlin extensions
- Material Design 1.11.0 - UI components
- Navigation Component 2.7.7 - Fragment navigation with SafeArgs
- Lifecycle 2.7.0 - ViewModel + LiveData + Runtime KTX
- ViewPager2 1.1.0 - Tab-based main navigation

**DI:**
- Hilt 2.52 (Dagger wrapper) - Dependency injection throughout

**Database:**
- Room 2.6.1 - SQLite ORM with migrations (currently at DB version 15)

**Networking:**
- Retrofit 2.9.0 + OkHttp 4.12.0 + Gson 2.10.1

**Async:**
- Kotlinx Coroutines 1.7.3 (Android + Core)

**Background Work:**
- WorkManager 2.9.0 with Hilt-Work 1.1.0

**Camera & ML:**
- CameraX 1.3.1 (core, camera2, lifecycle, view)
- ML Kit Barcode Scanning 17.2.0
- ML Kit Text Recognition 16.0.0
- ML Kit Image Labeling 17.0.7

**On-Device ML Inference:**
- TensorFlow Lite 2.14.0 (runtime, support, metadata, GPU)
- ONNX Runtime 1.20.0 (onnxruntime-android)
- MNN (Alibaba Mobile Neural Network) - via JNI native bridge + bundled AAR

**Firebase:**
- Firebase BOM 32.7.0 (Auth KTX, Analytics KTX)
- Google Play Services Auth 20.7.0

**Image Loading:**
- Glide 4.16.0 (with compiler)
- Coil 2.6.0

**Data Persistence:**
- DataStore Preferences 1.0.0

**UI Libraries:**
- Speed Dial FAB 3.3.0 (`com.leinardi.android:speed-dial`)

**Build Plugins:**
- `com.android.application` 8.9.1
- `org.jetbrains.kotlin.android` 2.0.21
- `org.jetbrains.kotlin.kapt` 2.0.21
- `com.google.dagger.hilt.android` 2.52
- `androidx.navigation.safeargs.kotlin` 2.7.7
- `kotlin-parcelize`
- `com.google.gms.google-services` 4.4.0

## Key Dependencies

**Critical:**
- Room 2.6.1 - All persistent data storage, 10 entities, 10 DAOs
- Hilt 2.52 - All dependency injection wiring
- MNN (JNI) - On-device YOLO detection and LLM inference
- ONNX Runtime 1.20.0 - YOLO detection engine (production default)
- Ollama/LM Studio APIs - Remote LLM food classification

**Infrastructure:**
- OkHttp - Raw HTTP client for Ollama/LM Studio communication
- Retrofit - Structured API calls (Open Food Facts, TheMealDB)
- WorkManager - Daily expiry notification scheduling

## Configuration

**Environment:**
- Build config fields for API keys (defined in `app/build.gradle.kts` lines 78-84):
  - `RAPIDAPI_KEY`, `GOOGLE_VISION_API_KEY`, `OPENAI_API_KEY`, `API_NINJAS_KEY`, `FOODDATA_CENTRAL_KEY` - placeholder values ("test_key")
  - `THEMEALDB_API_KEY` - set to "1" (free tier)
  - `HF_TOKEN` - read from `local.properties`
- Release signing configured via `local.properties` keys: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- MNN source root path via `local.properties` key: `MNN_SOURCE_ROOT`
- DataStore preferences for runtime settings (Ollama server URL, provider selection, UI style)

**Build:**
- `app/build.gradle.kts` - Main build configuration
- `app/src/main/cpp/CMakeLists.txt` - Native C++ build (CMake 3.22.1)
- `proguard-rules.pro` - ProGuard config (minification disabled in release)
- Core library desugaring enabled for `java.time` on older APIs

## Platform Requirements

**Development:**
- Android Studio (any recent version supporting AGP 8.9.1)
- Android SDK with compileSdk 36
- NDK for arm64-v8a C++ compilation
- CMake 3.22.1
- Ollama or LM Studio server for AI food classification features (optional)

**Production:**
- Android 8.0+ (API 26) minimum
- arm64-v8a device (only ABI filtered)
- Camera (optional, `android:required="false"`)
- Internet for barcode lookup, recipe search, and remote LLM classification
- Notification permission (Android 13+)

---

*Stack analysis: 2026-04-21*
