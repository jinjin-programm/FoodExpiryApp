---
phase: 06-detection
plan: 01
subsystem: ml-inference
tags: [yolo, mnn, llm, food-detection, batch-pipeline, hilt, kotlin, android]

# Dependency graph
requires:
  - phase: 05-engine
    provides: MnnLlmEngine, ModelLifecycleManager, ModelStorageManager
provides:
  - Domain models: DetectionResult, BatchDetectionResult, PipelineState, DetectionStatus
  - YoloDetectionRepository interface with Flow-based detectFoods API
  - MnnYoloEngine singleton with lifecycle-managed YOLO inference
  - DetectionPipeline orchestrating sequential YOLO detect → LLM classify
  - YoloDetectionRepositoryImpl and DetectionModule DI wiring
affects: [06-detection, 07-scan-ui]

# Tech tracking
tech-stack:
  added: [mnn-yolo-engine, detection-pipeline, channel-flow]
  patterns: [sequential-model-lifecycle, bitmap-recycling, nms-postprocessing, normalized-coordinates]

key-files:
  created:
    - app/src/main/java/com/example/foodexpiryapp/domain/model/DetectionResult.kt
    - app/src/main/java/com/example/foodexpiryapp/domain/model/BatchDetectionResult.kt
    - app/src/main/java/com/example/foodexpiryapp/domain/model/PipelineState.kt
    - app/src/main/java/com/example/foodexpiryapp/domain/repository/YoloDetectionRepository.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloConfig.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloPostprocessor.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt
    - app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt
    - app/src/main/java/com/example/foodexpiryapp/data/repository/YoloDetectionRepositoryImpl.kt
    - app/src/main/java/com/example/foodexpiryapp/di/DetectionModule.kt
  modified: []

key-decisions:
  - "YOLO engine uses same lifecycle pattern as LLM engine with ModelType.YOLO for mutual exclusion"
  - "DetectionPipeline uses channelFlow for state emission with cancellation support via isActive check"
  - "Bounding box coordinates normalized to 0f-1f space (not 640x640) for device-independent rendering"
  - "Crop bitmaps recycled immediately after each LLM classification to prevent memory cascade"
  - "YOLO native inference is stub initially — actual MNN JNI bridge to be added when native code is ready"

patterns-established:
  - "Sequential model lifecycle: YOLO unload before LLM load (never both simultaneously)"
  - "channelFlow for multi-stage pipeline with PipelineState sealed class emissions"
  - "Normalized coordinate space (0f-1f) for bounding boxes across domain models"
  - "Immediate bitmap recycling after use in detection pipeline"

requirements-completed: [YOLO-01, YOLO-03, YOLO-04, YOLO-05, YOLO-07, YOLO-08]

# Metrics
duration: 6min
completed: 2026-04-08
---

# Phase 6 Plan 1: YOLO+LLM Batch Detection Pipeline Summary

**Domain contracts, MNN YOLO engine, and sequential detection pipeline with YOLO detect → unload → LLM classify per-crop → unload lifecycle**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-08T21:43:16Z
- **Completed:** 2026-04-08T21:49:42Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Created complete domain model layer for batch detection: DetectionResult, BatchDetectionResult, PipelineState sealed class, DetectionStatus enum
- Built MnnYoloEngine following same singleton/lifecycle pattern as MnnLlmEngine with ModelType.YOLO mutual exclusion
- Implemented DetectionPipeline orchestrator with sequential YOLO→LLM lifecycle, bitmap recycling, and Flow-based state emission
- Wired full DI chain: DetectionModule → YoloDetectionRepositoryImpl → DetectionPipeline → MnnYoloEngine + MnnLlmEngine

## Task Commits

Each task was committed atomically:

1. **Task 1: Create domain contracts and YOLO engine** - `a45366a` (feat)
2. **Task 2: Create DetectionPipeline and wire repository + DI** - `cd893b9` (feat)

## Files Created/Modified
- `domain/model/DetectionResult.kt` - Domain detection result with id, boundingBox (normalized 0f-1f), cropBitmap, label, category, confidence, foodIdentification, status
- `domain/model/BatchDetectionResult.kt` - Batch session result with sessionId, results list, classified/failed counts
- `domain/model/PipelineState.kt` - Sealed class: Idle, Detecting, Classifying(n/total), Complete, Error, Cancelled
- `domain/repository/YoloDetectionRepository.kt` - Domain interface with detectFoods(bitmap): Flow<PipelineState>
- `inference/yolo/MnnYoloConfig.kt` - YOLO config with maxDetections=8 per YOLO-07
- `inference/yolo/MnnYoloPostprocessor.kt` - NMS, coordinate normalization, and bitmap cropping utilities
- `inference/yolo/MnnYoloEngine.kt` - @Singleton YOLO engine with lifecycle-managed model loading
- `inference/pipeline/DetectionPipeline.kt` - Core orchestrator: YOLO detect → unload → LLM classify → unload
- `data/repository/YoloDetectionRepositoryImpl.kt` - Repository impl wrapping pipeline with StateFlow
- `di/DetectionModule.kt` - Hilt module with @Binds repository and @Provides MnnYoloConfig

## Decisions Made
- YOLO engine uses same lifecycle pattern as LLM engine with ModelType.YOLO for mutual exclusion (consistent with existing MnnLlmEngine pattern)
- DetectionPipeline uses channelFlow for state emission with cancellation support via isActive check (correct coroutine pattern for multi-emission)
- Bounding box coordinates normalized to 0f-1f space instead of 640x640 for device-independent rendering and UI scaling
- Crop bitmaps recycled immediately after each LLM classification to prevent memory cascade (YOLO-08/PITFALL-6)
- YOLO native inference is stub initially — actual MNN JNI bridge to be added when native code is ready

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Domain contracts and YOLO engine ready for UI integration in Phase 07
- DetectionPipeline ready for ViewModels to collect PipelineState flows
- YOLO native bridge (JNI stubs) needs implementation when MNN YOLO model is available
- Pipeline currently returns empty detections (stub mode) until native bridge is implemented

---
*Phase: 06-detection*
*Completed: 2026-04-08*

## Self-Check: PASSED

All 11 created files verified on disk. Both task commits (a45366a, cd893b9) present in git log.
