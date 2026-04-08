# Domain Pitfalls: MNN/YOLO/LLM Integration on Android

**Domain:** Mobile on-device AI inference (MNN engine, YOLO detection, LLM classification, large model download)
**Researched:** 2026-04-08
**Context:** FoodExpiryApp v2.0 — Replacing llama.cpp (GGUF) with MNN, adding YOLO+LLM multi-object food detection pipeline, dynamic 1.2GB model download from HuggingFace, scan UI overhaul.

---

## Critical Pitfalls

Mistakes that cause crashes, data corruption, or require rewrites.

### PITFALL-1: OOM from Running YOLO + MNN LLM Simultaneously

**Severity:** CRITICAL
**What goes wrong:** Loading both the YOLO TFLite interpreter (~5-30MB) and the MNN LLM (Qwen3.5-2B-MNN, ~1.2GB mmap weight file) into memory at the same time causes OutOfMemoryError on devices with <6GB RAM. The MNN model alone uses ~1.5-2GB of virtual address space with mmap, and YOLO adds its own working memory. The existing `LlamaBridge` loads the entire model into memory via `llama_model_load_from_file` with `use_mmap = true` — MNN will do the same, but the concurrent YOLO interpreter is an extra consumer.

**Why it happens:** The current architecture loads both models eagerly. `YoloDetector` loads in the Fragment constructor, and `LlamaBridge` loads on first use. If the user navigates to the YOLO scan tab (loading the TFLite interpreter) and then triggers LLM classification without unloading YOLO first, memory pressure spikes.

**Consequences:** App crash (OOM), ANR from GC thrashing, system killing the app process.

**Prevention:**
- Implement a **model lifecycle manager** that enforces mutual exclusion: only one heavy model loaded at a time
- Unload YOLO interpreter (`interpreter?.close()`) before loading MNN LLM, and vice-versa
- Use MNN's `use_mmap: true` and `memory: "low"` config options (runtime quantization) to reduce memory footprint
- Set `chunk: 128` in MNN config to limit per-token memory usage
- Monitor `ActivityManager.MemoryInfo.availMem` before model load — reject if <2GB available
- Use `kvcache_mmap: true` and `tmp_path` to spill KV cache to disk when under memory pressure

**Detection:** Profile with Android Studio Memory Profiler. Watch for `adb shell dumpsys meminfo <pid>` showing >80% of available memory. Log `Debug.getNativeHeapAllocatedSize()` before/after model loads.

---

### PITFALL-2: CMake/NDK Build Conflict Between llama.cpp and MNN

**Severity:** CRITICAL
**What goes wrong:** The existing `CMakeLists.txt` links against `libllama.so`, `libggml.so`, `libggml-base.so`, `libggml-cpu.so`, and `libmtmd.so`. Adding MNN requires linking against `libMNN.so` and `libllm.so` (MNN's LLM library — same name as llama.cpp's!). Symbol conflicts, duplicate definitions, and linker errors will occur.

**Why it happens:** Both llama.cpp and MNN ship a library named `libllm.so`. The current `CMakeLists.txt` uses `IMPORTED` shared libraries with hardcoded paths from `jniLibs/arm64-v8a/`. Simply adding MNN libraries to the same CMake config will cause naming collisions.

**Consequences:** Build failure at link time, or worse: runtime symbol resolution picking the wrong library, causing crashes during inference.

**Prevention:**
- **Complete migration approach:** Remove ALL llama.cpp references before adding MNN. Delete `jniLibs/arm64-v8a/libllama.so`, `libggml*.so`, `libmtmd.so`. Remove the entire `llamajni.cpp` and `LlamaBridge.kt`
- **New CMakeLists.txt:** Build MNN from source or reference prebuilt MNN AARs. Use MNN's official build script: `project/android/build_64.sh` with flags `-DMNN_BUILD_LLM=true -DMNN_LOW_MEMORY=true -DMNN_CPU_WEIGHT_DEQUANT_GEMM=true -DMNN_SUPPORT_TRANSFORMER_FUSE=true -DMNN_ARM82=true -DMNN_USE_LOGCAT=true`
- **Do NOT attempt a gradual migration.** The two engines are fundamentally incompatible at the native layer
- Use MNN's Kotlin API directly from `MnnLlmChat` reference app rather than writing a new JNI bridge

**Detection:** Build the project after removing llama.cpp. If it compiles clean, you're good. Watch for `UnsatisfiedLinkError` at runtime.

