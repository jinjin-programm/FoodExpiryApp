---
phase: 06-detection
verified: 2026-04-09T00:30:00Z
status: gaps_found
score: 14/16 must-haves verified
overrides_applied: 0
gaps:
  - truth: "Processing caps at 5-8 items per scan and recycles bitmaps immediately — no memory issues on 6GB devices"
    status: failed
    reason: "MIGRATION_10_11 is defined in AppDatabase but NOT registered in DatabaseModule.addMigrations(). Room will crash at runtime when upgrading from v10→v11, preventing any detection results from being persisted or displayed. DetectionResultDao is also not provided by DatabaseModule."
    artifacts:
      - path: "app/src/main/java/com/example/foodexpiryapp/di/DatabaseModule.kt"
        issue: "addMigrations() only goes up to MIGRATION_9_10 — missing MIGRATION_10_11. Also missing DetectionResultDao provider."
      - path: "app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt"
        issue: "MIGRATION_10_11 is correctly defined but never used"
    missing:
      - "Add AppDatabase.MIGRATION_10_11 to DatabaseModule.addMigrations() call"
      - "Add DetectionResultDao provider method in DatabaseModule"
  - truth: "YOLO detects multiple food items in a single camera frame and returns bounding boxes"
    status: partial
    reason: "MnnYoloEngine.detect() returns empty list (stub) — actual MNN native inference not implemented. YOLO JNI stubs return floatArrayOf(). This is explicitly documented as intentional (same pattern as MnnLlmEngine) but means no real detection will occur until native bridge is implemented."
    artifacts:
      - path: "app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt"
        issue: "detect() returns empty list (val outputArray = floatArrayOf()) — JNI stub mode"
    missing:
      - "MNN YOLO JNI native bridge implementation (deferred pending MNN YOLO model availability)"
---

# Phase 6: Detection Verification Report

**Phase Goal:** Users can scan a photo with multiple food items and get all of them identified and listed for confirmation
**Verified:** 2026-04-09T00:30:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | YOLO detects multiple food items in a single camera frame and returns bounding boxes | ⚠️ PARTIAL | MnnYoloEngine exists with detect() but returns empty list (JNI stub). Full pipeline wiring is correct. |
| 2 | Each detected region is cropped and classified by the local LLM sequentially | ✓ VERIFIED | DetectionPipeline.kt:104-135 — sequential for-loop with llmEngine.runInference() per crop |
| 3 | LLM returns structured food data (name, name_zh) for each cropped region | ✓ VERIFIED | MnnLlmEngine.runInference() returns FoodIdentification(name, nameZh). DetectionPipeline.kt:114 captures result |
| 4 | Processing caps at 8 items per scan and recycles bitmaps immediately | ✗ FAILED | Cap is 8 (MnnYoloConfig.maxDetections=8), recycling works (line 132). BUT MIGRATION_10_11 not registered in DatabaseModule — will crash at runtime |
| 5 | Detection pipeline emits progress state (Detecting → Classifying → Complete) | ✓ VERIFIED | DetectionPipeline.kt:64,110,146 emits PipelineState.Detecting, .Classifying, .Complete via channelFlow |
| 6 | User can switch to a new YOLO scan tab via horizontal swipe | ✓ VERIFIED | ScanContainerFragment with ViewPager2, ScanPagerAdapter with 3 tabs, default TAB_YOLO |
| 7 | Bounding boxes are drawn around detected food items on camera preview | ✓ VERIFIED | DetectionOverlayView in fragment_yolo_scan.xml, wired to pipeline results |
| 8 | Staged progress screen shows after capture | ✓ VERIFIED | YoloScanFragment observeUiState() shows "Detecting food items..." → "Identifying item (n/total)..." → auto-navigate |
| 9 | Detection results displayed as a list for user confirmation before saving | ✓ VERIFIED | ConfirmationFragment with RecyclerView, DetectionResultAdapter, reads from Room via Flow |
| 10 | User can review, edit name, and delete items before confirming | ✓ VERIFIED | DetectionResultAdapter: edit via AlertDialog, delete via viewModel.removeItem() |
| 11 | 0 items detected shows helpful message with retry option | ✓ VERIFIED | YoloScanFragment:159-162 — NoDetection state → "No food items detected" with retry button |
| 12 | User taps 'Add All to Fridge' and items are saved with smart defaults | ✓ VERIFIED | SaveDetectedFoodsUseCase iterates entities → DefaultAttributeEngine.inferDefaults() → foodRepository.insertFoodItem() |
| 13 | After save, user returns to YOLO scan with camera preview resumed | ✓ VERIFIED | ConfirmationFragment sends FragmentResult, popBackStack to YoloScanFragment |
| 14 | Snackbar shows with 'View' action navigating to inventory | ✓ VERIFIED | YoloScanFragment.setupSaveResultListener() — Snackbar with "View" action + highlight_new_items |
| 15 | Quick Mode auto-confirms single items after 3-second countdown | ✓ VERIFIED | ConfirmationViewModel.startQuickModeCountdownIfNeeded() — 3s countdown, cancelable |
| 16 | DefaultAttributeEngine maps food names (EN+ZH) to category and shelf life | ✓ VERIFIED | DefaultAttributeEngine with 40+ keyword entries, longest-match-wins, fallback OTHER/7days |

