---
phase: 04-foundation
plan: 03
subsystem: infra
tags: [mnn, native-libs, jni, proguard, android]

# Dependency graph
requires:
  - phase: 04-foundation/02
    provides: llama.cpp fully removed (no libllm.so collision)
provides:
  - MNN v3.5.0 native libraries (libMNN.so, libllm.so) in jniLibs/arm64-v8a/
  - MNN Java wrapper classes (com.taobao.android.mnn.*)
  - ProGuard keep rules for MNN classes and native methods
  - Verified: build succeeds, app launches, no native library errors
affects: [05-engine, 06-detection]

# Tech tracking
tech-stack:
  added: [MNN v3.5.0 native libs, com.taobao.android.mnn Java wrappers]
  patterns: [jniLibs-direct-integration, no-AAR-dependency]

key-files:
  created:
    - app/src/main/jniLibs/arm64-v8a/libMNN.so
    - app/src/main/jniLibs/arm64-v8a/libllm.so
    - app/src/main/java/com/taobao/android/mnn/MNNNetInstance.java
    - app/src/main/java/com/taobao/android/mnn/MNNNetNative.java
    - app/src/main/java/com/taobao/android/mnn/MNNForwardType.java
    - app/src/main/java/com/taobao/android/mnn/MNNImageProcess.java
    - app/src/main/java/com/taobao/android/mnn/MNNPortraitNative.java
    - app/src/main/java/com/taobao/android/utils/Common.java
  modified:
    - app/proguard-rules.pro
    - .gitignore

key-decisions:
  - "No AAR dependency — MNN integrated via jniLibs .so files + Java source files directly"
  - "Created com.taobao.android.utils.Common utility class to satisfy MNNNetNative import"

patterns-established:
  - "MNN native libs placed in jniLibs/arm64-v8a/ (AGP auto-includes in APK)"
  - "MNN Java wrappers in com.taobao.android.mnn package (from official MNN demo)"

requirements-completed: [MNN-01]

# Metrics
duration: 10min
completed: 2026-04-08
---

# Phase 4 Plan 3: MNN Integration Summary

**MNN v3.5.0 native libraries and Java wrappers integrated via direct jniLibs — no AAR dependency, build succeeds, app launches clean**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-08T13:15:00Z
- **Completed:** 2026-04-08T13:25:00Z
- **Tasks:** 3 (Task 1 was pre-completed by user, Tasks 2-3 executed)
- **Files modified:** 10

## Accomplishments
- MNN native libraries (libMNN.so 2.3MB, libllm.so 1.2MB) added to jniLibs/arm64-v8a/
- 5 MNN Java wrapper classes from official MNN demo app added to source tree
- ProGuard keep rules added for MNN classes and native methods
- Build succeeds (`./gradlew assembleDebug` — 49 tasks, 35s)
- App installs and launches on device without crashes or UnsatisfiedLinkError
- Both libMNN.so and libllm.so verified present in APK

## Task Commits

Each task was committed atomically:

1. **Task 1: Obtain MNN v3.5.0 files** - `6c424f9` (feat)
2. **Task 2: Update Gradle + ProGuard rules** - `95d9ff0` (feat)
3. **Task 3: Verify app launches** - Verification only, no file changes

## Files Created/Modified
- `app/src/main/jniLibs/arm64-v8a/libMNN.so` - MNN core native library (2.3MB)
- `app/src/main/jniLibs/arm64-v8a/libllm.so` - MNN LLM engine native library (1.2MB)
- `app/src/main/java/com/taobao/android/mnn/MNNNetInstance.java` - Model loading, session/tensor management
- `app/src/main/java/com/taobao/android/mnn/MNNNetNative.java` - JNI bridge to MNN native methods
- `app/src/main/java/com/taobao/android/mnn/MNNForwardType.java` - Backend type enum (CPU/OpenCL/Vulkan/OpenGL)
- `app/src/main/java/com/taobao/android/mnn/MNNImageProcess.java` - Bitmap/buffer to tensor conversion
- `app/src/main/java/com/taobao/android/mnn/MNNPortraitNative.java` - Portrait mask utilities
- `app/src/main/java/com/taobao/android/utils/Common.java` - TAG constant for MNN logging
- `app/proguard-rules.pro` - Added MNN keep rules
- `.gitignore` - Added Eclipse IDE artifacts

## Decisions Made
- **No AAR dependency:** The plan specified adding `implementation(files("libs/MNN-release.aar"))` but the user provided .so files directly in jniLibs/ and Java source files. Android Gradle Plugin automatically picks up .so files from jniLibs/, so no AAR dependency is needed.
- **Created Common utility:** MNNNetNative.java imports `com.taobao.android.utils.Common` which didn't exist in the copied files. Created a minimal Common class with the TAG constant to satisfy the import.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created missing com.taobao.android.utils.Common class**
- **Found during:** Task 2 (Gradle configuration)
- **Issue:** MNNNetNative.java imports `com.taobao.android.utils.Common` which was not among the 5 copied Java files. Without it, compilation would fail.
- **Fix:** Created `app/src/main/java/com/taobao/android/utils/Common.java` with `public static final String TAG = "MNN"`
- **Files modified:** app/src/main/java/com/taobao/android/utils/Common.java (created)
- **Verification:** Build succeeds with no compilation errors
- **Committed in:** 95d9ff0 (Task 2 commit)

**2. [Rule 2 - Missing Critical] Cleaned stale .cxx cmake cache**
- **Found during:** Task 2 (pre-build cleanup)
- **Issue:** The `app/.cxx/` directory contained stale cmake build cache from the llama.cpp era (referencing `CMakeLists.txt`, `llama_jni`, etc.). While not causing build failures (since externalNativeBuild was removed in Plan 02), leaving stale native build artifacts is a correctness risk.
- **Fix:** Deleted `app/.cxx/` directory before clean build
- **Files modified:** app/.cxx/ (deleted)
- **Verification:** Clean build succeeds without cmake references
- **Committed in:** 95d9ff0 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 missing critical)
**Impact on plan:** Both auto-fixes necessary for build correctness. No scope creep.

## Issues Encountered
None — plan executed smoothly.

## User Setup Required
None — no external service configuration required.

## Next Phase Readiness
- MNN v3.5.0 fully integrated: native libs in APK, Java wrappers available, build clean
- Ready for Phase 5 (Engine): MnnLlmEngine domain service can be built on top of these wrappers
- No blockers identified

## Self-Check: PASSED

- libMNN.so: FOUND in jniLibs/arm64-v8a/
- libllm.so: FOUND in jniLibs/arm64-v8a/
- MNNNetInstance.java: FOUND
- 95d9ff0 (Task 2): FOUND
- 6c424f9 (Task 1): FOUND

---
*Phase: 04-foundation*
*Completed: 2026-04-08*