---

### PITFALL-3: mmap Weights Deadlock on Partial Load (Known MNN Bug)

**Severity:** CRITICAL
**What goes wrong:** MNN 3.5.0 release notes explicitly document a fix for "mmap weights deadlock — enhanced state validation to avoid deadlock from partially initialized mmap weights." This means earlier versions (including the one referenced in PROJECT.md as v3.5.0) may have this bug in certain edge cases during model loading.

**Why it happens:** If the model download is interrupted (network drop, user kills app), the weight file may be partially written. When MNN attempts to mmap this partially-written file, it can deadlock waiting for state validation that never completes.

**Consequences:** App freezes permanently on the loading screen. No crash — just a hang that requires force-killing the app.

**Prevention:**
- Use MNN **v3.5.0+** which includes the deadlock fix
- **Always validate model files before loading:** check file sizes match expected sizes from the model manifest
- Download to a **temporary file** first, then atomically rename to final path only after SHA-256 integrity check
- Never load a model file while a download is in progress — enforce a state machine: `IDLE → DOWNLOADING → VALIDATING → READY`
- Add a startup check: if model directory contains a `.downloading` marker file, delete the incomplete files

**Detection:** The deadlock manifests as the UI thread hanging after calling MNN's load function. Test by intentionally corrupting a weight file and attempting to load.

---

### PITFALL-4: 1.2GB Download Without Resume or Integrity Check

**Severity:** CRITICAL
**What goes wrong:** Downloading a 1.2GB model file from HuggingFace in a single HTTP request. Network drops on mobile are common (tunnel, elevator, handoff between WiFi/cellular). Without resumable downloads and integrity verification, the user may waste data repeatedly downloading partial files.

**Why it happens:** HuggingFace serves files via direct URLs. The naive approach uses OkHttp's standard download which doesn't handle partial content well for files this large. Android's `DownloadManager` doesn't support integrity verification.

**Consequences:** Wasted data (1.2GB per failed attempt), corrupted model files that cause MNN to crash or produce garbage output, terrible UX on metered connections.

**Prevention:**
- Use **HTTP Range requests** for resumable downloads: `OkHttpClient` with custom interceptor that tracks `Content-Range`, `ETag`, and supports `206 Partial Content`
- Download to a `.part` file, track progress in a companion `.meta` JSON file containing: total bytes, downloaded bytes, ETag, SHA-256 of completed chunks
- On resume: send `Range: bytes=<downloaded>-` header, verify ETag hasn't changed
- After complete download: verify **SHA-256** checksum of entire file against a value shipped in the app or fetched from HuggingFace API
- Show download progress with: percentage, speed (MB/s), estimated time remaining, downloaded/total MB
- Allow cancel and resume from Settings
- Only show the download prompt on WiFi by default — warn about data usage on cellular
- Store model in app's external files directory (`getExternalFilesDir(null)`) to avoid consuming internal storage quota

**Detection:** Test download interruption: start download, kill app at 30%, 60%, 90%, restart and verify resume works. Test on flaky network (network throttling in Android emulator).

---

## High Severity Pitfalls

### PITFALL-5: MNN LLM Blocking the Main Thread During Load

**Severity:** HIGH
**What goes wrong:** MNN model loading (even with mmap) takes 2-5 seconds on mid-range devices. The existing `LlmScanFragment` initializes on `onViewCreated` using a coroutine on `Dispatchers.Main`, then calls `llmVisionService.initialize()` which internally uses `Dispatchers.IO`. But if the MNN bridge's `load()` call isn't properly moved off the main thread, the UI will freeze.

**Why it happens:** The MNN reference app uses a C++ `llm->load()` call that does file I/O and mmap setup. If the Kotlin wrapper calls this synchronously (even from `Dispatchers.IO`), and the coroutine's `withContext` isn't used correctly, the main thread can still be blocked during the callback to native code if there's JNI overhead.

**Consequences:** ANR (Application Not Responding) after 5 seconds of frozen UI. Users will force-kill the app.

**Prevention:**
- All MNN native calls (load, generate, free) MUST be called from `Dispatchers.IO` using `withContext(Dispatchers.IO) { ... }`
- Show a **progress indicator** during model load — don't just show a spinner; show "Loading AI model..." with a progress animation
- Lazy-load the model: only load when the user actually navigates to the scan tab, not on app startup
- Pre-warm the model in the background after first successful download, but don't block any UI path
- The existing `LlmScanFragment` pattern of `scope.launch { updateStatus(...) }` is correct — replicate it

