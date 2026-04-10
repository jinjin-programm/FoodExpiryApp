# Phase 8: YOLO Detection Hardening - Context

**Gathered:** 2026-04-11
**Status:** Ready for planning

<domain>
## Phase Boundary

YOLO scan works end-to-end with real detections, bounding boxes rendered, and all code review warnings addressed. This closes audit gaps from prior phases (YOLO-01, YOLO-03, INT-01, INT-02, INT-03, FLOW-01, WR-01–WR-06, IN-01, IN-04) and upgrades the YOLO pipeline from stub code to working inference.

</domain>

<decisions>
## Implementation Decisions

### YOLO Model Source
- **D-01:** Use the food-specific YOLO model from the YOLO_CLIP_targetDetection reference repo (github.com/tishuo-wang/YOLO_CLIP_targetDetection). Already food-class labeled, proven for food detection. Convert to MNN format.
- **D-02:** Evaluate YOLOv11 (YOLO26) as an alternative if the reference model is difficult to convert to MNN. User expressed interest in using the latest YOLO version if feasible.
- **D-03:** Model bundled in APK assets as `yolo_food.mnn` (~5-20MB). No dynamic download needed.

### MNN Native Bridge
- **D-04:** Custom JNI bridge pattern (same as existing MnnLlmNative). New `MnnYoloNative` class with native methods for loading YOLO model, running detection, and destroying session. Full control over input/output tensor handling.
- **D-05:** Keep existing MnnYoloPostprocessor output format (6+ values per row: x1,y1,x2,y2,conf,class_id). Researcher must ensure MNN conversion produces this exact layout.

### Bounding Box Rendering (Two-Phase Visual)
- **D-06:** Wire `DetectionOverlayView.setDetections()` directly in `YoloScanFragment`. Show bounding boxes immediately after YOLO detection completes, BEFORE LLM classification starts.
- **D-07:** Two-phase visual state for bounding boxes:
  - **DETECTED (pending):** White/light gray border (indicates "detected, awaiting identification")
  - **CLASSIFIED (complete):** Green solid border + LLM-returned food name label
- **D-08:** No dashed animation — simplified to color changes only. Clean and minimal.
- **D-09:** Use the existing `DetectionResult.status` field (PENDING/CLASSIFIED/FAILED) to drive visual state in DetectionOverlayView. Update individual detection status as LLM completes each item.

### Code Hardening & Safety (All Success Criteria)
- **D-10:** Fix bitmap double-recycle in `DetectionPipeline:132` — `detection.cropBitmap?.recycle()` may recycle a bitmap still referenced by the DetectionResult. Null out the reference after recycling or use a defensive copy.
- **D-11:** Add defensive bitmap copy in `YoloScanFragment.captureAndDetect()` — `latestBitmap` is used directly but the camera frame callback can overwrite/recycle it during inference. Copy before passing to pipeline.
- **D-12:** Fix `ConfirmationViewModel.saveAll()` silent error swallowing (line 104 empty catch). Report errors via `_saveResult` StateFlow so UI can show retry/dismiss options.
- **D-13:** Extract all 6 hardcoded `Color.parseColor()` calls in `DetectionResultAdapter` to `colors.xml` resource references. Aligns with project visual identity convention (Color Semantics: Red=high risk, Orange=attention, Green=safe).
- **D-14:** Verify `cancelDetection()` propagates cancellation to running pipeline coroutine — ensure `isActive` check in `DetectionPipeline.detectAndClassify()` loop is properly wired through ViewModel job cancellation.

### End-to-End Testing Strategy
- **D-15:** Three-phase testing approach:
  1. **Manual device testing first** — plug in phone, run YOLO inference, capture real Logcat output
  2. **Agent fixes for compilation/linking errors** — targeted debugging when real hardware reveals issues
  3. **Unit tests for core algorithms** — MnnYoloPostprocessor only (parseDetections, applyNms, cropDetection, mapLabelToCategory)
- **D-16:** Test on two Android devices (user has two available). Verify no hardware-specific issues.
- **D-17:** Unit test scope: MnnYoloPostprocessor pure functions only — no mocked engines, no native dependencies. Prevents math errors in coordinate normalization, NMS, and crop validation.

