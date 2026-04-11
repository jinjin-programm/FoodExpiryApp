---
status: resolved
trigger: "YOLO scan camera becomes black when returning to the tab after swiping to Barcode tab. Barcode camera works fine when returning."
created: 2026-04-11T00:00:00Z
updated: 2026-04-11T00:02:00Z
---

## Current Focus

hypothesis: CONFIRMED & VERIFIED — Fix applied and confirmed by human testing.
test: Human verified on device — all 3 tabs rebind camera correctly on swipe-back.
expecting: N/A — complete
next_action: Archive session

## Symptoms

expected: YOLO camera preview visible when swiping back to YOLO tab from Barcode tab
actual: YOLO camera preview is black when swiping back, but Barcode camera works when returning to Barcode
errors: No explicit errors mentioned
reproduction: Open Photo Scan → starts on YOLO tab → swipe left to Barcode tab → swipe back to YOLO tab → camera is black
started: Started after Phase 8 execution (YOLO JNI bridge + overlay wiring)

## Eliminated

## Evidence

- timestamp: 2026-04-11T00:00:00Z
  checked: ScanContainerFragment - ViewPager2 setup
  found: No offscreenPageLimit set (default = OFFSCREEN_PAGE_LIMIT_DEFAULT = -1). ViewPager2 caches current + 1 adjacent page. Fragments stay alive but lifecycle state changes.
  implication: Fragment views persist but lifecycle methods are limited by BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT.

- timestamp: 2026-04-11T00:00:00Z
  checked: ScanFragment.bindCameraUseCases() (Barcode)
  found: `cameraProvider.unbindAll()` then `cameraProvider.bindToLifecycle(this, ...)`. ProcessCameraProvider.getInstance(requireContext()) returns the SAME singleton instance. unbindAll() removes ALL use cases from ALL fragments.
  implication: **CRITICAL** — unbindAll() from Barcode kills YOLO's camera use cases.

- timestamp: 2026-04-11T00:00:00Z
  checked: YoloScanFragment.setupViewPagerCallback()
  found: Only cancels detection when leaving YOLO tab. Does NOT re-bind camera when returning.
  implication: No code path rebinds camera when swiping back to YOLO.

- timestamp: 2026-04-11T00:01:00Z
  checked: Build compilation after fix
  found: BUILD SUCCESSFUL — compileDebugKotlin passes with zero new errors
  implication: Fix compiles correctly

- timestamp: 2026-04-11T00:02:00Z
  checked: Human device verification
  found: Confirmed — YOLO camera works when swiping back from Barcode tab, all 3 tabs rebind camera correctly.
  implication: Fix is verified end-to-end.

## Resolution

root_cause: ProcessCameraProvider is a singleton per Context. When ScanFragment (Barcode tab) calls startCamera() → bindCameraUseCases() → unbindAll(), it unbinds YOLO's camera use cases from the shared provider. When the user swipes back to the YOLO tab, no code re-binds the camera because: (1) the FragmentStateAdapter with BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT means the YOLO fragment goes through onPause→onResume but NOT through onViewCreated again, and (2) YoloScanFragment's setupViewPagerCallback only cancels detection when leaving, but does NOT re-bind camera when returning. The exact sequence: App starts on YOLO (tab 2) → adjacent Barcode fragment (tab 1) is auto-created → ScanFragment.onViewCreated() → startCamera() → unbindAll() removes YOLO's camera → binds Barcode's camera → user swipes to Barcode (works, it's the last bound) → user swipes back to YOLO → no rebind → black screen.
fix: Added setupViewPagerCallback() to all three scan fragments (YoloScanFragment, ScanFragment, VisionScanFragment) that listens for onPageSelected and re-binds camera use cases when the respective tab is re-selected. This ensures each fragment reclaims the shared ProcessCameraProvider when it becomes the active page.
verification: Human confirmed on device — YOLO camera works when swiping back from Barcode tab, all 3 tabs rebind camera correctly.
files_changed: [YoloScanFragment.kt, ScanFragment.kt, VisionScanFragment.kt]