**Detection:** Enable `StrictMode.enableDefaults()` in debug builds. Run on a slow device. If you see `StrictMode policy violation: io` on the main thread, it's not properly dispatched.

---

### PITFALL-6: YOLO → LLM Batch Processing Memory Cascade

**Severity:** HIGH
**What goes wrong:** When the YOLO detector identifies N food items in a single camera frame, the naive approach crops each detection and sends N crops to the LLM sequentially. Each crop is a 640x640 bitmap (~1.5MB). If YOLO finds 10 items, that's 15MB of bitmaps plus the LLM context window accumulating N prompt+response pairs. With `contextSize: 1024` and multiple rounds, the KV cache grows unbounded.

**Why it happens:** The current architecture has no concept of batch detection → batch classification. `LlmVisionService.analyzeImage()` processes a single image. The new pipeline needs: YOLO detects → crop N regions → classify each via LLM → aggregate results. Without careful memory management, this cascade is lethal.

**Consequences:** OOM during multi-item classification, extremely slow UX (10 items × 5s inference = 50 seconds), inconsistent results as KV cache fills up.

**Prevention:**
- **Process YOLO crops sequentially:** Detect all items first, show bounding boxes, then classify one at a time
- **Free each crop bitmap immediately** after LLM classification: `bitmap.recycle()` in a `finally` block
- **Reset KV cache between items** — don't reuse conversation context across different food detections
- Use MNN's `reuse_kv: false` for food detection (each item is independent)
- Set `max_new_tokens` low for classification tasks (64-128 tokens is enough for food name + expiry)
- Limit simultaneous detections: cap at 5-8 items per scan, show "X more items detected — scan again" for the rest
- Show per-item progress: "Analyzing item 3 of 7..."

**Detection:** Profile memory during a scan with many items. Watch for monotonically increasing native heap that doesn't drop after each item.

---

### PITFALL-7: Model Version Confusion — Qwen3.5-2B-MNN vs Qwen3-2B-MNN

**Severity:** HIGH
**What goes wrong:** The PROJECT.md references "Qwen3.5-2B-MNN" but MNN's model hub and HuggingFace also have "Qwen3-2B-MNN" and "Qwen3.5-2B-MNN" (different models!). Using the wrong model files (e.g., Qwen3 model files with Qwen3.5 config, or mismatched `llm.mnn` and `llm.mnn.weight` versions) will produce garbage output or crash on load.

**Why it happens:** MNN model exports are tightly coupled — `llm.mnn` (architecture) + `llm.mnn.weight` (weights) + `llm_config.json` (metadata) + `config.json` (runtime config) must all be from the same export run. Mixing files from different exports or versions causes silent misalignment.

**Consequences:** Garbage classification output (model produces random tokens), crash on model load (shape mismatch), or subtle accuracy degradation that's hard to debug.

**Prevention:**
- Always download the **entire model directory** as a unit from HuggingFace — never mix files from different versions
- Include a `model_version` field in the download manifest and verify it matches what the app expects
- The MNN model config `llm_config.json` contains model metadata — verify `hidden_size`, `layer_nums`, `n_vocab` match expected values for Qwen3.5-2B
- Pin the exact HuggingFace commit hash or release tag for the model download URL
- Add a sanity check: run a known prompt ("What is 2+2?") after model load and verify the output contains "4" — if not, mark model as corrupted

**Detection:** After model load, run a test inference with a known prompt. If the output is incoherent, the model files are mismatched.

---

### PITFALL-8: CameraX Preview Frame Stale Reference After Capture

**Severity:** HIGH
**What goes wrong:** The current `LlmScanFragment` stores `latestBitmap` from the `ImageAnalysis` callback. When the user taps capture, it uses this cached bitmap. But CameraX's `STRATEGY_KEEP_ONLY_LATEST` means frames are dropped — the `latestBitmap` may be from several seconds ago, showing a different scene than what the user sees in the preview.

**Why it happens:** The camera preview (`Preview`) and image analysis (`ImageAnalysis`) are separate use cases running on different threads. The analysis callback provides frames asynchronously. By the time the user taps capture, the displayed preview frame may have moved on, but `latestBitmap` is stale.

**Consequences:** User captures what they see, but analysis runs on an older frame. Items in the captured frame may be different from what the user intended to scan. Very confusing UX.