### Agent's Discretion
- Exact YOLO model variant selection (reference model vs YOLOv11) based on MNN conversion feasibility
- JNI bridge implementation details (native method signatures, tensor format handling)
- Exact colors for bounding box states (which shade of white/gray for DETECTED, which green for CLASSIFIED)
- DetectionOverlayView redraw strategy (full invalidate vs partial update on status change)
- Bitmap lifecycle management approach (null-after-recycle vs defensive copy vs weak reference)
- Test file structure and naming conventions

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### YOLO Model & Conversion
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt` — Current YOLO engine (stub detect() at line 120, loadModel/unloadModel lifecycle)
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloPostprocessor.kt` — Output format (6+ values per row), NMS, coordinate normalization, cropDetection, label-to-category mapping
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloConfig.kt` — YOLO config (inputSize, iouThreshold, maxDetections, modelAssetPath)
- https://github.com/tishuo-wang/YOLO_CLIP_targetDetection — Reference food YOLO model source

### MNN Integration (from Phase 5)
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmNative.kt` — JNI bridge pattern for MNN inference (reference for new MnnYoloNative)
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmEngine.kt` — Load/run/unload pattern to follow
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/ModelLifecycleManager.kt` — Mutual exclusion (acquire/release), needs YOLO type already registered

### Files to Fix
- `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt` — Bitmap double-recycle at line 132
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt` — Missing defensive copy in captureAndDetect(), DetectionOverlayView not wired
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/DetectionOverlayView.kt` — Needs two-phase visual state (white→green)
- `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt` — Silent error catch at line 104
- `app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt` — 6 hardcoded colors to extract
- `app/src/main/res/values/colors.xml` — Target for extracted color resources

### Architecture References
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture layers, data flow patterns
- `.planning/codebase/STRUCTURE.md` — Directory layout, naming conventions
- `.planning/codebase/CONVENTIONS.md` — Coding patterns, Hilt usage, coroutine patterns

### Requirements
- `.planning/REQUIREMENTS.md` §YOLO+LLM Food Detection (YOLO-01, YOLO-03) — Batch pipeline, bounding boxes on camera preview

### Prior Phase Context
- `.planning/phases/05-engine/05-CONTEXT.md` — Phase 5 decisions (UseCase/Repository pattern, MNN engine patterns)
- `.planning/phases/06-detection/06-CONTEXT.md` — Phase 6 decisions (YOLO model bundled in APK, MnnYoloPostprocessor format, DetectionOverlayView, batch flow, temp Room table)
- `.planning/phases/07-scan-ui/07-CONTEXT.md` — Phase 7 decisions (full-bleed camera, DetectionOverlayView separate from static frames, flash animation)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MnnYoloEngine`: Complete skeleton with load/detect/unload lifecycle — just needs real inference replacing the stub
- `MnnYoloPostprocessor`: Fully implemented pure functions (parseDetections, applyNms, cropDetection, mapLabelToCategory) — already handles the expected output format
- `MnnYoloConfig`: Config class with inputSize (640), iouThreshold, maxDetections (8), modelAssetPath
- `DetectionOverlayView`: Complete custom view with setDetections(), clearDetections(), bounding box drawing, label rendering — just needs color parameterization
- `ModelLifecycleManager`: Mutual exclusion with acquire/release — YOLO type already registered
- `DetectionPipeline`: Full orchestrator (YOLO detect → crop → LLM classify) — works once engines produce real results

### Established Patterns
- MnnLlmNative JNI bridge pattern (nativeCreateLlm, nativeRunInference, nativeDestroyLlm) — replicate for YOLO
- MnnLlmEngine load/run/unload on Dispatchers.IO — same pattern for YOLO engine
- ModelLifecycleManager acquire/release mutual exclusion — already supports YOLO ModelType
- HiltViewModel with StateFlow for reactive UI state
- FragmentResultListener for cross-fragment communication

### Integration Points
- `YoloScanFragment.captureAndDetect()` — entry point, currently starts pipeline with bitmap
- `YoloScanFragment` layout — `DetectionOverlayView` exists in XML but is never used in Kotlin code
- `MnnYoloEngine.detect()` line 120 — stub `floatArrayOf()` needs replacing with real JNI call
- `MnnYoloEngine.loadModel()` line 75-76 — TODO stub for JNI initialization
- `DetectionPipeline` line 132 — bitmap recycle location (fix double-recycle)
- `ConfirmationViewModel.saveAll()` line 104 — empty catch block (fix error reporting)

</code_context>

<specifics>
## Specific Ideas

- Two-phase bounding box rendering: white border (DETECTED) → green border + food name (CLASSIFIED). Simplified color-only, no dashed animation.
- Food-specific YOLO model from reference repo, converted to MNN format, bundled as `yolo_food.mnn` in assets.
- Custom JNI bridge for YOLO (new MnnYoloNative class following MnnLlmNative pattern).
- Three-phase testing: manual device → agent fixes → unit tests for MnnYoloPostprocessor.
- Test on two Android devices to catch hardware-specific issues.
- All 5 code hardening issues from success criteria must be fixed (bitmap safety, error reporting, color extraction, cancel propagation).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 08-yolo-hardening*
*Context gathered: 2026-04-11*
