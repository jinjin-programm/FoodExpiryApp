---
phase: "05-engine"
plan: "05"
subsystem: "repository-wiring"
tags: ["repository", "usecase", "di", "hilt", "clean-architecture", "mnn"]
dependency_graph:
  requires: ["05-01 (LlmInferenceRepository interface)", "05-03 (ModelDownloadManager)", "05-04 (MnnLlmEngine)"]
  provides: ["LlmInferenceRepositoryImpl", "IdentifyFoodUseCase", "InferenceModule"]
  affects: ["06-01 (YOLO+LLM pipeline)", "07-01 (scan UI)", "presentation layer"]
tech_stack:
  added: []
  patterns: ["Mutex for concurrent inference protection", "StateFlow mapping from download to model state", "SupervisorJob scope for safe state observation"]
key_files:
  created:
    - "app/src/main/java/com/example/foodexpiryapp/data/repository/LlmInferenceRepositoryImpl.kt"
    - "app/src/main/java/com/example/foodexpiryapp/domain/usecase/IdentifyFoodUseCase.kt"
    - "app/src/main/java/com/example/foodexpiryapp/di/InferenceModule.kt"
  modified: []
decisions:
  - "Used CoroutineScope(SupervisorJob) instead of GlobalScope for download state observation — child failures won't cancel the scope"
  - "Mapped DownloadUiState.Paused to ModelState.NotDownloaded since ModelState has no Paused variant"
  - "Mapped DownloadUiState.Complete to ModelState.Ready since ModelState has no Loaded variant"
  - "Emit ModelState.Loading during engine.loadModel() for real-time UI feedback"
  - "Created separate InferenceModule rather than adding to RepositoryModule — inference is a distinct subsystem"
metrics:
  duration_seconds: 61
  completed_date: "2026-04-09"
  tasks: 2
  files: 3
---

# Phase 05 Plan 05: Repository Wiring Summary

**One-liner:** LlmInferenceRepositoryImpl orchestrating ModelDownloadManager + MnnLlmEngine behind the domain interface, IdentifyFoodUseCase for ViewModel consumption, and InferenceModule Hilt binding — completing the D-01/D-02/D-03 architecture.

## What Was Built

Three files completing the data→domain wiring for MNN inference:

1. **LlmInferenceRepositoryImpl.kt** — `@Singleton` implementing the domain `LlmInferenceRepository` interface. Orchestrates the full inference flow: download readiness check → lazy model loading → MNN inference → error handling. Maps `DownloadUiState` from `ModelDownloadManager` to domain `ModelState` via a `MutableStateFlow` observed on a `SupervisorJob` coroutine scope. Uses `Mutex` to prevent concurrent inference (T-05-15 DoS mitigation).

2. **IdentifyFoodUseCase.kt** — Thin `@Inject` UseCase wrapping `LlmInferenceRepository.inferFood(bitmap)`. Follows existing UseCase pattern (`operator fun invoke`). ViewModels call this, never the raw engine (D-01).

3. **InferenceModule.kt** — Hilt `@Module` with `@Binds @Singleton` binding `LlmInferenceRepositoryImpl` to `LlmInferenceRepository`. Separate from `RepositoryModule` since inference is a distinct subsystem. `MnnLlmEngine` and `ModelDownloadManager` are auto-provided by Hilt as `@Singleton @Inject` classes.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| CoroutineScope(SupervisorJob) instead of GlobalScope | GlobalScope is a code smell — SupervisorJob ensures child failures don't cancel the scope or sibling coroutines |
| DownloadUiState.Paused → ModelState.NotDownloaded | ModelState sealed class has no Paused variant — NotDownloaded is the closest semantic match |
| DownloadUiState.Complete → ModelState.Ready | ModelState has no Loaded variant — Complete means files are ready, mapping to Ready is correct |
| Emit ModelState.Loading during loadModel() | Gives UI real-time feedback that model is being loaded into memory |
| Separate InferenceModule | Inference is a distinct subsystem from food data repos — keeps DI organized |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed ModelState mapping — no Loaded/Paused variants exist**
- **Found during:** Task 1 (LlmInferenceRepositoryImpl init block)
- **Issue:** Plan code mapped `DownloadUiState.Complete → ModelState.Loaded` and `DownloadUiState.Paused → ModelState.Paused`, but `ModelState` sealed class only has: `NotDownloaded`, `Downloading`, `Verifying`, `WifiCheckRequired`, `Loading`, `Ready`, `Error`
- **Fix:** Mapped `Complete → Ready` and `Paused → NotDownloaded`
- **Files modified:** LlmInferenceRepositoryImpl.kt
- **Commit:** `61dac91`

