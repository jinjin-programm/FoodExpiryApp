# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## yolo-scan-camera-black-on-return — Camera preview black on ViewPager2 swipe-back
- **Date:** 2026-04-11
- **Error patterns:** camera black, ViewPager2, ProcessCameraProvider, unbindAll, singleton, swipe back, preview black, CameraX
- **Root cause:** ProcessCameraProvider is a singleton per Context. All fragments sharing it call unbindAll() during initialization, which removes use cases from ALL fragments. ViewPager2's FragmentStateAdapter keeps adjacent fragments alive but onViewCreated doesn't re-fire on swipe-back, so no camera rebind happens.
- **Fix:** Add ViewPager2.OnPageChangeCallback to each fragment that re-binds camera use cases via onPageSelected when the respective tab is re-selected.
- **Files changed:** YoloScanFragment.kt, ScanFragment.kt, VisionScanFragment.kt
---