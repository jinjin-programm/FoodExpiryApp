# Technology Stack

**Analysis Date:** 2026-04-08

## Languages

**Primary:**
- Kotlin 1.9.0 (JVM Target 17) - Primary application code
- XML - Android layouts, drawables, and resources

**Secondary:**
- C/C++ - Native code integration (via CMake `src/main/jni/CMakeLists.txt`)

## Runtime

**Environment:**
- Android OS
  - minSdk: 26 (Android 8.0)
  - targetSdk: 34 (Android 14)
  - compileSdk: 36

**Package Manager:**
- Gradle (Kotlin DSL `build.gradle.kts`)
- Lockfile: missing (standard for Android projects without dependency locking configured)

## Frameworks

**Core:**
- AndroidX Core (`androidx.core:core-ktx:1.12.0`) - Core application framework
- Android Architecture Components (Lifecycle, ViewModel, LiveData 2.7.0) - UI State management
- Material Design (`com.google.android.material:material:1.11.0`) - UI components

**Testing:**
- JUnit 4 (`junit:junit:4.13.2`) - Unit testing framework
- Mockito/Mockito-Kotlin (`org.mockito:mockito-core:5.8.0`) - Mocking library
- Espresso (`androidx.test.espresso:espresso-core:3.5.1`) - UI testing

**Build/Dev:**
- Gradle - Build system
- Kapt - Kotlin Annotation Processing Tool
- CMake (3.22.1) - Native code build tool

## Key Dependencies

**Critical:**
- Hilt (`com.google.dagger:hilt-android:2.52`) - Dependency Injection for loose coupling
- Room (`androidx.room:room-runtime:2.6.1`) - SQLite database abstraction layer
- Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`) - Asynchronous programming
- Navigation Component (`androidx.navigation:navigation-fragment-ktx:2.7.7`) - In-app navigation

**Infrastructure:**
- Retrofit (`com.squareup.retrofit2:retrofit:2.9.0`) - Networking and API requests
- CameraX (`androidx.camera:camera-core:1.3.1`) - Camera access
- WorkManager (`androidx.work:work-runtime-ktx:2.9.0`) - Background task scheduling
- DataStore (`androidx.datastore:datastore-preferences:1.0.0`) - Shared preferences replacement
- Glide / Coil - Image loading and caching

**Machine Learning & AI:**
- ML Kit (`com.google.mlkit:barcode-scanning`, `text-recognition`, `image-labeling`) - On-device ML
- TensorFlow Lite (`org.tensorflow:tensorflow-lite:2.14.0`) - Custom model inference
- Llama4j (`com.llama4j:gguf:0.1.1`) - Pure Java GGUF parser for local LLM inference

## Configuration

**Environment:**
- Configured via `local.properties` for local development
- API keys injected into `BuildConfig` during compilation

**Build:**
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts` (implied by typical structure)

## Platform Requirements

**Development:**
- Android Studio
- JDK 17
- Android SDK 36, NDK for CMake/C++ builds

**Production:**
- Android devices running API 26 (Android 8.0) or higher

---

*Stack analysis: 2026-04-08*