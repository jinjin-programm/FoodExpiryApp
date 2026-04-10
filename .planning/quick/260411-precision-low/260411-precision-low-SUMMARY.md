# Quick Task 260411-model-perf: Model Performance & Camera UX Optimization

**Date:** 2026-04-11
**Status:** Completed
**Backup:** `backup/pre-0.8b-migration` branch on origin

## Summary

Optimized on-device VLM inference through multiple strategies: tested smaller model (rejected), made precision configurable (retained), and improved camera UX by stopping camera during inference to free ~30MB RAM and reduce GC pressure. Added dedicated Retake and Cancel buttons for better user flow.

---

## Experiment Log

### Baseline (Qwen3.5-2B-MNN, original config)

| Setting | Value |
|---------|-------|
| Model | `taobao-mnn/Qwen3.5-2B-MNN` (~1.7GB) |
| precision | `"high"` (hardcoded in C++) |
| threadNum | 8 |
| maxSide | 1024 |
| JPEG quality | 90 |
| maxNewTokens | 512 |
| **Inference time** | **~28.6s** |
| **Result** | "watermelon" — Correct |

Device: Snapdragon 855 (SM8150), `memory_mode=low`

---

### Experiment 1: Qwen3.5-0.8B-MNN + precision=low + maxSide=420 + threads=4

| Setting | Value |
|---------|-------|
| Model | `taobao-mnn/Qwen3.5-0.8B-MNN` (~548MB) |
| precision | `"low"` (INT8) |
| threadNum | 4 |
| maxSide | 420 |
| JPEG quality | 75 |
| maxNewTokens | 32 |
| **Inference time** | **~11.6s** |
| **Result** | `"44444444444444444444444444444444"` — **GARBAGE (repetition loop)** |

**Verdict: FAILED.** Triple quantization (4-bit model + INT8 precision + low memory mode) destroyed output quality.

**Root cause analysis:**
- `precision=low` on top of already-4-bit quantized model = over-quantization
- `maxSide=420` reduced image to 315x420 = too few visual details for weak model
- `JPEG quality=75` further degraded visual input
- `maxNewTokens=32` gave no room for model to recover from bad token generation

---

### Experiment 2: Qwen3.5-0.8B-MNN + precision=high + maxSide=1024 + threads=8

| Setting | Value |
|---------|-------|
| Model | `taobao-mnn/Qwen3.5-0.8B-MNN` (~548MB) |
| precision | `"high"` |
| threadNum | 8 |
| maxSide | 1024 |
| JPEG quality | 90 |
| maxNewTokens | 32 |
| **Inference time** | **~12-15s (estimated)** |
| **Result** | **Still poor — model too weak for visual classification** |

**Verdict: FAILED.** Qwen3.5-0.8B is fundamentally insufficient for VLM food classification.

---

### Experiment 3: Revert to Qwen3.5-2B-MNN + Camera UX Optimization

| Setting | Value |
|---------|-------|
| Model | `taobao-mnn/Qwen3.5-2B-MNN` (~1.7GB) |
| precision | `"high"` (now configurable, not hardcoded) |
| threadNum | 8 |
| maxSide | 1024 |
| JPEG quality | 90 |
| maxNewTokens | 512 |
| Camera during inference | **Stopped** (was running) |
| **Inference time** | **~26-27s (est., 1-3s faster)** |
| **Result** | Correct food identification |

**Verdict: ACCEPTED.** Camera stop frees ~30MB RAM, eliminates GC pressure from camera frames.

---

## What Was Retained (Improvements Kept)

### 1. Configurable Precision Parameter

**Before:** `precision` hardcoded to `"high"` in `mnn_llm_bridge.cpp:59`
**After:** `precision` passed dynamically from `MnnLlmConfig` through JNI to C++

Files changed:
- `MnnLlmConfig.kt` — added `precision: String = "high"` field
- `MnnLlmNative.kt` — added `precision` param to `nativeCreateLlm()` JNI signature
- `mnn_llm_bridge.cpp` — accepts `jstring precision`, uses dynamically in config JSON
- `MnnLlmEngine.kt` — passes `config.precision` to native call

### 2. Camera Stop During Inference

**Before:** Camera keeps running during inference (~30MB RAM, GC every ~1.3s)
**After:** Camera stops when capture is tapped, restarts when result dismissed or cancelled

Files changed:
- `VisionScanFragment.kt` — `stopCamera()`, `restartCamera()`, `showBlurredBackground()`, `hideBlurredBackground()`

### 3. Improved UX: Retake + Cancel Buttons

| Button | Location | When Visible | Action |
|--------|----------|-------------|--------|
| `btn_retake` | Result card (outlined) | After inference complete | Dismiss result, restart camera |
| `btn_cancel_bottom` | Bottom (red solid) | During inference | Stop inference, restart camera |
| `btnCancelProgress` | Center overlay | Hidden (replaced by bottom) | — |

