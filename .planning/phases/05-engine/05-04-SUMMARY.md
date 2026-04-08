---
phase: 05-engine
plan: 04
subsystem: inference
tags: [mnn, llm, engine, lifecycle, jni]
requires: [05-01, 05-02, 05-03]
provides: [MnnLlmEngine, ModelLifecycleManager, StructuredOutputParser, MnnLlmNative]
affects: []
tech-stack:
  added: [JNI C++ bridge, CMake NDK, Coroutines Mutex]
  patterns: [Singleton, Lifecycle-aware cleanup, JSON parsing]
key-files:
  created:
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmConfig.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/ModelLifecycleManager.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmEngine.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/StructuredOutputParser.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmNative.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/mnn/ProcessLifecycleObserver.kt
    - app/src/main/cpp/mnn_llm_bridge.cpp
    - app/src/main/cpp/CMakeLists.txt
  modified:
    - app/build.gradle.kts
decisions: []
metrics:
  duration: 3 minutes
  completed: "2026-04-08"
  tasks: 3
  files: 9
---

# Phase 05 Plan 04: MNN LLM Inference Engine Summary

## One-Liner

MNN LLM inference engine singleton with lifecycle-aware model management, mutual exclusion enforcement, and structured JSON output parser — JNI bridge stub ready for MNN LLM C++ API integration.

## Objective

Build the MNN LLM inference engine — MnnLlmEngine singleton, model lifecycle manager with mutual exclusion, config, and structured JSON output parser.

Per MNN-05, lifecycle manager prevents OOM. Per MNN-06, exposed as Hilt singleton. Per D-01, NOT exposed directly to ViewModels (they go through repository).

## Completed Tasks

| Task | Name | Commit | Files |
| ---- | ----------- | ------ | ---------------------------- |
| 1 | Create MnnLlmConfig and ModelLifecycleManager | b70bb3b | MnnLlmConfig.kt, ModelLifecycleManager.kt |
| 2 | Create MnnLlmEngine and StructuredOutputParser | ac379a6 | MnnLlmEngine.kt, StructuredOutputParser.kt |
| 3 | Create JNI bridge, CMake config, and ProcessLifecycleObserver | f49b375 | MnnLlmNative.kt, ProcessLifecycleObserver.kt, mnn_llm_bridge.cpp, CMakeLists.txt, build.gradle.kts |

## Key Artifacts

### MnnLlmConfig.kt

Configuration data class with:
- `modelDirPath`, `backendType`, `threadNum`, `maxNewTokens` fields
- `createOptimal()` factory method that caps thread count at `min(cores-1, 4)`
- Defaults: CPU backend, low memory mode, 128 max tokens

### ModelLifecycleManager.kt

`@Singleton` class with:
- `Mutex` for mutual exclusion (only one heavy model at a time)
- `hasEnoughMemory()` checking ≥ 2GB available
- `acquire(modelType)` / `release(modelType)` with suspend support
- `ModelType { YOLO, LLM }` enum

### MnnLlmEngine.kt

`@Singleton` class with:
- `loadModel()` — checks files ready, acquires lifecycle, calls JNI bridge
- `runInference(bitmap)` — converts to JPEG, builds Qwen3.5 prompt template, calls JNI
- `unloadModel()` — releases native handle and lifecycle
- `isModelLoaded()` — state check
- All native calls on `Dispatchers.IO` (per PITFALL-5)
- Loads `libmnn_llm_bridge.so` in companion init

### StructuredOutputParser.kt

`object` with:
- `parse(rawResponse)` — extracts JSON from various formats
- Handles markdown code blocks (`json`)
- Has fallback regex extraction for malformed output
- Returns `FoodIdentification` domain model

### MnnLlmNative.kt

`object` with JNI declarations:
- `nativeCreateLlm(modelDir, threadNum, memoryMode): Long`
- `nativeRunInference(handle, imageData, width, height): String`
- `nativeDestroyLlm(handle)`
- Loads `libmnn_llm_bridge.so`

### mnn_llm_bridge.cpp

Stub C++ JNI bridge:
- Three `extern "C"` JNI functions matching Kotlin declarations
- Logs calls for debugging
- Returns `0` handle (stub) until MNN LLM C++ headers available

### ProcessLifecycleObserver.kt

`@Singleton` `DefaultLifecycleObserver`:
- `onStop()` triggers `engine.unloadModel()` when model loaded
- Uses `CoroutineScope(SupervisorJob() + Dispatchers.Main)` for async cleanup

### CMakeLists.txt + build.gradle.kts

- CMake builds `libmnn_llm_bridge.so`
- `externalNativeBuild` added to defaultConfig (cppFlags `-std=c++17`) and android block (path to CMakeLists.txt)

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: native_bridge | mnn_llm_bridge.cpp | JNI bridge exposes native layer — stub implementation needs validation when full MNN LLM integration is ready |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

| Stub | File | Line | Reason |
|------|------|------|--------|
| Native bridge stub | mnn_llm_bridge.cpp | All functions | Full implementation requires MNN LLM C++ headers (`mnn/llm/llm.hpp`) — deferred until MNN C++ SDK integration |
| nativeHandle=0 return | mnn_llm_bridge.cpp | Line 15 | Stub always returns 0 — model won't actually load until bridge is implemented |

## Verification Checklist

- [x] MnnLlmConfig.kt and ModelLifecycleManager.kt exist
- [x] MnnLlmEngine.kt exists with @Singleton
- [x] StructuredOutputParser.kt exists with parse() method
- [x] MnnLlmNative.kt exists with JNI declarations
- [x] mnn_llm_bridge.cpp exists in app/src/main/cpp/
- [x] CMakeLists.txt builds libmnn_llm_bridge.so
- [x] ProcessLifecycleObserver.kt auto-cleans up on backgrounding
- [x] Engine uses Dispatchers.IO for all native calls
- [x] Lifecycle manager enforces mutual exclusion
- [x] Parser handles JSON extraction from various formats

## Self-Check: PASSED

All files created, all commits verified:
- b70bb3b: MnnLlmConfig + ModelLifecycleManager
- ac379a6: MnnLlmEngine + StructuredOutputParser
- f49b375: JNI bridge + CMake + ProcessLifecycleObserver

---

*Completed: 2026-04-08*