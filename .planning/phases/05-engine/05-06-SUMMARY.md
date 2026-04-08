---
phase: "05-engine"
plan: "06"
subsystem: "ui-integration"
tags: ["fragment", "viewmodel", "application", "di", "hilt", "mnn", "download", "inference", "ui-wiring"]
dependency_graph:
  requires:
    - phase: "05-05"
      provides: ["LlmInferenceRepositoryImpl", "IdentifyFoodUseCase", "InferenceModule"]
  provides:
    - "VisionScanFragment wired to MNN inference pipeline"
    - "ChatViewModel with live model status from ModelDownloadManager"
    - "FoodExpiryApp startup cleanup of incomplete downloads"
  affects: ["06-01 (YOLO+LLM pipeline)", "07-01 (scan UI)", "end-to-end food identification flow"]
tech_stack:
  added: []
  patterns: ["Download-then-inference flow in UI", "WiFi check dialog before cellular download", "FragmentResultListener for cross-fragment result passing", "Startup cleanup in Application class"]
key_files:
  created: []
  modified:
    - "app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt"
    - "app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt"
    - "app/src/main/java/FoodExpiryApp.kt"
key-decisions:
  - "Download-then-inference pattern: runAskAiInference checks model ready, triggers download if needed, then retries inference"
  - "WiFi warning dialog shown before cellular download, with user choice to proceed or wait"
  - "No eager model warmup in Application.onCreate — per PITFALL-5, lazy loading on first use preferred"
  - "ChatViewModel does not directly call IdentifyFoodUseCase — chat LLM integration deferred to future phase"
requirements-completed: ["MNN-03", "MNN-04", "MNN-05", "MNN-06", "DL-01", "DL-02", "DL-03", "DL-04", "DL-05", "DL-06", "DL-07"]

metrics:
  duration_seconds: 180
  completed_date: "2026-04-09"
  tasks: 2
  files: 3
---

# Phase 05 Plan 06: UI Integration Summary

**VisionScanFragment wired to MNN inference with download trigger and WiFi check, ChatViewModel with live model status, FoodExpiryApp with startup download cleanup — completing end-to-end MNN accessibility.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-08T17:06:48Z
- **Completed:** 2026-04-08T17:09:48Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- VisionScanFragment now uses IdentifyFoodUseCase for AI food identification instead of TODO stub
- VisionScanFragment triggers automatic model download with WiFi check dialog when model not ready
- ChatViewModel checks ModelDownloadManager for real model status on init
- FoodExpiryApp cleans up incomplete .part downloads on startup (PITFALL-3 mitigation)
- All Phase 5 TODO stubs removed from VisionScanFragment, ChatViewModel, and FoodExpiryApp

## Task Commits

Each task was committed atomically:

1. **Task 1: Update VisionScanFragment with MNN integration** - `9549b94` (feat)
2. **Task 2: Update ChatViewModel and FoodExpiryApp** - `c2c90b2` (feat)

## Files Created/Modified
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` - Replaced TODO stubs with MNN inference integration, download trigger, WiFi dialog, FragmentResultListener result passing
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt` - Replaced TODO stubs with ModelDownloadManager integration, model status checking, startModelDownload()
- `app/src/main/java/FoodExpiryApp.kt` - Added ModelStorageManager injection and cleanupIncompleteDownloads() in onCreate

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Download-then-inference in runAskAiInference | User taps AI button → check model → download if needed → retry inference automatically |
| WiFi warning dialog before cellular | Model is ~1.2GB; downloading over cellular could surprise users with data charges |
| No IdentifyFoodUseCase in ChatViewModel constructor | ChatViewModel only needs model status for now; full chat LLM is a future enhancement |
| No eager model warmup in FoodExpiryApp.onCreate | Per PITFALL-5, lazy loading on first use is preferred over blocking app startup |
| cleanupIncompleteDownloads in Application.onCreate | Per PITFALL-3, stale .part files must be removed before any code tries to load model files |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Wired WiFi dialog download states to full UI updates**
- **Found during:** Task 1 (showWifiWarningDialog implementation)
- **Issue:** Plan's WiFi dialog "Download" button had a comment `// Handle download states (same as above)` — left unimplemented. Without full state handling, the UI would not update during the WiFi-confirmed download.
- **Fix:** Added complete download state handling (Downloading, Complete, Error) in the dialog's positive button callback, matching the pattern from runAskAiInference
- **Files modified:** VisionScanFragment.kt
- **Committed in:** `9549b94` (Task 1 commit)

