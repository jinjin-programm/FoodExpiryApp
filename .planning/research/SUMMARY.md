# Project Research Summary

**Project:** FoodExpiryApp v2.0 — AI Vision Engine Overhaul
**Domain:** Android on-device AI inference (mobile food scanning with LLM + object detection)
**Researched:** 2026-04-08
**Confidence:** HIGH

## Executive Summary

FoodExpiryApp v2.0 transforms the existing barcode/OCR scanning experience into an AI-powered multi-object food detection system. The core shift is replacing the llama.cpp (GGUF) inference engine with Alibaba's MNN v3.5.0, which delivers 8.6x faster prefill and 2.3x faster decode on mobile — enabling practical on-device LLM inference with Qwen3.5-2B-MNN (~2GB, 4-bit quantized). A YOLO11n object detection model (exported to MNN format) runs first to identify multiple food items in a single camera frame, then crops are sequentially classified by the LLM for structured food identification with expiry dates.

The recommended approach uses a **unified MNN inference engine** for both YOLO and LLM (avoiding dual runtime overhead), **dynamic model download from HuggingFace** (reducing APK by ~40MB since models aren't bundled), and **Android Bitmap-based preprocessing** (avoiding a 50MB OpenCV dependency). The existing MVVM Clean Architecture is preserved — new components slot into `inference/mnn/`, `inference/pipeline/`, and `data/remote/` layers. The critical architectural constraint is **sequential model loading**: YOLO and LLM must never run simultaneously to prevent OOM on devices with <6GB RAM.

The biggest risks are (1) native-layer conflicts during the llama.cpp→MNN migration (`libllm.so` naming collision), (2) reliable 1.2GB model download with resume on flaky mobile networks, and (3) memory management during batch YOLO→LLM processing. All three have clear prevention strategies documented in the pitfalls research. The migration must be atomic — complete llama.cpp removal before adding MNN — not gradual.

## Key Findings

### Recommended Stack

The v2.0 milestone adds three major subsystems while removing llama.cpp and potentially TFLite. The net APK size impact is **-35 to -45MB**.

**Core technologies:**
- **MNN v3.5.0** (AAR): Unified inference runtime for both YOLO and LLM — 2-5x faster than llama.cpp, mmap-based memory management, official Qwen3.5 support. Use as prebuilt AAR, NOT built from source.
- **Qwen3.5-2B-MNN** (HuggingFace): 4-bit quantized LLM (~2GB), pre-exported MNN format with tokenizer. No conversion needed. Downloaded at runtime.
- **YOLO11n** (Ultralytics → ONNX → MNN): Smallest YOLO11 variant (~2.6M params) for food object detection. Export pipeline: train → ONNX → MNNConvert.
- **OkHttp + WorkManager** (existing): Resumable HTTP Range downloads with background scheduling. Already in project at v4.12.0 / v2.9.0.
- **Android Bitmap API** (existing): Image preprocessing for YOLO (letterbox resize, crop, normalize). Avoids OpenCV's 50MB overhead. ~2-3x slower than OpenCV native but sufficient for YOLO11n's speed.

**Dependencies to remove:** `com.llama4j:gguf:0.1.1`, TFLite artifacts (4 libraries), 6 llama.cpp native `.so` files.

**Dependencies to add:** `MNN-release.aar` (prebuilt), 3 MNN native libraries (~15-25MB total).

### Expected Features

**Must have (table stakes):**
- Multi-object food detection — YOLO identifies multiple items in a single frame, existing `YoloDetector` already returns `List<DetectionResult>`
- Dynamic model download — 1.2GB LLM model downloaded from HuggingFace on first use, not bundled in APK
- Capture animation + loading screen — Shutter feedback, frozen frame, staged progress ("Detecting..." → "Analyzing 3 items...")
- Manual barcode capture — Replace auto-scan with tap-to-capture button, remove flash/close buttons

**Should have (competitive differentiators):**
- YOLO + LLM batch processing pipeline — Detect → crop → classify each item → structured JSON output with food name + expiry
- MNN speed advantage — 8.6x faster prefill, 2.3x faster decode vs llama.cpp; enables practical mobile LLM inference
- Horizontal (landscape) scan frame — Better framing for fridge shelves, matches natural multi-item layout

**Defer (v2+):**
- Advanced model management (multi-model switching)
- Custom model training UI
- Real-time streaming LLM output
- OpenCV preprocessing (Bitmap fallback sufficient until proven otherwise)

### Architecture Approach

The existing MVVM Clean Architecture is preserved. Three new subsystems integrate at specific layers:

**Major components:**
1. **MNN Inference Layer** (`inference/mnn/`) — Singleton `MnnLlmEngine` managing session lifecycle, config, prompt building, structured output parsing. Exposed via Hilt `@Singleton` to ViewModels through `IdentifyFoodUseCase`.
2. **YOLO+LLM Pipeline Coordinator** (`inference/pipeline/`) — Orchestrates the sequential detect→crop→classify→aggregate flow. Enforces mutual exclusion between YOLO and LLM. Emits `PipelineResult` sealed class (Loading | Processing | Complete | Error) as Flow.
3. **Dynamic Model Download Service** (`data/remote/`) — HTTP Range-based resumable downloads from HuggingFace, SHA-256 integrity verification, progress tracking via Flow, state machine (IDLE → DOWNLOADING → VALIDATING → READY).
4. **Scan UI Components** (`presentation/ui/scan/`) — Horizontal frame overlay, capture animation, inference progress view. CameraX modifications for landscape capture and manual ImageCapture.

**Key architectural decisions:**
- Sequential YOLO→LLM processing (not parallel) — prevents OOM on <6GB devices
- Complete llama.cpp removal (not gradual) — `libllm.so` naming conflict makes coexistence impossible
- MNN as prebuilt AAR (not source build) — avoids complex CMake in CI
- Model lifecycle manager (singleton) — enforces mutual exclusion, auto-cleanup on backgrounding

### Critical Pitfalls

15 pitfalls identified across 4 severity levels. The top 5 are:

1. **OOM from simultaneous YOLO + LLM** (CRITICAL) — Implement model lifecycle manager with mutual exclusion; unload YOLO before loading LLM; check `availMem > 2GB` before any model load.
2. **CMake/NDK `libllm.so` naming collision** (CRITICAL) — Both llama.cpp and MNN ship `libllm.so`. Complete llama.cpp removal before adding MNN; do NOT attempt gradual migration.
3. **mmap weights deadlock on partial download** (CRITICAL) — MNN v3.5.0 fixes this, but partial files from interrupted downloads can still hang. Download to `.part` file, rename atomically after SHA-256 verification.
4. **1.2GB download without resume** (CRITICAL) — Use HTTP Range requests, `.part` + `.meta` file pattern, always resume from original HuggingFace URL (not cached redirect). Add ModelScope mirror as fallback.
5. **Batch processing memory cascade** (HIGH) — Cap detections at 5-8 items per scan, recycle bitmaps immediately, reset KV cache between items, set `max_new_tokens` low (64-128).

## Implications for Roadmap

Based on research, the build has a strict dependency chain and several independent workstreams. Suggested phase structure:

### Phase 1: Foundation — MNN Engine + Dynamic Download
**Rationale:** Everything depends on MNN being integrated and models being downloadable. This is the critical path — no other phase can start without it. The llama.cpp removal must be atomic (PITFALL-2), so it should be done in one clean pass.
**Delivers:** MNN LLM inference working with Qwen3.5-2B-MNN, resumable model download from HuggingFace, structured JSON output parsing
**Addresses:** MNN inference engine (table stake), dynamic model download (table stake), structured JSON output (differentiator)
**Avoids:** PITFALL-2 (CMake conflict — atomic removal), PITFALL-3 (mmap deadlock — v3.5.0 + validation), PITFALL-4 (no resume — Range requests + integrity)

### Phase 2: Vision Pipeline — YOLO + LLM Integration
**Rationale:** Depends on Phase 1 (MNN engine available). This phase builds the core differentiating feature: multi-object food detection with batch LLM classification. Must address memory management carefully.
**Delivers:** YOLO multi-object detection, crop→classify pipeline, batch processing with progress tracking, multi-item results UI
**Addresses:** Multi-object food detection (table stake), YOLO+LLM batch pipeline (differentiator), horizontal scan frame (differentiator)
**Avoids:** PITFALL-1 (OOM — sequential loading), PITFALL-6 (memory cascade — cap items, recycle bitmaps), PITFALL-8 (stale frame — ImageCapture use case)
**Key research needed:** Test MNNConvert with YOLO11n ONNX output; verify MNN's NMS quality (may need custom Kotlin NMS)

### Phase 3: UX Polish — Scan Redesign + Capture Experience
**Rationale:** Independent of ML pipeline (can be built in parallel with Phase 2). Cleans up the scan experience: manual barcode capture, capture animation, inference progress screen, title bar removal.
**Delivers:** Manual barcode capture, capture animation + loading screen, inference progress view, title bar removal, camera lifecycle fixes
**Addresses:** Capture animation (table stake), manual barcode (table stake), loading screen (table stake)
**Avoids:** PITFALL-8 (stale frame), PITFALL-11 (camera leak — release on tab switch)
**Standard patterns:** All CameraX UI work is well-documented; no research phase needed

### Phase 4: Hardening — Performance + Device Compatibility
**Rationale:** Only after the full pipeline works on one device. This phase optimizes for device diversity and edge cases.
**Delivers:** ARM capability detection (FP16 vs FP32), device-tier testing (4GB/6GB/8GB), thread count optimization, model version pinning
**Addresses:** Cross-device compatibility, performance tuning
**Avoids:** PITFALL-9 (precision mismatch), PITFALL-10 (thread starvation), PITFALL-7 (model version confusion)

### Phase Ordering Rationale

- **Phase 1 first** because MNN integration is the foundation — YOLO pipeline (Phase 2) and scan UI (Phase 3) both depend on the inference engine being available
- **Phase 2 before Phase 3** because the pipeline architecture (sequential processing, model lifecycle) must be proven before polishing the UX around it — UX polish should wrap a working pipeline, not the reverse
- **Phase 3 can partially overlap** with Phase 2 since the scan UI cleanup (title bar removal, manual barcode) is independent of the ML pipeline
- **Phase 4 last** because device compatibility work is wasted if the core pipeline doesn't work yet

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1:** MNN AAR build process (exact `build_64.sh` output, AAR contents), MNN JNI bridge API study (`MnnLlmChat` Kotlin code for `response()` API), HuggingFace LFS download reliability on mobile
- **Phase 2:** MNNConvert with YOLO11n ONNX output — verify NMS behavior (HIGH priority, potential blocker). MNN's built-in NMS quality is unverified.
- **Phase 4:** OpenCV vs Bitmap benchmarking for preprocessing (LOW priority, Bitmap fallback exists)

Phases with standard patterns (skip research-phase):
- **Phase 3:** CameraX UI modifications, capture animations, manual barcode — all well-documented Android patterns

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | MNN v3.5.0 release notes verified on GitHub (2026-04-07), Qwen3.5-2B-MNN confirmed on HuggingFace, Android build instructions in official docs |
| Features | HIGH | Existing codebase directly inspected (YoloScanFragment, LlmScanFragment, VisionScanFragment), MNN performance benchmarks from official docs |
| Architecture | HIGH | Existing MVVM structure well-understood, MNN Chat reference app provides proven integration pattern, dependency chain is clear |
| Pitfalls | HIGH | 15 pitfalls identified from official MNN release notes (mmap deadlock fix), existing codebase analysis (CMake conflicts, stale frames), and established mobile ML patterns |

**Overall confidence: HIGH**

### Gaps to Address

- **MNN YOLO NMS quality:** MNN's built-in NMS is unverified with YOLO11n. May need custom Kotlin NMS implementation. Validate early in Phase 2.
- **MNN JNI bridge API:** The exact Kotlin API surface for `MnnLlmChat` needs hands-on study. The reference app exists but the `response()` API contract needs documentation.
- **HuggingFace LFS redirect token expiry:** Signed CDN URLs may expire between download sessions. Always resume from the original URL, not cached redirect — but this needs testing on real mobile networks.
- **OpenCV vs Bitmap decision:** Research recommends starting with Bitmap (simpler, no 50MB dependency). If profiling shows preprocessing is a bottleneck, add OpenCV. This is a deferred decision, not a blocker.

## Sources

### Primary (HIGH confidence)
- [MNN v3.5.0 Release Notes](https://github.com/alibaba/MNN/releases/tag/3.5.0) — mmap deadlock fix, Vulkan LLM support, TurboQuant
- [MNN LLM Documentation](https://mnn-docs.readthedocs.io/en/latest/transformers/llm.html) — config.json options, prompt templates, mobile settings
- [MNN Chat Android Reference App](https://github.com/alibaba/MNN/tree/master/apps/Android/MnnLlmChat) — proven Android integration pattern
- [taobao-mnn/Qwen3.5-2B-MNN on HuggingFace](https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN) — verified model, Apache-2.0
- [Ultralytics YOLO11](https://github.com/ultralytics/ultralytics) — YOLO11n export pipeline
- Existing codebase analysis: LlamaBridge.kt, YoloDetector.kt, LlmScanFragment.kt, CMakeLists.txt — direct code inspection

### Secondary (MEDIUM confidence)
- [HuggingFace Hub Download Guide](https://huggingface.co/docs/huggingface_hub/guides/download) — API patterns, LFS redirect behavior
- [OpenCV Android](https://opencv.org/android/) — SDK distribution, versioning inconsistencies
- [CameraX Developer Guide](https://developer.android.com/training/camerax) — ImageCapture API, ResolutionSelector
- Android Memory Management patterns — platform-level knowledge for OOM prevention

### Tertiary (LOW confidence)
- HuggingFace LFS signed token expiry — observed behavior, not officially documented
- MNN YOLO NMS quality — MNN docs mention object detection support but don't benchmark NMS specifically

---
*Research completed: 2026-04-08*
*Ready for roadmap: yes*
