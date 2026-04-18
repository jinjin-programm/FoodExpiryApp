---
phase: 06-detection
reviewed: 2026-04-09T12:00:00Z
depth: standard
files_reviewed: 23
files_reviewed_list:
  - app/src/main/java/com/example/foodexpiryapp/domain/model/DetectionResult.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/model/BatchDetectionResult.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/model/PipelineState.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/model/FoodItem.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/repository/YoloDetectionRepository.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/engine/DefaultAttributeEngine.kt
  - app/src/main/java/com/example/foodexpiryapp/domain/usecase/SaveDetectedFoodsUseCase.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloConfig.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloPostprocessor.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt
  - app/src/main/java/com/example/foodexpiryapp/data/local/database/DetectionResultEntity.kt
  - app/src/main/java/com/example/foodexpiryapp/data/local/dao/DetectionResultDao.kt
  - app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt
  - app/src/main/java/com/example/foodexpiryapp/data/repository/DetectionResultRepository.kt
  - app/src/main/java/com/example/foodexpiryapp/data/repository/YoloDetectionRepositoryImpl.kt
  - app/src/main/java/com/example/foodexpiryapp/di/DetectionModule.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanContainerFragment.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanPagerAdapter.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/YoloScanViewModel.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/ui/detection/ConfirmationFragment.kt
  - app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt
  - app/src/main/res/layout/fragment_scan_container.xml
  - app/src/main/res/layout/fragment_confirmation.xml
  - app/src/main/res/layout/item_detection_result.xml
  - app/src/main/res/layout/fragment_yolo_scan.xml
  - app/src/main/res/navigation/nav_graph.xml
findings:
  critical: 0
  warning: 6
  info: 5
  total: 11
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-04-09T12:00:00Z
**Depth:** standard
**Files Reviewed:** 29 (23 Kotlin + 4 XML layouts + 1 XML nav graph + 1 Room migration)
**Status:** issues_found

## Summary

Reviewed all 29 source files from Phase 6 (Detection) covering 4 sub-plans: ML inference pipeline, Room persistence, Scan UI, and Save Flow. The codebase follows Clean Architecture MVVM consistently with clear separation between domain, data, and presentation layers. Hilt DI wiring is correct. The YOLO+LLM sequential pipeline lifecycle (mutual exclusion via ModelLifecycleManager) is well-designed.

**Key concerns:** Several bitmap memory management issues (double-recycle risk, missing recycle on error path, unbounded bitmap accumulation in YoloScanFragment). The `cancelDetection()` implementation is incomplete — it only sets state but doesn't propagate cancellation to the active pipeline coroutine. There's also a redundant code branch in the adapter edit click handler. No security issues or critical bugs found.

## Critical Issues

No critical issues found.

## Warnings

### WR-01: Double bitmap recycle risk in DetectionPipeline

**File:** `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt:131-132`
**Issue:** After LLM classification, `detection.cropBitmap?.recycle()` is called on the original `detection` reference. However, if `detection.cropBitmap` is non-null and `llmEngine.runInference()` throws, the `catch` block creates a new `result` via `detection.copy(status = FAILED)` but then recycles `detection.cropBitmap`. Since `copy()` is shallow and `cropBitmap` is the same `Bitmap` reference, the bitmap is correctly recycled. However, if `detection.cropBitmap` is non-null but the result path assigns `result = detection.copy(...)` AND `result` is later accessed (e.g., its `cropBitmap` is read), it would reference a recycled bitmap. While `results` list is only used for status/count at line 138-145, the `BatchDetectionResult` carries these results with their `cropBitmap` references still pointing to recycled bitmaps. Any downstream consumer accessing `result.cropBitmap` will crash.

**Fix:**
```kotlin
// In the result-adding section, null out the crop bitmap after recycling:
detection.cropBitmap?.recycle()

results.add(result.copy(cropBitmap = null))
```
Alternatively, ensure `BatchDetectionResult.Complete` consumers never access `cropBitmap`.