**Prevention:**
- On capture button press, **take a still image** using `ImageCapture` use case instead of using the analysis frame
- OR: Use `takePicture()` on the `Preview` surface to get the exact frame the user sees
- The correct pattern: add `ImageCapture` use case alongside `Preview` and `ImageAnalysis`. On capture, call `imageCapture.takePicture()` to get a high-quality still
- For the new horizontal (landscape) frame: ensure the `ImageCapture` target rotation matches the landscape orientation
- Show a brief "capture flash" animation to indicate the exact moment of capture

**Detection:** Open camera, move the phone quickly, then tap capture. Compare the analyzed image with what was displayed. They should match.

---

## Medium Severity Pitfalls

### PITFALL-9: MNN config.json Precision/Backend Mismatch

**Severity:** MEDIUM
**What goes wrong:** MNN's `config.json` controls backend type (`cpu`, `opencl`), precision (`low` = fp16, `high` = fp32), memory strategy, and thread count. Using `precision: "low"` on a device without ARMv8.2 FP16 support causes fallback to software FP16 emulation, which is slower than fp32.

**Why it happens:** The `arm64-v8a` ABI filter in the existing build.gradle covers all ARM64 devices, but not all support FP16 instructions. Devices with ARMv8.0 (no FP16) will silently fall back to software emulation.

**Consequences:** 2-3x slower inference than expected on older ARM64 devices. Users on budget phones (common in food management app demographic) will see terrible performance.

**Prevention:**
- Detect ARMv8.2 support at runtime: check `android.os.Build.SUPPORTED_ABIS` and use `Build.SUPPORTED_64_BIT_ABIS` with a native check for `__ARM_FEATURE_FP16_VECTOR_ARITHMETIC`
- Default to `precision: "low"` (fp16) on ARMv8.2+ devices, `precision: "high"` on older devices
- Expose a "Performance Mode" setting in the app for users to override
- For the `backend_type`: default to `"cpu"` — OpenCL support is device-specific and MNN's own docs warn about first-run tuning latency

**Detection:** Test on an ARMv8.0 device (e.g., Snapdragon 625). Compare inference speed with fp16 vs fp32 config.

---

### PITFALL-10: Thread Count Starvation — YOLO and MNN Fighting for Cores

**Severity:** MEDIUM
**What goes wrong:** The existing `YoloDetector` uses `Interpreter.Options().apply { numThreads = 4 }`. MNN defaults to `thread_num: 4`. If both are loaded simultaneously (see PITFALL-1), they compete for 4-8 CPU cores, causing context switching overhead and worse performance than running either alone.

**Why it happens:** Neither TFLite nor MNN coordinate thread pools. Both allocate their own thread pools that compete for the same CPU cores. On a 4-core device, this means 8 threads fighting for 4 cores.

**Consequences:** 3-5x slowdown for both YOLO and MNN inference. The scan takes 15+ seconds instead of 3-5 seconds.

**Prevention:**
- **Never run both simultaneously** (see PITFALL-1)
- When YOLO is active: `YoloDetector` gets all 4 threads
- When MNN is active: MNN gets `min(available_cores - 1, 6)` threads (matching existing `LlamaBridge.defaultThreadCount()` logic)
- On 8-core devices: MNN can use 6 threads, leaving 2 for UI and system
- MNN config: `"thread_num": 4` is a safe default — don't go above 6 even on 8-core devices

**Detection:** Use `adb shell top -m 10 -s CPU` during inference. If you see >8 threads at high CPU, they're competing.

---

### PITFALL-11: Scan Fragment Lifecycle Leak — Camera Not Released

**Severity:** MEDIUM
**What goes wrong:** The existing `LlmScanFragment.onDestroyView()` correctly shuts down camera and coroutine scope. But if the fragment is replaced (e.g., navigating to results screen) without calling `onDestroyView` (e.g., using `replace` instead of `add` in the back stack), the camera stays active.

**Why it happens:** Fragment lifecycle is tricky with Navigation Component. If the scan fragment is added to the back stack and the user presses back, `onDestroyView` fires. But if the fragment is removed from the back stack entirely, or if the activity is recreated (rotation), the camera may not be properly released.

**Consequences:** Camera stays active in background, draining battery. Other apps can't access the camera. Memory leak from the `cameraExecutor` thread pool.

