# Requirements: FoodExpiryApp

**Defined:** 2026-04-08
**Core Value:** Let a new user add their first food item within 30 seconds and reduce the mental burden of food management.

## v2.0 Requirements

Requirements for v2.0 AI Vision Engine Overhaul. Each maps to roadmap phases.

### Backup & Safety

- [ ] **SAFE-01**: Full project folder backup created before any v2.0 code changes
- [ ] **SAFE-02**: Git tag `v1.1.0-backup` created pointing to current stable state

### MNN Inference Engine

- [ ] **MNN-01**: MNN v3.5.0 AAR integrated into project build (replaces llama.cpp)
- [ ] **MNN-02**: llama.cpp completely removed (native libs, JNI code, CMake references)
- [ ] **MNN-03**: Qwen3.5-2B-MNN model loads and runs inference successfully via MNN engine
- [ ] **MNN-04**: LLM returns structured JSON output for food identification (`{"name", "name_zh"}`)
- [ ] **MNN-05**: Model lifecycle manager enforces mutual exclusion (only one heavy model loaded at a time)
- [ ] **MNN-06**: MNN engine wrapper exposed as Hilt singleton with clean Kotlin API

### Dynamic Model Download

- [ ] **DL-01**: Qwen3.5-2B-MNN downloads from HuggingFace (`taobao-mnn/Qwen3.5-2B-MNN`) on first use
- [ ] **DL-02**: Download supports HTTP Range-based resume after interruption
- [ ] **DL-03**: Downloaded model files verified with SHA-256 checksum before use
- [ ] **DL-04**: Partial downloads use `.part` file pattern, renamed atomically after verification
- [ ] **DL-05**: Download progress shown in UI with percentage and estimated time remaining
- [ ] **DL-06**: WiFi-only prompt before starting large download (~1.2GB)
- [ ] **DL-07**: Download state persisted across app restarts (can resume after kill)

### YOLO+LLM Food Detection

- [ ] **YOLO-01**: YOLO+LLM batch pipeline: detect → crop → classify → aggregate results
- [ ] **YOLO-02**: New YOLO scan tab added alongside existing barcode and photo scan tabs
- [ ] **YOLO-03**: YOLO detects multiple food items and draws bounding boxes on camera preview
- [ ] **YOLO-04**: Detected regions cropped and sent sequentially to local LLM for identification
- [ ] **YOLO-05**: LLM returns structured food data (name, name_zh) for each cropped region
- [ ] **YOLO-06**: Detection results displayed as a list for user confirmation before saving
- [ ] **YOLO-07**: Maximum 5-8 items processed per scan to prevent memory issues
- [ ] **YOLO-08**: Bitmaps recycled immediately after each classification step

### Scan UI Overhaul

- [ ] **UI-01**: Top "FoodExpiryApp" title bar removed from all pages, only back button remains
- [ ] **UI-02**: Camera scan frame changed to horizontal (landscape) rectangle orientation
- [ ] **UI-03**: Capture animation plays after pressing capture button (shutter flash + frame freeze)
- [ ] **UI-04**: Loading screen shows AI inference progress ("Detecting objects..." → "Analyzing N items..." → "Done")
- [ ] **UI-05**: Barcode scan changed from auto-scan to manual capture (tap-to-capture button)
- [ ] **UI-06**: Flash button removed from barcode scan screen
- [ ] **UI-07**: Close button replaced with back button on scan screens
- [ ] **UI-08**: Capture button placed at bottom center of scan screen (recipe scan style)

## Future Requirements

Deferred to future release. Not in current roadmap.

### Advanced AI Features
- **AI-01**: Real-time streaming LLM output during inference
- **AI-02**: Custom model training UI for food categories
- **AI-03**: Multi-model switching (different LLM sizes for different devices)
- **AI-04**: OpenCV preprocessing optimization (if Bitmap API proves too slow)

### Enhanced Download
- **DL-08**: ModelScope mirror as fallback download source
- **DL-09**: Background download via WorkManager (download while app is closed)

## Out of Scope

| Feature | Reason |
|---------|--------|
| CLIP classifier | Overlaps with LLM capability, adds unnecessary complexity |
| GGUF model support | Replaced entirely by MNN inference |
| Gradual llama.cpp → MNN migration | libllm.so naming collision makes coexistence impossible |
| Parallel YOLO+LLM processing | OOM risk on <6GB devices, sequential is safer |
| OpenCV dependency | Android Bitmap API sufficient for YOLO preprocessing, saves ~50MB |
| Full app redesign | Visual identity preserved, UX optimization only |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SAFE-01 | Phase 4 | Pending |
| SAFE-02 | Phase 4 | Pending |
| MNN-01 | Phase 4 | Pending |
| MNN-02 | Phase 4 | Pending |
| MNN-03 | Phase 5 | Pending |
| MNN-04 | Phase 5 | Pending |
| MNN-05 | Phase 5 | Pending |
| MNN-06 | Phase 5 | Pending |
| DL-01 | Phase 5 | Pending |
| DL-02 | Phase 5 | Pending |
| DL-03 | Phase 5 | Pending |
| DL-04 | Phase 5 | Pending |
| DL-05 | Phase 5 | Pending |
| DL-06 | Phase 5 | Pending |
| DL-07 | Phase 5 | Pending |
| YOLO-01 | Phase 6 | Pending |
| YOLO-02 | Phase 6 | Pending |
| YOLO-03 | Phase 6 | Pending |
| YOLO-04 | Phase 6 | Pending |
| YOLO-05 | Phase 6 | Pending |
| YOLO-06 | Phase 6 | Pending |
| YOLO-07 | Phase 6 | Pending |
| YOLO-08 | Phase 6 | Pending |
| UI-01 | Phase 7 | Pending |
| UI-02 | Phase 7 | Pending |
| UI-03 | Phase 7 | Pending |
| UI-04 | Phase 7 | Pending |
| UI-05 | Phase 7 | Pending |
| UI-06 | Phase 7 | Pending |
| UI-07 | Phase 7 | Pending |
| UI-08 | Phase 7 | Pending |

**Coverage:**
- v2.0 requirements: 27 total
- Mapped to phases: 27
- Unmapped: 0

---
*Requirements defined: 2026-04-08*
*Last updated: 2026-04-08 after roadmap creation (phase mappings updated to Phases 4-7)*