**Score:** 14/16 truths verified (1 partial, 1 failed)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `inference/yolo/MnnYoloEngine.kt` | YOLO inference engine via MNN | ✓ VERIFIED (stub) | 180 lines, @Singleton, loadModel/detect/unloadModel, lifecycle managed |
| `inference/pipeline/DetectionPipeline.kt` | Batch detection pipeline orchestrator | ✓ VERIFIED | 158 lines, channelFlow, sequential lifecycle, bitmap recycling |
| `domain/repository/YoloDetectionRepository.kt` | Domain interface for batch detection | ✓ VERIFIED | 47 lines, detectFoods(bitmap): Flow<PipelineState> |
| `data/local/database/DetectionResultEntity.kt` | Room entity for detection results | ✓ VERIFIED | 56 lines, @Entity with sessionId, boundingBox, status fields |
| `data/local/dao/DetectionResultDao.kt` | DAO for detection result CRUD | ✓ VERIFIED | 35 lines, 6 operations (insert, query Flow, query sync, delete, delete old, update) |
| `data/local/database/AppDatabase.kt` | Updated database with new entity | ✓ VERIFIED | v11, DetectionResultEntity in entities, MIGRATION_10_11 defined |
| `data/repository/DetectionResultRepository.kt` | Repository for detection results | ✓ VERIFIED | 68 lines, insertResults, getResults, clearSession, cleanupOldResults |
| `di/DetectionModule.kt` | DI wiring | ✓ VERIFIED | @Binds repository, @Provides MnnYoloConfig + DefaultAttributeEngine |
| `di/DatabaseModule.kt` | Database provider with migrations | ✗ STUB | Missing MIGRATION_10_11 in addMigrations(). Missing DetectionResultDao provider |
| `presentation/ui/scan/ScanContainerFragment.kt` | ViewPager2 container | ✓ VERIFIED | 42 lines, ViewPager2 with ScanPagerAdapter |
| `presentation/ui/scan/ScanPagerAdapter.kt` | FragmentStateAdapter | ✓ VERIFIED | 34 lines, 3 tabs: VisionScanFragment, ScanFragment, YoloScanFragment |
| `presentation/ui/yolo/YoloScanFragment.kt` | YOLO scan tab | ✓ VERIFIED | 359 lines, camera, capture, progress overlay, save result listener, ViewPager2 lifecycle |
| `presentation/viewmodel/YoloScanViewModel.kt` | Scan VM with pipeline | ✓ VERIFIED | 125 lines, startDetection, cancelDetection, saveResults to Room |
| `presentation/ui/detection/ConfirmationFragment.kt` | Batch confirmation screen | ✓ VERIFIED | 190 lines, RecyclerView, Quick Mode, save flow, FragmentResult |
| `presentation/viewmodel/ConfirmationViewModel.kt` | Confirmation VM | ✓ VERIFIED | 157 lines, SavedStateHandle sessionId, results from Room, saveAll, Quick Mode |
| `presentation/adapter/DetectionResultAdapter.kt` | RecyclerView adapter | ✓ VERIFIED | 163 lines, DiffUtil, failed item styling, edit dialog |
| `domain/engine/DefaultAttributeEngine.kt` | Food attribute inference | ✓ VERIFIED | 142 lines, 40+ keywords EN+ZH, longest-match-wins |
| `domain/usecase/SaveDetectedFoodsUseCase.kt` | Batch save use case | ✓ VERIFIED | 75 lines, iterate → inferDefaults → insertFoodItem → clearSession |
| `res/layout/fragment_scan_container.xml` | ViewPager2 layout | ✓ VERIFIED | 14 lines, ViewPager2 fills parent |
| `res/layout/fragment_confirmation.xml` | Confirmation screen layout | ✓ VERIFIED | 100 lines, top bar, RecyclerView, bottom bar with Cancel + Add All |
| `res/layout/item_detection_result.xml` | Detection result row | ✓ VERIFIED | 96 lines, thumbnail, name, confidence badge, edit/delete buttons |
| `res/layout/fragment_yolo_scan.xml` | YOLO scan layout | ✓ VERIFIED | 278 lines, PreviewView, DetectionOverlayView, progress overlay, capture button |
| `res/navigation/nav_graph.xml` | Navigation graph | ✓ VERIFIED | navigation_scan_container, navigation_confirmation with sessionId arg, action |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| DetectionPipeline | MnnYoloEngine | detect() for YOLO inference | ✓ WIRED | yoloEngine.detect(bitmap) at line 74 |
| DetectionPipeline | MnnLlmEngine | runInference for each crop | ✓ WIRED | llmEngine.runInference(detection.cropBitmap) at line 114 |
| DetectionPipeline | ModelLifecycleManager | acquire/release for mutual exclusion | ✓ WIRED | Via yoloEngine/llmEngine internally, sequential unload/load |
| YoloScanFragment | YoloScanViewModel | camera capture triggers pipeline | ✓ WIRED | viewModel.startDetection(bitmap) at line 281 |
| YoloScanViewModel | YoloDetectionRepository | detectFoods call | ✓ WIRED | yoloDetectionRepository.detectFoods(bitmap) at line 55 |
| YoloScanViewModel | DetectionResultRepository | save results to Room | ✓ WIRED | detectionResultRepository.insertResults(entities) at line 118 |
| ConfirmationFragment | DetectionResultRepository | Room DB read for results | ✓ WIRED | Via ConfirmationViewModel init → detectionResultRepository.getResults(sessionId) |
| ConfirmationViewModel | SaveDetectedFoodsUseCase | saveAll button triggers save | ✓ WIRED | saveDetectedFoodsUseCase(sessionId) at line 102 |
| SaveDetectedFoodsUseCase | DefaultAttributeEngine | category/shelf life inference | ✓ WIRED | attributeEngine.inferDefaults(entity.foodName, entity.foodNameZh) at line 52 |
| SaveDetectedFoodsUseCase | FoodRepository | batch insert to Room | ✓ WIRED | foodRepository.insertFoodItem(foodItem) at line 66 |
| DetectionModule | YoloDetectionRepositoryImpl | Hilt @Binds | ✓ WIRED | bindYoloDetectionRepository at line 33 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| DetectionPipeline | `detections` | yoloEngine.detect() | No — stub returns empty | ⚠️ STATIC (expected — JNI stub) |
| DetectionPipeline | `results` | llmEngine.runInference(cropBitmap) | Yes — uses real MnnLlmEngine | ✓ FLOWING |
| YoloScanViewModel | `_uiState` | PipelineState from repository | No — pipeline returns empty detections | ⚠️ STATIC (upstream stub) |
| ConfirmationViewModel | `_results` | detectionResultRepository.getResults() | Yes — reads from Room | ✓ FLOWING |
| SaveDetectedFoodsUseCase | `foodItem` | DefaultAttributeEngine + entity data | Yes — creates real FoodItem with defaults | ✓ FLOWING |