### WR-02: Missing bitmap recycle on pipeline error/empty path

**File:** `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt:79-84`
**Issue:** When YOLO returns empty detections, the pipeline emits `Complete` and returns early at line 83 — but the `bitmap` parameter (the full camera frame) is never recycled. Similarly, if the YOLO model fails to load (line 68-71), or the LLM fails to load (line 96-100), the bitmap leaks. The `finally` block only unloads the LLM; it doesn't recycle the input bitmap.

**Fix:** Document that the caller (YoloScanViewModel) is responsible for recycling the bitmap, or explicitly recycle it before returning:
```kotlin
// Add at the start of detectAndClassify or in a finally block:
// Note: Caller is responsible for bitmap lifecycle — do NOT recycle here
// since the caller (YoloScanViewModel) holds a reference to latestBitmap
// which should not be recycled as it may be reused.
```
If the intent is that the caller owns the bitmap, add an explicit comment documenting this contract. If the pipeline should own it, add `bitmap.recycle()` to all early-return paths and the `finally` block.

### WR-03: cancelDetection() does not actually cancel the running pipeline

**File:** `app/src/main/java/com/example/foodexpiryapp/data/repository/YoloDetectionRepositoryImpl.kt:40-43`
**Issue:** `cancelDetection()` only sets `_pipelineState.value = PipelineState.Cancelled` but does NOT cancel the active coroutine collecting from `pipeline.detectAndClassify()`. The `pipelineState` StateFlow update has no effect on the running `channelFlow` in DetectionPipeline — the `isActive` check at line 105 of DetectionPipeline checks the coroutine's cancellation status, not the StateFlow value. The flow will continue running until it naturally completes or the coroutine scope is cancelled.

In `YoloScanViewModel.cancelDetection()`, `pipelineJob?.cancel()` IS called which would cancel the collecting coroutine. However, the `YoloDetectionRepositoryImpl.cancelDetection()` method itself is misleading — it claims to cancel but doesn't propagate cancellation.

**Fix:**
```kotlin
// Option A: Store the collection Job and cancel it in cancelDetection()
private var collectionJob: Job? = null

override fun detectFoods(bitmap: Bitmap): Flow<PipelineState> = flow {
    // ...
}

// Option B: Remove cancelDetection() and rely on flow cancellation
// The current YoloScanViewModel.cancelDetection() correctly cancels pipelineJob
// which cancels the flow collection. Remove the misleading cancelDetection().
```

### WR-04: latestBitmap is never recycled in YoloScanFragment

**File:** `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt:65`
**Issue:** `latestBitmap` is continuously overwritten by `FrameCapturer.analyze()` (line 311) as new camera frames arrive. The old bitmap is never recycled before being replaced. With camera frames arriving at ~30fps, each bitmap can be several MB. While GC will eventually collect them, this creates unnecessary memory pressure especially on low-end devices.

**Fix:**
```kotlin
// In FrameCapturer.analyze(), recycle the old bitmap:
setLatestBitmap(newBitmap) // use a synchronized setter

// Or in the fragment:
private fun setLatestBitmap(bitmap: Bitmap?) {
    latestBitmap?.recycle()
    latestBitmap = bitmap
}
```
Note: Be careful about thread safety — `latestBitmap` is set on the camera executor thread and read on the main thread. Consider using `@Volatile` or synchronization.

### WR-05: YoloScanFragment passes bitmap to pipeline without copy — use-after-free if recycled