**2. [Rule 1 - Bug] Removed IdentifyFoodUseCase from ChatViewModel constructor**
- **Found during:** Task 2 (ChatViewModel rewrite)
- **Issue:** Plan specified ChatViewModel should inject IdentifyFoodUseCase, but ChatViewModel doesn't use it — it only checks model status and provides a download trigger. Injecting an unused dependency would create misleading code and potential DI confusion.
- **Fix:** Removed IdentifyFoodUseCase from ChatViewModel constructor, keeping only ModelDownloadManager which is actually used
- **Files modified:** ChatViewModel.kt
- **Committed in:** `c2c90b2` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 bug)
**Impact on plan:** Both fixes improve correctness. WiFi dialog now fully functional; ChatViewModel has no unused dependencies.

## Issues Encountered
None — plan executed smoothly with all dependencies (Plan 05 outputs) available and correctly structured.

## Verification Checklist

- [x] VisionScanFragment.kt has `@Inject lateinit var identifyFoodUseCase: IdentifyFoodUseCase`
- [x] VisionScanFragment.kt has `@Inject lateinit var modelDownloadManager: ModelDownloadManager`
- [x] loadModelIfNeeded() NO LONGER contains "MNN upgrade pending" text
- [x] loadModelIfNeeded() calls `modelDownloadManager.isModelReady()` and `observeDownloadState()`
- [x] runAskAiInference() NO LONGER contains "MNN upgrade pending" text
- [x] runAskAiInference() calls `identifyFoodUseCase.invoke(bitmap)`
- [x] runAskAiInference() sends result via `setFragmentResult("llm_scan_result", bundle)` with food_name and food_name_zh
- [x] runAskAiInference() calls `modelDownloadManager.downloadModel()` when model not ready
- [x] Has WiFi warning dialog before cellular download
- [x] Existing FoodClassifier quick scan flow preserved
- [x] ChatViewModel.kt injects ModelDownloadManager
- [x] ChatViewModel NO LONGER contains "MNN integration pending (Phase 5)" text
- [x] ChatViewModel.init calls checkModelStatus() which uses modelDownloadManager.isModelReady()
- [x] ChatViewModel.sendMessage checks model status before responding
- [x] ChatViewModel has startModelDownload() method
- [x] FoodExpiryApp.kt injects ModelStorageManager
- [x] FoodExpiryApp.onCreate() calls modelStorageManager.cleanupIncompleteDownloads()
- [x] FoodExpiryApp.kt NO LONGER contains "TODO: MNN model warmup" text
- [x] No Phase 5 TODO stubs remain

## Known Stubs

None — all three files have complete implementations with no placeholder values.

## Threat Surface

No new threat surface introduced beyond what the plan's threat model anticipated:
- T-05-16 (FragmentResultListener): Intra-app communication, no external input — accepted risk
- T-05-17 (Concurrent inference): isProcessing flag in VisionScanFragment prevents concurrent calls — mitigated

## Self-Check: PASSED

- ✅ VisionScanFragment.kt modified and committed
- ✅ ChatViewModel.kt modified and committed
- ✅ FoodExpiryApp.kt modified and committed
- ✅ Commit 9549b94 verified in git log
- ✅ Commit c2c90b2 verified in git log
- ✅ SUMMARY.md created at .planning/phases/05-engine/

---
*Phase: 05-engine*
*Completed: 2026-04-09*
