---
phase: 06-detection
plan: 03
subsystem: scan-ui
tags: [viewpager2, viewmodel, fragment, navigation, room, hilt, kotlin, android]

# Dependency graph
requires:
  - phase: 06-detection
    plan: 01
    provides: YoloDetectionRepository, PipelineState, DetectionResult, BatchDetectionResult
  - phase: 06-detection
    plan: 02
    provides: DetectionResultRepository, DetectionResultEntity
provides:
  - ScanContainerFragment: ViewPager2 host for Photo/Barcode/YOLO scan modes
  - YoloScanFragment: YOLO scan tab with camera, capture, staged progress, navigation to confirmation
  - YoloScanViewModel: Orchestrates detection pipeline via YoloDetectionRepository, persists to Room
  - ConfirmationFragment: Batch confirmation screen for reviewing detected items
  - ConfirmationViewModel: Reads persisted results from Room, supports edit/delete
  - DetectionResultAdapter: RecyclerView adapter with failed item handling
affects: [06-detection]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ViewPager2 for horizontal swipe between scan modes (D-04)"
    - "ViewModel + StateFlow for reactive pipeline state observation"
    - "SavedStateHandle for nav arg extraction in ViewModels"
    - "FragmentResult for cross-fragment communication"
    - "Room DB for process-death-surviving detection results"

key-files:
  created:
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanContainerFragment.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanPagerAdapter.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/YoloScanViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/detection/ConfirmationFragment.kt
    - app/src/main/res/layout/fragment_scan_container.xml
    - app/src/main/res/layout/fragment_confirmation.xml
    - app/src/main/res/layout/item_detection_result.xml
  modified:
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt
    - app/src/main/res/layout/fragment_yolo_scan.xml
    - app/src/main/res/navigation/nav_graph.xml

key-decisions:
  - "ViewPager2 hosts 3 scan modes with default YOLO tab — swipe between modes without visible tab bar (D-04, D-06)"
  - "YoloScanFragment rewritten from TFLite to MNN pipeline via ViewModel — cleaner separation of concerns"
  - "Progress overlay shows staged progress: Detecting → Identifying(n/total) → Auto-navigate to Confirmation (D-07)"
  - "Results persisted to Room before navigation to survive process death during LLM inference (D-17, D-20)"
  - "Single-item optimization: button text changes to 'Add to Fridge' for single detections (D-10)"
  - "Failed items rendered with orange background and 'Fix' button for user correction (D-11)"

patterns-established:
  - "ViewPager2 with FragmentStateAdapter for multi-mode scanning"
  - "ViewModel observing Flow from domain repository, emitting UI state"
  - "SavedStateHandle for extracting nav args in ViewModel"
  - "Room temp table for batch result persistence across process death"

requirements-completed: [YOLO-02, YOLO-03, YOLO-06]

# Metrics
duration: 7min
completed: 2026-04-08
---

# Phase 6 Plan 3: Scan UI Layer Summary

**ViewPager2 scan container, YOLO scan tab with staged progress, and batch confirmation UI with Room-backed persistence for process death survival**

## Performance

- **Duration:** 7 min
- **Started:** 2026-04-08T21:52:29Z
- **Completed:** 2026-04-08T21:59:48Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- Created ScanContainerFragment with ViewPager2 hosting Photo/Barcode/YOLO scan modes via horizontal swipe (D-04, D-06)
- Rewrote YoloScanFragment to use MNN pipeline via YoloScanViewModel with staged progress overlay (D-07)
- Built YoloScanViewModel orchestrating detection pipeline and persisting results to Room before navigation (D-17, D-20)
- Created ConfirmationFragment with batch result list, edit/delete actions, single-item optimization (D-09, D-10, D-11)
- Implemented DetectionResultAdapter with confidence badges and failed item UI styling
- Updated nav_graph with scan_container and confirmation destinations with Safe Args (D-16)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ScanContainerFragment with ViewPager2 and update YoloScanFragment** - `48956d1` (feat)
2. **Task 2: Create ConfirmationFragment with batch confirmation UI** - `6157fca` (feat)

## Files Created/Modified
- `presentation/ui/scan/ScanContainerFragment.kt` - ViewPager2 host for 3 scan modes
- `presentation/ui/scan/ScanPagerAdapter.kt` - FragmentStateAdapter for scan mode tabs
- `presentation/ui/yolo/YoloScanFragment.kt` - Rewritten with MNN pipeline integration, progress overlay
- `presentation/viewmodel/YoloScanViewModel.kt` - Pipeline orchestration, Room persistence
- `presentation/viewmodel/ConfirmationViewModel.kt` - Reads from Room via SavedStateHandle
- `presentation/adapter/DetectionResultAdapter.kt` - RecyclerView with failed item handling
- `presentation/ui/detection/ConfirmationFragment.kt` - Batch confirmation screen
- `res/layout/fragment_scan_container.xml` - ViewPager2 layout
- `res/layout/fragment_yolo_scan.xml` - Added progress overlay UI
- `res/layout/fragment_confirmation.xml` - Confirmation screen layout
- `res/layout/item_detection_result.xml` - Detection result row layout
- `res/navigation/nav_graph.xml` - Added scan_container and confirmation destinations

## Decisions Made
- ViewPager2 hosts 3 scan modes with default YOLO tab — swipe between modes without visible tab bar (consistent with D-04, D-06)
- YoloScanFragment rewritten from TFLite to MNN pipeline via ViewModel — cleaner separation, removes tight coupling to YoloDetector
- Progress overlay shows staged progress with cancel option — user can abort long-running LLM classification
- Results persisted to Room before navigation to ConfirmationFragment — survives process death during memory pressure from LLM inference
- Failed items rendered distinctly with orange background and "Fix" button — draws attention to items needing manual correction

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Scan UI layer complete with ViewPager2 container and batch confirmation flow
- Ready for Plan 04: DefaultAttributeEngine + save flow to FoodItem table
- YoloScanViewModel.saveResults maps domain DetectionResult → DetectionResultEntity for Room persistence
- ConfirmationFragment passes results via FragmentResult "confirmation_complete" for save flow integration

## Self-Check: PASSED

- All 12 task files verified on disk
- Both task commits (48956d1, 6157fca) present in git log
- Build compiles successfully with no new errors

---
*Phase: 06-detection*
*Completed: 2026-04-08*