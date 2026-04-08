# Architecture Research: MNN/YOLO/LLM Integration

**Project:** FoodExpiryApp v2.0 — AI Vision Engine Overhaul
**Researched:** 2026-04-08
**Scope:** How MNN inference, YOLO+LLM pipeline, dynamic model download integrate with existing MVVM Clean Architecture

---

## Existing Architecture (Unchanged)

```
presentation/  →  domain/  →  data/
(Fragments,     (UseCases,   (Repository,
 ViewModels)     Models)      Room, API)
```

- MVVM + Clean Architecture (3 layers)
- Hilt dependency injection
- Kotlin Coroutines + Flow for reactive data
- Room as single source of truth

---

## New Components

### 1. MNN Inference Layer (NEW)

**Location:** `app/src/main/java/com/example/foodexpiryapp/inference/mnn/`

```
inference/mnn/
  MnnLlmEngine.kt        — Singleton managing MNN LLM session lifecycle
  MnnLlmConfig.kt        — Model config (backend_type, thread_num, memory settings)
  MnnModelManager.kt     — Model loading/unloading, lifecycle enforcement
  MnnPromptBuilder.kt    — Build prompts for food identification
  StructuredOutputParser.kt — Parse JSON {"name", "name_zh"} from LLM response
```

**Integration with MVVM:**
- `MnnLlmEngine` is a domain service (interface in `domain/`, implementation in `inference/mnn/`)
- Exposed via Hilt `@Singleton` with `@Provides`
- ViewModels call it through a UseCase: `IdentifyFoodUseCase`

### 2. YOLO+LLM Pipeline Coordinator (NEW)

**Location:** `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/`

```
inference/pipeline/
  FoodDetectionPipeline.kt — Orchestrates YOLO → crop → LLM → results
  ImageCropper.kt          — Crop bounding box regions (Bitmap API, no OpenCV needed)
  BatchProcessor.kt        — Sequential processing with memory management
  PipelineResult.kt        — Sealed class: Loading | Processing(count) | Complete(items) | Error
```

**Data Flow:**
```
Camera Bitmap
  → YoloDetector.detect(bitmap)          — returns List<DetectionResult>
  → ImageCropper.crop(bitmap, detection)  — returns cropped Bitmap per item
  → BatchProcessor.process(croppedImages) — sequential: each → MnnLlmEngine.identify()
  → StructuredOutputParser.parse(response)— returns List<IdentifiedFood>
  → PipelineResult.Complete(items)        — consumed by ViewModel
```

**Critical:** Sequential processing, NOT parallel. Unload YOLO before loading LLM (PITFALL-1).

### 3. Dynamic Model Download Service (NEW)

**Location:** `app/src/main/java/com/example/foodexpiryapp/data/remote/`

```
data/remote/
  HuggingFaceDownloadService.kt — HTTP Range-based resumable downloads
  ModelDownloadManager.kt       — Download orchestration, progress tracking, integrity check
  ModelManifest.kt              — File list with SHA-256 hashes from HuggingFace repo

data/local/
  ModelStorageManager.kt        — File system management for downloaded models
```

**Integration with MVVM:**
- `ModelDownloadManager` is a data-layer service, injected via Hilt
- Exposes `Flow<DownloadProgress>` to ViewModel
- ViewModel shows progress UI, persists download state in Room

### 4. Scan UI Components (MODIFIED)

```
presentation/ui/scan/
  ScanFragment.kt              — Modified: remove auto-scan, add manual capture
  YoloScanFragment.kt          — Modified: horizontal frame, capture animation
  LlmScanFragment.kt           — Modified: MNN inference instead of llama.cpp
  VisionScanFragment.kt        — Modified: pipeline coordinator integration

presentation/ui/scan/components/
  HorizontalFrameOverlay.kt    — NEW: landscape crop guide overlay
  CaptureAnimationView.kt      — NEW: shutter flash + freeze frame animation
  InferenceProgressView.kt     — NEW: "Detecting..." → "Analyzing 3 items..." → "Done"
```

---

## Modified Components

| Component | Change | Risk |
|-----------|--------|------|
| `YoloDetector.kt` | Add unload/lifecycle method | LOW |
| `LlmScanFragment.kt` | Replace llama.cpp JNI calls with MNN engine | HIGH |
| `ScanFragment.kt` | Remove auto-scan, add capture button | LOW |
| `CMakeLists.txt` | Remove llama.cpp, add MNN references | HIGH |
| `build.gradle` | Add MNN AAR dependency, remove llama.cpp artifacts | MEDIUM |
| `MainActivity.kt` | Remove top title bar | LOW |

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│  CameraX Preview (horizontal frame overlay)              │
│  User taps capture → shutter animation                   │
└────────────────────┬────────────────────────────────────┘
                     │ Bitmap
                     ▼
┌─────────────────────────────────────────────────────────┐
│  FoodDetectionPipeline (Sequential)                     │
│                                                          │
│  1. YoloDetector.detect(bitmap)                         │
│     → List<DetectionResult> (bounding boxes)            │
│                                                          │
│  2. yoloDetector.unload()  ← FREE MEMORY               │
│                                                          │
│  3. FOR EACH detection:                                  │
│     ImageCropper.crop(bitmap, box) → cropped Bitmap     │
│     MnnLlmEngine.load()  (if not loaded)                │
│     MnnLlmEngine.identify(cropped) → JSON string        │
│     StructuredOutputParser.parse(json) → FoodItem       │
│     recycle(cropped)                                    │
│                                                          │
│  4. MnnLlmEngine.unload()  ← FREE MEMORY               │
└────────────────────┬────────────────────────────────────┘
                     │ List<IdentifiedFood>
                     ▼
┌─────────────────────────────────────────────────────────┐
│  ViewModel → Confirmation UI → Save to Room             │
└─────────────────────────────────────────────────────────┘
```

---

## Build Order (Dependency-Based)

1. **Full project backup** — Safety net before any changes
2. **Remove llama.cpp** — Clean cutover (PITFALL-2). Delete native libs, JNI code, update CMake
3. **MNN AAR integration** — Add prebuilt MNN libraries, verify build
4. **Dynamic model download** — HuggingFace download service with resume + integrity
5. **MNN LLM engine wrapper** — Kotlin wrapper around MNN, test with Qwen3.5-2B-MNN
6. **Scan UI cleanup** — Remove title bar, horizontal frame, capture animation (independent of ML)
7. **Barcode scan redesign** — Manual capture, remove flash/close (independent of ML)
8. **YOLO+LLM pipeline** — Detection → crop → identify → structured output
9. **Integration & testing** — End-to-end flow, memory profiling, device tier testing

---

## Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| Sequential YOLO→LLM (not parallel) | Prevent OOM on <6GB devices |
| Complete llama.cpp removal (not gradual) | libllm.so naming conflict |
| MNN as prebuilt AAR (not source build) | Avoid CMake complexity, use official build |
| Android Bitmap for cropping (not OpenCV) | Avoid ~50MB dependency, Bitmap API sufficient |
| Model lifecycle manager (singleton) | Enforce mutual exclusion, prevent OOM |