**Prevention:**
- Use `viewLifecycleOwner` (already done) for camera binding — this is correct
- Release camera in `onStop()` or `onPause()` in addition to `onDestroyView()`
- Use a `Lazy<ProcessCameraProvider>` that's scoped to the fragment's view lifecycle
- In the new scan UI with tabs: when switching tabs away from the scan tab, release the camera immediately — don't keep it running in the background
- CameraX `bindToLifecycle` with `viewLifecycleOwner` handles most cases, but explicitly call `cameraProvider.unbindAll()` when leaving the scan tab

**Detection:** Open the camera, navigate away, then try opening camera in another app. If it fails, the camera wasn't released.

---

### PITFALL-12: HuggingFace Download API Rate Limiting and LFS Redirects

**Severity:** MEDIUM
**What goes wrong:** HuggingFace serves large model files through git-lfs redirects. The initial request returns a 302 to an LFS storage URL (often on a CDN). Some LFS URLs have signed tokens that expire, and HuggingFace may rate-limit unauthenticated downloads. The model file download URL isn't stable.

**Why it happens:** HuggingFace's file download API works like this:
1. `GET https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN/resolve/main/llm.mnn.weight`
2. Response: 302 redirect to `https://cdn-lfs.huggingface.co/...` (with signed token)
3. The signed token may expire after hours

If you cache the redirect URL and try to resume later, the signed token may have expired.

**Consequences:** Download fails with 403 Forbidden on resume. User sees "Download failed" and can't get the model.

**Prevention:**
- Always start resume from the **original HuggingFace URL**, not the redirect target — let the redirect happen fresh each time
- Cache the `ETag` from the redirect response for validation, not the URL
- Add a `User-Agent` header identifying your app — HuggingFace is more lenient with identified traffic
- Consider using HuggingFace's official API endpoint: `https://huggingface.co/api/models/taobao-mnn/Qwen3.5-2B-MNN` which returns file metadata with CDN URLs
- Have a fallback mirror: ModelScope (`modelscope.cn/taobao-mnn/Qwen3.5-2B-MNN`) is the Chinese mirror and may be faster for users in Asia
- Implement exponential backoff on 429/503 responses

**Detection:** Test download with network throttling (300kbps in emulator). Kill the app mid-download and restart. Verify the resume URL works.

---

## Low Severity Pitfalls

### PITFALL-13: MNN Prompt Template Format Mismatch with Qwen3.5

**Severity:** LOW
**What goes wrong:** Qwen3.5 uses a specific chat template format (`<|im_start|>user\n...<|im_end|>\n<|im_start|>assistant\n`). If you use the wrong template (e.g., Qwen2 format or Llama format), the model will produce garbage or refuse to respond properly.

**Why it happens:** MNN's `llm_config.json` includes a `prompt_template` field that's used by `apply_chat_template`. But if you bypass this and construct prompts manually (like the current `LlmVisionService.buildPrompt()` does with a free-form prompt), the template wrapping won't be applied.

**Consequences:** The LLM produces confused responses, fails to follow JSON output format instructions, or enters an infinite loop generating template tokens.

**Prevention:**
- Always use MNN's `ChatMessage` API with proper role tags (`"user"`, `"assistant"`, `"system"`) rather than raw string prompts
- Set a system prompt that constrains output format: "You are a food identification assistant. Respond in JSON format only: {\"name\": \"...\", \"expiry\": \"...\", \"confidence\": \"...\"}"
- For food detection, keep prompts short — the model's context window is limited on mobile
- Test with the exact prompt format before integrating into the pipeline

**Detection:** Send a test prompt and verify the output format matches expectations. If you see template artifacts in the output (`<|im_start|>`), the template isn't being applied.

---

### PITFALL-14: Asset Model Copy on First Launch Blocking UI

**Severity:** LOW
**What goes wrong:** The existing `LlamaBridge.copyAssetIfMissing()` copies model files from assets to internal storage. For a 1.2GB model, this would take 10-30 seconds on first launch and block the UI thread if not properly dispatched.

**Why it happens:** With dynamic download (no bundling), this specific pitfall is avoided. But if any small models (like tokenizer or config files) are bundled in assets, the copy should be non-blocking.

**Consequences:** App appears frozen on first launch. Users may force-kill before copy completes.

**Prevention:**
- **Don't bundle large model files in assets** — the entire MNN model should be downloaded dynamically
- Only bundle tiny config files (like `config.json`, `llm_config.json`) in assets if needed for offline-first experience
- All file copies must be in `Dispatchers.IO` with progress indication
- Consider showing a welcome/onboarding screen during first-time model download rather than blocking any existing screen

