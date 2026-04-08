---
phase: 07-scan-ui
plan: 02
subsystem: ui
tags: [android, layout, camera, scan, constraintlayout, material-design, floating-button]

# Dependency graph
requires:
  - phase: 06-detection
    provides: DetectionOverlayView, scan fragments, existing layout files
provides:
  - Full-bleed camera layouts for all three scan modes
  - Floating back arrow drawable (shape_fab_back.xml)
  - Unified capture button style across barcode and YOLO scans
  - Clean scan UI with no static frame guides
affects: [07-scan-ui, scan-ui-overhaul, kotlin-fragments]

# Tech tracking
tech-stack:
  added: []
  patterns: [floating-back-arrow, full-bleed-camera, unified-shutter-button]

key-files:
  created:
    - app/src/main/res/drawable/shape_fab_back.xml
  modified:
    - app/src/main/res/layout/fragment_vision_scan.xml
    - app/src/main/res/layout/fragment_scan.xml
    - app/src/main/res/layout/fragment_yolo_scan.xml

key-decisions:
  - "Kept hidden stub views (visibility=gone) for removed UI elements to maintain Kotlin code compatibility"
  - "Used YOLO scan shutter button pattern (shape_circle_shutter_ring) for barcode scan capture button"

patterns-established:
  - "Floating back arrow: ImageButton with shape_fab_back semi-transparent circle, top-left corner"
  - "Full-bleed camera: PreviewView constrained parent top to bottom, no overlays"

requirements-completed: [UI-02, UI-07, UI-08]

# Metrics
duration: 6min
completed: 2026-04-09
---

# Phase 7 Plan 2: Scan Layout Redesign Summary

**Full-bleed camera layouts with floating back arrows, no static frame guides, and unified capture button style across all three scan modes**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-08T23:39:18Z
- **Completed:** 2026-04-08T23:45:27Z
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments
- Removed all static frame guides (simulated_box, scan_box, frame_card) from three scan layouts
- Added floating back arrow (btn_back) with semi-transparent circular background to all scan screens
- Unified capture button style: barcode scan now uses YOLO shutter pattern with shape_circle_shutter_ring
- Removed tint overlays from all scan layouts for clean full-bleed camera experience
- Removed top_controls (mode pills + close button) from YOLO scan
- Removed Flash/Close FABs from barcode scan

## Task Commits

Each task was committed atomically:

1. **Task 1: Create floating back arrow drawable and redesign all three scan layouts** - `cf8230e` (feat)

## Files Created/Modified
- `app/src/main/res/drawable/shape_fab_back.xml` - Semi-transparent oval drawable for floating back button background
- `app/src/main/res/layout/fragment_vision_scan.xml` - Removed AppBarLayout, simulated_box, tint overlay; added floating btn_back; PreviewView full-bleed
- `app/src/main/res/layout/fragment_scan.xml` - Removed scan_box, Flash/Close FABs, instructions, tint overlay; added floating btn_back and capture button
- `app/src/main/res/layout/fragment_yolo_scan.xml` - Removed frame_card, top_controls, tint overlay; added floating btn_back; kept DetectionOverlayView

## Decisions Made
- **Kept hidden stub views** for removed UI elements (btn_close, simulated_box, tv_status, viewStatusIndicator, tv_instruction, tv_instruction_detail) to maintain Kotlin code compatibility without modifying source files
- **Used YOLO shutter button pattern** for barcode scan capture button for visual consistency across all scan modes

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added hidden stub views for Kotlin code compatibility**
- **Found during:** Task 1 (build verification)
- **Issue:** Removing AppBarLayout, simulated_box, scan_box, top_controls, btn_close, tv_status, etc. from layouts caused Kotlin compilation errors because VisionScanFragment, ScanFragment, and YoloScanFragment reference these view IDs via ViewBinding
- **Fix:** Added hidden stub views (visibility="gone") for all removed elements: appBar, toolbar, btn_close, viewStatusIndicator, simulated_box, tv_status in vision scan; btn_close, tv_instruction, tv_instruction_detail in barcode scan; btn_close in YOLO scan
- **Files modified:** fragment_vision_scan.xml, fragment_scan.xml, fragment_yolo_scan.xml
- **Verification:** `./gradlew compileDebugKotlin` passes successfully
- **Committed in:** cf8230e (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Essential fix to maintain code compatibility. Stubs are invisible (gone) and will be cleaned up when Kotlin fragments are updated in subsequent plans.

## Issues Encountered
- Pre-existing Hilt/Dagger error (DetectionResultDao missing binding) prevents `assembleDebug` but is unrelated to layout changes. Kotlin compilation (`compileDebugKotlin`) passes cleanly.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All three scan layouts redesigned with full-bleed camera and floating back arrows
- Kotlin fragments still reference old view IDs via stubs — fragment code updates needed in subsequent plans
- Ready for Plan 03 (capture animation) and Plan 04 (barcode scan behavior changes)

---
*Phase: 07-scan-ui*
*Completed: 2026-04-09*

## Self-Check: PASSED
- All 4 created/modified files exist on disk
- Commit cf8230e exists in git history
- SUMMARY.md created successfully