**File:** `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt:280-281`
**Issue:** `captureAndDetect()` passes `latestBitmap` directly to `viewModel.startDetection(bitmap)`. The bitmap is then used asynchronously by the pipeline. If `FrameCapturer` replaces `latestBitmap` with a new frame (and if WR-04's fix recycles old bitmaps), the pipeline would be operating on a recycled bitmap, causing a crash.

**Fix:**
```kotlin
private fun captureAndDetect() {
    if (isCapturing) return
    val bitmap = latestBitmap?.copy(Bitmap.Config.ARGB_8888, false) // defensive copy
    if (bitmap == null) {
        Toast.makeText(context, "No image captured — try again", Toast.LENGTH_SHORT).show()
        return
    }
    isCapturing = true
    viewModel.startDetection(bitmap)
}
```

### WR-06: ConfirmationViewModel.saveAll() silently swallows exceptions

**File:** `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt:104-105`
**Issue:** The catch block in `saveAll()` has an empty body with only a comment. If `saveDetectedFoodsUseCase()` throws (e.g., Room disk I/O error), the user gets no feedback — `_isSaving` becomes `false` but `_saveResult` stays `null`, leaving the UI stuck on the confirmation screen with no error indication.

**Fix:**
```kotlin
} catch (e: Exception) {
    _saveResult.value = SaveDetectedFoodsUseCase.SaveResult(
        savedCount = 0, skippedCount = 0, sessionId = sessionId
    )
    // Or add a dedicated error state:
    // _saveError.value = e.message
}
```

## Info

### IN-01: Redundant conditional branch in DetectionResultAdapter

**File:** `app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt:57-63`
**Issue:** The `if (isFailed) { showEditDialog(entity) } else { showEditDialog(entity) }` is identical in both branches — the condition has no effect.

**Fix:** Remove the conditional:
```kotlin
binding.btnEdit.setOnClickListener {
    showEditDialog(entity)
}
```

### IN-02: TODO comments in MnnYoloEngine for JNI stubs

**File:** `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt:75,118-119`
**Issue:** Two TODO comments mark the JNI bridge as stub. These are expected and documented, but should be tracked for completion.

**Fix:** No action needed — these are intentional stubs waiting for native code. Ensure they are tracked in the project backlog.

### IN-03: MIGRATION_10_11 not registered in Hilt database builder

**File:** `app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt:161-182`
**Issue:** The `MIGRATION_10_11` is defined but may not be added to the Room database builder's `.addMigrations()` call. The summary mentions "ready to be registered in the Hilt database builder." This needs verification in the DatabaseModule DI provider.

**Fix:** Verify that the DatabaseModule (or equivalent Hilt provider) includes `MIGRATION_10_11` in its migration list. Example:
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "food_expiry_db")
    .addMigrations(
        MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
        MIGRATION_10_11
    )
    .build()
```

### IN-04: Hardcoded color strings in DetectionResultAdapter

**File:** `app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt:81,89,91,93,97,110,114,117`
**Issue:** Multiple `Color.parseColor()` calls with hardcoded hex values (#212121, #2E7D32, #F57F17, #C62828, #9E9E9E, #E65100, #FFF3E0). These should be color resources for theme consistency and maintainability.

**Fix:** Extract to `colors.xml` resources:
```kotlin
ContextCompat.getColor(binding.root.context, R.color.confidence_high)
```

### IN-05: BatchDetectionResult computed properties may be stale

**File:** `app/src/main/java/com/example/foodexpiryapp/domain/model/BatchDetectionResult.kt:18-20`
**Issue:** `totalCount`, `classifiedCount`, and `failedCount` are computed at construction time from `results`. In `DetectionPipeline.kt:138-145`, these are explicitly passed, overriding the default computed values. This means the `data class` default computations are dead code when constructed via `DetectionPipeline`. Not a bug, but slightly misleading API design.

**Fix:** Consider making these purely computed properties without constructor parameters:
```kotlin
data class BatchDetectionResult(
    val sessionId: String,
    val results: List<DetectionResult>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalCount: Int get() = results.size
    val classifiedCount: Int get() = results.count { it.status == DetectionStatus.CLASSIFIED }
    val failedCount: Int get() = results.count { it.status == DetectionStatus.FAILED }
}
```

---

_Reviewed: 2026-04-09T12:00:00Z_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