---

### PITFALL-15: TFLite Model Not Released After Switching to MNN for YOLO

**Severity:** LOW
**What goes wrong:** The current `YoloDetector` uses TFLite and is loaded as a property of scan fragments. If the YOLO detection is kept using TFLite (which it should be — MNN is for LLM only), the TFLite interpreter lifecycle needs to be managed carefully alongside the MNN LLM lifecycle.

**Why it happens:** TFLite's `Interpreter` allocates native memory that isn't automatically freed by Kotlin's garbage collector. If multiple fragments create `YoloDetector` instances without properly closing them, native memory leaks.

**Consequences:** Gradual memory leak, eventually triggering OOM on repeated scan sessions.

**Prevention:**
- Make `YoloDetector` a singleton scoped to the application or scan session (via Hilt `@Singleton`)
- Always call `yoloDetector.close()` in `onDestroy()` or when switching away from scan
- If using Hilt: `@Inject lateinit var yoloDetector: YoloDetector` with `@Singleton` scope ensures one instance
- The existing `YoloDetector.isModelLoaded()` and `close()` methods are correct — use them

---

## Prevention Strategies

### Architecture: Model Lifecycle Manager

Create a centralized `ModelLifecycleManager` (injectable via Hilt) that:

```
States: IDLE → DOWNLOADING → LOADING_YOLO → YOLO_READY → LOADING_LLM → LLM_READY → INFERRING
Rules:
- Only one heavy model active at a time
- Download and load are on Dispatchers.IO
- State transitions emit Flow for UI observation
- Memory check before any model load
- Automatic cleanup on app backgrounding (onStop)
```

### Download: Resumable with Integrity

Implement `ModelDownloadManager` with:
- HTTP Range requests for resume
- `.part` + `.meta` file pattern
- SHA-256 verification post-download
- Progress reporting (bytes, percentage, speed)
- WiFi-only default with cellular override
- Cancel/pause/resume support

### Memory: Defense in Depth

1. Check `availMem > 2GB` before any model load
2. Use MNN `use_mmap: true` to avoid loading full weights into RAM
3. Use `memory: "low"` for runtime quantization
4. Use `kvcache_mmap: true` to spill KV cache to disk
5. `chunk: 128` to limit per-token allocation
6. Recycle all bitmaps immediately after use
7. Reset KV cache between independent inferences

### Testing: Device Matrix

Test on at minimum:
- **High-end:** Snapdragon 8 Gen 3 (8GB RAM) — target performance
- **Mid-range:** Snapdragon 7s Gen 2 (6GB RAM) — acceptable performance  
- **Budget:** Snapdragon 680 (4GB RAM) — graceful degradation
- Verify MNN loads and runs on all three tiers

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| MNN engine integration | PITFALL-2 (CMake conflict) | Complete llama.cpp removal before adding MNN |
| Model download | PITFALL-4 (no resume), PITFALL-12 (HF rate limits) | Resumable download + integrity check + mirror |
| YOLO+LLM pipeline | PITFALL-1 (OOM), PITFALL-6 (memory cascade) | Sequential processing, single-model lifecycle |
| Scan UI redesign | PITFALL-8 (stale frame), PITFALL-11 (camera leak) | Use ImageCapture for stills, release on tab switch |
| Model configuration | PITFALL-7 (version confusion), PITFALL-9 (precision mismatch) | Pin exact model version, detect ARM capabilities |

---

## Sources

- MNN GitHub: https://github.com/alibaba/MNN (v3.5.0, 2026-04-07) — HIGH confidence
- MNN LLM Docs: https://mnn-docs.readthedocs.io/en/latest/transformers/llm.html — HIGH confidence
- Qwen3.5-2B-MNN: https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN — HIGH confidence
- MNN Chat Android App: https://github.com/alibaba/MNN/tree/master/apps/Android/MnnLlmChat — HIGH confidence
- MNN 3.5.0 Release Notes: https://github.com/alibaba/MNN/releases/tag/3.5.0 — HIGH confidence (mmap deadlock fix confirmed)
- Existing codebase analysis: LlamaBridge.kt, YoloDetector.kt, LlmScanFragment.kt, CMakeLists.txt — HIGH confidence (direct read)
- Android Memory Management patterns — MEDIUM confidence (general platform knowledge)
- HuggingFace LFS download behavior — MEDIUM confidence (observed behavior, not officially documented)