### Behavioral Spot-Checks

Step 7b: SKIPPED (Android project — no runnable entry points without device/emulator)

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| YOLO-01 | 06-01, 06-02, 06-04 | YOLO+LLM batch pipeline: detect → crop → classify → aggregate | ⚠️ PARTIAL | Pipeline fully wired but YOLO native stub returns empty |
| YOLO-02 | 06-03 | New YOLO scan tab added alongside existing tabs | ✓ SATISFIED | ScanContainerFragment + ScanPagerAdapter with 3 tabs |
| YOLO-03 | 06-03 | YOLO detects multiple food items and draws bounding boxes | ⚠️ PARTIAL | DetectionOverlayView exists; MnnYoloEngine stub returns empty |
| YOLO-04 | 06-01 | Detected regions cropped and sent sequentially to local LLM | ✓ SATISFIED | DetectionPipeline sequential for-loop with llmEngine.runInference() |
| YOLO-05 | 06-01 | LLM returns structured food data (name, name_zh) for each crop | ✓ SATISFIED | MnnLlmEngine.runInference() returns FoodIdentification(name, nameZh) |
| YOLO-06 | 06-02, 06-03, 06-04 | Detection results displayed as a list for user confirmation | ✗ BLOCKED | UI complete but runtime crash from missing migration |
| YOLO-07 | 06-01 | Maximum 5-8 items processed per scan | ✓ SATISFIED | MnnYoloConfig.maxDetections=8, nmsResults.take(config.maxDetections) |
| YOLO-08 | 06-01 | Bitmaps recycled immediately after each classification step | ✓ SATISFIED | DetectionPipeline line 132: detection.cropBitmap?.recycle() |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| MnnYoloEngine.kt | 75,118,146 | TODO: JNI native bridge | ℹ️ Info | Expected — same pattern as MnnLlmEngine, deferred to native implementation |
| MnnYoloEngine.kt | 120 | `val outputArray = floatArrayOf()` — empty stub return | ⚠️ Warning | YOLO detection returns no results until native bridge implemented |
| DatabaseModule.kt | 39-49 | Missing MIGRATION_10_11 in addMigrations() | 🛑 Blocker | Runtime crash on DB upgrade from v10→v11 |

