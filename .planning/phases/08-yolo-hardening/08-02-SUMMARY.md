---
phase: 08-yolo-hardening
plan: 02
subsystem: ui, pipeline, safety
tags: [overlay, bounding-box, bitmap, detection, mvvm, stateflow, colors]

# Dependency graph
requires:
  - phase: 06-detection
    provides: DetectionOverlayView, DetectionPipeline, YoloScanFragment, MnnYoloPostprocessor
  - phase: 07-scan-ui
    provides: YoloScanFragment camera layout, fragment_yolo_scan.xml
provides:
  - Two-phase bounding box rendering (PENDING → CLASSIFIED → FAILED color states)
  - PipelineState.Detected for intermediate overlay updates after YOLO
  - Defensive bitmap copy preventing camera callback race condition
  - Bitmap double-recycle fix in DetectionPipeline
  - Error reporting via StateFlow in ConfirmationViewModel
  - Extracted color resources for detection UI components
affects: [08-yolo-hardening, 09-verification]

# Tech tracking
tech-stack:
  added: []
  patterns: [status-driven-paint-selection, pipeline-detected-intermediate-state, defensive-bitmap-copy]

key-files:
  created: []
  modified:
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/DetectionOverlayView.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/YoloScanViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt
    - app/src/main/java/com/example/foodexpiryapp/domain/model/PipelineState.kt
    - app/src/main/java/com/example/foodexpiryapp/domain/usecase/SaveDetectedFoodsUseCase.kt
    - app/src/main/res/values/colors.xml

key-decisions:
  - "Added PipelineState.Detected intermediate state to expose raw YOLO detections before LLM classification starts"
  - "Status-driven paint selection in DetectionOverlayView using when-expression for PENDING/CLASSIFIED/FAILED"
  - "Null-out cropBitmap on result after recycling to prevent stale reference (D-10)"
  - "Added error field to SaveResult data class for error propagation via StateFlow"

patterns-established:
  - "Status-driven rendering: when-expression selects paint based on DetectionStatus enum"
  - "Pipeline intermediate state: PipelineState.Detected fires between Detecting and Classifying"
  - "Defensive bitmap copy: always copy latestBitmap before passing to ViewModel for async processing"

requirements-completed: [YOLO-03]

# Metrics
duration: 10min
completed: 2026-04-10
---

# Phase 8 Plan 2: Overlay Wiring + Code Hardening Summary

**Two-phase bounding box overlay with status-driven paint selection (PENDING=blue-gray → CLASSIFIED=green → FAILED=red), PipelineState.Detected intermediate state, defensive bitmap copy, bitmap double-recycle fix, error reporting in ConfirmationViewModel, and extracted color resources**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-04-10T23:14:00Z
- **Completed:** 2026-04-10T23:24:33Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments
- DetectionOverlayView renders bounding boxes with status-based colors: PENDING (blue-gray), CLASSIFIED (green), FAILED (red)
- Overlay wired to YoloScanFragment — shows bounding boxes immediately after YOLO detection, before LLM classification
- Bitmap double-recycle bug fixed: cropBitmap nulled on result after recycling
- Defensive bitmap copy prevents camera callback race condition in captureAndDetect()
- ConfirmationViewModel.saveAll() reports errors via StateFlow instead of silently swallowing
- All 7 hardcoded Color.parseColor() calls in DetectionResultAdapter extracted to colors.xml resources

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire DetectionOverlayView + two-phase visual states** - `6e0fad0` (feat)
2. **Task 2: Fix bitmap double-recycle + cancel propagation** - `7e1470b` (fix)
3. **Task 3: Fix ConfirmationViewModel error swallowing + extract adapter colors** - `6e077d9` (fix)

## Files Created/Modified
- `app/src/main/java/.../presentation/ui/yolo/DetectionOverlayView.kt` - Two-phase status-driven bounding box rendering
- `app/src/main/java/.../presentation/ui/yolo/YoloScanFragment.kt` - Overlay wiring + defensive bitmap copy
- `app/src/main/java/.../inference/pipeline/DetectionPipeline.kt` - Bitmap double-recycle fix + cancel propagation comment
- `app/src/main/java/.../presentation/viewmodel/YoloScanViewModel.kt` - Detections StateFlow for overlay
- `app/src/main/java/.../presentation/viewmodel/ConfirmationViewModel.kt` - Error reporting in catch block
- `app/src/main/java/.../presentation/adapter/DetectionResultAdapter.kt` - Replaced 7 hardcoded colors with resources
- `app/src/main/java/.../domain/model/PipelineState.kt` - Added Detected intermediate state
- `app/src/main/java/.../domain/usecase/SaveDetectedFoodsUseCase.kt` - Added error field to SaveResult
- `app/src/main/res/values/colors.xml` - 8 new color resources for detection UI

## Decisions Made
- Added `PipelineState.Detected` as intermediate state between Detecting and Classifying to enable overlay rendering immediately after YOLO completes, before LLM classification begins
- Used `when` expression for status-driven paint selection in DetectionOverlayView — clean, type-safe, no animation overhead
- Added nullable `error` field to existing `SaveResult` data class rather than creating separate error StateFlow — simpler API

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 5 code hardening issues from success criteria resolved (D-06 through D-14)
- Overlay rendering pipeline fully wired end-to-end
- Ready for next plan in phase 08 (if any) or phase 09 verification

---
*Phase: 08-yolo-hardening*
*Completed: 2026-04-10*

## Self-Check: PASSED

All 6 key files verified on disk. All 3 task commits verified in git log.