**2. [Rule 2 - Missing Critical] Replaced GlobalScope with CoroutineScope(SupervisorJob)**
- **Found during:** Task 1 (download state observation in init block)
- **Issue:** Plan code used `GlobalScope.launch(Dispatchers.IO)` which is a code smell and can lead to leaked coroutines
- **Fix:** Used `CoroutineScope(SupervisorJob() + Dispatchers.IO)` as a class-level property. SupervisorJob ensures child failures don't cancel sibling coroutines
- **Files modified:** LlmInferenceRepositoryImpl.kt
- **Commit:** `61dac91`

**3. [Rule 2 - Missing Critical] Fixed field declaration order**
- **Found during:** Task 1 (class structure)
- **Issue:** Plan code declared `modelState` override before `_modelState` backing field and referenced `_modelState` in the `init` block before its declaration
- **Fix:** Declared `_modelState` before `modelState` override, ensuring correct initialization order
- **Files modified:** LlmInferenceRepositoryImpl.kt
- **Commit:** `61dac91`

**4. [Rule 1 - Bug] Added ModelState.Loading emission during model load**
- **Found during:** Task 1 (inferFood implementation)
- **Issue:** Plan didn't emit any state change when loading the model, leaving UI unaware of the loading phase
- **Fix:** Added `_modelState.value = ModelState.Loading` before `engine.loadModel()` and `_modelState.value = ModelState.Ready` after success
- **Files modified:** LlmInferenceRepositoryImpl.kt
- **Commit:** `61dac91`

---

**Total deviations:** 4 auto-fixed (2 bugs, 2 missing critical)
**Impact on plan:** All fixes necessary for correctness against actual sealed class definitions and Kotlin best practices. No scope creep.

## Verification Checklist

- [x] LlmInferenceRepositoryImpl.kt exists at data/repository/
- [x] Contains `class LlmInferenceRepositoryImpl` with `@Singleton` and `@Inject`
- [x] Implements `LlmInferenceRepository` interface
- [x] Has `override fun inferFood(bitmap: Bitmap): Flow<FoodIdentification>`
- [x] Has `override val modelState: StateFlow<ModelState>`
- [x] Uses `engine.loadModel()` and `engine.runInference(bitmap)`
- [x] Uses `downloadManager.isModelReady()` for download check
- [x] Has `inferenceMutex` for thread safety (T-05-15)
- [x] Package is `com.example.foodexpiryapp.data.repository`
- [x] IdentifyFoodUseCase.kt exists at domain/usecase/
- [x] Contains `class IdentifyFoodUseCase` with `@Inject` constructor
- [x] Has `operator fun invoke(bitmap: Bitmap): Flow<FoodIdentification>`
- [x] InferenceModule.kt exists at di/
- [x] Contains `@Module @InstallIn(SingletonComponent::class) abstract class InferenceModule`
- [x] Has `@Binds @Singleton abstract fun bindLlmInferenceRepository`
- [x] D-01 satisfied (UseCase/Repository pattern)
- [x] D-02 satisfied (single inferFood method, status via StateFlow)
- [x] D-03 satisfied (interface in domain/, impl in data/)

## Known Stubs

None — all three files are complete implementations with no placeholder values.

## Threat Surface

No new threat surface introduced beyond what the plan's threat model anticipated. The Mutex for concurrent inference protection (T-05-15) is implemented. Input validation in engine (T-05-14) was handled in Plan 04.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | `61dac91` | feat(05-05): create LlmInferenceRepositoryImpl |
| 2 | `d1f16ac` | feat(05-05): create IdentifyFoodUseCase and InferenceModule |

## Self-Check: PASSED

- ✅ LlmInferenceRepositoryImpl.kt exists
- ✅ IdentifyFoodUseCase.kt exists
- ✅ InferenceModule.kt exists
- ✅ Commit 61dac91 verified
- ✅ Commit d1f16ac verified

---

*Phase: 05-engine*
*Completed: 2026-04-09*