### Human Verification Required

### 1. YOLO Scan Camera Preview with Bounding Boxes

**Test:** Open YOLO scan tab, point camera at multiple food items, observe bounding boxes drawn on preview
**Expected:** Bounding boxes appear around detected food items
**Why human:** Visual rendering on camera preview requires device/emulator; YOLO stub returns empty so bounding boxes won't appear until native bridge is ready

### 2. Batch Confirmation List UI

**Test:** Complete a YOLO scan detection (with mock data), verify the confirmation screen shows items with edit/delete capabilities
**Expected:** Items displayed with confidence badges, edit dialog works, delete removes item, failed items show orange background
**Why human:** UI behavior, layout quality, touch interaction, dialog display

### 3. Save Flow End-to-End

**Test:** Tap "Add All to Fridge" on confirmation screen, verify items saved to inventory with correct category and expiry dates
**Expected:** Items appear in inventory with DefaultAttributeEngine-inferred category and shelf life
**Why human:** End-to-end flow requires running app, correct Room persistence, Snackbar appearance

### 4. ViewPager2 Swipe Between Scan Modes

**Test:** Swipe horizontally between Photo Scan, Barcode Scan, and YOLO Scan tabs
**Expected:** Smooth transitions between scan modes, camera continues working on each tab
**Why human:** Touch interaction, camera lifecycle across fragment switches

### Gaps Summary

**1. Database Migration Not Registered (Blocker)**

`AppDatabase.MIGRATION_10_11` is correctly defined in `AppDatabase.kt` (creating the `detection_results` table) and the database version is bumped to 11. However, `DatabaseModule.kt` line 39-49 only registers migrations up to `MIGRATION_9_10`. The `MIGRATION_10_11` is **not included** in the `addMigrations()` call. This will cause a runtime `IllegalStateException` when Room attempts to open the database on any device that was previously on schema version 10.

Additionally, `DatabaseModule` does not provide `DetectionResultDao` — while Hilt can auto-resolve it through `AppDatabase.detectionResultDao()`, explicit provision follows the established pattern in this codebase.

**Fix:** Add `AppDatabase.MIGRATION_10_11` to the `addMigrations()` call and add a `provideDetectionResultDao()` method.

**2. YOLO Native Inference Stub (Expected)**

`MnnYoloEngine.detect()` returns an empty `FloatArray` as a JNI stub. This is explicitly documented as intentional (same pattern as `MnnLlmEngine`) and will be implemented when the MNN YOLO model and native bridge are ready. The pipeline wiring is fully correct — when native inference is implemented, the full flow will work end-to-end.

---

_Verified: 2026-04-09T00:30:00Z_
_Verifier: the agent (gsd-verifier)_
