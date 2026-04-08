---
phase: 04-foundation
plan: 02
subsystem: infra
tags: [llama.cpp, cleanup, mnn-prep, native-libraries, jni]

# Dependency graph
requires:
  - phase: 04-01
    provides: project backup before destructive changes
provides:
  - Clean project state with zero llama.cpp artifacts
  - No libllm.so naming collision risk for MNN integration
  - Stubbed LLM features with clear MNN migration path
affects: [05-engine, 06-detection]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Navigation destination commenting: comment out (don't delete) removed fragments to preserve nav graph structure"

key-files:
  created: []
  modified:
    - app/build.gradle.kts
    - app/app/build.gradle.kts
    - app/src/main/java/FoodExpiryApp.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt
    - app/src/main/res/navigation/nav_graph.xml
    - app/app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt
    - app/app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt
    - app/app/src/main/res/navigation/nav_graph.xml

key-decisions:
  - "Commented out (not deleted) LlmScanFragment navigation destinations per threat model T-04-04"
  - "Stubbed ChatViewModel and VisionScanFragment AI features with clear Phase 5 TODO markers"
  - "Kept mlModelBinding=true in buildFeatures for TFLite/YOLO (not llama.cpp related)"

patterns-established:
  - "Dual directory cleanup: changes applied to both app/ and app/app/ (duplicate project structure)"

requirements-completed: [MNN-02]

# Metrics
duration: 16min
completed: 2026-04-08
---

# Phase 4 Plan 2: Remove llama.cpp Artifacts Summary

**Complete removal of llama.cpp native libraries, JNI code, Kotlin wrappers, and Gradle dependencies to eliminate libllm.so naming collision with MNN**

## Performance

- **Duration:** 16 min
- **Started:** 2026-04-08T12:24:58Z
- **Completed:** 2026-04-08T12:41:15Z
- **Tasks:** 2
- **Files modified:** 19

## Accomplishments
- Removed all 6 llama.cpp native .so files (~53MB) from both jniLibs directories
- Deleted JNI bridge code (CMakeLists.txt, llamajni.cpp, llama.cpp headers)
- Removed externalNativeBuild cmake block and com.llama4j:gguf dependency
- Deleted LlamaBridge.kt, LlmVisionService.kt, LlmScanFragment.kt
- Fixed all references in ChatViewModel, VisionScanFragment, FoodExpiryApp, and navigation
- Project builds successfully with zero llama.cpp remnants

## Task Commits

Each task was committed atomically:

1. **Task 1: Delete llama.cpp native libraries and JNI code** - `d257721` (feat)
2. **Task 2: Remove llama.cpp from Gradle and Kotlin code** - `c47cdd8` (feat)

## Files Created/Modified
- `app/build.gradle.kts` - Removed externalNativeBuild block and llama4j dependency
- `app/app/build.gradle.kts` - Same cleanup on duplicate
- `app/src/main/java/FoodExpiryApp.kt` - Removed LLM warmup coroutine, added TODO
- `app/src/main/.../chat/ChatViewModel.kt` - Stubbed with MNN-pending message
- `app/src/main/.../vision/VisionScanFragment.kt` - Removed LlamaBridge, stubbed AI inference
- `app/src/main/res/navigation/nav_graph.xml` - Commented out LlmScanFragment destination
- `app/app/.../chat/ChatViewModel.kt` - Same stub applied to duplicate
- `app/app/.../vision/VisionScanFragment.kt` - Same stub applied to duplicate
- `app/app/src/main/res/navigation/nav_graph.xml` - Same nav changes on duplicate

## Decisions Made
- Commented out (not deleted) LlmScanFragment navigation destinations per threat model T-04-04 — prevents broken nav links at runtime if any code references the ID
- Stubbed ChatViewModel and VisionScanFragment with clear TODO markers pointing to Phase 5 instead of deleting the classes — preserves the UI shell for MNN integration
- Kept `mlModelBinding = true` in buildFeatures because TFLite is still used for YOLO detection — this flag is not llama.cpp-specific

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Windows filesystem lock prevented deleting the empty `app/src/main/jni/` directory (`Device or resource busy`). The directory is empty and will be cleaned up on next build or filesystem refresh. No functional impact.
- The `app/app/` duplicate directory structure required applying all changes twice (build.gradle.kts, Kotlin files, nav_graph.xml). This was anticipated in the plan.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Zero llama.cpp artifacts remain — libllm.so naming collision with MNN is eliminated
- Build succeeds cleanly with `assembleDebug`
- LLM features are gracefully stubbed with Phase 5 TODO markers
- Ready for MNN AAR integration in plan 04-03

---
*Phase: 04-foundation*
*Completed: 2026-04-08*

## Self-Check: PASSED

- [x] Commit d257721 exists
- [x] Commit c47cdd8 exists
- [x] SUMMARY.md exists at .planning/phases/04-foundation/04-02-SUMMARY.md
- [x] Build successful (assembleDebug)
- [x] Zero LlamaBridge references in source
- [x] Zero llama4j references in build.gradle.kts
- [x] Zero native .so files in source directories