Files changed:
- `fragment_vision_scan.xml` — added `btn_retake`, `btn_cancel_bottom`, hidden `btnCancelProgress`
- `VisionScanFragment.kt` — click handlers, show/hide logic (`showCancelButton()`, `hideCancelButton()`)

### 4. Blurred Photo Background During Inference

Instead of black screen or white screen when camera stops, a blurred version of the captured photo is displayed as background behind the progress overlay.

---

## Settings Impact Analysis

| Setting | Affects Speed | Affects Intelligence | Notes |
|---------|:---:|:---:|-------|
| Model size (0.8B vs 2B) | Yes | **HIGH** | 0.8B too weak for VLM |
| Camera during inference | Yes | None | Camera off = ~30MB RAM freed, less GC |
| `precision` (low vs high) | Yes | **MEDIUM-HIGH** | `"low"` on 4-bit model = over-quantization |
| `maxSide` (420 vs 1024) | Yes | **MEDIUM** | Smaller = fewer vision tokens = less detail |
| JPEG quality (75 vs 90) | Minor | **MEDIUM** | 75 removes fine textures, hurts weak models |
| `maxNewTokens` (32 vs 512) | Minor | **MEDIUM** | Too low caps good output + amplifies garbage |
| `threadNum` (4 vs 8) | Yes | None | Only speed, not quality |

---

## Recommendations for Future Optimization

### Viable (can try again with 2B model)

| Option | Est. Speedup | Risk | Effort |
|--------|-------------|------|--------|
| `precision: "low"` on 2B only | ~20-30% | Medium — may degrade | Trivial (config change) |
| Reduce `maxSide` to 640 on 2B | ~15-20% | Low — 2B is stronger | Trivial |
| Thread pinning to big cores | ~10-20% | Low | Medium (C++ sched_setaffinity) |
| `"async": true` in MNN config | ~10-15% | Unknown | Trivial (config change) |

### Not Viable

| Option | Why |
|--------|-----|
| Qwen3.5-0.8B-MNN | Model too weak for visual food classification |
| `precision: "low"` on 0.8B | Over-quantization causes repetition loops |
| `maxSide=420` on 0.8B | Insufficient visual detail for weak model |

---

## Models Evaluated

| Model | Size | Vision | Food Classification | Speed (SD855) | Status |
|-------|------|--------|-------------------|---------------|--------|
| [Qwen3.5-2B-MNN](https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN) | ~1.7GB | Yes | Good | ~28.6s | **Active** |
| [Qwen3.5-0.8B-MNN](https://huggingface.co/taobao-mnn/Qwen3.5-0.8B-MNN) | ~548MB | Yes | Poor | ~11.6s | **Rejected** |

---

## Files Modified in This Task

| File | Change | Type |
|------|--------|------|
| `HuggingFaceDownloadService.kt` | `HF_REPO` back to `taobao-mnn/Qwen3.5-2B-MNN` | Revert |
| `ModelStorageManager.kt` | `REQUIRED_MODEL_FILES` without `llm.mnn.json` | Revert |
| `ModelManifest.kt` | `modelId` = `taobao-mnn/Qwen3.5-2B-MNN` | Revert |
| `ModelDownloadManager.kt` | `DEFAULT_MANIFEST` with 2B sizes | Revert |
| `MnnLlmConfig.kt` | Added `precision: String = "high"` field | **Improvement** |
| `MnnLlmNative.kt` | Added `precision` param to JNI signature | **Improvement** |
| `mnn_llm_bridge.cpp` | Dynamic precision param + ReleaseStringUTFChars | **Improvement** |
| `MnnLlmEngine.kt` | Passes `config.precision` to native, maxSide=1024, quality=90 | **Improvement** |
| `VisionScanFragment.kt` | Camera stop/restart, blurred bg, retake/cancel buttons | **Feature** |
| `fragment_vision_scan.xml` | `btn_retake`, `btn_cancel_bottom`, hidden `btnCancelProgress` | **Feature** |

## Update Log

| Date | Action |
|------|--------|
| 2026-04-11 | Initial plan: lower precision for faster inference |
| 2026-04-11 | Expanded to full 0.8B model migration |
| 2026-04-11 | Exp 1: 0.8B + precision=low → garbage output (repetition loop) |
| 2026-04-11 | Exp 2: 0.8B + precision=high + maxSide=1024 → still too weak |
| 2026-04-11 | Reverted to 2B model, retained configurable precision |
| 2026-04-11 | Camera stop during inference (free ~30MB RAM, reduce GC) |
| 2026-04-11 | Blurred photo background during inference |
| 2026-04-11 | Added Retake button (outlined, in result card) |
| 2026-04-11 | Added Cancel button (bottom, red solid, replaces capture during inference) |
| 2026-04-11 | Updated STATE.md with all session records |
